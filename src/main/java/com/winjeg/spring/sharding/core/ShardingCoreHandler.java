package com.winjeg.spring.sharding.core;

import com.winjeg.spring.sharding.annos.Sharding;
import com.winjeg.spring.sharding.annos.ShardingKey;
import com.winjeg.spring.sharding.factories.ClassManager;
import com.winjeg.spring.sharding.utils.ExpressionUtil;
import com.winjeg.spring.sharding.utils.NameUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 核心逻辑, 动态代理调用的时候实际做分库分表逻辑的地方
 *
 * @author winjeg
 */
@Slf4j
public class ShardingCoreHandler implements InvocationHandler {
    private final SqlSessionFactoryManager sessionFactoryManager;
    private final Sharding sharding;
    private final Class<?> clz;
    private final ClassManager classManager;

    public ShardingCoreHandler(Sharding s, SqlSessionFactoryManager sm, Class<?> clz, ClassManager classManager) {
        this.sessionFactoryManager = sm;
        this.sharding = s;
        this.clz = clz;
        this.classManager = classManager;
    }

    /**
     * 从参数列表中获取 ShardingKey
     *
     * @param method 被调用方法
     * @param args   参数别聊
     * @return 分表键值
     * @throws IllegalAccessException 非法访问字段
     */
    private static long getShardingKeyFromArgs(Method method, Object[] args) throws IllegalAccessException {
        val params = method.getParameters();
        int i = 0;
        Object shardingKey = null;
        for (val p : params) {
            val methodAnnos = p.getAnnotations();
            // 先从这个参数本身拿注解
            for (val a : methodAnnos) {
                if (a instanceof ShardingKey) {
                    shardingKey = args[i];
                    break;
                }
            }
            if (shardingKey != null) {
                break;
            }
            // 如果参数自身没有， 从参数的成员拿注解
            val fields = p.getType().getDeclaredFields();
            int j = 0;
            for (val f : fields) {
                val fieldAnnos = f.getAnnotations();
                for (val a : fieldAnnos) {
                    if (a instanceof ShardingKey) {
                        f.setAccessible(true);
                        shardingKey = f.get(args[i]);
                        break;
                    }
                }
                if (shardingKey != null) {
                    break;
                }
                j++;
            }
            i++;
        }
        if (shardingKey == null) {
            throw new RuntimeException("sharding key should not be null");
        }
        return (Long) shardingKey;
    }

    /**
     * 核心调用逻辑， 计算数据源，分库分表逻辑等
     * 根据规则计算出数据源，找到对应mapper， 然后调用对应mapper的对应方法
     *
     * @param proxy  the proxy instance that the method was invoked on
     * @param method the {@code Method} instance corresponding to
     *               the interface method invoked on the proxy instance.  The declaring
     *               class of the {@code Method} object will be the interface that
     *               the method was declared in, which may be a superinterface of the
     *               proxy interface that the proxy class inherits the method through.
     * @param args   an array of objects containing the values of the
     *               arguments passed in the method invocation on the proxy instance,
     *               or {@code null} if interface method takes no arguments.
     *               Arguments of primitive types are wrapped in instances of the
     *               appropriate primitive wrapper class, such as
     *               {@code java.lang.Integer} or {@code java.lang.Boolean}.
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        long shardingKeyVal = getShardingKeyFromArgs(method, args);
        String dsName = sharding.datasource()[0];
        if (sharding.dbRule().length() > 0) {
            dsName = ExpressionUtil.eval(sharding.dbRule(), sharding.shardingKey(), shardingKeyVal);
        }
        String className = NameUtils.buildClassName(dsName, clz.getCanonicalName());
        val mapperClass = classManager.getClass(dsName, className);
        val mapper = sessionFactoryManager.getMapper(mapperClass);
        if (sharding.tableRule().length() > 0) {
            String tableName = ExpressionUtil.eval(sharding.tableRule(), sharding.shardingKey(), shardingKeyVal);
            val builtArgs = buildArgs(args, tableName);
            Method targetMethod = classManager.getMethod(dsName, className, method.getName());
            return targetMethod.invoke(mapper, builtArgs);
        } else if (sharding.dbRule().length() > 0) {
            Method targetMethod = classManager.getMethod(dsName, className, method.getName());
            return targetMethod.invoke(mapper, args);
        }
        return null;
    }

    private Object[] buildArgs(Object[] args, String tableName) {
        Object[] result = new Object[args.length + 1];
        result[0] = tableName;
        System.arraycopy(args, 0, result, 1, args.length);
        return result;
    }
}

