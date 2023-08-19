package com.winjeg.spring.sharding.utils;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.Duplication;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.TypeCreation;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.ibatis.annotations.Param;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import static java.lang.reflect.Modifier.ABSTRACT;
import static java.lang.reflect.Modifier.PUBLIC;

/**
 * 主要目的的根据一个 interface 创建出一个全新的Interface
 * 新interface的方法前面都加一个 tableName的参数
 *
 * @author winjeg
 */
@Slf4j
public class ClassScanUtils {

    /**
     * 根据一个接口，生成一个全新的接口，视情况给每个方法加参数
     *
     * @param clz             原接口
     * @param isShardingTable 是否是分表， 如果分表，会自动加 tableName 参数
     * @return 返回新的接口类，并且加载到jvm中
     */
    public static Class<?> generateMapperViaMapper(Class<?> clz, String dsName, boolean isShardingTable) {
        String name = NameUtils.buildClassName(dsName, clz.getCanonicalName());
        return ClassScanUtils.genInterfaceViaInterface(clz, name, isShardingTable);
    }

    private static Class<?> genInterfaceViaInterface(Class<?> clz, String name, boolean isShardingTable) {
        if (clz == null || !clz.isInterface()) {
            return null;
        }
        Annotation[] clzAnnos = clz.getAnnotations();
        Method[] methods = clz.getMethods();
        Field[] fields = clz.getDeclaredFields();
        DynamicType.Builder<?> builder = new ByteBuddy()
                .makeInterface()
                .name(name);
        // 生成原注解
        for (val anno : clzAnnos) {
            builder = builder.annotateType(anno);
        }
        // 生成成员
        for (val f : fields) {
            try {
                builder = genFields(builder, f);
            } catch (Exception e) {
                log.warn("genInterfaceViaInterface - gen field failed:{}", f.getName());
            }
        }
        // 生成方法
        for (val m : methods) {
            builder = genBuilderMethod(builder, m, isShardingTable);
        }

        // 加载并返回class
        val unloaded = builder.make();
        saveClass2Target(unloaded);
        // 必须要使用 INJECTION 这种方法，否则class的可见性将会是问题， mybatis会使用 Class.forName 来读，读不到
        return unloaded.load(clz.getClassLoader(), ClassLoadingStrategy.Default.INJECTION).getLoaded();
    }

    private static DynamicType.Builder<?> genFields(DynamicType.Builder<?> builder, Field f) throws IllegalAccessException {
        if (f.getType().equals(Integer.TYPE)) {
            builder = builder.defineField(f.getName(), f.getType(), f.getModifiers()).value((int) f.get(null));
        } else if (f.getType() == char.class) {
            builder = builder.defineField(f.getName(), f.getType(), f.getModifiers()).value((char) f.get(null));
        } else if (f.getType() == short.class) {
            builder = builder.defineField(f.getName(), f.getType(), f.getModifiers()).value((short) f.get(null));
        } else if (f.getType() == byte.class) {
            builder = builder.defineField(f.getName(), f.getType(), f.getModifiers()).value((byte) f.get(null));
        } else if (f.getType().equals(Long.TYPE)) {
            builder = builder.defineField(f.getName(), f.getType(), f.getModifiers()).value((long) f.get(null));
        } else if (f.getType().equals(Float.TYPE)) {
            builder = builder.defineField(f.getName(), f.getType(), f.getModifiers()).value((float) f.get(null));
        } else if (f.getType().equals(Double.TYPE)) {
            builder = builder.defineField(f.getName(), f.getType(), f.getModifiers()).value((double) f.get(null));
        } else if (f.getType().equals(String.class)) {
            builder = builder.defineField(f.getName(), f.getType(), f.getModifiers()).value(f.get(null).toString());
        } else {
            // 如果不是primitive，那默认拿无参构造函数
            val initializer = new ByteCodeAppender() {
                @Override
                public Size apply(MethodVisitor mv, Implementation.Context ctx, MethodDescription md) {
                    StackManipulation.Size size = null;
                    try {
                        size = new StackManipulation.Compound(
                                TypeCreation.of(new TypeDescription.ForLoadedType(f.getType())),
                                Duplication.SINGLE,
                                MethodInvocation.invoke(new TypeDescription.ForLoadedType(f.getType()).getDeclaredMethods().filter(ElementMatchers.isDefaultConstructor()).getOnly()),
                                FieldAccess.forField(ctx.getInstrumentedType().getDeclaredFields().filter(ElementMatchers.named(f.getName())).getOnly()).write()
                        ).apply(mv, ctx);
                    } catch (Throwable t) {
                        log.warn("field:{} is not defined precisely", f.getName());
                    }
                    if (size != null) {
                        return new Size(size.getMaximalSize(), md.getStackSize());
                    }
                    return new Size(0, 0);
                }
            };
            builder = builder.initializer(initializer);
            builder = builder.define(f);
        }
        return builder;
    }


    private static DynamicType.Builder<?> genBuilderMethod(DynamicType.Builder<?> builder, Method m, boolean isShardingTable) {
        val params = m.getParameters();

        Type[] types = isShardingTable ? new Type[m.getParameterCount() + 1] : new Type[m.getParameterCount()];
        if (isShardingTable) {
            types[0] = String.class;
        }
        int i = 0;
        for (val p : params) {
            int idx = isShardingTable ? i + 1 : i;
            types[idx] = p.getType();
            i++;
        }
        DynamicType.Builder.MethodDefinition<?> methodBuilder = builder
                .defineMethod(m.getName(), m.getReturnType(), PUBLIC + ABSTRACT)
                .withParameters(types)
                .withoutCode();
        if (isShardingTable) {
            methodBuilder = methodBuilder.annotateParameter(0, AnnotationDescription.Builder.ofType(Param.class)
                    .define("value", "tableName").build());
        }
        int j = isShardingTable ? 1 : 0;
        for (val p : params) {
            methodBuilder = methodBuilder.annotateParameter(j, p.getAnnotations());
            j++;
        }
        return methodBuilder;
    }

    /**
     * 保存一下生成的类， 用于核验是否生成有误
     *
     * @param unloaded 未加载之前的类
     */
    private static void saveClass2Target(DynamicType.Unloaded<?> unloaded) {
        String path = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        path = path.substring(0, path.substring(0, path.length() - 1).lastIndexOf("/"));
        try {
            unloaded.saveIn(new File(path));
        } catch (IOException ignored) {
        }
    }
}
