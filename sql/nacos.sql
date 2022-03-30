create schema if not exists nacos collate utf8mb4_general_ci;
use nacos;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for config_info
-- ----------------------------
DROP TABLE IF EXISTS `config_info`;
CREATE TABLE `config_info`
(
    `id`           bigint(20)                    NOT NULL AUTO_INCREMENT COMMENT 'id',
    `data_id`      varchar(255) COLLATE utf8_bin NOT NULL COMMENT 'data_id',
    `group_id`     varchar(255) COLLATE utf8_bin          DEFAULT NULL,
    `content`      longtext COLLATE utf8_bin     NOT NULL COMMENT 'content',
    `md5`          varchar(32) COLLATE utf8_bin           DEFAULT NULL COMMENT 'md5',
    `gmt_create`   datetime                      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` datetime                      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `src_user`     text COLLATE utf8_bin COMMENT 'source user',
    `src_ip`       varchar(50) COLLATE utf8_bin           DEFAULT NULL COMMENT 'source ip',
    `app_name`     varchar(128) COLLATE utf8_bin          DEFAULT NULL,
    `tenant_id`    varchar(128) COLLATE utf8_bin          DEFAULT '' COMMENT '租户字段',
    `c_desc`       varchar(256) COLLATE utf8_bin          DEFAULT NULL,
    `c_use`        varchar(64) COLLATE utf8_bin           DEFAULT NULL,
    `effect`       varchar(64) COLLATE utf8_bin           DEFAULT NULL,
    `type`         varchar(64) COLLATE utf8_bin           DEFAULT NULL,
    `c_schema`     text COLLATE utf8_bin,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_configinfo_datagrouptenant` (`data_id`, `group_id`, `tenant_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 8
  DEFAULT CHARSET = utf8
  COLLATE = utf8_bin COMMENT ='config_info';

-- ----------------------------
-- Records of config_info
-- ----------------------------
BEGIN;
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`,
                           `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`)
VALUES (1, 'application.yml', 'DEFAULT_GROUP',
        'spring:\n  redis:\n    host: 10.61.20.211\n    password: wssy001\n    timeout: 1000\n    lettuce:\n      pool:\n        min-idle: 5\n        max-idle: 10',
        '2589275754f828bfba32e6198494f7fb', '2022-03-30 12:12:34', '2022-03-30 12:12:34', NULL, '172.17.0.1', '',
        'waibao', NULL, NULL, NULL, 'yaml', NULL);
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`,
                           `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`)
VALUES (2, 'waibao-user.yml', 'DEFAULT_GROUP',
        'spring:\n    shardingsphere:\n        datasource:\n        names: master,slave\n        master:\n            type: com.zaxxer.hikari.HikariDataSource\n            driverClassName: com.mysql.cj.jdbc.Driver\n            jdbcUrl: jdbc:mysql://10.61.20.211:33306/waibao_v2?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n            username: root\n            password: wssy001\n        slave:\n            type: com.zaxxer.hikari.HikariDataSource\n            driverClassName: com.mysql.cj.jdbc.Driver\n            jdbcUrl: jdbc:mysql://10.61.20.211:33307/waibao_v2?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n            username: root\n            password: wssy001\n\n        sharding:\n        tables:\n            user:\n            actualDataNodes: master.user_$->{0..1}\n            tableStrategy:\n                inline:\n                shardingColumn: id\n                algorithmExpression: user_$->{id % 2}\n            keyGenerator:\n                column: id\n                type: SNOWFLAKE\n\n        master-slave-rules:\n            master:\n            master-data-source-name: master\n            SlaveDataSourceNames: slave\nlogging:\n  level:\n    root: info',
        'c76cd3f0eece1b3f9e6f2e060b8d9a14', '2022-03-30 12:12:34', '2022-03-30 12:12:34', NULL, '172.17.0.1', '',
        'waibao', NULL, NULL, NULL, 'yaml', NULL);
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`,
                           `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`)
VALUES (3, 'waibao-gateway.yml', 'DEFAULT_GROUP',
        'spring:\n  cloud:\n    gateway:\n      discovery:\n        locator:\n          enabled: true\n      routes:\n        - id: waibao-user #payment_routh    #路由的ID，没有固定规则但要求唯一，简易配合服务名\n          #          uri: http://localhost:8001         #匹配后提供服务的路由地址\n          uri: lb://waibao-user   #匹配后提供服务的路由地址，lb后跟提供服务的微服务的名，不要写错\n          predicates:\n            - Path=/**          #断言，路径相匹配的进行路由\n      globalcors:\n        cors-configurations:\n          \'[/**]\': # 匹配所有请求\n            allowedOrigins: \"*\" #跨域处理 允许所有的域\n            allowedMethods: # 支持的方法\n              - GET\n              - POST\n              - PUT\n              - DELETE\n\n  main:\n    web-application-type: reactive',
        'e3a89b8930adbc35cdfc9ef47a8c0ebf', '2022-03-30 12:12:34', '2022-03-30 12:12:34', NULL, '172.17.0.1', '',
        'waibao', NULL, NULL, NULL, 'yaml', NULL);
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`,
                           `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`)
VALUES (4, 'waibao-rcde.yml', 'DEFAULT_GROUP',
        'spring:\n  datasource:\n    url: jdbc:mysql://10.61.20.211:33306/waibao_v2?useUnicode=true&useSSL=false&autoReconnect=true&characterEncoding=utf-8&serverTimezone=GMT%2B8&rewriteBatchedStatements=true\n    username: root\n    password: wssy001\n    hikari:\n      max-lifetime: 28770000\n      maximum-pool-size: 20\n      minimum-idle: 5\n      connection-timeout: 28770000\n\n  task:\n    execution:\n      pool:\n        max-size: 4\n        keep-alive: 60s\n        core-size: 2\n        queue-capacity: 2000\n      thread-name-prefix: 异步任务-\n    scheduling:\n      pool:\n        size: 4\n      thread-name-prefix: 定时任务-',
        '6c8603c255b93641ad2540a77b428654', '2022-03-30 12:12:34', '2022-03-30 12:12:34', NULL, '172.17.0.1', '',
        'waibao', NULL, NULL, NULL, 'yaml', NULL);
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`,
                           `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`)
VALUES (5, 'waibao-payment.yml', 'DEFAULT_GROUP',
        'spring:\n  shardingsphere:\n    datasource:\n      names: master,slave,master-credit-user,master-payment,slave-credit-user,slave-payment\n      master:\n        type: com.zaxxer.hikari.HikariDataSource\n        driverClassName: com.mysql.cj.jdbc.Driver\n        jdbcUrl: jdbc:mysql://10.61.20.211:33306/waibao_v2?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n        username: root\n        password: wssy001\n      slave:\n        type: com.zaxxer.hikari.HikariDataSource\n        driverClassName: com.mysql.cj.jdbc.Driver\n        jdbcUrl: jdbc:mysql://10.61.20.211:33307/waibao_v2?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n        username: root\n        password: wssy001\n      master-credit-user:\n        type: com.zaxxer.hikari.HikariDataSource\n        driverClassName: com.mysql.cj.jdbc.Driver\n        jdbcUrl: jdbc:mysql://10.61.20.211:33306/waibao_credit_user?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n        username: root\n        password: wssy001\n      master-payment:\n        type: com.zaxxer.hikari.HikariDataSource\n        driverClassName: com.mysql.cj.jdbc.Driver\n        jdbcUrl: jdbc:mysql://10.61.20.211:33306/waibao_payment?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n        username: root\n        password: wssy001\n      slave-credit-user:\n        type: com.zaxxer.hikari.HikariDataSource\n        driverClassName: com.mysql.cj.jdbc.Driver\n        jdbcUrl: jdbc:mysql://10.61.20.211:33307/waibao_credit_user?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n        username: root\n        password: wssy001\n      slave-payment:\n        type: com.zaxxer.hikari.HikariDataSource\n        driverClassName: com.mysql.cj.jdbc.Driver\n        jdbcUrl: jdbc:mysql://10.61.20.211:33307/waibao_payment?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n        username: root\n        password: wssy001\n\n    sharding:\n      tables:\n        mq_msg_compensation:\n          actualDataNodes: master.mq_msg_compensation\n        user_credit:\n          actualDataNodes: master-credit-user.user_credit_$->{0..1}\n          tableStrategy:\n            inline:\n              shardingColumn: user_id\n              algorithmExpression: user_credit_$->{user_id % 2}\n        payment:\n          actualDataNodes: master-payment.payment_$->{0..1}\n          tableStrategy:\n            inline:\n              shardingColumn: pay_id\n              algorithmExpression: payment_$->{pay_id % 2}\n\n      master-slave-rules:\n        master:\n          master-data-source-name: master\n          SlaveDataSourceNames: slave\n        master-order-user:\n          master-data-source-name: master-credit-user\n          SlaveDataSourceNames: slave-credit-user\n        master-order-retailer:\n          master-data-source-name: master-payment\n          SlaveDataSourceNames: slave-payment\n  task:\n    execution:\n      pool:\n        max-size: 16\n        keep-alive: \"10s\"\n      thread-name-prefix: 异步任务-\n    scheduling:\n      pool:\n        size: 5\n      thread-name-prefix: 定时任务-\nrocketmq:\n  name-server: 10.61.20.211:39876\n  producer:\n    group: order-producer\n\nmybatis-plus:\n  global-config:\n    db-config:\n      logic-delete-field: enable\n      logic-delete-value: false\n      logic-not-delete-value: true\nlogging:\n  level:\n    root: info\n',
        '0b10ca27ff254b450865d41b972978ab', '2022-03-30 12:12:34', '2022-03-30 12:12:34', NULL, '172.17.0.1', '',
        'waibao', NULL, NULL, NULL, 'yaml', NULL);
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`,
                           `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`)
VALUES (6, 'waibao-order.yml', 'DEFAULT_GROUP',
        'spring:\n  shardingsphere:\n    datasource:\n      names: master,slave,master-order-user,master-order-retailer,slave-order-user,slave-order-retailer\n      master:\n        type: com.zaxxer.hikari.HikariDataSource\n        driverClassName: com.mysql.cj.jdbc.Driver\n        jdbcUrl: jdbc:mysql://10.61.20.211:33306/waibao_v2?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n        username: root\n        password: wssy001\n      slave:\n        type: com.zaxxer.hikari.HikariDataSource\n        driverClassName: com.mysql.cj.jdbc.Driver\n        jdbcUrl: jdbc:mysql://10.61.20.211:33307/waibao_v2?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n        username: root\n        password: wssy001\n      master-order-user:\n        type: com.zaxxer.hikari.HikariDataSource\n        driverClassName: com.mysql.cj.jdbc.Driver\n        jdbcUrl: jdbc:mysql://10.61.20.211:33306/waibao_order_user?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n        username: root\n        password: wssy001\n      master-order-retailer:\n        type: com.zaxxer.hikari.HikariDataSource\n        driverClassName: com.mysql.cj.jdbc.Driver\n        jdbcUrl: jdbc:mysql://10.61.20.211:33306/waibao_order_retailer?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n        username: root\n        password: wssy001\n      slave-order-user:\n        type: com.zaxxer.hikari.HikariDataSource\n        driverClassName: com.mysql.cj.jdbc.Driver\n        jdbcUrl: jdbc:mysql://10.61.20.211:33307/waibao_order_user?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n        username: root\n        password: wssy001\n      slave-order-retailer:\n        type: com.zaxxer.hikari.HikariDataSource\n        driverClassName: com.mysql.cj.jdbc.Driver\n        jdbcUrl: jdbc:mysql://10.61.20.211:33307/waibao_order_retailer?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n        username: root\n        password: wssy001\n\n    sharding:\n      tables:\n        admin:\n          actualDataNodes: master.admin\n        mq_msg_compensation:\n          actualDataNodes: master.mq_msg_compensation\n        log_order_goods:\n          actualDataNodes: master.log_order_goods\n        seckill_goods:\n          actualDataNodes: master.seckill_goods\n        order_user:\n          actualDataNodes: master-order-user.order_user_$->{0..1}\n          tableStrategy:\n            inline:\n              shardingColumn: user_id\n              algorithmExpression: order_user_$->{user_id % 2}\n        order_retailer:\n          actualDataNodes: master-order-retailer.order_retailer_$->{0..1}\n          tableStrategy:\n            inline:\n              shardingColumn: retailer_id\n              algorithmExpression: order_retailer_$->{retailer_id % 2}\n\n      master-slave-rules:\n        master:\n          master-data-source-name: master\n          SlaveDataSourceNames: slave\n        master-order-user:\n          master-data-source-name: master-order-user\n          SlaveDataSourceNames: slave-order-user\n        master-order-retailer:\n          master-data-source-name: master-order-retailer\n          SlaveDataSourceNames: slave-order-retailer\n  task:\n    execution:\n      pool:\n        max-size: 16\n        keep-alive: \"10s\"\n      thread-name-prefix: 异步任务-\n    scheduling:\n      pool:\n        size: 5\n      thread-name-prefix: 定时任务-\nrocketmq:\n  name-server: 10.61.20.211:39876\n  producer:\n    group: order-producer\n\nmybatis-plus:\n  global-config:\n    db-config:\n      logic-delete-field: enable\n      logic-delete-value: false\n      logic-not-delete-value: true\n',
        'e0081f263be1f0d374dc47550f91f63f', '2022-03-30 12:12:34', '2022-03-30 12:12:34', NULL, '172.17.0.1', '',
        'waibao', NULL, NULL, NULL, 'yaml', NULL);
INSERT INTO `config_info` (`id`, `data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`,
                           `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`)
VALUES (7, 'waibao-seckill.yml', 'DEFAULT_GROUP',
        'spring:\n  shardingsphere:\n    datasource:\n      names: master,slave\n      master:\n        type: com.zaxxer.hikari.HikariDataSource\n        driverClassName: com.mysql.cj.jdbc.Driver\n        jdbcUrl: jdbc:mysql://10.61.20.211:33306/waibao_v2?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n        username: root\n        password: wssy001\n      slave:\n        type: com.zaxxer.hikari.HikariDataSource\n        driverClassName: com.mysql.cj.jdbc.Driver\n        jdbcUrl: jdbc:mysql://10.61.20.211:33307/waibao_v2?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n        username: root\n        password: wssy001\n\n    sharding:\n      tables:\n        user:\n          actualDataNodes: master.user_$->{0..1}\n          tableStrategy:\n            inline:\n              shardingColumn: id\n              algorithmExpression: user_$->{id % 2}\n          keyGenerator:\n            column: id\n            type: SNOWFLAKE\n\n      master-slave-rules:\n        master:\n          master-data-source-name: master\n          SlaveDataSourceNames: slave\n  task:\n    execution:\n      pool:\n        max-size: 20\n        keep-alive: 60s\n        core-size: 10\n        queue-capacity: 200\n      thread-name-prefix: 异步任务-\n    scheduling:\n      pool:\n        size: 5\n      thread-name-prefix: 定时任务-\nrocketmq:\n  name-server: 10.61.20.211:39876\n  producer:\n    group: order-producer\n\naj:\n  captcha:\n    cache-type: redis\n    interference-options: 2\n    type: blockpuzzle',
        '46812f71c772fc74eae1c38deed203e5', '2022-03-30 12:12:34', '2022-03-30 12:12:34', NULL, '172.17.0.1', '',
        'waibao', NULL, NULL, NULL, 'yaml', NULL);
COMMIT;

-- ----------------------------
-- Table structure for config_info_aggr
-- ----------------------------
DROP TABLE IF EXISTS `config_info_aggr`;
CREATE TABLE `config_info_aggr`
(
    `id`           bigint(20)                    NOT NULL AUTO_INCREMENT COMMENT 'id',
    `data_id`      varchar(255) COLLATE utf8_bin NOT NULL COMMENT 'data_id',
    `group_id`     varchar(255) COLLATE utf8_bin NOT NULL COMMENT 'group_id',
    `datum_id`     varchar(255) COLLATE utf8_bin NOT NULL COMMENT 'datum_id',
    `content`      longtext COLLATE utf8_bin     NOT NULL COMMENT '内容',
    `gmt_modified` datetime                      NOT NULL COMMENT '修改时间',
    `app_name`     varchar(128) COLLATE utf8_bin DEFAULT NULL,
    `tenant_id`    varchar(128) COLLATE utf8_bin DEFAULT '' COMMENT '租户字段',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_configinfoaggr_datagrouptenantdatum` (`data_id`, `group_id`, `tenant_id`, `datum_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_bin COMMENT ='增加租户字段';

-- ----------------------------
-- Records of config_info_aggr
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for config_info_beta
-- ----------------------------
DROP TABLE IF EXISTS `config_info_beta`;
CREATE TABLE `config_info_beta`
(
    `id`           bigint(20)                    NOT NULL AUTO_INCREMENT COMMENT 'id',
    `data_id`      varchar(255) COLLATE utf8_bin NOT NULL COMMENT 'data_id',
    `group_id`     varchar(128) COLLATE utf8_bin NOT NULL COMMENT 'group_id',
    `app_name`     varchar(128) COLLATE utf8_bin          DEFAULT NULL COMMENT 'app_name',
    `content`      longtext COLLATE utf8_bin     NOT NULL COMMENT 'content',
    `beta_ips`     varchar(1024) COLLATE utf8_bin         DEFAULT NULL COMMENT 'betaIps',
    `md5`          varchar(32) COLLATE utf8_bin           DEFAULT NULL COMMENT 'md5',
    `gmt_create`   datetime                      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` datetime                      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `src_user`     text COLLATE utf8_bin COMMENT 'source user',
    `src_ip`       varchar(50) COLLATE utf8_bin           DEFAULT NULL COMMENT 'source ip',
    `tenant_id`    varchar(128) COLLATE utf8_bin          DEFAULT '' COMMENT '租户字段',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_configinfobeta_datagrouptenant` (`data_id`, `group_id`, `tenant_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_bin COMMENT ='config_info_beta';

-- ----------------------------
-- Records of config_info_beta
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for config_info_tag
-- ----------------------------
DROP TABLE IF EXISTS `config_info_tag`;
CREATE TABLE `config_info_tag`
(
    `id`           bigint(20)                    NOT NULL AUTO_INCREMENT COMMENT 'id',
    `data_id`      varchar(255) COLLATE utf8_bin NOT NULL COMMENT 'data_id',
    `group_id`     varchar(128) COLLATE utf8_bin NOT NULL COMMENT 'group_id',
    `tenant_id`    varchar(128) COLLATE utf8_bin          DEFAULT '' COMMENT 'tenant_id',
    `tag_id`       varchar(128) COLLATE utf8_bin NOT NULL COMMENT 'tag_id',
    `app_name`     varchar(128) COLLATE utf8_bin          DEFAULT NULL COMMENT 'app_name',
    `content`      longtext COLLATE utf8_bin     NOT NULL COMMENT 'content',
    `md5`          varchar(32) COLLATE utf8_bin           DEFAULT NULL COMMENT 'md5',
    `gmt_create`   datetime                      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` datetime                      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `src_user`     text COLLATE utf8_bin COMMENT 'source user',
    `src_ip`       varchar(50) COLLATE utf8_bin           DEFAULT NULL COMMENT 'source ip',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_configinfotag_datagrouptenanttag` (`data_id`, `group_id`, `tenant_id`, `tag_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_bin COMMENT ='config_info_tag';

-- ----------------------------
-- Records of config_info_tag
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for config_tags_relation
-- ----------------------------
DROP TABLE IF EXISTS `config_tags_relation`;
CREATE TABLE `config_tags_relation`
(
    `id`        bigint(20)                    NOT NULL COMMENT 'id',
    `tag_name`  varchar(128) COLLATE utf8_bin NOT NULL COMMENT 'tag_name',
    `tag_type`  varchar(64) COLLATE utf8_bin  DEFAULT NULL COMMENT 'tag_type',
    `data_id`   varchar(255) COLLATE utf8_bin NOT NULL COMMENT 'data_id',
    `group_id`  varchar(128) COLLATE utf8_bin NOT NULL COMMENT 'group_id',
    `tenant_id` varchar(128) COLLATE utf8_bin DEFAULT '' COMMENT 'tenant_id',
    `nid`       bigint(20)                    NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (`nid`),
    UNIQUE KEY `uk_configtagrelation_configidtag` (`id`, `tag_name`, `tag_type`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_bin COMMENT ='config_tag_relation';

-- ----------------------------
-- Records of config_tags_relation
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for group_capacity
-- ----------------------------
DROP TABLE IF EXISTS `group_capacity`;
CREATE TABLE `group_capacity`
(
    `id`                bigint(20) unsigned           NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `group_id`          varchar(128) COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT 'Group ID，空字符表示整个集群',
    `quota`             int(10) unsigned              NOT NULL DEFAULT '0' COMMENT '配额，0表示使用默认值',
    `usage`             int(10) unsigned              NOT NULL DEFAULT '0' COMMENT '使用量',
    `max_size`          int(10) unsigned              NOT NULL DEFAULT '0' COMMENT '单个配置大小上限，单位为字节，0表示使用默认值',
    `max_aggr_count`    int(10) unsigned              NOT NULL DEFAULT '0' COMMENT '聚合子配置最大个数，，0表示使用默认值',
    `max_aggr_size`     int(10) unsigned              NOT NULL DEFAULT '0' COMMENT '单个聚合数据的子配置大小上限，单位为字节，0表示使用默认值',
    `max_history_count` int(10) unsigned              NOT NULL DEFAULT '0' COMMENT '最大变更历史数量',
    `gmt_create`        datetime                      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`      datetime                      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_id` (`group_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_bin COMMENT ='集群、各Group容量信息表';

-- ----------------------------
-- Records of group_capacity
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for his_config_info
-- ----------------------------
DROP TABLE IF EXISTS `his_config_info`;
CREATE TABLE `his_config_info`
(
    `id`           bigint(64) unsigned           NOT NULL,
    `nid`          bigint(20) unsigned           NOT NULL AUTO_INCREMENT,
    `data_id`      varchar(255) COLLATE utf8_bin NOT NULL,
    `group_id`     varchar(128) COLLATE utf8_bin NOT NULL,
    `app_name`     varchar(128) COLLATE utf8_bin          DEFAULT NULL COMMENT 'app_name',
    `content`      longtext COLLATE utf8_bin     NOT NULL,
    `md5`          varchar(32) COLLATE utf8_bin           DEFAULT NULL,
    `gmt_create`   datetime                      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` datetime                      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `src_user`     text COLLATE utf8_bin,
    `src_ip`       varchar(50) COLLATE utf8_bin           DEFAULT NULL,
    `op_type`      char(10) COLLATE utf8_bin              DEFAULT NULL,
    `tenant_id`    varchar(128) COLLATE utf8_bin          DEFAULT '' COMMENT '租户字段',
    PRIMARY KEY (`nid`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`),
    KEY `idx_did` (`data_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 8
  DEFAULT CHARSET = utf8
  COLLATE = utf8_bin COMMENT ='多租户改造';

-- ----------------------------
-- Records of his_config_info
-- ----------------------------
BEGIN;
INSERT INTO `his_config_info` (`id`, `nid`, `data_id`, `group_id`, `app_name`, `content`, `md5`, `gmt_create`,
                               `gmt_modified`, `src_user`, `src_ip`, `op_type`, `tenant_id`)
VALUES (0, 1, 'application.yml', 'DEFAULT_GROUP', '',
        'spring:\n  redis:\n    host: 10.61.20.211\n    password: wssy001\n    timeout: 1000\n    lettuce:\n      pool:\n        min-idle: 5\n        max-idle: 10',
        '2589275754f828bfba32e6198494f7fb', '2022-03-30 12:12:34', '2022-03-30 12:12:34', NULL, '172.17.0.1', 'I',
        'waibao');
INSERT INTO `his_config_info` (`id`, `nid`, `data_id`, `group_id`, `app_name`, `content`, `md5`, `gmt_create`,
                               `gmt_modified`, `src_user`, `src_ip`, `op_type`, `tenant_id`)
VALUES (0, 2, 'waibao-user.yml', 'DEFAULT_GROUP', '',
        'spring:\n    shardingsphere:\n        datasource:\n        names: master,slave\n        master:\n            type: com.zaxxer.hikari.HikariDataSource\n            driverClassName: com.mysql.cj.jdbc.Driver\n            jdbcUrl: jdbc:mysql://10.61.20.211:33306/waibao_v2?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n            username: root\n            password: wssy001\n        slave:\n            type: com.zaxxer.hikari.HikariDataSource\n            driverClassName: com.mysql.cj.jdbc.Driver\n            jdbcUrl: jdbc:mysql://10.61.20.211:33307/waibao_v2?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n            username: root\n            password: wssy001\n\n        sharding:\n        tables:\n            user:\n            actualDataNodes: master.user_$->{0..1}\n            tableStrategy:\n                inline:\n                shardingColumn: id\n                algorithmExpression: user_$->{id % 2}\n            keyGenerator:\n                column: id\n                type: SNOWFLAKE\n\n        master-slave-rules:\n            master:\n            master-data-source-name: master\n            SlaveDataSourceNames: slave\nlogging:\n  level:\n    root: info',
        'c76cd3f0eece1b3f9e6f2e060b8d9a14', '2022-03-30 12:12:34', '2022-03-30 12:12:34', NULL, '172.17.0.1', 'I',
        'waibao');
INSERT INTO `his_config_info` (`id`, `nid`, `data_id`, `group_id`, `app_name`, `content`, `md5`, `gmt_create`,
                               `gmt_modified`, `src_user`, `src_ip`, `op_type`, `tenant_id`)
VALUES (0, 3, 'waibao-gateway.yml', 'DEFAULT_GROUP', '',
        'spring:\n  cloud:\n    gateway:\n      discovery:\n        locator:\n          enabled: true\n      routes:\n        - id: waibao-user #payment_routh    #路由的ID，没有固定规则但要求唯一，简易配合服务名\n          #          uri: http://localhost:8001         #匹配后提供服务的路由地址\n          uri: lb://waibao-user   #匹配后提供服务的路由地址，lb后跟提供服务的微服务的名，不要写错\n          predicates:\n            - Path=/**          #断言，路径相匹配的进行路由\n      globalcors:\n        cors-configurations:\n          \'[/**]\': # 匹配所有请求\n            allowedOrigins: \"*\" #跨域处理 允许所有的域\n            allowedMethods: # 支持的方法\n              - GET\n              - POST\n              - PUT\n              - DELETE\n\n  main:\n    web-application-type: reactive',
        'e3a89b8930adbc35cdfc9ef47a8c0ebf', '2022-03-30 12:12:34', '2022-03-30 12:12:34', NULL, '172.17.0.1', 'I',
        'waibao');
INSERT INTO `his_config_info` (`id`, `nid`, `data_id`, `group_id`, `app_name`, `content`, `md5`, `gmt_create`,
                               `gmt_modified`, `src_user`, `src_ip`, `op_type`, `tenant_id`)
VALUES (0, 4, 'waibao-rcde.yml', 'DEFAULT_GROUP', '',
        'spring:\n  datasource:\n    url: jdbc:mysql://10.61.20.211:33306/waibao_v2?useUnicode=true&useSSL=false&autoReconnect=true&characterEncoding=utf-8&serverTimezone=GMT%2B8&rewriteBatchedStatements=true\n    username: root\n    password: wssy001\n    hikari:\n      max-lifetime: 28770000\n      maximum-pool-size: 20\n      minimum-idle: 5\n      connection-timeout: 28770000\n\n  task:\n    execution:\n      pool:\n        max-size: 4\n        keep-alive: 60s\n        core-size: 2\n        queue-capacity: 2000\n      thread-name-prefix: 异步任务-\n    scheduling:\n      pool:\n        size: 4\n      thread-name-prefix: 定时任务-',
        '6c8603c255b93641ad2540a77b428654', '2022-03-30 12:12:34', '2022-03-30 12:12:34', NULL, '172.17.0.1', 'I',
        'waibao');
INSERT INTO `his_config_info` (`id`, `nid`, `data_id`, `group_id`, `app_name`, `content`, `md5`, `gmt_create`,
                               `gmt_modified`, `src_user`, `src_ip`, `op_type`, `tenant_id`)
VALUES (0, 5, 'waibao-payment.yml', 'DEFAULT_GROUP', '',
        'spring:\n  shardingsphere:\n    datasource:\n      names: master,slave,master-credit-user,master-payment,slave-credit-user,slave-payment\n      master:\n        type: com.zaxxer.hikari.HikariDataSource\n        driverClassName: com.mysql.cj.jdbc.Driver\n        jdbcUrl: jdbc:mysql://10.61.20.211:33306/waibao_v2?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n        username: root\n        password: wssy001\n      slave:\n        type: com.zaxxer.hikari.HikariDataSource\n        driverClassName: com.mysql.cj.jdbc.Driver\n        jdbcUrl: jdbc:mysql://10.61.20.211:33307/waibao_v2?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n        username: root\n        password: wssy001\n      master-credit-user:\n        type: com.zaxxer.hikari.HikariDataSource\n        driverClassName: com.mysql.cj.jdbc.Driver\n        jdbcUrl: jdbc:mysql://10.61.20.211:33306/waibao_credit_user?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n        username: root\n        password: wssy001\n      master-payment:\n        type: com.zaxxer.hikari.HikariDataSource\n        driverClassName: com.mysql.cj.jdbc.Driver\n        jdbcUrl: jdbc:mysql://10.61.20.211:33306/waibao_payment?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n        username: root\n        password: wssy001\n      slave-credit-user:\n        type: com.zaxxer.hikari.HikariDataSource\n        driverClassName: com.mysql.cj.jdbc.Driver\n        jdbcUrl: jdbc:mysql://10.61.20.211:33307/waibao_credit_user?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n        username: root\n        password: wssy001\n      slave-payment:\n        type: com.zaxxer.hikari.HikariDataSource\n        driverClassName: com.mysql.cj.jdbc.Driver\n        jdbcUrl: jdbc:mysql://10.61.20.211:33307/waibao_payment?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n        username: root\n        password: wssy001\n\n    sharding:\n      tables:\n        mq_msg_compensation:\n          actualDataNodes: master.mq_msg_compensation\n        user_credit:\n          actualDataNodes: master-credit-user.user_credit_$->{0..1}\n          tableStrategy:\n            inline:\n              shardingColumn: user_id\n              algorithmExpression: user_credit_$->{user_id % 2}\n        payment:\n          actualDataNodes: master-payment.payment_$->{0..1}\n          tableStrategy:\n            inline:\n              shardingColumn: pay_id\n              algorithmExpression: payment_$->{pay_id % 2}\n\n      master-slave-rules:\n        master:\n          master-data-source-name: master\n          SlaveDataSourceNames: slave\n        master-order-user:\n          master-data-source-name: master-credit-user\n          SlaveDataSourceNames: slave-credit-user\n        master-order-retailer:\n          master-data-source-name: master-payment\n          SlaveDataSourceNames: slave-payment\n  task:\n    execution:\n      pool:\n        max-size: 16\n        keep-alive: \"10s\"\n      thread-name-prefix: 异步任务-\n    scheduling:\n      pool:\n        size: 5\n      thread-name-prefix: 定时任务-\nrocketmq:\n  name-server: 10.61.20.211:39876\n  producer:\n    group: order-producer\n\nmybatis-plus:\n  global-config:\n    db-config:\n      logic-delete-field: enable\n      logic-delete-value: false\n      logic-not-delete-value: true\nlogging:\n  level:\n    root: info\n',
        '0b10ca27ff254b450865d41b972978ab', '2022-03-30 12:12:34', '2022-03-30 12:12:34', NULL, '172.17.0.1', 'I',
        'waibao');
INSERT INTO `his_config_info` (`id`, `nid`, `data_id`, `group_id`, `app_name`, `content`, `md5`, `gmt_create`,
                               `gmt_modified`, `src_user`, `src_ip`, `op_type`, `tenant_id`)
VALUES (0, 6, 'waibao-order.yml', 'DEFAULT_GROUP', '',
        'spring:\n  shardingsphere:\n    datasource:\n      names: master,slave,master-order-user,master-order-retailer,slave-order-user,slave-order-retailer\n      master:\n        type: com.zaxxer.hikari.HikariDataSource\n        driverClassName: com.mysql.cj.jdbc.Driver\n        jdbcUrl: jdbc:mysql://10.61.20.211:33306/waibao_v2?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n        username: root\n        password: wssy001\n      slave:\n        type: com.zaxxer.hikari.HikariDataSource\n        driverClassName: com.mysql.cj.jdbc.Driver\n        jdbcUrl: jdbc:mysql://10.61.20.211:33307/waibao_v2?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n        username: root\n        password: wssy001\n      master-order-user:\n        type: com.zaxxer.hikari.HikariDataSource\n        driverClassName: com.mysql.cj.jdbc.Driver\n        jdbcUrl: jdbc:mysql://10.61.20.211:33306/waibao_order_user?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n        username: root\n        password: wssy001\n      master-order-retailer:\n        type: com.zaxxer.hikari.HikariDataSource\n        driverClassName: com.mysql.cj.jdbc.Driver\n        jdbcUrl: jdbc:mysql://10.61.20.211:33306/waibao_order_retailer?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n        username: root\n        password: wssy001\n      slave-order-user:\n        type: com.zaxxer.hikari.HikariDataSource\n        driverClassName: com.mysql.cj.jdbc.Driver\n        jdbcUrl: jdbc:mysql://10.61.20.211:33307/waibao_order_user?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n        username: root\n        password: wssy001\n      slave-order-retailer:\n        type: com.zaxxer.hikari.HikariDataSource\n        driverClassName: com.mysql.cj.jdbc.Driver\n        jdbcUrl: jdbc:mysql://10.61.20.211:33307/waibao_order_retailer?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n        username: root\n        password: wssy001\n\n    sharding:\n      tables:\n        admin:\n          actualDataNodes: master.admin\n        mq_msg_compensation:\n          actualDataNodes: master.mq_msg_compensation\n        log_order_goods:\n          actualDataNodes: master.log_order_goods\n        seckill_goods:\n          actualDataNodes: master.seckill_goods\n        order_user:\n          actualDataNodes: master-order-user.order_user_$->{0..1}\n          tableStrategy:\n            inline:\n              shardingColumn: user_id\n              algorithmExpression: order_user_$->{user_id % 2}\n        order_retailer:\n          actualDataNodes: master-order-retailer.order_retailer_$->{0..1}\n          tableStrategy:\n            inline:\n              shardingColumn: retailer_id\n              algorithmExpression: order_retailer_$->{retailer_id % 2}\n\n      master-slave-rules:\n        master:\n          master-data-source-name: master\n          SlaveDataSourceNames: slave\n        master-order-user:\n          master-data-source-name: master-order-user\n          SlaveDataSourceNames: slave-order-user\n        master-order-retailer:\n          master-data-source-name: master-order-retailer\n          SlaveDataSourceNames: slave-order-retailer\n  task:\n    execution:\n      pool:\n        max-size: 16\n        keep-alive: \"10s\"\n      thread-name-prefix: 异步任务-\n    scheduling:\n      pool:\n        size: 5\n      thread-name-prefix: 定时任务-\nrocketmq:\n  name-server: 10.61.20.211:39876\n  producer:\n    group: order-producer\n\nmybatis-plus:\n  global-config:\n    db-config:\n      logic-delete-field: enable\n      logic-delete-value: false\n      logic-not-delete-value: true\n',
        'e0081f263be1f0d374dc47550f91f63f', '2022-03-30 12:12:34', '2022-03-30 12:12:34', NULL, '172.17.0.1', 'I',
        'waibao');
INSERT INTO `his_config_info` (`id`, `nid`, `data_id`, `group_id`, `app_name`, `content`, `md5`, `gmt_create`,
                               `gmt_modified`, `src_user`, `src_ip`, `op_type`, `tenant_id`)
VALUES (0, 7, 'waibao-seckill.yml', 'DEFAULT_GROUP', '',
        'spring:\n  shardingsphere:\n    datasource:\n      names: master,slave\n      master:\n        type: com.zaxxer.hikari.HikariDataSource\n        driverClassName: com.mysql.cj.jdbc.Driver\n        jdbcUrl: jdbc:mysql://10.61.20.211:33306/waibao_v2?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n        username: root\n        password: wssy001\n      slave:\n        type: com.zaxxer.hikari.HikariDataSource\n        driverClassName: com.mysql.cj.jdbc.Driver\n        jdbcUrl: jdbc:mysql://10.61.20.211:33307/waibao_v2?useSSL=false&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true\n        username: root\n        password: wssy001\n\n    sharding:\n      tables:\n        user:\n          actualDataNodes: master.user_$->{0..1}\n          tableStrategy:\n            inline:\n              shardingColumn: id\n              algorithmExpression: user_$->{id % 2}\n          keyGenerator:\n            column: id\n            type: SNOWFLAKE\n\n      master-slave-rules:\n        master:\n          master-data-source-name: master\n          SlaveDataSourceNames: slave\n  task:\n    execution:\n      pool:\n        max-size: 20\n        keep-alive: 60s\n        core-size: 10\n        queue-capacity: 200\n      thread-name-prefix: 异步任务-\n    scheduling:\n      pool:\n        size: 5\n      thread-name-prefix: 定时任务-\nrocketmq:\n  name-server: 10.61.20.211:39876\n  producer:\n    group: order-producer\n\naj:\n  captcha:\n    cache-type: redis\n    interference-options: 2\n    type: blockpuzzle',
        '46812f71c772fc74eae1c38deed203e5', '2022-03-30 12:12:34', '2022-03-30 12:12:34', NULL, '172.17.0.1', 'I',
        'waibao');
COMMIT;

-- ----------------------------
-- Table structure for permissions
-- ----------------------------
DROP TABLE IF EXISTS `permissions`;
CREATE TABLE `permissions`
(
    `role`     varchar(50)  NOT NULL,
    `resource` varchar(255) NOT NULL,
    `action`   varchar(8)   NOT NULL,
    UNIQUE KEY `uk_role_permission` (`role`, `resource`, `action`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- ----------------------------
-- Records of permissions
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for roles
-- ----------------------------
DROP TABLE IF EXISTS `roles`;
CREATE TABLE `roles`
(
    `username` varchar(50) NOT NULL,
    `role`     varchar(50) NOT NULL,
    UNIQUE KEY `idx_user_role` (`username`, `role`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- ----------------------------
-- Records of roles
-- ----------------------------
BEGIN;
INSERT INTO `roles` (`username`, `role`)
VALUES ('nacos', 'ROLE_ADMIN');
COMMIT;

-- ----------------------------
-- Table structure for tenant_capacity
-- ----------------------------
DROP TABLE IF EXISTS `tenant_capacity`;
CREATE TABLE `tenant_capacity`
(
    `id`                bigint(20) unsigned           NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`         varchar(128) COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT 'Tenant ID',
    `quota`             int(10) unsigned              NOT NULL DEFAULT '0' COMMENT '配额，0表示使用默认值',
    `usage`             int(10) unsigned              NOT NULL DEFAULT '0' COMMENT '使用量',
    `max_size`          int(10) unsigned              NOT NULL DEFAULT '0' COMMENT '单个配置大小上限，单位为字节，0表示使用默认值',
    `max_aggr_count`    int(10) unsigned              NOT NULL DEFAULT '0' COMMENT '聚合子配置最大个数',
    `max_aggr_size`     int(10) unsigned              NOT NULL DEFAULT '0' COMMENT '单个聚合数据的子配置大小上限，单位为字节，0表示使用默认值',
    `max_history_count` int(10) unsigned              NOT NULL DEFAULT '0' COMMENT '最大变更历史数量',
    `gmt_create`        datetime                      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`      datetime                      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_id` (`tenant_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  COLLATE = utf8_bin COMMENT ='租户容量信息表';

-- ----------------------------
-- Records of tenant_capacity
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for tenant_info
-- ----------------------------
DROP TABLE IF EXISTS `tenant_info`;
CREATE TABLE `tenant_info`
(
    `id`            bigint(20)                    NOT NULL AUTO_INCREMENT COMMENT 'id',
    `kp`            varchar(128) COLLATE utf8_bin NOT NULL COMMENT 'kp',
    `tenant_id`     varchar(128) COLLATE utf8_bin DEFAULT '' COMMENT 'tenant_id',
    `tenant_name`   varchar(128) COLLATE utf8_bin DEFAULT '' COMMENT 'tenant_name',
    `tenant_desc`   varchar(256) COLLATE utf8_bin DEFAULT NULL COMMENT 'tenant_desc',
    `create_source` varchar(32) COLLATE utf8_bin  DEFAULT NULL COMMENT 'create_source',
    `gmt_create`    bigint(20)                    NOT NULL COMMENT '创建时间',
    `gmt_modified`  bigint(20)                    NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_info_kptenantid` (`kp`, `tenant_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 2
  DEFAULT CHARSET = utf8
  COLLATE = utf8_bin COMMENT ='tenant_info';

-- ----------------------------
-- Records of tenant_info
-- ----------------------------
BEGIN;
INSERT INTO `tenant_info` (`id`, `kp`, `tenant_id`, `tenant_name`, `tenant_desc`, `create_source`, `gmt_create`,
                           `gmt_modified`)
VALUES (1, '1', 'waibao', 'waibao', '外包', 'nacos', 1648642339859, 1648642339859);
COMMIT;

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`
(
    `username` varchar(50)  NOT NULL,
    `password` varchar(500) NOT NULL,
    `enabled`  tinyint(1)   NOT NULL,
    PRIMARY KEY (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- ----------------------------
-- Records of users
-- ----------------------------
BEGIN;
INSERT INTO `users` (`username`, `password`, `enabled`)
VALUES ('nacos', '$2a$10$EuWPZHzz32dJN7jexM34MOeYirDdFAZm2kuWj7VEOJhhZkDrxfvUu', 1);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
