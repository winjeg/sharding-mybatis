package com.winjeg.spring.sharding.utils;

import com.winjeg.spring.sharding.annos.Sharding;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.SystemPropertyUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 扫描类和注解的工具包
 * @author winjeg
 */
@Slf4j
public class ResourceUtil {

    private static final String[] SYSTEM_PATH = new String[]{"sun.", "java.", "javax.", "javafx.", "jdk.", "oracle.",
            "com.sun.", "com.oracle.", "netscape."};

    private static final Map<Class<?>, Sharding> SHARDING_MAP = new HashMap<>();


    /**
     * to judge if a class is a system class or not
     *
     * @param path class name full path
     * @return is jdk class or not
     */
    private static boolean isSystemClass(String path) {
        for (val p : SYSTEM_PATH) {
            if (path.startsWith(p)) {
                return true;
            }
        }
        return false;
    }


    public static List<Class<?>> findClasses(String basePackage) {
        ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resourcePatternResolver);

        List<Class<?>> candidates = new ArrayList<>();
        String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                resolveBasePackage(basePackage) + "/" + "**/*.class";

        try {
            Resource[] resources = resourcePatternResolver.getResources(packageSearchPath);
            for (Resource resource : resources) {
                if (resource.isReadable()) {
                    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
                    val className = metadataReader.getClassMetadata().getClassName();
                    if (className.length() == 0 || className.contains("$") || isSystemClass(className)) {
                        continue;
                    }
                    candidates.add(Class.forName(className));
                }
            }
        } catch (IOException | ClassNotFoundException e) {

        }
        return candidates;
    }

    public static Pair<List<Class<?>>, List<Class<?>>> getClassesWithAnno(String[] packages) {
        List<Class<?>> shardingClasses = new ArrayList<>();
        List<Class<?>> nonShardingClasses = new ArrayList<>();
        for (val pkg : packages) {
            val cs = findClasses(pkg);
            for (val clz : cs) {
                for (val a : clz.getAnnotations()) {
                    if (a instanceof Sharding) {
                        if (((Sharding) a).dbRule().length() == 0 && ((Sharding) a).tableRule().length() == 0) {
                            nonShardingClasses.add(clz);
                        } else {
                            shardingClasses.add(clz);
                        }
                        break;
                    }
                }
            }
        }
        return new Pair<>(nonShardingClasses, shardingClasses);
    }

    private static String resolveBasePackage(String basePackage) {
        return ClassUtils.convertClassNameToResourcePath(SystemPropertyUtils.resolvePlaceholders(basePackage));
    }

    public static Sharding getShardingAnno(Class<?> clz) {
        if (clz == null) {
            return null;
        }
        if (SHARDING_MAP.containsKey(clz)) {
            return SHARDING_MAP.get(clz);
        }
        for (val a : clz.getAnnotations()) {
            if (a instanceof Sharding) {
                SHARDING_MAP.putIfAbsent(clz, (Sharding) a);
                return (Sharding) a;
            }
        }
        return null;
    }

}
