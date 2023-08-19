package com.winjeg.spring.sharding.factories;

import com.winjeg.spring.sharding.core.ShardingCoreHandler;
import com.winjeg.spring.sharding.core.SqlSessionFactoryManager;
import lombok.val;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.winjeg.spring.sharding.utils.ResourceUtil.getShardingAnno;


/**
 * 核心逻辑之二， 分库分表动态代理的实现处
 *
 * @author winjeg
 */
public class ShardingMapperFactory {

    private static final Map<Class<?>, ShardingCoreHandler> HANDLER_MAP = new ConcurrentHashMap<>();

    private final SqlSessionFactoryManager sessionFactoryManager;

    private final ClassManager classManager;

    public ShardingMapperFactory(SqlSessionFactoryManager sessionFactoryManager, ClassManager classManager) {
        this.sessionFactoryManager = sessionFactoryManager;
        this.classManager = classManager;
    }

    /**
     * 创建动态代理， 基于jdk
     *
     * @param clz 代理接口类型
     */
    public <T> T createProxy(Class<T> clz) {
        val sharding = getShardingAnno(clz);
        boolean isSharding = sharding.dbRule().length() != 0 || sharding.tableRule().length() != 0;
        if (isSharding) {
            ShardingCoreHandler handler = HANDLER_MAP.get(clz);
            if (handler == null) {
                handler = new ShardingCoreHandler(sharding, sessionFactoryManager, clz, classManager);
                HANDLER_MAP.putIfAbsent(clz, handler);
            }
            return (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{clz}, handler);
        } else {
            return null;
        }
    }
}
