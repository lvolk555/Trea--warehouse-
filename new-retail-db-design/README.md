# 新零售数据结构设计教程

> **立足需求，转化成果，向优秀致敬！**
> 
> 本教程基于慕课网神思者老师的新零售数据库设计课程整理完善

---

## 📚 教程简介

本教程详细讲解新零售平台数据库设计的完整知识体系，从核心概念（SPU/SKU）到各个业务模块的数据表设计，包括商品管理、客户管理、订单管理、员工管理、进销存管理等。

### 适合人群

- 数据库设计初学者
- 电商/新零售系统开发者
- 希望系统学习企业级数据库设计的工程师

---

## 📖 目录结构

```
new-retail-db-design/
├── README.md                          # 本文件
├── docs/                              # 详细教程文档
│   └── 新零售数据结构设计完整教程.md
├── sql/                               # SQL脚本
│   ├── 01_数据库创建.sql
│   ├── 02_商品模块.sql
│   ├── 03_客户模块.sql
│   ├── 04_订单模块.sql
│   ├── 05_员工模块.sql
│   ├── 06_进销存模块.sql
│   └── 完整建表脚本.sql
├── er-diagrams/                       # ER图
├── examples/                          # 示例数据
│   └── 示例数据插入脚本.sql
└── references/                        # 参考资料
    └── 相关文章链接.md
```

---

## 🎯 核心知识点

### 1. SPU与SKU概念

| 概念 | 全称 | 说明 |
|------|------|------|
| **SPU** | Standard Product Unit | 标准化产品单元，描述一类商品的共有属性 |
| **SKU** | Stock Keeping Unit | 库存量单位，具体的可售商品 |

**示例**：
- SPU：华为nova手机（描述一类商品）
- SKU：华为nova 幻夜黑 6+128G（具体可售商品）

### 2. 模块划分

| 模块 | 包含数据表 | 说明 |
|------|-----------|------|
| 商品模块 | 品类表、参数表、品牌表、分类表、产品表、商品表、库存表 | 商品信息管理 |
| 客户模块 | 会员等级表、客户表、购物券表 | 客户与营销 |
| 订单模块 | 订单表、订单详情表、快递表、退货表、评价表 | 交易流程 |
| 员工模块 | 部门表、职位表、员工表、角色表、用户表 | 权限管理 |
| 进销存模块 | 供应商表、采购表、入库表 | 供应链管理 |

---

## 🚀 快速开始

### 1. 创建数据库

```sql
CREATE DATABASE neti CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE neti;
```

### 2. 执行建表脚本

```bash
# 按顺序执行SQL脚本
mysql -u root -p neti < sql/01_数据库创建.sql
mysql -u root -p neti < sql/02_商品模块.sql
mysql -u root -p neti < sql/03_客户模块.sql
mysql -u root -p neti < sql/04_订单模块.sql
mysql -u root -p neti < sql/05_员工模块.sql
mysql -u root -p neti < sql/06_进销存模块.sql
```

### 3. 插入示例数据

```bash
mysql -u root -p neti < examples/示例数据插入脚本.sql
```

---

## 📊 数据表清单

### 商品模块（11张表）

| 表名 | 说明 |
|------|------|
| t_spec_group | 品类表 |
| t_spec_param | 参数表 |
| t_brand | 品牌表 |
| t_category | 分类表 |
| t_brand_category | 品牌分类关联表 |
| t_product | 产品表(SPU) |
| t_sku | 商品表(SKU) |
| t_warehouse | 仓库表 |
| t_warehouse_sku | 仓库商品库存表 |
| t_shop | 零售店表 |
| t_shop_sku | 店铺商品库存表 |

### 客户模块（4张表）

| 表名 | 说明 |
|------|------|
| t_level | 会员等级表 |
| t_customer | 客户表 |
| t_coupon | 购物券表 |
| t_customer_coupon | 客户购物券关联表 |

### 订单模块（5张表）

| 表名 | 说明 |
|------|------|
| t_order | 订单表 |
| t_order_detail | 订单详情表 |
| t_delivery | 快递表 |
| t_return | 退货表 |
| t_rating | 评价表 |

### 员工模块（5张表）

| 表名 | 说明 |
|------|------|
| t_dept | 部门表 |
| t_job | 职位表 |
| t_emp | 员工表 |
| t_role | 角色表 |
| t_user | 用户表 |

### 进销存模块（6张表）

| 表名 | 说明 |
|------|------|
| t_supplier | 供应商表 |
| t_supplier_sku | 供应商商品关联表 |
| t_purchase | 采购表 |
| t_stock_in | 入库表 |
| t_stock_in_detail | 入库详情表 |
| t_purchase_stock_in | 采购入库关联表 |

---

## 📝 设计规范

### 命名规范

1. **表名**：使用`t_`前缀，小写字母，下划线分隔
   - 示例：`t_order`、`t_customer`

2. **字段名**：小写字母，下划线分隔
   - 示例：`cust_id`、`create_time`

3. **索引名**：
   - 普通索引：`idx_字段名`
   - 唯一索引：`unq_字段名`

### 字段设计原则

1. **主键**：统一使用`INT UNSIGNED AUTO_INCREMENT`
2. **状态字段**：使用`TINYINT UNSIGNED`，并添加注释说明
3. **金额字段**：使用`DECIMAL(10,2) UNSIGNED`
4. **时间字段**：
   - 创建时间：`create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP`
   - 更新时间：`update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`

---

## 🔗 参考资料

- [慕课网 - 新零售数据库设计课程](https://coding.imooc.com/)
- [阿里巴巴新零售数据库设计](https://blog.csdn.net/weixin_39200308/article/details/107497918)
- [电商商品模型设计 - SPU与SKU](https://blog.csdn.net/weixin_43844521/article/details/159049991)

---

## 📄 许可证

本教程仅供学习交流使用，版权归原作者所有。

---

**Made with ❤️ by 学习者社区**
