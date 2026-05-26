-- ============================================
-- 新零售数据库 - 03. 客户模块
-- ============================================

USE neti;

-- 会员等级表
CREATE TABLE t_level (
    level_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '等级ID',
    level_name VARCHAR(20) NOT NULL COMMENT '等级名称',
    discount DECIMAL(3,2) UNSIGNED NOT NULL COMMENT '折扣率(0.00-1.00)',
    UNIQUE KEY unq_level_name (level_name)
) COMMENT '会员等级表';

-- 客户表
CREATE TABLE t_customer (
    cust_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '客户ID',
    cust_name VARCHAR(50) NOT NULL COMMENT '客户姓名',
    cust_mobile VARCHAR(11) NOT NULL COMMENT '手机号',
    cust_email VARCHAR(100) COMMENT '邮箱',
    cust_password VARCHAR(200) COMMENT '登录密码',
    level_id INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '会员等级ID',
    cust_integral INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '积分',
    cust_balance DECIMAL(10,2) UNSIGNED NOT NULL DEFAULT 0.00 COMMENT '账户余额',
    cust_status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态(0禁用1启用)',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY unq_cust_mobile (cust_mobile),
    INDEX idx_level_id (level_id)
) COMMENT '客户表';

-- 购物券表
CREATE TABLE t_coupon (
    coupon_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '购物券ID',
    coupon_name VARCHAR(50) NOT NULL COMMENT '购物券名称',
    coupon_type TINYINT UNSIGNED NOT NULL COMMENT '类型(1满减2折扣)',
    coupon_value DECIMAL(10,2) UNSIGNED NOT NULL COMMENT '优惠值',
    min_amount DECIMAL(10,2) UNSIGNED COMMENT '最低消费金额',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    total_num INT UNSIGNED NOT NULL COMMENT '发放总数',
    remain_num INT UNSIGNED NOT NULL COMMENT '剩余数量',
    coupon_status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态',
    INDEX idx_type (coupon_type),
    INDEX idx_dates (start_date, end_date)
) COMMENT '购物券表';

-- 客户购物券关联表
CREATE TABLE t_customer_coupon (
    cust_id INT UNSIGNED NOT NULL COMMENT '客户ID',
    coupon_id INT UNSIGNED NOT NULL COMMENT '购物券ID',
    receive_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    use_time TIMESTAMP NULL COMMENT '使用时间',
    order_id INT UNSIGNED COMMENT '关联订单ID',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态(0未使用1已使用2已过期)',
    PRIMARY KEY (cust_id, coupon_id, receive_time)
) COMMENT '客户购物券关联表';

SELECT '客户模块数据表创建成功！' AS message;
