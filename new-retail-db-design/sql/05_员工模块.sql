-- ============================================
-- 新零售数据库 - 05. 员工模块
-- ============================================

USE neti;

-- 部门表
CREATE TABLE t_dept (
    dept_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '部门ID',
    dept_name VARCHAR(50) NOT NULL COMMENT '部门名称',
    UNIQUE KEY unq_dept_name (dept_name)
) COMMENT '部门表';

-- 职位表
CREATE TABLE t_job (
    job_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '职位ID',
    job_name VARCHAR(50) NOT NULL COMMENT '职位名称',
    UNIQUE KEY unq_job_name (job_name)
) COMMENT '职位表';

-- 员工表
CREATE TABLE t_emp (
    emp_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '员工ID',
    wid VARCHAR(20) NOT NULL COMMENT '工号',
    emp_name VARCHAR(20) NOT NULL COMMENT '员工姓名',
    sex CHAR(1) NOT NULL COMMENT '性别(M男F女)',
    married TINYINT(1) NOT NULL COMMENT '婚否(0未婚1已婚)',
    education TINYINT UNSIGNED COMMENT '学历(1大专2本科3研究生)',
    tel VARCHAR(11) NOT NULL COMMENT '电话',
    email VARCHAR(100) COMMENT '邮箱',
    address VARCHAR(200) COMMENT '住址',
    job_id INT UNSIGNED NOT NULL COMMENT '职位ID',
    dept_id INT UNSIGNED NOT NULL COMMENT '部门ID',
    mgr_id INT UNSIGNED COMMENT '上司ID',
    hire_date DATE NOT NULL COMMENT '入职日期',
    leave_date DATE COMMENT '离职日期',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态(1在职2休假3离职)',
    UNIQUE KEY unq_wid (wid),
    INDEX idx_job_id (job_id),
    INDEX idx_dept_id (dept_id),
    INDEX idx_mgr_id (mgr_id)
) COMMENT '员工表';

-- 角色表
CREATE TABLE t_role (
    role_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_desc VARCHAR(200) COMMENT '角色描述',
    UNIQUE KEY unq_role_name (role_name)
) COMMENT '角色表';

-- 用户表
CREATE TABLE t_user (
    user_id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(200) NOT NULL COMMENT '密码(AES加密)',
    emp_id INT UNSIGNED COMMENT '关联员工ID',
    role_id INT UNSIGNED NOT NULL COMMENT '角色ID',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态(0禁用1启用)',
    last_login_time TIMESTAMP COMMENT '最后登录时间',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY unq_username (username),
    INDEX idx_emp_id (emp_id),
    INDEX idx_role_id (role_id)
) COMMENT '用户表';

SELECT '员工模块数据表创建成功！' AS message;
