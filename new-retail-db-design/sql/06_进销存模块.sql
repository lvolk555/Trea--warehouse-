-- ============================================
-- 新零售数据库 - 06. 进销存模块
-- ============================================

USE neti;

-- 供应商表
CREATE TABLE t_supplier (
    sup_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '供应商ID',
    sup_code VARCHAR(20) NOT NULL COMMENT '供应商编号',
    sup_name VARCHAR(100) NOT NULL COMMENT '供应商名称',
    sup_type TINYINT UNSIGNED NOT NULL COMMENT '类型(1厂家2代理商)',
    linkman VARCHAR(50) COMMENT '联系人',
    tel VARCHAR(20) COMMENT '联系电话',
    address VARCHAR(200) COMMENT '联系地址',
    bank_name VARCHAR(100) COMMENT '开户银行',
    bank_account VARCHAR(50) COMMENT '银行账号',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态',
    UNIQUE KEY unq_sup_code (sup_code),
    INDEX idx_sup_type (sup_type)
) COMMENT '供应商表';

-- 供应商商品关联表
CREATE TABLE t_supplier_sku (
    sup_id INT UNSIGNED NOT NULL COMMENT '供应商ID',
    sku_id INT UNSIGNED NOT NULL COMMENT '商品ID',
    supply_price DECIMAL(10,2) UNSIGNED NOT NULL COMMENT '供货价',
    PRIMARY KEY (sup_id, sku_id)
) COMMENT '供应商商品关联表';

-- 采购表
CREATE TABLE t_purchase (
    pur_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '采购ID',
    pur_code VARCHAR(50) NOT NULL COMMENT '采购单号',
    sku_id INT UNSIGNED NOT NULL COMMENT '商品ID',
    number INT UNSIGNED NOT NULL COMMENT '采购数量',
    warehouse_id INT UNSIGNED NOT NULL COMMENT '目标仓库ID',
    supplier_id INT UNSIGNED NOT NULL COMMENT '供应商ID',
    purchaser_id INT UNSIGNED NOT NULL COMMENT '采购员ID',
    pur_price DECIMAL(10,2) UNSIGNED NOT NULL COMMENT '采购单价',
    total_amount DECIMAL(10,2) UNSIGNED NOT NULL COMMENT '总金额',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态(0待确认1已确认2已到货)',
    remark VARCHAR(500) COMMENT '备注',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY unq_pur_code (pur_code),
    INDEX idx_sku_id (sku_id),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_supplier_id (supplier_id),
    INDEX idx_purchaser_id (purchaser_id)
) COMMENT '采购表';

-- 入库表
CREATE TABLE t_stock_in (
    in_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '入库ID',
    in_code VARCHAR(50) NOT NULL COMMENT '入库单号',
    warehouse_id INT UNSIGNED NOT NULL COMMENT '仓库ID',
    operator_id INT UNSIGNED NOT NULL COMMENT '操作员ID',
    total_amount DECIMAL(10,2) UNSIGNED NOT NULL COMMENT '入库总金额',
    pay_type TINYINT UNSIGNED COMMENT '支付方式',
    is_invoice TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否开票(0否1是)',
    remark VARCHAR(500) COMMENT '备注',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY unq_in_code (in_code),
    INDEX idx_warehouse_id (warehouse_id)
) COMMENT '入库表';

-- 入库详情表
CREATE TABLE t_stock_in_detail (
    in_id INT UNSIGNED NOT NULL COMMENT '入库ID',
    sku_id INT UNSIGNED NOT NULL COMMENT '商品ID',
    number INT UNSIGNED NOT NULL COMMENT '入库数量',
    unit_price DECIMAL(10,2) UNSIGNED NOT NULL COMMENT '入库单价',
    PRIMARY KEY (in_id, sku_id)
) COMMENT '入库详情表';

-- 采购入库关联表
CREATE TABLE t_purchase_stock_in (
    pur_id INT UNSIGNED NOT NULL COMMENT '采购ID',
    in_id INT UNSIGNED NOT NULL COMMENT '入库ID',
    PRIMARY KEY (pur_id, in_id)
) COMMENT '采购入库关联表';

SELECT '进销存模块数据表创建成功！' AS message;
