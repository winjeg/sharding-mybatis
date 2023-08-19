package com.winjeg.spring.sharding.annos;

import com.winjeg.spring.sharding.starter.ShardingStarter;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 是否开启sharding， 如果要使用这个中间件，此注解必不可少
 * 必须要配置扫描的 packages, 即mapper 所在包名， 包里可以放其他东西，但是不推荐
 * 注意一点：在同一个Mapper内，方法名称不可以重复，如果重复的话，则会导致问题
 *
 * @author winjeg
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@AutoConfigurationPackage
@Import(ShardingStarter.class)
public @interface EnableSharding {
    String[] packages();
}
