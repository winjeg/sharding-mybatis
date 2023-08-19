package com.winjeg.spring.sharding.annos;

import java.lang.annotation.*;

/**
 * 标记sharding规则， 仅支持简单sharding
 * 只能放在interface上面，一个interface 只能访问一个逻辑表
 * 可以支持仅分表， 也可以支持仅分库， 也可以支持分库+分表
 *
 * @author winjeg
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Sharding {

    /**
     * datasource names
     *
     * @return datasource names, exactly the name what you have configured
     */
    String[] datasource() default {};

    /**
     * specify the location of the mapper xml files
     *
     * @return location of mapper files
     */
    String mapperLocation();


    /**
     * example:  'ds-' + userId % 1024 / 64
     *
     * @return actual database rule
     */
    String dbRule() default "";


    /**
     * example: 'trade_order_' + userId % 1024 % 64
     *
     * @return actual table rule
     */
    String tableRule() default "";


    /**
     * 分库或者分表的时候表达式里面用到的占位符字段
     *
     * @return 占位符字段名称
     */
    String shardingKey() default "";
}
