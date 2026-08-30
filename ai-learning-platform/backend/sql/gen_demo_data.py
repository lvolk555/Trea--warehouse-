#!/usr/bin/env python3
"""生成模拟真实用户操作的演示数据 SQL（demo_data.sql）

使用方式：先执行 init.sql 建库（含基础种子），再执行本脚本产出的 demo_data.sql 追加数据。
一致性保证：
  1. 积分账户余额 = 注册赠送 + 明细收入 - 明细支出（逐学生核算）
  2. 课程完成度 = 已完成视频数 / 课程视频总数（与后端 refreshCourseProgress 口径一致）
  3. 完课视频数与 video_finish 积分明细条数一致
  4. 用户注册、签到、AI 消息、积分变动时间分布在近 14 天，保证看板趋势图有数据
"""
import random
from datetime import datetime, timedelta

random.seed(42)  # 固定随机种子，保证可复现
NOW = datetime.now().replace(microsecond=0)
PASSWORD = "$2b$10$KO2eaxV7GvYAeg73m6fEgeerC3B.Aks1B0ErEck3RMHT8RblAne1m"  # 123456


def dt(days_ago, hour=None, minute=None):
    """返回 N 天前的时间，可选指定时分"""
    d = NOW - timedelta(days=days_ago)
    if hour is not None:
        d = d.replace(hour=hour, minute=minute or random.randint(0, 59), second=random.randint(0, 59))
    return d.strftime("%Y-%m-%d %H:%M:%S")


def sql_str(s):
    return "'" + s.replace("'", "''") + "'"


lines = []
add = lines.append

add("-- ============================================================")
add("-- AI 辅助在线学习平台 演示数据（模拟真实用户操作）")
add("-- 前置：先执行 init.sql；本脚本为追加数据，不删表")
add("-- 账号密码均为 123456")
add("-- ============================================================")
add("USE ai_learning;")
add("")

# ============================================================
# 1. 用户：新增 1 教师 + 20 学生，注册时间分散在近 14 天（填充用户增长趋势）
# ============================================================
add("-- ---------- 1. 用户（注册日期分散近 14 天，填充用户增长趋势图） ----------")
nicknames = ["陈晨", "刘一诺", "赵子墨", "孙悦", "周雨桐", "吴磊", "郑好", "冯晓", "蒋欣怡", "沈从文",
             "韩梅", "杨光", "朱琳", "秦朗", "许诺", "何静", "吕途", "施雨", "张浩然", "曹雪"]
user_rows = []
uid = 6  # init.sql 已用 1-5
add(f"INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `role`, `status`, `create_time`) VALUES")
teacher2_id = uid
user_rows.append((uid, "teacher2", "李老师", 2, dt(13, 9)))
uid += 1
student_ids = []
for i, nick in enumerate(nicknames):
    reg_day = 13 - (i % 14)  # 每天 1-2 个新注册
    reg_time = dt(reg_day, random.randint(8, 22))
    user_rows.append((uid, f"stu{i+1:02d}", nick, 1, reg_time))
    student_ids.append(uid)
    uid += 1
values = ",\n".join(
    f"({r[0]}, {sql_str(r[1])}, {sql_str(PASSWORD)}, {sql_str(r[2])}, {r[3]}, 1, {sql_str(r[4])})"
    for r in user_rows)
add(values + ";")
add("")

# ============================================================
# 2. 课程：新增 7 门（3 门 teacher2），章节、视频
# ============================================================
add("-- ---------- 2. 课程 / 章节 / 视频 ----------")
courses = [
    # (id, teacher, title, category, desc, price_type, points_price, status, 章节数)
    (4, 2, "MySQL 数据库从入门到精通", "编程", "系统讲解 SQL 语法、表设计、索引优化与事务，配套大量实操练习。", 1, 0, 1, 3),
    (5, 2, "数据结构与算法（Java 版）", "编程", "讲解数组、链表、栈、队列、树与常用算法，提升编程内功。", 2, 600, 1, 3),
    (6, 2, "Linux 操作系统基础", "编程", "从命令行到 Shell 脚本，掌握服务器运维必备技能。", 1, 0, 1, 2),
    (7, teacher2_id, "高等数学（上）精讲", "数学", "极限、导数、积分系统讲解，例题丰富，适合专升本与考研打基础。", 1, 0, 1, 3),
    (8, teacher2_id, "线性代数入门", "数学", "矩阵、行列式、向量空间与线性方程组，配合几何直观理解。", 1, 0, 1, 2),
    (9, teacher2_id, "大学英语四级冲刺", "外语", "词汇、听力、阅读、写作四模块专项突破，真题精讲。", 2, 500, 1, 2),
    (10, teacher2_id, "UI 设计基础", "设计", "设计原则、配色、排版与 Figma 实操，零基础可学。", 1, 0, 1, 2),
]
add("INSERT INTO `course` (`id`, `teacher_id`, `title`, `cover`, `category`, `description`, `price_type`, `points_price`, `status`, `create_time`) VALUES")
add(",\n".join(
    f"({c[0]}, {c[1]}, {sql_str(c[2])}, {sql_str('https://picsum.photos/seed/c' + str(c[0]) + '/640/360')}, "
    f"{sql_str(c[3])}, {sql_str(c[4])}, {c[5]}, {c[6]}, {c[7]}, {sql_str(dt(random.randint(10, 13), 10))})"
    for c in courses) + ";")
add("")

# 章节与视频：每课程按章节数生成，每章 2-3 个视频，时长 600-1500 秒（真实量级）
chapter_rows = []
video_rows = []
chapter_id = 4  # init.sql 已用 1-3
video_id = 7    # init.sql 已用 1-6
course_structure = {}  # course_id -> list of chapter -> list of video_ids
chapter_titles_map = {
    4: ["SQL 基础语法", "数据库设计与范式", "索引与事务"],
    5: ["线性结构", "树与二叉树", "图与算法"],
    6: ["Linux 命令入门", "Shell 脚本编程"],
    7: ["函数与极限", "导数与微分", "不定积分"],
    8: ["矩阵与行列式", "向量与线性方程组"],
    9: ["词汇与听力突破", "阅读与写作冲刺"],
    10: ["设计原则与配色", "Figma 实操"],
}
for c in courses:
    cid, n_chapters = c[0], c[8]
    course_structure[cid] = []
    for ci in range(n_chapters):
        chapter_rows.append((chapter_id, cid, chapter_titles_map[cid][ci], ci + 1))
        vids = []
        for vi in range(random.randint(2, 3)):
            duration = random.randint(600, 1500)
            video_rows.append((video_id, chapter_id, f"{ci+1}.{vi+1} 第{ci+1}章第{vi+1}节",
                               f"https://www.w3schools.com/html/mov_bbb.mp4", duration, vi + 1))
            vids.append((video_id, duration))
            video_id += 1
        course_structure[cid].append(vids)
        chapter_id += 1

add("INSERT INTO `chapter` (`id`, `course_id`, `title`, `sort_order`) VALUES")
add(",\n".join(f"({r[0]}, {r[1]}, {sql_str(r[2])}, {r[3]})" for r in chapter_rows) + ";")
add("")
add("INSERT INTO `video` (`id`, `chapter_id`, `title`, `url`, `duration`, `sort_order`) VALUES")
add(",\n".join(f"({r[0]}, {r[1]}, {sql_str(r[2])}, {sql_str(r[3])}, {r[4]}, {r[5]})" for r in video_rows) + ";")
add("")

# ============================================================
# 3. 题库：为新课程补充题目（含易错题，供教师看板易错题 TOP）
# ============================================================
add("-- ---------- 3. 题库（新课程 + 易错设计） ----------")
question_rows = []
qid = 6  # init.sql 已用 1-5
question_bank = {
    4: [  # MySQL
        ("SQL 中用于查询数据的关键字是？", '["SELECT", "INSERT", "UPDATE", "DELETE"]', "A", "SELECT 用于检索数据。", 1),
        ("主键可以为空。", None, "错", "主键唯一且非空。", 1),
        ("以下哪个是聚合函数？", '["COUNT", "CONCAT", "SUBSTRING", "TRIM"]', "A", "COUNT 统计行数，是聚合函数。", 1),
        ("事务的 ACID 特性包括哪些？", '["原子性", "一致性", "隔离性", "持久性"]', "ABCD", "ACID 四特性缺一不可。", 1),
    ],
    5: [  # 数据结构
        ("栈的特点是？", '["先进先出", "先进后出", "随机访问", "双端操作"]', "B", "栈是后进先出（LIFO）。", 1),
        ("二叉树的前序遍历顺序是？", '["根左右", "左根右", "左右根", "根右左"]', "A", "前序：根→左→右。", 1),
        ("链表支持随机访问。", None, "错", "链表只能顺序访问，数组才支持随机访问。", 1),
    ],
    6: [  # Linux
        ("Linux 中查看当前目录的命令是？", '["pwd", "cd", "ls", "dir"]', "A", "pwd 打印工作目录。", 1),
        ("chmod 755 表示所有者可读写执行。", None, "对", "7=读写执行，所有者权限为 7。", 1),
    ],
    7: [  # 高数
        ("函数在某点可导则一定连续。", None, "对", "可导必连续，连续不一定可导。", 1),
        ("lim(x→0) sinx/x 的值是？", '["0", "1", "∞", "不存在"]', "B", "重要极限，结果为 1。", 1),
    ],
    8: [  # 线代
        ("n 阶行列式共有 n! 项。", None, "对", "行列式展开共 n! 项。", 1),
        ("矩阵乘法满足交换律。", None, "错", "矩阵乘法一般不满足交换律。", 1),
    ],
    9: [  # 英语
        ("'abandon' 的中文意思是？", '["放弃", "丰富", "吸收", "调整"]', "A", "abandon 意为放弃、抛弃。", 1),
    ],
    10: [  # UI
        ("三原色不包括以下哪种颜色？", '["红", "绿", "蓝", "黄"]', "D", "光的三原色是红绿蓝（RGB）。", 1),
    ],
}
# 每课程题目归属到第一章
first_chapter_of_course = {}
for r in chapter_rows:
    if r[1] not in first_chapter_of_course:
        first_chapter_of_course[r[1]] = r[0]
for cid, qs in question_bank.items():
    for q in qs:
        question_rows.append((qid, cid, first_chapter_of_course[cid], 1 if q[4] == 1 and q[2] in "ABCD" and len(q[2]) == 1 else (3 if q[1] is None else 2), q))
        qid += 1
# 修正题型判断：单选(1)/多选(2)/判断(3)
fixed_rows = []
for (i, c, ch, _, q) in question_rows:
    content, options, answer, analysis, source = q
    qtype = 3 if options is None else (2 if len(answer) > 1 else 1)
    fixed_rows.append((i, c, ch, qtype, content, options, answer, analysis, source))
add("INSERT INTO `question` (`id`, `course_id`, `chapter_id`, `type`, `content`, `options`, `answer`, `analysis`, `source`) VALUES")
add(",\n".join(
    f"({r[0]}, {r[1]}, {r[2]}, {r[3]}, {sql_str(r[4])}, {sql_str(r[5]) if r[5] else 'NULL'}, {sql_str(r[6])}, {sql_str(r[7])}, {r[8]})"
    for r in fixed_rows) + ";")
add("")

# ============================================================
# 4. 选课 + 学习记录（核心：模拟真实学习行为）
# ============================================================
add("-- ---------- 4. 选课与学习记录 ----------")
# 免费课程：4,6,7,8,10；积分课程：5(600),9(500)
free_courses = [1, 4, 6, 7, 8, 10]
paid_courses = {5: 600, 9: 500}
enrollment_rows = []
learning_rows = []
enroll_id = 3  # init.sql 已用 2 条（无显式 id，实际自增，这里用显式 id 从 3 开始）
lr_id = 1

# 每个学生的学习画像：选课列表 + 每门课的完成比例
profiles = {}
for i, sid in enumerate(student_ids):
    # 每人选 2-4 门免费课（课程 1 和 4 最热门）
    n_courses = random.randint(2, 4)
    chosen = random.sample([1, 1, 4, 4, 6, 7, 8, 10], n_courses)  # 加权：1 和 4 更热门
    chosen = list(dict.fromkeys(chosen))  # 去重保序
    profiles[sid] = {"free": chosen, "paid": [], "finish_ratio": {}}
    for cid in chosen:
        profiles[sid]["finish_ratio"][cid] = random.choice([0.0, 0.25, 0.5, 0.75, 1.0])
# 部分学生兑换积分课程（积分足够者）
for sid in student_ids[:8]:
    if random.random() < 0.6:
        cid = random.choice([5, 9])
        profiles[sid]["paid"].append(cid)
        profiles[sid]["finish_ratio"][cid] = random.choice([0.0, 0.5, 1.0])

# 生成选课与学习记录
for sid, prof in profiles.items():
    all_courses = prof["free"] + prof["paid"]
    for cid in all_courses:
        enroll_day = random.randint(1, 13)
        enrollment_rows.append((enroll_id, sid, cid, enroll_day))
        enroll_id += 1
        # 按完成比例决定完成哪些视频
        videos_in_course = [(vid, dur) for ch in course_structure.get(cid, []) for (vid, dur) in ch]
        if not videos_in_course:
            continue
        ratio = prof["finish_ratio"].get(cid, 0)
        n_finish = int(len(videos_in_course) * ratio)
        for idx, (vid, dur) in enumerate(videos_in_course):
            if idx < n_finish:
                # 已完成：position 取接近结尾的值（模拟看完），finished=1
                pos = int(dur * random.uniform(0.96, 1.0))
                learning_rows.append((lr_id, sid, vid, pos, 1, dt(random.randint(0, enroll_day), random.randint(8, 23))))
            elif idx == n_finish and random.random() < 0.7:
                # 进行中：看了一部分，finished=0
                pos = int(dur * random.uniform(0.1, 0.8))
                learning_rows.append((lr_id, sid, vid, pos, 0, dt(random.randint(0, max(enroll_day - 1, 0)), random.randint(8, 23))))
            lr_id += 1

add("INSERT INTO `course_enrollment` (`id`, `student_id`, `course_id`, `progress`, `create_time`) VALUES")
# progress 稍后按实际完成视频数回填，这里先占位 0
add(",\n".join(f"({r[0]}, {r[1]}, {r[2]}, 0.00, {sql_str(dt(r[3], 10))})" for r in enrollment_rows) + ";")
add("")
add("INSERT INTO `learning_record` (`id`, `student_id`, `video_id`, `position`, `finished`, `update_time`) VALUES")
add(",\n".join(f"({r[0]}, {r[1]}, {r[2]}, {r[3]}, {r[4]}, {sql_str(r[5])})" for r in learning_rows) + ";")
add("")

# 回填选课进度：按后端口径 已完成视频数/总视频数
add("-- 回填选课进度（口径与后端 refreshCourseProgress 一致）")
add("UPDATE `course_enrollment` e SET e.progress = (")
add("  SELECT ROUND(COUNT(*) * 100.0 / (")
add("    SELECT COUNT(v.id) FROM video v JOIN chapter ch ON v.chapter_id = ch.id WHERE ch.course_id = e.course_id")
add("  ), 2) FROM learning_record lr")
add("  JOIN video v ON lr.video_id = v.id JOIN chapter ch ON v.chapter_id = ch.id")
add("  WHERE lr.student_id = e.student_id AND ch.course_id = e.course_id AND lr.finished = 1")
add(");")
add("")

# ============================================================
# 5. 练习记录（含错题，供易错题统计与错题本）
# ============================================================
add("-- ---------- 5. 练习记录（错题分布：q1/q3 为高频易错题） ----------")
practice_rows = []
pr_id = 1
# 所有客观题（排除简答 type=4）
objective_qs = [(r[0], r[1], r[6]) for r in fixed_rows if r[3] in (1, 2, 3)] + \
               [(1, 1, "B"), (2, 1, "错"), (3, 1, "C"), (4, 1, "ABC")]
for sid in student_ids:
    for _ in range(random.randint(5, 15)):
        q = random.choice(objective_qs)
        qid_, qcourse, correct_ans = q
        # 70% 答对；q1/q3/q6 设为易错题（答错率更高）
        wrong_bias = 0.55 if qid_ in (1, 3, 6) else 0.3
        is_correct = random.random() > wrong_bias
        if is_correct:
            stu_ans = correct_ans
        else:
            stu_ans = "A" if correct_ans != "A" else "B"
        practice_rows.append((pr_id, sid, qid_, stu_ans, 1 if is_correct else 0,
                              1 if (not is_correct and random.random() < 0.3) else 0,
                              dt(random.randint(0, 13), random.randint(8, 23))))
        pr_id += 1
add("INSERT INTO `practice_record` (`id`, `student_id`, `question_id`, `student_answer`, `correct`, `mastered`, `create_time`) VALUES")
add(",\n".join(f"({r[0]}, {r[1]}, {r[2]}, {sql_str(r[3])}, {r[4]}, {r[5]}, {sql_str(r[6])})" for r in practice_rows) + ";")
add("")

# ============================================================
# 6. 考试：2 套已发布试卷 + 考试记录 + 答题明细
# ============================================================
add("-- ---------- 6. 考试（试卷 + 记录 + 答题明细） ----------")
add("INSERT INTO `exam` (`id`, `course_id`, `title`, `duration`, `question_ids`, `status`, `create_time`) VALUES")
add(f"(1, 1, 'Java 基础阶段测验', 60, '[1, 2, 3, 4]', 1, {sql_str(dt(10, 9))}),")
add(f"(2, 4, 'MySQL 基础测验', 45, '[6, 7, 8]', 1, {sql_str(dt(8, 9))});")
add("")

# 考试记录：部分学生参加，分数 40-100
exam_record_rows = []
exam_answer_rows = []
er_id = 1
ea_id = 1
exam_questions = {1: [1, 2, 3, 4], 2: [6, 7, 8]}
q_answer_map = {r[0]: r[6] for r in fixed_rows}
q_answer_map.update({1: "B", 2: "错", 3: "C", 4: "ABC"})
for sid in student_ids:
    for exam_id in [1, 2]:
        if random.random() < 0.5:  # 50% 参加每场考试
            qs = exam_questions[exam_id]
            n_correct = random.randint(0, len(qs))
            score = round(n_correct * 100.0 / len(qs), 1)
            submit_day = random.randint(1, 8)
            exam_record_rows.append((er_id, exam_id, sid, score, dt(submit_day, random.randint(10, 22))))
            correct_set = set(random.sample(qs, n_correct))
            for q in qs:
                is_correct = q in correct_set
                stu_ans = q_answer_map[q] if is_correct else ("A" if q_answer_map[q] != "A" else "B")
                exam_answer_rows.append((ea_id, er_id, q, stu_ans, 1 if is_correct else 0))
                ea_id += 1
            er_id += 1
add("INSERT INTO `exam_record` (`id`, `exam_id`, `student_id`, `score`, `submit_time`) VALUES")
add(",\n".join(f"({r[0]}, {r[1]}, {r[2]}, {r[3]}, {sql_str(r[4])})" for r in exam_record_rows) + ";")
add("")
add("INSERT INTO `exam_answer` (`id`, `record_id`, `question_id`, `student_answer`, `correct`) VALUES")
add(",\n".join(f"({r[0]}, {r[1]}, {r[2]}, {sql_str(r[3])}, {r[4]})" for r in exam_answer_rows) + ";")
add("")

# ============================================================
# 7. AI 会话与消息（近 14 天分布，填充 AI 调用趋势）
# ============================================================
add("-- ---------- 7. AI 答疑会话（近 14 天分布，填充 AI 调用趋势图） ----------")
ai_questions = [
    "什么是面向对象的多态？", "Java 中 == 和 equals 的区别？", "如何优化慢 SQL？",
    "索引为什么能加快查询？", "二叉树和平衡树有什么区别？", "Linux 如何查看端口占用？",
    "导数的几何意义是什么？", "矩阵什么时候可逆？", "四级听力有什么技巧？",
]
session_rows = []
msg_rows = []
sess_id = 1
msg_id = 1
for sid in student_ids:
    for _ in range(random.randint(0, 3)):
        day = random.randint(0, 13)
        q = random.choice(ai_questions)
        course = random.choice([1, 4, 5, 7, None])
        session_rows.append((sess_id, sid, course, q[:30], dt(day, random.randint(9, 22))))
        t1 = dt(day, random.randint(9, 22))
        msg_rows.append((msg_id, sess_id, "user", q, t1))
        msg_id += 1
        msg_rows.append((msg_id, sess_id, "assistant",
                         "这是一个很好的问题。" + q + " 涉及到本课程的核心概念，建议结合章节视频中的示例理解，关键在于掌握其适用场景与边界条件。",
                         t1))
        msg_id += 1
        sess_id += 1
add("INSERT INTO `ai_chat_session` (`id`, `student_id`, `course_id`, `title`, `create_time`) VALUES")
add(",\n".join(f"({r[0]}, {r[1]}, {r[2] if r[2] else 'NULL'}, {sql_str(r[3])}, {sql_str(r[4])})" for r in session_rows) + ";")
add("")
add("INSERT INTO `ai_chat_message` (`id`, `session_id`, `role`, `content`, `create_time`) VALUES")
add(",\n".join(f"({r[0]}, {r[1]}, {sql_str(r[2])}, {sql_str(r[3])}, {sql_str(r[4])})" for r in msg_rows) + ";")
add("")

# ============================================================
# 8. 积分体系（账户 + 明细 + 签到 + 兑换，严格核算平衡）
# ============================================================
add("-- ---------- 8. 积分（签到/完课/考试奖励/兑换，账户与明细严格平衡） ----------")
# 统计每个学生的完课数（用于 video_finish 明细）
finish_count = {}
for (lid, sid, vid, pos, fin, t) in learning_rows:
    if fin == 1:
        finish_count[sid] = finish_count.get(sid, 0) + 1
# 考试及格（>=60）次数
exam_pass_count = {}
for (rid, eid, sid, score, t) in exam_record_rows:
    if score >= 60:
        exam_pass_count[sid] = exam_pass_count.get(sid, 0) + 1

sign_rows = []
points_rows = []
exchange_rows = []
sg_id = 1
pt_id = 7  # init.sql 已用自增 id 1-6
ex_id = 1
accounts = {}
for sid in student_ids:
    earned = 100  # 注册赠送
    spent = 0
    points_rows.append((pt_id, sid, 6, 100, "注册赠送积分", dt(13, 10)))
    pt_id += 1
    # 签到：近 14 天随机若干天（每天 +5）
    sign_days = sorted(random.sample(range(0, 14), random.randint(3, 10)))
    for d in sign_days:
        sign_rows.append((sg_id, sid, (NOW - timedelta(days=d)).strftime("%Y-%m-%d"), dt(d, 8)))
        sg_id += 1
        points_rows.append((pt_id, sid, 2, 5, "每日签到", dt(d, 8)))
        pt_id += 1
        earned += 5
    # 完课奖励（每个完课视频 +10）
    for _ in range(finish_count.get(sid, 0)):
        points_rows.append((pt_id, sid, 1, 10, "完成视频学习", dt(random.randint(0, 12), random.randint(9, 22))))
        pt_id += 1
        earned += 10
    # 考试及格奖励（每次 +20）
    for _ in range(exam_pass_count.get(sid, 0)):
        points_rows.append((pt_id, sid, 3, 20, "考试及格奖励", dt(random.randint(0, 8), random.randint(10, 22))))
        pt_id += 1
        earned += 20
    # AI 提问奖励（每个会话 +2）
    n_sessions = sum(1 for s in session_rows if s[1] == sid)
    for _ in range(n_sessions):
        points_rows.append((pt_id, sid, 4, 2, "AI 答疑提问奖励", dt(random.randint(0, 13), random.randint(9, 22))))
        pt_id += 1
        earned += 2
    # 兑换积分课程（扣积分 + 兑换记录）
    for cid in profiles[sid]["paid"]:
        cost = paid_courses[cid]
        points_rows.append((pt_id, sid, 5, -cost, f"兑换课程《{[c for c in courses if c[0]==cid][0][2]}》", dt(random.randint(0, 10), 15)))
        pt_id += 1
        spent += cost
        exchange_rows.append((ex_id, sid, cid, cost, 1, dt(random.randint(0, 10), 15)))
        ex_id += 1
    accounts[sid] = (earned - spent, earned, spent)

add("INSERT INTO `sign_record` (`id`, `user_id`, `sign_date`, `create_time`) VALUES")
add(",\n".join(f"({r[0]}, {r[1]}, {sql_str(r[2])}, {sql_str(r[3])})" for r in sign_rows) + ";")
add("")
add("INSERT INTO `points_record` (`id`, `user_id`, `type`, `change_value`, `description`, `create_time`) VALUES")
add(",\n".join(f"({r[0]}, {r[1]}, {r[2]}, {r[3]}, {sql_str(r[4])}, {sql_str(r[5])})" for r in points_rows) + ";")
add("")
add("INSERT INTO `course_exchange_record` (`id`, `user_id`, `course_id`, `points_cost`, `status`, `create_time`) VALUES")
add(",\n".join(f"({r[0]}, {r[1]}, {r[2]}, {r[3]}, {r[4]}, {sql_str(r[5])})" for r in exchange_rows) + ";")
add("")
add("INSERT INTO `points_account` (`user_id`, `balance`, `total_earned`, `total_spent`) VALUES")
add(",\n".join(f"({sid}, {v[0]}, {v[1]}, {v[2]})" for sid, v in accounts.items()) + ";")
add("")

# ============================================================
# 9. 评论与公告补充
# ============================================================
add("-- ---------- 9. 课程评论（多课程、多状态） ----------")
comment_texts = [
    "课程内容很扎实，讲解循序渐进，推荐！", "老师举例贴近实际，学完收获很大。",
    "希望增加更多课后练习。", "视频节奏适中，适合自学。", "章节划分清晰，重点突出。",
    "有些概念讲得偏快，需要多看几遍。", "实战项目很有帮助，点赞！",
]
cm_id = 3  # init.sql 已用 2 条
comment_rows = []
for cid in [1, 4, 5, 6, 7]:
    for _ in range(random.randint(2, 4)):
        sid = random.choice(student_ids)
        status = random.choices([1, 0, 2], weights=[7, 2, 1])[0]
        comment_rows.append((cm_id, sid, cid, random.choice(comment_texts), status, dt(random.randint(0, 12), random.randint(9, 22))))
        cm_id += 1
add("INSERT INTO `course_comment` (`id`, `user_id`, `course_id`, `content`, `status`, `create_time`) VALUES")
add(",\n".join(f"({r[0]}, {r[1]}, {r[2]}, {sql_str(r[3])}, {r[4]}, {sql_str(r[5])})" for r in comment_rows) + ";")
add("")
add("-- ---------- 10. 公告补充 ----------")
add("INSERT INTO `notice` (`title`, `content`, `type`, `top`, `status`, `create_time`) VALUES")
add(f"('《MySQL 数据库从入门到精通》新课上线', '王老师新课已上架，免费学习，快来选课吧！', 3, 0, 1, {sql_str(dt(9, 10))}),")
add(f"('学习打卡活动开启', '连续签到 7 天可额外获得积分奖励，坚持学习从今天开始。', 2, 0, 1, {sql_str(dt(6, 10))}),")
add(f"('系统维护通知', '平台将于本周六凌晨 2:00-4:00 进行例行维护，期间服务可能短暂中断。', 1, 0, 1, {sql_str(dt(3, 10))});")
add("")
add("-- 演示数据导入完成")

with open("/workspace/backend/sql/demo_data.sql", "w", encoding="utf-8") as f:
    f.write("\n".join(lines))

# 一致性自检
print("=== 一致性自检 ===")
total_points_check = True
for sid, (bal, earned, spent) in accounts.items():
    if bal != earned - spent:
        total_points_check = False
        print(f"  [FAIL] 学生 {sid} 积分不平衡")
print(f"  积分账户平衡: {'OK' if total_points_check else 'FAIL'}")
print(f"  用户数: {len(user_rows)}（1 教师 + {len(student_ids)} 学生）")
print(f"  新增课程: {len(courses)} 门，章节 {len(chapter_rows)}，视频 {len(video_rows)}")
print(f"  选课记录: {len(enrollment_rows)}，学习记录: {len(learning_rows)}")
print(f"  练习记录: {len(practice_rows)}，考试记录: {len(exam_record_rows)}，答题明细: {len(exam_answer_rows)}")
print(f"  AI 会话: {len(session_rows)}，消息: {len(msg_rows)}")
print(f"  签到: {len(sign_rows)}，积分明细: {len(points_rows)}，兑换: {len(exchange_rows)}")
print(f"  评论: {len(comment_rows)}")
