package com.winjeg.spring.sharding.config;

import lombok.Data;

/**
 * 数据库数据源配置， 支持多数据源
 * 如果需要其他属性，可以在此添加，然后在
 * HikariCpFactory 中设置相应属性即可
 *
 * @author winjeg
 */
@Data
public class DataSourceProps {

    private HikariProps[] list;

    @Data
    public static class HikariProps {

        /**
         * name 是作为map的KEY使用需要保证唯一性
         */
        private String name;

        private long connectionTimeout = 2000;
        private long idleTimeout = 2000;
        private String connectionTestQuery = "SELECT 1";
        private int maximumPoolSize = 10;
        private String driverClassName;
        private String jdbcUrl;
        private String username;
        private String password;
    }
}
