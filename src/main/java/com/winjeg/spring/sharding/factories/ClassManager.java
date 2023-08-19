package com.winjeg.spring.sharding.factories;

import com.winjeg.spring.sharding.utils.ClassScanUtils;
import com.winjeg.spring.sharding.utils.NameUtils;
import com.winjeg.spring.sharding.utils.ResourceUtil;
import lombok.val;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * mapper类管理器， 方法管理
 * 生成的类全部由此管理
 *
 * @author winjeg
 */
public class ClassManager {
    private static final Map<String, Map<String, Class<?>>> CLASS_MAP = new HashMap<>();
    /**
     * KEY1： dsName， Key2: Interface.class  Key3 方法名
     */
    private static final Map<String, Map<String, Map<String, Method>>> METHOD_CACHE = new HashMap<>();

    private final List<Class<?>> classes;

    public ClassManager(List<Class<?>> cs) {
        classes = cs;
        init();
    }

    private void init() {
        for (val clz : classes) {
            val sharding = ResourceUtil.getShardingAnno(clz);
            if (sharding.tableRule().length() > 0) {
                for (val dsName : sharding.datasource()) {
                    generate(dsName, clz, true);
                }
            } else if (sharding.dbRule().length() > 0) {
                for (val dsName : sharding.datasource()) {
                    generate(dsName, clz, false);
                }
            }
        }
    }

    private void generate(String dsName, Class<?> clz, boolean sharding) {
        Map<String, Class<?>> dsMap = CLASS_MAP.get(dsName);
        if (dsMap == null) {
            dsMap = new HashMap<>();
        }
        val genClz = ClassScanUtils.generateMapperViaMapper(clz, dsName, sharding);
        String name = NameUtils.buildClassName(dsName, clz.getCanonicalName());
        dsMap.put(name, genClz);
        CLASS_MAP.put(dsName, dsMap);


        Map<String, Map<String, Method>> dsMethodMap = METHOD_CACHE.get(dsName);
        if (dsMethodMap == null) {
            dsMethodMap = new HashMap<>();
        }
        Map<String, Method> methodMap = dsMethodMap.get(name);
        if (methodMap == null) {
            methodMap = new HashMap<>();
        }
        for (val m : genClz.getDeclaredMethods()) {
            methodMap.put(m.getName(), m);
        }
        dsMethodMap.put(name, methodMap);
        METHOD_CACHE.put(dsName, dsMethodMap);
    }

    /**
     * 根据数据源和类名获取对应的类
     *
     * @param dsName    数据源
     * @param className 类名
     * @return 类
     */
    public Class<?> getClass(String dsName, String className) {
        return CLASS_MAP.get(dsName).get(className);
    }

    /**
     * 根据数据源和类名方法名获取对应的方法
     *
     * @param dsName    数据源
     * @param className 类名
     * @param name      方法名
     * @return 类
     */
    public Method getMethod(String dsName, String className, String name) {
        return METHOD_CACHE.get(dsName).get(className).get(name);
    }
}
