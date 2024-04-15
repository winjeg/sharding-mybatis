# starters-sharding

Production ready simple sharding tool for mybatis.

生产可用的简单的基于mybatis的分库分表工具， 目前已经应用于生产环境。

## 欢迎提交PR

Plus: 有没有人可以帮忙把包传到公开的maven仓库的， 可以联系我 [邮件](mailto://winjeg@qq.com) 


为啥要使用他？

- 支持不同种的数据源，只要实现了 标准的jdbc 和datasource相关接口都可以使用
- 支持不同类型数据源混用，分库分表
- 支持任何对应数据库的SQL语法， 这个是基于AST语法树的ShardingSphere不具备的
- 效率非常高， 整个组件在性能损耗方面可以忽略不计

## 简介：

## 功能列表

- 多数据源支持
- 支持分库分表（仅支持long类型的sharding Key)
- 支持只分库
- 支持只分表
- 支持使用事务 (参考示例)
- 支持分库分表规则自定义（aviator 表达式引擎)
- 支持不同数据源混用

## 使用说明

### 1. 引入依赖， 最好禁用掉spring-boot 的自身datasource装配，因为没用

```xml

<dependency>
    <groupId>com.winjeg.spring</groupId>
    <artifactId>sharding-mybatis</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置好数据源

```yaml
datasource:
  list:
    - name: demo-1
      driver-class-name: com.mysql.cj.jdbc.Driver
      jdbc-url: jdbc:mysql://10.10.10.10:3306/demo_1?useSSL=false&useUnicode=true&characterEncoding=UTF-8
      username: demo_user
      password: 123456
    - name: demo-2
      driver-class-name: com.mysql.cj.jdbc.Driver
      jdbc-url: jdbc:mysql://10.10.10.10:3306/demo_2?useSSL=false&useUnicode=true&characterEncoding=UTF-8
      username: demo_user
      password: 123456

```

### 3. 开启分库分表支持的配置

```java

@SpringBootApplication
@EnableSharding(packages = {"com.winjeg.spring.test.mapper", "com.winjeg.spring.test.dao"})
public class SpringApplicationDemo {
    public static void main(String[] args) {
        SpringApplication.run(SpringApplicationDemo.class, args);
    }
}
```

### 4. 在mapper上添加注解 & 标记出分表键, 如不需要分库分表， 仅配置datasource 和 mapperLocation

```java

@Sharding(datasource = {"demo-1", "demo-2"},
        mapperLocation = "classpath:mappers/demo/*.xml",
        dbRule = "'demo-' + (id % 16 / 8 + 1)",
        tableRule = "'user_' + (id % 16 % 4)",
        shardingKey = "id")
public interface ShardingMapper {

    int updateUser(@ShardingKey @Param("id") final long id,
                   @Param("name") final String name);
}

```

> 样例Mapper

```xml

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.winjeg.spring.sharding.test.mapper.ShardingMapper">
    <insert id="addUser">
        INSERT INTO ${tableName}
        (id, name) VALUES(#{id}, #{name})
    </insert>

    <update id="updateUser">
        UPDATE ${tableName}
        SET name=#{name}
        WHERE id = #{id}
    </update>
</mapper>
```

> 注： 在不分库分表的时候， datasource只能设置一个， 设置多个则无用
> 仅分库的时候，只设置 dbRule， 仅分表的时候设置 tableRule

### 5. 代码中使用

```java
    @Autowired
private NormalMapper testMapper;

@Autowired
private WonderMapper wonderMapper;

@GetMapping("/normal")
public List<String> noneSharding(@RequestParam(value = "uid", defaultValue = "1") final long userId){
        val tables=testMapper.getTables();
        tables.add(wonderMapper.getUser());
        return tables;
        }


```

使用事务

```java
    @GetMapping("/trans")
public String Sharding(@RequestParam(value = "uid", defaultValue = "1") final long userId,
@RequestParam(value = "name", defaultValue = "") final String name){
        val manager=transactionManager.getTransactionManager(ShardingMapper.class,userId);
        TransactionStatus status=manager.getTransaction(definition);
        try{
        if(shardingMapper.addUser(userId,name)< 1){
        manager.rollback(status);
        return"add_trans_fail";
        }
        if(shardingMapper.updateUser(userId,name+"updated")< 1){
        manager.rollback(status);
        return"update_trans_fail";
        }
        manager.commit(status);
        return"trans_success";
        }catch(Exception e){
        manager.rollback(status);
        }
        return"trans_fail";
        }
```



