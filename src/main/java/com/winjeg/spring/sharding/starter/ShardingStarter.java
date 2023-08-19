package com.winjeg.spring.sharding.starter;

import com.winjeg.spring.sharding.annos.EnableSharding;
import com.winjeg.spring.sharding.config.DataSourceProps;
import com.winjeg.spring.sharding.core.DatasourceManager;
import com.winjeg.spring.sharding.core.ShardingTransactionManager;
import com.winjeg.spring.sharding.core.SqlSessionFactoryManager;
import com.winjeg.spring.sharding.factories.ClassManager;
import com.winjeg.spring.sharding.factories.ShardingMapperFactory;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.ArrayList;
import java.util.List;

import static com.winjeg.spring.sharding.utils.ResourceUtil.getClassesWithAnno;


/**
 * 1. 注入Mapper， 包括动态代理的， 和非动态代理的
 * 2. 注入SqlSessionFactory
 * 3. 注入Datasource 以及DatasourceManager
 * 4. 注入事务管理器
 *
 * @author winjeg
 */
@Slf4j
public class ShardingStarter implements ImportBeanDefinitionRegistrar, BeanFactoryAware {
    private ConfigurableBeanFactory beanFactory;

    public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (ConfigurableBeanFactory) beanFactory;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metaData, @NonNull BeanDefinitionRegistry registry) {
        val anno = metaData.getAnnotations().get(EnableSharding.class);
        if (!anno.isPresent()) {
            return;
        }
        val packages = anno.getStringArray("packages");
        if (packages.length == 0) {
            return;
        }
        val classesPair = getClassesWithAnno(packages);
        // 生成sharding所依赖的interface，并且load到jvm
        ClassManager classManager = null;
        if (classesPair.right().size() > 0) {
            classManager = new ClassManager(classesPair.right());
        }
        val datasourceCfg = getDatasourceCfg();
        val datasourceManager = new DatasourceManager(datasourceCfg);
        registerDatasource(datasourceManager);

        List<Class<?>> classes = new ArrayList<>();
        classes.addAll(classesPair.left());
        classes.addAll(classesPair.right());
        val sessionManager = new SqlSessionFactoryManager(classes, datasourceManager);
        if (classManager != null) {
            registerSharding(classesPair.right(), sessionManager, classManager);
        }
        if (classesPair.left().size() > 0) {
            registerNonShardingClasses(classesPair.left(), sessionManager);
        }
        beanFactory.registerSingleton("session_factory_manager", sessionManager);
        beanFactory.registerSingleton("sharding_trans_mgr", new ShardingTransactionManager(datasourceManager));
        beanFactory.registerSingleton("sharding_trans_def", new DefaultTransactionDefinition(0));
    }


    private void registerDatasource(DatasourceManager datasourceManager) {
        datasourceManager.foreach((name, ds) -> {
            beanFactory.registerSingleton(name, ds);
        });
    }

    private void registerSharding(List<Class<?>> classes, SqlSessionFactoryManager sessionManager, ClassManager classManager) {
        ShardingMapperFactory factory = new ShardingMapperFactory(sessionManager, classManager);
        for (val clz : classes) {
            val mapper = factory.createProxy(clz);
            beanFactory.registerSingleton(clz.getCanonicalName(), mapper);
        }
    }

    private void registerNonShardingClasses(List<Class<?>> classes, SqlSessionFactoryManager sessionManager) {
        for (val clz : classes) {
            val mapper = sessionManager.getMapper(clz);
            beanFactory.registerSingleton(clz.getCanonicalName(), mapper);
        }
    }

    private DataSourceProps getDatasourceCfg() {
        val env = beanFactory.getBean(Environment.class);
        try {
            Iterable<ConfigurationPropertySource> sources = ConfigurationPropertySources.get(env);
            Binder binder = new Binder(sources);
            val config = binder.bind("datasource", DataSourceProps.class);
            if (config == null || config.get() == null
                    || config.get().getList() == null
                    || config.get().getList().length == 0) {
                return null;
            }
            return config.get();
        } catch (Exception ignored) {
        }
        return null;
    }
}
