-- ============================================
-- 新零售数据库 - 示例数据插入脚本
-- ============================================

USE neti;

-- 插入品类数据
INSERT INTO t_spec_group (spg_name) VALUES 
('手机'), ('电脑'), ('数码配件'), ('服装'), ('鞋靴');

-- 插入参数数据
INSERT INTO t_spec_param (spg_id, spp_name, spp_unit, is_key, is_sale) VALUES 
(1, '屏幕尺寸', '英寸', 1, 0),
(1, 'CPU型号', '', 1, 0),
(1, '运行内存', 'GB', 0, 1),
(1, '存储容量', 'GB', 0, 1),
(1, '颜色', '', 0, 1);

-- 插入品牌数据
INSERT INTO t_brand (brand_name, brand_letter) VALUES 
('苹果', 'P'), ('华为', 'H'), ('小米', 'X'), ('耐克', 'N'), ('阿迪达斯', 'A');

-- 插入分类数据
INSERT INTO t_category (cate_name, parent_id, level) VALUES 
('电子产品', 0, 1), ('手机', 1, 2), ('电脑', 1, 2),
('服装鞋包', 0, 1), ('男装', 4, 2), ('女装', 4, 2);

-- 插入会员等级数据
INSERT INTO t_level (level_name, discount) VALUES 
('普通会员', 1.00), ('铜牌会员', 0.98), ('银牌会员', 0.95), ('金牌会员', 0.90), ('钻石会员', 0.85);

-- 插入部门数据
INSERT INTO t_dept (dept_name) VALUES 
('董事会'), ('总裁办'), ('零售部'), ('网商部'), ('技术部'), ('售后部'), ('仓储部');

-- 插入职位数据
INSERT INTO t_job (job_name) VALUES 
('董事长'), ('总经理'), ('部门经理'), ('主管'), ('店长'), ('售货员'), ('仓库管理员'), ('客服');

-- 插入员工数据
INSERT INTO t_emp (wid, emp_name, sex, married, education, tel, job_id, dept_id, hire_date, status) VALUES 
('EMP001', '李娜', 'F', 1, 2, '13800138001', 5, 3, '2020-01-15', 1),
('EMP002', '刘畅', 'M', 0, 2, '13800138002', 6, 3, '2021-03-20', 1),
('EMP003', '王强', 'M', 1, 3, '13800138003', 7, 7, '2019-06-10', 1);

-- 插入角色数据
INSERT INTO t_role (role_name, role_desc) VALUES 
('超级管理员', '系统所有权限'),
('零售店长', '管理零售店日常运营'),
('仓库管理员', '管理库存和出入库'),
('客服', '处理客户咨询和售后');

-- 插入用户数据
INSERT INTO t_user (username, password, emp_id, role_id) VALUES 
('admin', 'encrypted_password', NULL, 1),
('lina', 'encrypted_password', 1, 2),
('liuchang', 'encrypted_password', 2, 2);

-- 插入客户数据
INSERT INTO t_customer (cust_name, cust_mobile, level_id, cust_integral) VALUES 
('张三', '13900139001', 1, 100),
('李四', '13900139002', 2, 500),
('王五', '13900139003', 3, 1000);

-- 插入产品数据(SPU)
INSERT INTO t_product (prod_title, prod_subtitle, cate_id, brand_id, spg_id) VALUES 
('iPhone 15', '强悍性能，出色体验', 2, 1, 1),
('华为Mate 60', '遥遥领先', 2, 2, 1),
('小米14', '徕卡影像', 2, 3, 1);

-- 插入商品数据(SKU)
INSERT INTO t_sku (prod_id, sku_title, sku_price, sku_params) VALUES 
(1, 'iPhone 15 黑色 128GB', 5999.00, '{"color":"黑色","storage":"128GB"}'),
(1, 'iPhone 15 白色 256GB', 6999.00, '{"color":"白色","storage":"256GB"}'),
(2, '华为Mate 60 雅川青 256GB', 6999.00, '{"color":"雅川青","storage":"256GB"}'),
(3, '小米14 黑色 12+256GB', 3999.00, '{"color":"黑色","ram":"12GB","storage":"256GB"}');

-- 插入仓库数据
INSERT INTO t_warehouse (city_id, wh_address, wh_tel) VALUES 
(110100, '北京市朝阳区 Warehouse路1号', '010-12345678'),
(310100, '上海市浦东新区 Warehouse路2号', '021-87654321');

-- 插入零售店数据
INSERT INTO t_shop (city_id, shop_name, shop_address, shop_tel) VALUES 
(110100, '北京朝阳店', '北京市朝阳区 Retail街1号', '010-11111111'),
(310100, '上海浦东店', '上海市浦东新区 Retail街2号', '021-22222222');

-- 插入库存数据
INSERT INTO t_warehouse_sku (wh_id, sku_id, stock, warning_stock) VALUES 
(1, 1, 1000, 50), (1, 2, 800, 50), (2, 3, 500, 30);

INSERT INTO t_shop_sku (shop_id, sku_id, stock) VALUES 
(1, 1, 100), (1, 2, 80), (2, 3, 50);

SELECT '示例数据插入成功！' AS message;
