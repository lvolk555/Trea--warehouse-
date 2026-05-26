-- ============================================
-- 新零售数据库 - 04. 订单模块
-- ============================================

USE neti;

-- 订单表
CREATE TABLE t_order (
    order_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    order_code VARCHAR(50) NOT NULL COMMENT '订单流水号',
    cust_id INT UNSIGNED NOT NULL COMMENT '客户ID',
    shop_id INT UNSIGNED COMMENT '店铺ID（实体店订单）',
    type TINYINT UNSIGNED NOT NULL COMMENT '销售方式(1实体店2线上)',
    amount DECIMAL(10,2) UNSIGNED NOT NULL COMMENT '商品总金额',
    actual_amount DECIMAL(10,2) UNSIGNED NOT NULL COMMENT '实际支付金额',
    coupon_id INT UNSIGNED COMMENT '使用的购物券ID',
    freight DECIMAL(10,2) UNSIGNED COMMENT '运费',
    weight DECIMAL(8,2) UNSIGNED COMMENT '商品总重量(kg)',
    pay_type TINYINT UNSIGNED COMMENT '支付方式',
    status TINYINT UNSIGNED NOT NULL COMMENT '订单状态',
    receiver_name VARCHAR(50) COMMENT '收货人姓名',
    receiver_tel VARCHAR(11) COMMENT '收货人电话',
    receiver_address VARCHAR(200) COMMENT '收货地址',
    remark VARCHAR(500) COMMENT '订单备注',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    pay_time TIMESTAMP COMMENT '支付时间',
    deliver_time TIMESTAMP COMMENT '发货时间',
    receive_time TIMESTAMP COMMENT '收货时间',
    UNIQUE KEY unq_order_code (order_code),
    INDEX idx_cust_id (cust_id),
    INDEX idx_shop_id (shop_id),
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) COMMENT '订单表';

-- 订单详情表
CREATE TABLE t_order_detail (
    order_id INT UNSIGNED NOT NULL COMMENT '订单ID',
    sku_id INT UNSIGNED NOT NULL COMMENT '商品ID',
    price DECIMAL(10,2) UNSIGNED NOT NULL COMMENT '商品原价',
    actual_price DECIMAL(10,2) UNSIGNED NOT NULL COMMENT '实际购买价格',
    number INT UNSIGNED NOT NULL COMMENT '购买数量',
    PRIMARY KEY (order_id, sku_id)
) COMMENT '订单详情表';

-- 快递表
CREATE TABLE t_delivery (
    del_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '快递ID',
    order_id INT UNSIGNED NOT NULL COMMENT '订单ID',
    sku_list JSON NOT NULL COMMENT '快递商品列表(JSON)',
    company VARCHAR(50) NOT NULL COMMENT '快递公司',
    tracking_no VARCHAR(50) NOT NULL COMMENT '快递单号',
    sender_name VARCHAR(50) NOT NULL COMMENT '发货人姓名',
    sender_tel VARCHAR(11) NOT NULL COMMENT '发货人电话',
    sender_address VARCHAR(200) NOT NULL COMMENT '发货地址',
    receiver_name VARCHAR(50) NOT NULL COMMENT '收货人姓名',
    receiver_tel VARCHAR(11) NOT NULL COMMENT '收货人电话',
    receiver_address VARCHAR(200) NOT NULL COMMENT '收货地址',
    freight DECIMAL(8,2) UNSIGNED NOT NULL COMMENT '运费',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_order_id (order_id),
    INDEX idx_tracking_no (tracking_no)
) COMMENT '快递表';

-- 退货表
CREATE TABLE t_return (
    ret_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '退货ID',
    order_id INT UNSIGNED NOT NULL COMMENT '订单ID',
    sku_id INT UNSIGNED NOT NULL COMMENT '商品ID',
    cust_id INT UNSIGNED NOT NULL COMMENT '客户ID',
    reason VARCHAR(500) NOT NULL COMMENT '退货原因',
    description VARCHAR(1000) COMMENT '问题描述',
    images JSON COMMENT '凭证图片(JSON数组)',
    refund_amount DECIMAL(10,2) UNSIGNED NOT NULL COMMENT '退款金额',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态',
    handler_id INT UNSIGNED COMMENT '处理人ID',
    handle_time TIMESTAMP COMMENT '处理时间',
    handle_remark VARCHAR(500) COMMENT '处理备注',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_order_id (order_id),
    INDEX idx_cust_id (cust_id),
    INDEX idx_status (status)
) COMMENT '退货表';

-- 评价表
CREATE TABLE t_rating (
    rating_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '评价ID',
    order_id INT UNSIGNED NOT NULL COMMENT '订单ID',
    sku_id INT UNSIGNED NOT NULL COMMENT '商品ID',
    cust_id INT UNSIGNED NOT NULL COMMENT '客户ID',
    rating_star TINYINT UNSIGNED NOT NULL COMMENT '评分(1-5星)',
    rating_content VARCHAR(1000) COMMENT '评价内容',
    rating_images JSON COMMENT '晒图(JSON数组)',
    is_anonymous TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否匿名(0否1是)',
    reply_content VARCHAR(1000) COMMENT '商家回复',
    reply_time TIMESTAMP COMMENT '回复时间',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_order_id (order_id),
    INDEX idx_sku_id (sku_id),
    INDEX idx_cust_id (cust_id)
) COMMENT '评价表';

SELECT '订单模块数据表创建成功！' AS message;
