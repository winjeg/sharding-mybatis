package com.winjeg.spring.sharding.annos;

import java.lang.annotation.*;

/**
 * sharding key only supports int or long columns
 * 仅支持 long 类型的shardingKey
 *
 * @author winjeg
 */

@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ShardingKey {
}
