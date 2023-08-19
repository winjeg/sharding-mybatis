package com.winjeg.spring.sharding.factories;

import com.winjeg.spring.sharding.config.DataSourceProps;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.sql.DataSource;
import java.util.*;

/**
 * 创建hikaricp的统一工厂
 *
 * @author winjeg
 */
@Slf4j
public class HikariCPFactory {

    public static DataSource createOne(DataSourceProps.HikariProps props) {
        HikariConfig config = new HikariConfig();
        config.setConnectionTestQuery(props.getConnectionTestQuery());
        config.setConnectionTimeout(props.getConnectionTimeout());
        // config.setIdleTimeout(props.getIdleTimeout());
        config.setDriverClassName(props.getDriverClassName());
        config.setPoolName(props.getName());
        config.setJdbcUrl(props.getJdbcUrl());
        config.setUsername(props.getUsername());
        config.setPassword(props.getPassword());
        config.setMaximumPoolSize(props.getMaximumPoolSize());
        config.addDataSourceProperty("dataSource.cachePrepStmts", "true");
        config.addDataSourceProperty("dataSource.prepStmtCacheSize", "250");
        config.addDataSourceProperty("dataSource.prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("dataSource.useServerPrepStmts", "true");
        log.info("createOne ----  datasource:{} of type:{} created...", props.getName(), props.getDriverClassName());
        return new HikariDataSource(config);
    }


    /**
     * 创建结果为list
     */
    public static List<DataSource> createAll(DataSourceProps props) {
        if (props.getList() == null || props.getList().length == 0) {
            return Collections.emptyList();
        }
        List<DataSource> result = new ArrayList<>(props.getList().length);
        for (val prop : props.getList()) {
            result.add(createOne(prop));
        }
        return result;
    }

    /**
     * 创建结果为map
     */
    public static Map<String, DataSource> createAllMap(DataSourceProps props) {
        if (props.getList() == null || props.getList().length == 0) {
            return Collections.emptyMap();
        }
        Map<String, DataSource> result = new HashMap<>(props.getList().length, 1);
        for (val prop : props.getList()) {
            result.put(prop.getName(), createOne(prop));
        }
        return result;
    }
}
