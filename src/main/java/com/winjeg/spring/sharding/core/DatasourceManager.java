package com.winjeg.spring.sharding.core;


import com.winjeg.spring.sharding.config.DataSourceProps;
import com.winjeg.spring.sharding.factories.HikariCPFactory;
import lombok.val;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * 管理所有的datasource, 不管是否分库分表
 *
 * @author winjeg
 */
public class DatasourceManager {
    private static final Map<String, DataSource> DATASOURCE_MAP = new ConcurrentHashMap<>();
    private final DataSourceProps props;

    public DataSource get(String name) {
        return DATASOURCE_MAP.get(name);
    }

    /**
     * provide a method to traverse all the datasource managed by the manager
     *
     * @param consumer the consumer that need access datasource
     */
    public void foreach(BiConsumer<String, DataSource> consumer) {
        for (val entry : DATASOURCE_MAP.entrySet()) {
            consumer.accept(entry.getKey(), entry.getValue());
        }
    }

    public DatasourceManager(DataSourceProps props) {
        this.props = props;
        init();
    }

    public void init() {
        DATASOURCE_MAP.putAll(HikariCPFactory.createAllMap(props));
    }
}
