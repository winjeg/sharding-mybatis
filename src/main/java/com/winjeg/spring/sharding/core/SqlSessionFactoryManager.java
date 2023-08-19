package com.winjeg.spring.sharding.core;

import com.winjeg.spring.sharding.annos.Sharding;
import com.winjeg.spring.sharding.utils.ResourceUtil;
import com.winjeg.spring.sharding.utils.XmlUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * 管理sql session factory的东西， 同时管理 transaction 相关
 *
 * @author winjeg
 */
@Slf4j
public class SqlSessionFactoryManager {
    private static final Map<String, SqlSessionFactoryBean> SESSION_FACTORY_MAP = new ConcurrentHashMap<>();
    private static final Map<String, SqlSessionTemplate> TEMPLATE_MAP = new ConcurrentHashMap<>();
    private static final Map<String, String> RES_MAP = new ConcurrentHashMap<>();
    private static final ResourcePatternResolver RESOLVER = new PathMatchingResourcePatternResolver();
    private final List<Class<?>> classes;
    private final DatasourceManager datasourceManager;

    private Interceptor interceptor;

    public SqlSessionFactoryManager(List<Class<?>> cs, DatasourceManager dsManager) {
        this.classes = cs;
        this.datasourceManager = dsManager;
        init();
    }

    /**
     * 创建SQLSessionFactoryBean
     *
     * @param name       名称
     * @param dataSource 数据源
     * @param locations  对应xml文件
     * @return 对象
     */
    public SqlSessionFactoryBean createSqlSession(String name, DataSource dataSource, Resource... locations) {
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        if (interceptor != null) {
            configuration.addInterceptor(interceptor);
        }
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setMapperLocations(locations);
        factoryBean.setDataSource(dataSource);
        factoryBean.setConfiguration(configuration);
        return factoryBean;
    }

    public Interceptor getInterceptor() {
        return interceptor;
    }


    public void init() {
        //初始化所有的Mapper.xml
        datasourceManager.foreach((name, ds) -> {
            val resources = extractResources(name);
            for (val entry : resources.entrySet()) {
                RES_MAP.put(entry.getKey(), name);
            }
            val sessionFactory = createSqlSession(name, ds, toArray(resources.values()));

            SESSION_FACTORY_MAP.putIfAbsent(name, sessionFactory);
            SqlSessionTemplate template = null;
            try {
                template = new SqlSessionTemplate(Objects.requireNonNull(sessionFactory.getObject()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            TEMPLATE_MAP.putIfAbsent(name, template);
        });
    }

    /**
     * 利用MyBatis的方法获取Mapper
     *
     * @param clz 类型
     * @param <T> 参数类型
     * @return mapper对象
     */
    public <T> T getMapper(Class<?> clz) {
        val template = TEMPLATE_MAP.get(RES_MAP.get(clz.getCanonicalName()));
        if (template == null) {
            throw new RuntimeException("datasource init failed, template null!");
        }
        return (T) template.getMapper(clz);
    }

    /**
     * 解析resource
     *
     * @param dsName 数据源名称
     * @return 资源列表
     */
    private Map<String, Resource> extractResources(String dsName) {
        Map<String, Resource> result = new HashMap<>();
        for (val clz : classes) {
            Sharding sharding = ResourceUtil.getShardingAnno(clz);
            if (sharding == null || !contains(sharding.datasource(), dsName)) {
                continue;
            }
            boolean isSharding = sharding.dbRule().length() > 0 || sharding.tableRule().length() > 0;
            Resource[] resources;
            try {
                resources = RESOLVER.getResources(sharding.mapperLocation());
            } catch (IOException e) {
                log.warn("extractShardingResources - mapper location error", e);
                continue;
            }
            for (val r : resources) {
                if (XmlUtils.extractNamespace(r).equals(clz.getCanonicalName())) {
                    if (isSharding) {
                        val resourcePair = XmlUtils.changeMapperNameSpace(dsName, r);
                        if (resourcePair != null) {
                            result.putIfAbsent(resourcePair.left(), resourcePair.right());
                        }
                    } else {
                        result.putIfAbsent(XmlUtils.extractNamespace(r), r);
                    }
                }
            }
        }
        return result;
    }

    private static boolean contains(String[] arr, String element) {
        if (arr == null || element == null) {
            return false;
        }
        for (val e : arr) {
            if (element.equals(e)) {
                return true;
            }
        }
        return false;
    }

    private static Resource[] toArray(Collection<Resource> collection) {
        if (collection == null || collection.size() == 0) {
            return new Resource[]{};
        }
        val result = new Resource[collection.size()];
        int i = 0;
        for (val e : collection) {
            result[i] = e;
            i++;
        }
        return result;
    }


    public void foreach(BiConsumer<String, SqlSessionTemplate> consumer) {
        TEMPLATE_MAP.forEach(consumer);
    }
}
