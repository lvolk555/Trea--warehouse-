-- ============================================================
-- AI 辅助在线学习平台 数据库初始化脚本
-- 数据库：MySQL 8.x  字符集：utf8mb4
-- 共 21 张表：用户域 1 + 课程域 4 + 学习域 2 + 考试域 5
--            + AI 域 2 + 积分域 4 + 运营域 2 + 签到记录 1
-- 测试账号密码均为 123456（BCrypt 加密）
-- ============================================================

CREATE DATABASE IF NOT EXISTS ai_learning DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_learning;

-- ============================================================
-- 一、用户域
-- ============================================================

-- 1. 用户表
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
  `password`    VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密密码',
  `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
  `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像 URL',
  `role`        TINYINT      NOT NULL DEFAULT 1 COMMENT '角色：1学生 2教师 3管理员',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB COMMENT = '用户表';

-- ============================================================
-- 二、课程域
-- ============================================================

-- 2. 课程表
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `teacher_id`   BIGINT       NOT NULL COMMENT '授课教师',
  `title`        VARCHAR(100) NOT NULL COMMENT '课程名称',
  `cover`        VARCHAR(255) DEFAULT NULL COMMENT '封面 URL',
  `category`     VARCHAR(50)  DEFAULT NULL COMMENT '分类（编程/数学等）',
  `description`  TEXT         COMMENT '课程简介（作为 AI 答疑上下文）',
  `price_type`   TINYINT      NOT NULL DEFAULT 1 COMMENT '定价方式：1免费 2积分兑换',
  `points_price` INT          NOT NULL DEFAULT 0 COMMENT '兑换所需积分（price_type=2 时有效）',
  `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0待审核 1已上架 2已下架',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_teacher` (`teacher_id`),
  KEY `idx_status` (`status`)
) ENGINE = InnoDB COMMENT = '课程表';

-- 3. 章节表
DROP TABLE IF EXISTS `chapter`;
CREATE TABLE `chapter` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `course_id`  BIGINT       NOT NULL COMMENT '所属课程',
  `title`      VARCHAR(100) NOT NULL COMMENT '章节标题',
  `sort_order` INT          NOT NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`),
  KEY `idx_course` (`course_id`)
) ENGINE = InnoDB COMMENT = '章节表';

-- 4. 视频表
DROP TABLE IF EXISTS `video`;
CREATE TABLE `video` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `chapter_id` BIGINT       NOT NULL COMMENT '所属章节',
  `title`      VARCHAR(100) NOT NULL COMMENT '视频标题',
  `url`        VARCHAR(255) DEFAULT NULL COMMENT '视频地址',
  `duration`   INT          NOT NULL DEFAULT 0 COMMENT '时长（秒）',
  `sort_order` INT          NOT NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`),
  KEY `idx_chapter` (`chapter_id`)
) ENGINE = InnoDB COMMENT = '视频表';

-- 5. 选课表
DROP TABLE IF EXISTS `course_enrollment`;
CREATE TABLE `course_enrollment` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id`  BIGINT       NOT NULL COMMENT '学生',
  `course_id`   BIGINT       NOT NULL COMMENT '课程',
  `progress`    DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '完成度百分比',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '选课时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_course` (`student_id`, `course_id`),
  KEY `idx_course` (`course_id`)
) ENGINE = InnoDB COMMENT = '选课表';

-- ============================================================
-- 三、学习域
-- ============================================================

-- 6. 学习进度表
DROP TABLE IF EXISTS `learning_record`;
CREATE TABLE `learning_record` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id`  BIGINT   NOT NULL COMMENT '学生',
  `video_id`    BIGINT   NOT NULL COMMENT '视频',
  `position`    INT      NOT NULL DEFAULT 0 COMMENT '播放位置（秒）',
  `finished`    TINYINT  NOT NULL DEFAULT 0 COMMENT '0未完成 1已完成',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近学习时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_video` (`student_id`, `video_id`)
) ENGINE = InnoDB COMMENT = '学习进度表';

-- 7. 学习笔记表
DROP TABLE IF EXISTS `study_note`;
CREATE TABLE `study_note` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id`  BIGINT   NOT NULL COMMENT '学生',
  `video_id`    BIGINT   NOT NULL COMMENT '关联视频',
  `content`     TEXT     COMMENT '笔记内容',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_student` (`student_id`)
) ENGINE = InnoDB COMMENT = '学习笔记表';

-- ============================================================
-- 四、考试域
-- ============================================================

-- 8. 题库表（课程 → 章节 两级归属）
DROP TABLE IF EXISTS `question`;
CREATE TABLE `question` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `course_id`   BIGINT       NOT NULL COMMENT '所属课程',
  `chapter_id`  BIGINT       NOT NULL COMMENT '所属章节',
  `type`        TINYINT      NOT NULL COMMENT '题型：1单选 2多选 3判断 4简答',
  `content`     TEXT         NOT NULL COMMENT '题干',
  `options`     JSON         DEFAULT NULL COMMENT '选项（客观题）',
  `answer`      VARCHAR(500) NOT NULL COMMENT '正确答案',
  `analysis`    TEXT         COMMENT '解析',
  `source`      TINYINT      NOT NULL DEFAULT 1 COMMENT '来源：1人工录入 2AI生成',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_course_chapter` (`course_id`, `chapter_id`)
) ENGINE = InnoDB COMMENT = '题库表';

-- 9. 试卷表
DROP TABLE IF EXISTS `exam`;
CREATE TABLE `exam` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `course_id`    BIGINT       NOT NULL COMMENT '所属课程',
  `title`        VARCHAR(100) NOT NULL COMMENT '试卷名称',
  `duration`     INT          NOT NULL DEFAULT 60 COMMENT '考试时长（分钟）',
  `question_ids` JSON         DEFAULT NULL COMMENT '题目 ID 列表',
  `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '0草稿 1已发布',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_course` (`course_id`)
) ENGINE = InnoDB COMMENT = '试卷表';

-- 10. 考试记录表
DROP TABLE IF EXISTS `exam_record`;
CREATE TABLE `exam_record` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `exam_id`     BIGINT       NOT NULL COMMENT '试卷',
  `student_id`  BIGINT       NOT NULL COMMENT '学生',
  `score`       DECIMAL(5,1) DEFAULT NULL COMMENT '得分',
  `submit_time` DATETIME     DEFAULT NULL COMMENT '提交时间',
  PRIMARY KEY (`id`),
  KEY `idx_exam` (`exam_id`),
  KEY `idx_student` (`student_id`)
) ENGINE = InnoDB COMMENT = '考试记录表';

-- 11. 答题明细表
DROP TABLE IF EXISTS `exam_answer`;
CREATE TABLE `exam_answer` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `record_id`      BIGINT       NOT NULL COMMENT '考试记录',
  `question_id`    BIGINT       NOT NULL COMMENT '题目',
  `student_answer` TEXT         COMMENT '学生答案',
  `correct`        TINYINT      DEFAULT NULL COMMENT '0错 1对（主观题可空）',
  `ai_score`       DECIMAL(3,1) DEFAULT NULL COMMENT 'AI 评分（主观题）',
  `ai_comment`     TEXT         COMMENT 'AI 批改建议',
  PRIMARY KEY (`id`),
  KEY `idx_record` (`record_id`)
) ENGINE = InnoDB COMMENT = '答题明细表';

-- 11.5 练习/错题记录表（章节练习即时判分 + 错题本）
DROP TABLE IF EXISTS `practice_record`;
CREATE TABLE `practice_record` (
  `id`             BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id`     BIGINT   NOT NULL COMMENT '学生',
  `question_id`    BIGINT   NOT NULL COMMENT '题目',
  `student_answer` TEXT     COMMENT '学生答案',
  `correct`        TINYINT  NOT NULL COMMENT '0错 1对',
  `mastered`       TINYINT  NOT NULL DEFAULT 0 COMMENT '错题是否已标记掌握：0否 1是',
  `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作答时间',
  PRIMARY KEY (`id`),
  KEY `idx_student` (`student_id`),
  KEY `idx_question` (`question_id`)
) ENGINE = InnoDB COMMENT = '练习/错题记录表';

-- ============================================================
-- 五、AI 域
-- ============================================================

-- 12. AI 会话表
DROP TABLE IF EXISTS `ai_chat_session`;
CREATE TABLE `ai_chat_session` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id`  BIGINT       NOT NULL COMMENT '学生',
  `course_id`   BIGINT       DEFAULT NULL COMMENT '关联课程',
  `title`       VARCHAR(100) DEFAULT NULL COMMENT '会话标题（取首条提问）',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_student` (`student_id`)
) ENGINE = InnoDB COMMENT = 'AI 会话表';

-- 13. AI 消息表
DROP TABLE IF EXISTS `ai_chat_message`;
CREATE TABLE `ai_chat_message` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id`  BIGINT      NOT NULL COMMENT '所属会话',
  `role`        VARCHAR(10) NOT NULL COMMENT 'user / assistant',
  `content`     TEXT        COMMENT '消息内容',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`)
) ENGINE = InnoDB COMMENT = 'AI 消息表';

-- ============================================================
-- 六、积分域
-- ============================================================

-- 14. 积分账户表（一人一户）
DROP TABLE IF EXISTS `points_account`;
CREATE TABLE `points_account` (
  `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`      BIGINT   NOT NULL COMMENT '学生（一人一户）',
  `balance`      INT      NOT NULL DEFAULT 0 COMMENT '当前可用积分',
  `total_earned` INT      NOT NULL DEFAULT 0 COMMENT '累计获得',
  `total_spent`  INT      NOT NULL DEFAULT 0 COMMENT '累计消耗',
  `update_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近变动时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`)
) ENGINE = InnoDB COMMENT = '积分账户表';

-- 15. 积分明细表
DROP TABLE IF EXISTS `points_record`;
CREATE TABLE `points_record` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`      BIGINT       NOT NULL COMMENT '学生',
  `type`         TINYINT      NOT NULL COMMENT '1完课 2签到 3考试奖励 4AI提问 5兑换扣减 6注册赠送',
  `change_value` INT          NOT NULL COMMENT '变动值（正为获得，负为消耗）',
  `description`  VARCHAR(200) DEFAULT NULL COMMENT '变动说明',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE = InnoDB COMMENT = '积分明细表';

-- 16. 积分规则表
DROP TABLE IF EXISTS `points_rule`;
CREATE TABLE `points_rule` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rule_key`    VARCHAR(50) NOT NULL COMMENT '规则键（video_finish / daily_sign / exam_pass / ai_ask / register_gift）',
  `rule_value`  INT         NOT NULL DEFAULT 0 COMMENT '奖励积分值',
  `daily_limit` INT         NOT NULL DEFAULT 0 COMMENT '每日获取上限（0 表示不限）',
  `enabled`     TINYINT     NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_key` (`rule_key`)
) ENGINE = InnoDB COMMENT = '积分规则表';

-- 17. 课程兑换记录表
DROP TABLE IF EXISTS `course_exchange_record`;
CREATE TABLE `course_exchange_record` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT   NOT NULL COMMENT '兑换学生',
  `course_id`   BIGINT   NOT NULL COMMENT '兑换课程',
  `points_cost` INT      NOT NULL COMMENT '消耗积分',
  `status`      TINYINT  NOT NULL DEFAULT 1 COMMENT '1成功 2失败（积分不足）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '兑换时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE = InnoDB COMMENT = '课程兑换记录表';

-- 18. 签到记录表（支撑每日签到与每日上限校验）
DROP TABLE IF EXISTS `sign_record`;
CREATE TABLE `sign_record` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`    BIGINT   NOT NULL COMMENT '学生',
  `sign_date`  DATE     NOT NULL COMMENT '签到日期',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签到时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`, `sign_date`)
) ENGINE = InnoDB COMMENT = '签到记录表';

-- ============================================================
-- 七、运营域
-- ============================================================

-- 19. 公告表
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`       VARCHAR(100) NOT NULL COMMENT '公告标题',
  `content`     TEXT         COMMENT '公告内容',
  `type`        TINYINT      NOT NULL DEFAULT 1 COMMENT '1系统通知 2活动公告 3课程上新',
  `top`         TINYINT      NOT NULL DEFAULT 0 COMMENT '0否 1置顶',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '0已撤回 1已发布',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB COMMENT = '公告表';

-- 20. 课程评论表
DROP TABLE IF EXISTS `course_comment`;
CREATE TABLE `course_comment` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT   NOT NULL COMMENT '评论用户',
  `course_id`   BIGINT   NOT NULL COMMENT '所属课程',
  `content`     TEXT     COMMENT '评论内容',
  `status`      TINYINT  NOT NULL DEFAULT 0 COMMENT '0待审核 1已展示 2已隐藏',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
  PRIMARY KEY (`id`),
  KEY `idx_course` (`course_id`)
) ENGINE = InnoDB COMMENT = '课程评论表';

-- ============================================================
-- 测试数据（密码均为 123456）
-- ============================================================

-- 用户：1 管理员 / 1 教师 / 3 学生
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `role`, `status`) VALUES
(1, 'admin',    '$2b$10$KO2eaxV7GvYAeg73m6fEgeerC3B.Aks1B0ErEck3RMHT8RblAne1m', '系统管理员', 3, 1),
(2, 'teacher1', '$2b$10$KO2eaxV7GvYAeg73m6fEgeerC3B.Aks1B0ErEck3RMHT8RblAne1m', '王老师',     2, 1),
(3, 'student1', '$2b$10$KO2eaxV7GvYAeg73m6fEgeerC3B.Aks1B0ErEck3RMHT8RblAne1m', '张同学',     1, 1),
(4, 'student2', '$2b$10$KO2eaxV7GvYAeg73m6fEgeerC3B.Aks1B0ErEck3RMHT8RblAne1m', '李同学',     1, 1),
(5, 'student3', '$2b$10$KO2eaxV7GvYAeg73m6fEgeerC3B.Aks1B0ErEck3RMHT8RblAne1m', '王同学',     1, 1);

-- 课程：1 免费已上架 / 1 积分兑换已上架 / 1 待审核
INSERT INTO `course` (`id`, `teacher_id`, `title`, `cover`, `category`, `description`, `price_type`, `points_price`, `status`) VALUES
(1, 2, 'Java 面向对象程序设计', '/api/files/course-cover-java.jpg', '编程',
 '本课程系统讲解 Java 面向对象编程，涵盖类与对象、封装、继承、多态、接口与异常处理，配合大量代码示例，帮助初学者建立扎实的 OOP 基础。', 1, 0, 1),
(2, 2, 'Python 数据分析入门', '/api/files/course-cover-python.jpg', '编程',
 '本课程介绍 Python 数据分析基础，涵盖 NumPy、Pandas 数据处理与 Matplotlib 可视化，适合零基础学员入门数据科学。', 2, 800, 1),
(3, 2, 'Web 前端开发实战', '/api/files/course-cover-web.jpg', '编程',
 '本课程讲解 HTML、CSS、JavaScript 前端三件套，并通过实战项目掌握页面开发流程。', 1, 0, 0);

-- 章节（课程 1 两章，课程 2 一章）
INSERT INTO `chapter` (`id`, `course_id`, `title`, `sort_order`) VALUES
(1, 1, '第一章 Java 语言基础', 1),
(2, 1, '第二章 面向对象核心', 2),
(3, 2, '第一章 Python 与数据处理', 1);

-- 视频（使用公开可播放的示例视频，便于本地演示）
INSERT INTO `video` (`id`, `chapter_id`, `title`, `url`, `duration`, `sort_order`) VALUES
(1, 1, '1.1 开发环境搭建', '/api/files/videos/mov_bbb.mp4', 10, 1),
(2, 1, '1.2 变量与数据类型', '/api/files/videos/movie.mp4', 12, 2),
(3, 2, '2.1 类与对象', '/api/files/videos/mov_bbb.mp4', 10, 1),
(4, 2, '2.2 封装与访问控制', '/api/files/videos/movie.mp4', 12, 2),
(5, 2, '2.3 继承与多态', '/api/files/videos/mov_bbb.mp4', 10, 3),
(6, 3, '1.1 NumPy 数组基础', '/api/files/videos/movie.mp4', 12, 1);

-- 选课
INSERT INTO `course_enrollment` (`student_id`, `course_id`, `progress`) VALUES
(3, 1, 40.00),
(4, 1, 20.00);

-- 题库（归属：课程 → 章节）
INSERT INTO `question` (`course_id`, `chapter_id`, `type`, `content`, `options`, `answer`, `analysis`, `source`) VALUES
(1, 1, 1, 'Java 中哪个关键字用于定义常量？', '["static", "final", "const", "constant"]', 'B',
 'final 修饰的变量一旦赋值不可修改，常用于定义常量。', 1),
(1, 1, 3, 'Java 是纯面向对象语言，基本数据类型也是对象。', NULL, '错',
 'Java 有 8 种基本数据类型（如 int、double），它们不是对象，包装类才是。', 1),
(1, 2, 1, '下列关于继承的说法，正确的是？', '["Java 支持多继承", "子类可以访问父类所有成员", "Java 中一个类只能有一个父类", "继承使用 implements 关键字"]', 'C',
 'Java 类是单继承（可多实现接口），继承使用 extends 关键字，私有成员不可直接访问。', 1),
(1, 2, 2, '以下哪些是面向对象的特性？', '["封装", "继承", "多态", "递归"]', 'ABC',
 '面向对象三大特性：封装、继承、多态。递归是一种算法思想。', 2),
(1, 2, 4, '请简述 Java 中多态的实现机制。', NULL, '多态通过继承、方法重写和父类引用指向子类对象实现，运行时根据实际对象类型调用对应方法。',
 '答题要点：继承/接口、方法重写、向上转型、动态绑定。', 1);

-- 积分规则
INSERT INTO `points_rule` (`rule_key`, `rule_value`, `daily_limit`, `enabled`) VALUES
('video_finish', 10, 50, 1),
('daily_sign', 5, 5, 1),
('exam_pass', 20, 0, 1),
('ai_ask', 2, 10, 1),
('register_gift', 100, 0, 1);

-- 积分账户（学生注册赠送 100，student1 额外通过完课获得 30）
INSERT INTO `points_account` (`user_id`, `balance`, `total_earned`, `total_spent`) VALUES
(3, 130, 130, 0),
(4, 100, 100, 0),
(5, 100, 100, 0);

INSERT INTO `points_record` (`user_id`, `type`, `change_value`, `description`) VALUES
(3, 6, 100, '注册赠送积分'),
(4, 6, 100, '注册赠送积分'),
(5, 6, 100, '注册赠送积分'),
(3, 1, 10, '完成视频《1.1 开发环境搭建》'),
(3, 1, 10, '完成视频《1.2 变量与数据类型》'),
(3, 1, 10, '完成视频《2.1 类与对象》');

-- 公告
INSERT INTO `notice` (`title`, `content`, `type`, `top`, `status`) VALUES
('欢迎使用 AI 辅助在线学习平台', '平台已上线，支持课程学习、练习考试与 AI 智能答疑，快来体验吧！', 1, 1, 1),
('积分商城开放公告', '完成课程视频、每日签到、考试及格均可获得积分，积分可兑换《Python 数据分析入门》等付费课程。', 2, 0, 1);

-- 课程评论
INSERT INTO `course_comment` (`user_id`, `course_id`, `content`, `status`) VALUES
(3, 1, '老师讲得很清楚，继承多态那节收获很大！', 1),
(4, 1, '希望多一些实战案例。', 0);
