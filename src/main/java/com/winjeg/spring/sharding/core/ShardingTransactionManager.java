package com.winjeg.spring.sharding.core;

import com.winjeg.spring.sharding.annos.Sharding;
import com.winjeg.spring.sharding.utils.ExpressionUtil;
import com.winjeg.spring.sharding.utils.ResourceUtil;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.HashMap;
import java.util.Map;

/**
 * 分库分表情况下事务管理器
 * 有三种方法获取事务管理器
 *
 * @author winjeg
 */
public class ShardingTransactionManager {

    private static final Map<String, DataSourceTransactionManager> TRANS_MAP = new HashMap<>();

    private final DatasourceManager datasourceManager;

    public ShardingTransactionManager(DatasourceManager ds) {
        datasourceManager = ds;
        init();
    }

    private void init() {
        datasourceManager.foreach((name, ds) -> {
            TRANS_MAP.put(name, new DataSourceTransactionManager(ds));
        });
    }


    /**
     * 对于分库分表的情况， 不能明确计算是哪个数据源的，
     */
    public PlatformTransactionManager getTransactionManager(Class<?> mapperClz, long shardingVal) {
        Sharding sharding = ResourceUtil.getShardingAnno(mapperClz);
        if (sharding == null) {
            throw new IllegalArgumentException("this mapper is illegal");
        }
        if (sharding.dbRule().length() == 0) {
            return TRANS_MAP.get(sharding.datasource()[0]);
        } else {
            String dsName = ExpressionUtil.eval(sharding.dbRule(), sharding.shardingKey(), shardingVal);
            return TRANS_MAP.get(dsName);
        }
    }

    /**
     * 根据分库规则，以及key 计算出实际数据源，拿到对应的管理器
     */
    public PlatformTransactionManager getTransactionManager(String dbRule, String key, long shardingVal) {
        String dsName = ExpressionUtil.eval(dbRule, key, shardingVal);
        return TRANS_MAP.get(dsName);

    }

    /**
     * 对于可以明确是哪个datasource的来说， 用这个就足以， 这个效率会高一些
     *
     * @param ds 数据源名称
     * @return 事务管理器
     */
    public PlatformTransactionManager getTransactionManager(String ds) {
        return TRANS_MAP.get(ds);
    }
}
