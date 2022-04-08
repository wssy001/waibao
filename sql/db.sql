create schema if not exists waibao_credit_user collate utf8_general_ci;

create schema if not exists waibao_order_retailer collate utf8mb4_general_ci;

create schema if not exists waibao_order_user collate utf8mb4_general_ci;

create schema if not exists waibao_payment collate utf8_general_ci;

create schema if not exists waibao_v2 collate utf8mb4_general_ci;

create table if not exists waibao_v2.admin
(
    id bigint(12) unsigned auto_increment comment '自增ID'
        primary key,
    name varchar(255) default '0' not null comment '管理员名称',
    password varchar(255) not null comment '密码',
    level tinyint unsigned default 1 null comment '管理员级别',
    create_time datetime not null comment '创建时间',
    update_time datetime not null comment '更新时间'
)
    comment '管理员表';

create table if not exists waibao_v2.deposit
(
    id bigint(12) unsigned auto_increment comment '自增ID'
        primary key,
    user_id bigint(12) unsigned not null comment '还款人ID',
    due_date datetime not null comment '欠款期限',
    debt_amount decimal(10,2) unsigned not null comment '欠债金额',
    deposit_amount decimal(10,2) unsigned not null comment '还款金额',
    deposit_date datetime not null comment '还款日',
    create_time datetime not null comment '创建时间',
    update_time datetime not null comment '更新时间'
);

create index deposit_user_id_index
    on waibao_v2.deposit (user_id);

create table if not exists waibao_v2.goods
(
    id bigint(12) unsigned auto_increment comment '自增ID'
        primary key,
    name varchar(255) null comment '商品名',
    description varchar(255) null comment '描述',
    start_time datetime null comment '生效时间',
    end_time datetime null comment '失效时间',
    price decimal(10,2) null comment '金额',
    create_time datetime not null comment '创建时间',
    update_time datetime not null comment '更新时间'
);

create table if not exists waibao_v2.log_order_goods
(
    id bigint(12) unsigned auto_increment comment '自增ID'
        primary key,
    order_id varchar(255) not null comment '订单ID',
    goods_id bigint(12) unsigned not null comment '商品ID',
    user_id bigint(12) unsigned not null comment '购买者ID',
    retailer_id bigint(12) unsigned null comment '卖家ID',
    tags varchar(255) not null comment '标签',
    goods_price decimal(10,2) unsigned not null comment '商品金额',
    count int(7) unsigned default 1 not null comment '购买数量',
    order_price decimal(10,2) unsigned not null comment '订单金额',
    purchase_time datetime null comment '购买时间',
    paid tinyint(1) unsigned default 0 not null comment '是否支付',
    status varchar(255) null comment '状态'
);

create table if not exists waibao_payment.log_payment
(
    id bigint(12) unsigned auto_increment comment '自增id'
        primary key,
    pay_id varchar(50) not null comment '支付id',
    user_id bigint(12) unsigned not null comment '用户编号',
    order_id varchar(255) not null comment '订单id',
    goods_id bigint(12) unsigned not null comment '商品id',
    money decimal(10,2) not null comment '支付金额',
    operation varchar(10) charset utf8mb4 not null comment '操作类型',
    create_time datetime not null comment '创建时间',
    update_time datetime not null comment '修改时间'
);

create table if not exists waibao_v2.log_seckill_goods
(
    id bigint(12) unsigned auto_increment comment '自增ID'
        primary key,
    retailer_id bigint(12) unsigned not null comment '卖家ID',
    price decimal(10,2) unsigned not null comment '原价',
    seckill_price decimal(10,2) unsigned not null comment '秒杀价',
    storage int(7) unsigned not null comment '库存量',
    purchase_limit int(3) unsigned default 1 null comment '每位顾客可购买量',
    seckill_start_time datetime not null comment '秒杀开始时间',
    seckill_end_time datetime not null comment '秒杀结束时间',
    goods_id bigint(12) null comment '商品ID',
    operation varchar(255) not null comment '操作类型'
);

create table if not exists waibao_credit_user.log_user_credit
(
    id bigint(12) auto_increment comment '自增id'
        primary key,
    user_id bigint(12) not null comment '用户id',
    pay_id varchar(50) not null comment '交易id',
    order_id varchar(255) charset utf8mb4 not null comment '订单ID',
    old_money decimal(10,2) not null comment '先前的余额',
    money decimal(10,2) not null comment '余额',
    operation varchar(10) charset utf8mb4 not null comment '操作类型',
    create_time datetime not null comment '创建时间',
    update_time datetime not null comment '修改时间'
);

create table if not exists waibao_v2.mq_msg_compensation
(
    msg_id varchar(255) not null comment '消息id'
        primary key,
    topic varchar(50) not null comment 'topic',
    tags varchar(255) not null comment 'tags',
    status varchar(255) default '补偿消息未发送' not null comment '消息状态',
    business_key varchar(255) null comment '业务key',
    content text not null comment '消息内容（JSON）',
    exception_msg text not null comment '异常信息',
    create_time datetime not null comment '创建时间',
    update_time datetime not null comment '更新时间',
    constraint mq_msg_compensation_business_key_uindex
        unique (business_key)
);

create index mq_msg_compensation_status_index
    on waibao_v2.mq_msg_compensation (status);

create index mq_msg_compensation_tags_index
    on waibao_v2.mq_msg_compensation (tags);

create index mq_msg_compensation_topic_index
    on waibao_v2.mq_msg_compensation (topic);

create table if not exists waibao_order_retailer.order_retailer_0
(
    order_id varchar(255) not null comment '订单ID'
        primary key,
    goods_id bigint(12) unsigned not null comment '商品ID',
    user_id bigint(12) unsigned not null comment '购买者ID',
    retailer_id bigint(12) unsigned null comment '卖家ID',
    goods_price decimal(10,2) unsigned not null comment '商品金额',
    count int(7) unsigned default 1 not null comment '购买数量',
    order_price decimal(10,2) unsigned not null comment '订单金额',
    purchase_time datetime null comment '购买时间',
    paid tinyint(1) unsigned default 0 not null comment '是否支付',
    STATUS varchar(255) null comment '状态',
    ENABLE tinyint(1) unsigned default 1 not null comment '逻辑删除',
    create_time datetime not null comment '创建时间',
    update_time datetime not null comment '更新时间'
);

create table if not exists waibao_order_retailer.order_retailer_1
(
    order_id varchar(255) not null comment '订单ID'
        primary key,
    goods_id bigint(12) unsigned not null comment '商品ID',
    user_id bigint(12) unsigned not null comment '购买者ID',
    retailer_id bigint(12) unsigned null comment '卖家ID',
    goods_price decimal(10,2) unsigned not null comment '商品金额',
    count int(7) unsigned default 1 not null comment '购买数量',
    order_price decimal(10,2) unsigned not null comment '订单金额',
    purchase_time datetime null comment '购买时间',
    paid tinyint(1) unsigned default 0 not null comment '是否支付',
    STATUS varchar(255) null comment '状态',
    ENABLE tinyint(1) unsigned default 1 not null comment '逻辑删除',
    create_time datetime not null comment '创建时间',
    update_time datetime not null comment '更新时间'
);

create table if not exists waibao_order_user.order_user_0
(
    order_id varchar(255) not null comment '订单ID'
        primary key,
    pay_id varchar(50) not null comment '支付单ID',
    goods_id bigint(12) unsigned not null comment '商品ID',
    user_id bigint(12) unsigned not null comment '购买者ID',
    retailer_id bigint(12) unsigned null comment '卖家ID',
    goods_price decimal(10,2) unsigned not null comment '商品金额',
    count int(7) unsigned default 1 not null comment '购买数量',
    order_price decimal(10,2) unsigned not null comment '订单金额',
    purchase_time datetime null comment '购买时间',
    paid tinyint(1) unsigned default 0 not null comment '是否支付',
    status varchar(255) null comment '状态',
    enable tinyint(1) unsigned default 1 not null comment '逻辑删除',
    create_time datetime not null comment '创建时间',
    update_time datetime not null comment '更新时间'
);

create table if not exists waibao_order_user.order_user_1
(
    order_id varchar(255) not null comment '订单ID'
        primary key,
    pay_id varchar(50) not null comment '支付单ID',
    goods_id bigint(12) unsigned not null comment '商品ID',
    user_id bigint(12) unsigned not null comment '购买者ID',
    retailer_id bigint(12) unsigned null comment '卖家ID',
    goods_price decimal(10,2) unsigned not null comment '商品金额',
    count int(7) unsigned default 1 not null comment '购买数量',
    order_price decimal(10,2) unsigned not null comment '订单金额',
    purchase_time datetime null comment '购买时间',
    paid tinyint(1) unsigned default 0 not null comment '是否支付',
    status varchar(255) null comment '状态',
    enable tinyint(1) unsigned default 1 not null comment '逻辑删除',
    create_time datetime not null comment '创建时间',
    update_time datetime not null comment '更新时间'
);

create table if not exists waibao_payment.payment_0
(
    id bigint(12) unsigned auto_increment comment '自增id'
        primary key,
    pay_id varchar(50) not null comment '支付id',
    user_id bigint(12) unsigned not null comment '用户编号',
    order_id varchar(255) not null comment '订单id',
    goods_id bigint(12) unsigned not null comment '商品id',
    money decimal(10,2) not null comment '支付金额',
    create_time datetime not null comment '创建时间',
    update_time datetime not null comment '修改时间'
)
    comment '支付记录表' charset=utf8mb4;

create table if not exists waibao_payment.payment_1
(
    id bigint(12) unsigned auto_increment comment '自增id'
        primary key,
    pay_id varchar(50) not null comment '支付id',
    user_id bigint(12) unsigned not null comment '用户编号',
    order_id varchar(255) not null comment '订单id',
    goods_id bigint(12) unsigned not null comment '商品id',
    money decimal(10,2) not null comment '支付金额',
    create_time datetime not null comment '创建时间',
    update_time datetime not null comment '修改时间'
)
    comment '支付记录表' charset=utf8mb4;

create table if not exists waibao_v2.rule
(
    id bigint(12) unsigned auto_increment comment '自增ID'
        primary key,
    goods_id bigint(12) unsigned not null comment '秒杀商品id',
    rule_code tinyint(2) unsigned not null comment '规则码',
    allow_overdue_delayed_days int(3) unsigned default 0 not null comment '允许延迟几天还款',
    deny_defaulter tinyint(1) unsigned default 1 not null comment '拒绝失信人',
    deny_overdue_times int(2) unsigned null comment '拒绝拥有几条逾期记录',
    collect_years int(2) unsigned null comment '逾期记录统计年限',
    ignore_overdue_amount decimal(10,2) unsigned null comment '忽略欠款多少的逾期记录',
    deny_age_below int(2) null comment '拒绝低于这个年龄的',
    user_type int(2) unsigned default 0 not null comment '用户类型',
    create_time datetime not null comment '创建时间',
    update_time datetime not null comment '更新时间',
    deny_work_status varchar(255) null comment '拒绝这类工作状态的人'
);

create index rule_goods_id_index
    on waibao_v2.rule (goods_id);

create table if not exists waibao_v2.seckill_goods
(
    id bigint(12) unsigned auto_increment comment '自增ID'
        primary key,
    goods_id bigint(12) not null comment '商品ID',
    retailer_id bigint(12) unsigned not null comment '卖家ID',
    price decimal(10,2) unsigned not null comment '原价',
    seckill_price decimal(10,2) unsigned not null comment '秒杀价',
    storage int(7) unsigned not null comment '库存量',
    purchase_limit int(3) unsigned default 1 null comment '每位顾客可购买量',
    seckill_start_time datetime not null comment '秒杀开始时间',
    seckill_end_time datetime not null comment '秒杀结束时间'
);

create table if not exists waibao_v2.user_0
(
    id bigint(12) unsigned not null comment '自增ID'
        primary key,
    mobile varchar(11) not null comment '手机号',
    email varchar(255) null comment '邮箱地址',
    password varchar(255) not null comment '密码',
    sex tinyint(2) unsigned default 3 null comment '性别',
    nickname varchar(50) null comment '用户昵称',
    create_time datetime not null comment '创建时间',
    update_time datetime not null comment '更新时间'
)
    comment '用户基本信息表';

create table if not exists waibao_v2.user_1
(
    id bigint(12) unsigned not null comment '自增ID'
        primary key,
    mobile varchar(11) not null comment '手机号',
    email varchar(255) null comment '邮箱地址',
    password varchar(255) not null comment '密码',
    sex tinyint(2) unsigned default 3 null comment '性别',
    nickname varchar(50) null comment '用户昵称',
    create_time datetime not null comment '创建时间',
    update_time datetime not null comment '更新时间'
)
    comment '用户基本信息表';

create table if not exists waibao_credit_user.user_credit_0
(
    user_id bigint(12) unsigned not null comment '用户id'
        primary key,
    money decimal(10,2) not null comment '余额',
    create_time datetime not null comment '创建时间',
    update_time datetime not null comment '修改时间'
)
    comment '账户信息表' charset=utf8mb4;

create table if not exists waibao_credit_user.user_credit_1
(
    user_id bigint(12) unsigned not null comment '用户id'
        primary key,
    money decimal(10,2) not null comment '余额',
    create_time datetime not null comment '创建时间',
    update_time datetime not null comment '修改时间'
)
    comment '账户信息表' charset=utf8mb4;

create table if not exists waibao_v2.user_extra
(
    user_id bigint(12) unsigned not null comment '用户ID'
        primary key,
    defaulter tinyint(1) unsigned not null comment '是否为失信人',
    age tinyint unsigned not null comment '年龄',
    work_status varchar(255) default '无业/失业' not null comment '工作状态',
    create_time datetime not null comment '创建时间',
    update_time datetime not null comment '更新时间'
);