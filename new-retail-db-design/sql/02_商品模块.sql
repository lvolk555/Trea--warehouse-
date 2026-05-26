-- ============================================
-- 新零售数据库 - 02. 商品模块
-- ============================================

USE neti;

-- 品类表
CREATE TABLE t_spec_group (
    spg_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '品类ID',
    spg_name VARCHAR(50) NOT NULL COMMENT '品类名称',
    UNIQUE KEY unq_spg_name (spg_name)
) COMMENT '品类表';

-- 参数表
CREATE TABLE t_spec_param (
    spp_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '参数ID',
    spg_id INT UNSIGNED NOT NULL COMMENT '品类ID',
    spp_name VARCHAR(50) NOT NULL COMMENT '参数名称',
    spp_unit VARCHAR(20) COMMENT '参数单位',
    is_key TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否关键参数(0否1是)',
    is_sale TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否销售属性(0否1是)',
    INDEX idx_spg_id (spg_id)
) COMMENT '参数表';

-- 品牌表
CREATE TABLE t_brand (
    brand_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '品牌ID',
    brand_name VARCHAR(50) NOT NULL COMMENT '品牌名称',
    brand_image VARCHAR(200) COMMENT '商标图片路径',
    brand_letter CHAR(1) NOT NULL COMMENT '品牌首字母',
    UNIQUE KEY unq_brand_name (brand_name),
    INDEX idx_brand_letter (brand_letter)
) COMMENT '品牌表';

-- 分类表
CREATE TABLE t_category (
    cate_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    cate_name VARCHAR(50) NOT NULL COMMENT '分类名称',
    parent_id INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父分类ID',
    level TINYINT UNSIGNED NOT NULL COMMENT '分类层级',
    INDEX idx_parent_id (parent_id)
) COMMENT '分类表';

-- 品牌分类关联表
CREATE TABLE t_brand_category (
    brand_id INT UNSIGNED NOT NULL COMMENT '品牌ID',
    cate_id INT UNSIGNED NOT NULL COMMENT '分类ID',
    PRIMARY KEY (brand_id, cate_id)
) COMMENT '品牌分类关联表';

-- 产品表(SPU)
CREATE TABLE t_product (
    prod_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '产品ID',
    prod_title VARCHAR(100) NOT NULL COMMENT '产品标题',
    prod_subtitle VARCHAR(200) COMMENT '产品副标题',
    cate_id INT UNSIGNED NOT NULL COMMENT '分类ID',
    brand_id INT UNSIGNED COMMENT '品牌ID',
    spg_id INT UNSIGNED NOT NULL COMMENT '品类ID',
    is_saleable TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否上架(0否1是)',
    is_valid TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否有效(0否1是)',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_cate_id (cate_id),
    INDEX idx_brand_id (brand_id),
    INDEX idx_spg_id (spg_id)
) COMMENT '产品表';

-- 商品表(SKU)
CREATE TABLE t_sku (
    sku_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
    prod_id INT UNSIGNED NOT NULL COMMENT '产品ID',
    sku_title VARCHAR(100) NOT NULL COMMENT '商品标题',
    sku_images JSON COMMENT '商品图片JSON数组',
    sku_price DECIMAL(10,2) UNSIGNED NOT NULL COMMENT '商品价格',
    sku_params JSON NOT NULL COMMENT '参数规格JSON',
    sku_status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '商品状态',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_prod_id (prod_id)
) COMMENT '商品表';

-- 仓库表
CREATE TABLE t_warehouse (
    wh_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '仓库ID',
    city_id INT UNSIGNED NOT NULL COMMENT '城市编号',
    wh_address VARCHAR(200) NOT NULL COMMENT '仓库地址',
    wh_tel VARCHAR(20) COMMENT '联系电话',
    INDEX idx_city_id (city_id)
) COMMENT '仓库表';

-- 仓库商品库存表
CREATE TABLE t_warehouse_sku (
    wh_id INT UNSIGNED NOT NULL COMMENT '仓库ID',
    sku_id INT UNSIGNED NOT NULL COMMENT '商品ID',
    stock INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '库存数量',
    warning_stock INT UNSIGNED NOT NULL DEFAULT 10 COMMENT '预警库存',
    PRIMARY KEY (wh_id, sku_id)
) COMMENT '仓库商品库存表';

-- 零售店表
CREATE TABLE t_shop (
    shop_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '店铺ID',
    city_id INT UNSIGNED NOT NULL COMMENT '城市编号',
    shop_name VARCHAR(50) NOT NULL COMMENT '店铺名称',
    shop_address VARCHAR(200) NOT NULL COMMENT '店铺地址',
    shop_tel VARCHAR(20) COMMENT '联系电话',
    INDEX idx_city_id (city_id)
) COMMENT '零售店表';

-- 店铺商品库存表
CREATE TABLE t_shop_sku (
    shop_id INT UNSIGNED NOT NULL COMMENT '店铺ID',
    sku_id INT UNSIGNED NOT NULL COMMENT '商品ID',
    stock INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '库存数量',
    PRIMARY KEY (shop_id, sku_id)
) COMMENT '店铺商品库存表';

SELECT '商品模块数据表创建成功！' AS message;
