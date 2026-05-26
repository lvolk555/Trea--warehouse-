-- ============================================
-- 新零售数据库 - 01. 数据库创建
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS neti 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE neti;

SELECT '数据库 neti 创建成功！' AS message;
