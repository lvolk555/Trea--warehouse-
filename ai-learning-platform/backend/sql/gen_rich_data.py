#!/usr/bin/env python3
"""生成第三阶段演示数据 rich_data.sql（在 init.sql + demo_data.sql 之后执行）

定位：补齐 demo_data 未覆盖的表，并扩充数据量偏少的表，使每个功能模块都有可观数据：
  - study_note            学习笔记（此前 0 条）
  - points_activity_record 积分活动领取记录（此前 0 条）
  - user_coupon           用户优惠券（此前 0 条）
  - exam / notice / course_exchange_record / question / points_activity 扩充到 10+

与后端逻辑严格对齐（生成前会解析 init.sql + demo_data.sql 的现状，保证外键引用真实）：
  1. 任务领取 → points_activity_record + points_record(type=7) + 账户余额增量；
     领取人必须真实完成任务：profile=有头像昵称 / ai_ask=有 AI 会话 /
     chapter_finish=学完某章全部视频 / exam_pass=有及格记录（与 ActivityService.checkTaskDone 同口径）
  2. 优惠券领取 → points_activity_record(reward=0) + user_coupon 快照（含未使用/已使用/已过期）
  3. 课程兑换 → 积分扣减(type=5) + 自动选课 + 优惠券核销(status=1, used_time)；
     折扣计算与 ExchangeService.calcDiscount 一致：满减券 min(value, cost)，折扣券 round(cost*value/100)
  4. 考试及格 → points_record(type=3, +20)（与 ExamService 的 grantOnceByRule 口径一致）
  5. 完成视频 → points_record(type=1, +10)；选课进度回填与 refreshCourseProgress 口径一致
  6. 兑换只发生在未选课/未兑换过该课的学生上（与 ExchangeService 防重复逻辑一致）

生成后自检：所有账户最终余额 = demo 余额 + 新增收入 - 新增支出，且 ≥ 0。
"""
import json
import os
import random
import re
from collections import defaultdict
from datetime import datetime, timedelta

random.seed(2026)
NOW = datetime.now().replace(microsecond=0)
SQL_DIR = os.path.dirname(os.path.abspath(__file__))


def dt(days_ago, hour=None, minute=None):
    d = NOW - timedelta(days=days_ago)
    if hour is not None:
        d = d.replace(hour=hour, minute=minute if minute is not None else random.randint(0, 59),
                      second=random.randint(0, 59))
    return d.strftime("%Y-%m-%d %H:%M:%S")


def sql_str(s):
    return "'" + str(s).replace("'", "''") + "'"


# ============================================================
# 一、解析 init.sql / demo_data.sql（保证引用的 ID 真实存在）
# ============================================================
def split_fields(s):
    """按顶层逗号切分（引号感知，处理 '' 转义）"""
    fields, cur, in_str, i, n = [], [], False, 0, len(s)
    while i < n:
        c = s[i]
        if in_str:
            cur.append(c)
            if c == "'":
                if i + 1 < n and s[i + 1] == "'":
                    cur.append("'")
                    i += 2
                    continue
                in_str = False
            i += 1
            continue
        if c == "'":
            in_str = True
            cur.append(c)
            i += 1
            continue
        if c == ',':
            fields.append(''.join(cur).strip())
            cur = []
            i += 1
            continue
        cur.append(c)
        i += 1
    fields.append(''.join(cur).strip())
    return fields


def parse_value(f):
    if f == 'NULL':
        return None
    if f.startswith("'"):
        return f[1:-1].replace("''", "'")
    try:
        return int(f)
    except ValueError:
        try:
            return float(f)
        except ValueError:
            return f


def parse_tuples(values_text):
    """解析 VALUES 后的元组（元组可跨行，引号感知）"""
    tuples, row, cur, in_str, i, n = [], None, [], False, 0, len(values_text)
    while i < n:
        c = values_text[i]
        if in_str:
            cur.append(c)
            if c == "'":
                if i + 1 < n and values_text[i + 1] == "'":
                    cur.append("'")
                    i += 2
                    continue
                in_str = False
            i += 1
            continue
        if c == "'":
            in_str = True
            cur.append(c)
            i += 1
            continue
        if c == '(':
            row, cur = [], []
            i += 1
            continue
        if c == ',' and row is not None:
            row.append(parse_value(''.join(cur).strip()))
            cur = []
            i += 1
            continue
        if c == ')':
            row.append(parse_value(''.join(cur).strip()))
            tuples.append(row)
            row, cur = None, []
            i += 1
            continue
        cur.append(c)
        i += 1
    return tuples


def parse_inserts(text):
    """返回 {table: [ {col: val}, ... ]}（init 在前 demo 在后调用合并）"""
    result = {}
    for m in re.finditer(r"INSERT INTO `(\w+)`\s*\(([^)]+)\) VALUES\n(.*?);\n", text, re.S):
        table, cols_text, values_text = m.group(1), m.group(2), m.group(3)
        cols = [c.strip().strip('`') for c in cols_text.split(',')]
        rows = [dict(zip(cols, t)) for t in parse_tuples(values_text)]
        result.setdefault(table, []).extend(rows)
    return result


def read(fname):
    with open(os.path.join(SQL_DIR, fname), encoding='utf-8') as f:
        return f.read()


init_rows = parse_inserts(read('init.sql'))
demo_rows = parse_inserts(read('demo_data.sql'))


def raw_rows(t):
    return init_rows.get(t, []) + demo_rows.get(t, [])


def table(t):
    """为无显式 id 的行按插入顺序补 id（init.sql 在全新库上自增即此顺序）"""
    out, counter = [], 0
    for r in raw_rows(t):
        r = dict(r)
        if r.get('id') is not None:
            counter = max(counter, r['id'])
        else:
            counter += 1
            r['id'] = counter
        out.append(r)
    return out


users = table('user')
courses = {c['id']: c for c in table('course')}
chapters = table('chapter')
videos = table('video')
questions = table('question')
enrollments = table('course_enrollment')
learning = table('learning_record')
exam_records = table('exam_record')
sessions = table('ai_chat_session')
messages = table('ai_chat_message')
accounts = {a['user_id']: a for a in table('points_account')}
activities = table('points_activity')  # init 7 条，按顺序 id=1..7
exchanges = table('course_exchange_record')

students_demo = [u['id'] for u in users if re.fullmatch(r'stu\d+', u['username'])]
students_init = [u['id'] for u in users if re.fullmatch(r'student\d', u['username'])]

enrolled = defaultdict(set)
for e in enrollments:
    enrolled[e['student_id']].add(e['course_id'])
learned_pairs = {(lr['student_id'], lr['video_id']) for lr in learning}
finished = defaultdict(set)
for lr in learning:
    if lr['finished'] == 1:
        finished[lr['student_id']].add(lr['video_id'])
videos_of_chapter = defaultdict(list)
for v in videos:
    videos_of_chapter[v['chapter_id']].append(v['id'])
chapters_of_course = defaultdict(list)
for ch in chapters:
    chapters_of_course[ch['course_id']].append(ch['id'])
videos_of_course = defaultdict(list)
for ch in chapters_of_course:
    for vid in videos_of_chapter[ch]:
        videos_of_course[ch].append(vid)
passed = {r['student_id'] for r in exam_records if (r['score'] or 0) >= 60}
ai_students = {s['student_id'] for s in sessions}
q_by_course = defaultdict(list)
for q in questions:
    q_by_course[q['course_id']].append(q)
exchanged_pairs = {(x['user_id'], x['course_id']) for x in exchanges if x['status'] == 1}


def chapter_completed(sid, chapter_id):
    vids = videos_of_chapter[chapter_id]
    return bool(vids) and all(v in finished[sid] for v in vids)


def has_finished_chapter(sid):
    return any(chapter_completed(sid, ch) for ch in {v['chapter_id'] for v in videos
                                                     if v['id'] in finished[sid]})

# ============================================================
# 二、生成 SQL
# ============================================================
lines = []
add = lines.append
add("-- ============================================================")
add("-- AI 辅助在线学习平台 第三阶段演示数据（rich_data.sql）")
add("-- 前置：依次执行 init.sql → demo_data.sql → 本文件")
add("-- 定位：补齐学习笔记/活动领取/优惠券等表，并扩充试卷、公告、兑换记录")
add("-- 由 gen_rich_data.py 生成（随机种子固定，可重复生成）")
add("-- ============================================================")
add("USE ai_learning;")
add("")

# ---------- 1. 用户头像（激活"完善个人资料"任务，本地图片） ----------
avatar_students = students_init + students_demo[:10]
add("-- ---------- 1. 用户头像（完善个人资料任务的真实完成条件） ----------")
for i, sid in enumerate(avatar_students):
    add(f"UPDATE `user` SET `avatar` = '/api/files/avatar-{i % 8 + 1:02d}.jpg' WHERE `id` = {sid};")
add("")

# ---------- 2. 课程状态演示：课程3置为已驳回；新增 待审核/已下架 课程 ----------
add("-- ---------- 2. 课程状态机演示（待审核/已上架/已下架/已驳回 四态齐全） ----------")
add("UPDATE `course` SET `status` = 3 WHERE `id` = 3;  -- Web 前端开发实战 → 已驳回（教师需重新修改）")
new_course_id = max(courses) + 1
c_pending, c_offline = new_course_id, new_course_id + 1
add("INSERT INTO `course` (`id`, `teacher_id`, `title`, `cover`, `category`, `description`, `price_type`, `points_price`, `status`, `create_time`) VALUES")
add(f"({c_pending}, 2, 'Vue.js 3.0 组件化开发', '/api/files/course-cover-web.jpg', '编程', "
    f"'系统讲解 Vue3 组合式 API、组件通信、状态管理与路由，配合实战项目构建现代前端应用。', 1, 0, 0, {sql_str(dt(2, 9))}),")
add(f"({c_offline}, 2, 'Python 网络爬虫实战', '/api/files/course-cover-python.jpg', '编程', "
    f"'从 HTTP 协议到 Requests 与 BeautifulSoup，讲解数据采集、清洗与存储的完整流程。', 1, 0, 2, {sql_str(dt(12, 15))});")
add("")
new_chapter_id = max(c['id'] for c in chapters) + 1
new_video_id = max(v['id'] for v in videos) + 1
ch_rows, vd_rows = [], []
for cid, names in [(c_pending, ['组件基础与模板语法', '组合式 API']),
                   (c_offline, ['HTTP 与 Requests 入门', '页面解析与数据存储'])]:
    for ci, ch_name in enumerate(names):
        ch_rows.append((new_chapter_id, cid, ch_name, ci + 1))
        for vi in range(2):
            vd_rows.append((new_video_id, new_chapter_id, f"{ci + 1}.{vi + 1} {ch_name}第{vi + 1}节",
                            '/api/files/videos/mov_bbb.mp4' if vi == 0 else '/api/files/videos/movie.mp4',
                            random.randint(700, 1400), vi + 1))
            new_video_id += 1
        new_chapter_id += 1
add("INSERT INTO `chapter` (`id`, `course_id`, `title`, `sort_order`) VALUES")
add(",\n".join(f"({r[0]}, {r[1]}, {sql_str(r[2])}, {r[3]})" for r in ch_rows) + ";")
add("")
add("INSERT INTO `video` (`id`, `chapter_id`, `title`, `url`, `duration`, `sort_order`) VALUES")
add(",\n".join(f"({r[0]}, {r[1]}, {sql_str(r[2])}, {sql_str(r[3])}, {r[4]}, {r[5]})" for r in vd_rows) + ";")
add("")
# 新课视频也纳入章节完成判定数据源
for r in ch_rows:
    chapters_of_course[r[1]].append(r[0])
for r in vd_rows:
    videos_of_chapter[r[1]].append(r[0])

# ---------- 3. 补充题库（课程 2/6/7/8/9/10 扩充，使每门课可组卷） ----------
add("-- ---------- 3. 补充题库（让每门已上架课程都有可组卷的题目量） ----------")
new_q_id = max(q['id'] for q in questions) + 1
new_questions = [
    # (course, type, content, options, answer, analysis)
    (2, 1, "Python 中哪个库主要用于科学计算与多维数组？", '["NumPy", "Requests", "Flask", "Scrapy"]', "A", "NumPy 提供多维数组与数值计算能力，是数据分析生态的基石。"),
    (2, 1, "以下哪项是 Pandas 的核心数据结构？", '["Series", "List", "Tuple", "Set"]', "A", "Series 是一维带标签数组，配合 DataFrame 构成 Pandas 核心。"),
    (2, 2, "以下属于 Python 内置类型的有？", '["list", "dict", "set", "DataFrame"]', "ABC", "DataFrame 属于 Pandas 第三方库，不是内置类型。"),
    (2, 3, "Matplotlib 主要用于数据可视化。", '["对", "错"]', "对", "Matplotlib 是 Python 最基础的绘图库。"),
    (6, 1, "Linux 中查看文件内容的命令是？", '["cat", "mv", "cp", "mkdir"]', "A", "cat 用于输出文件内容到终端。"),
    (7, 1, "函数 f(x)=x² 在 x=1 处的导数值是？", '["1", "2", "0", "3"]', "B", "(x²)'=2x，代入 x=1 得 2。"),
    (8, 1, "单位矩阵的主对角线元素为？", '["0", "1", "-1", "2"]', "B", "单位矩阵对角线全为 1，其余为 0。"),
    (9, 1, "'abundant' 的中文意思是？", '["丰富的", "贫乏的", "吸收", "放弃"]', "A", "abundant 意为大量的、丰富的。"),
    (9, 3, "四级写作建议先列提纲再动笔。", '["对", "错"]', "对", "列提纲能保证逻辑连贯，避免跑题。"),
    (10, 1, "界面排版中对齐的主要作用是？", '["建立视觉秩序", "增加颜色", "放大字号", "减少文字"]', "A", "对齐让元素建立关联，形成整洁的视觉流。"),
    (10, 3, "留白是界面设计中的空间浪费。", '["对", "错"]', "错", "留白是提升可读性与层次感的重要手段。"),
]
first_chapter = {cid: chapters_of_course[cid][0] for cid in courses if chapters_of_course.get(cid)}
q_rows = []
for cid, qtype, content, options, answer, analysis in new_questions:
    q_rows.append((new_q_id, cid, first_chapter[cid], qtype, content, options, answer, analysis))
    q_by_course[cid].append({'id': new_q_id, 'type': qtype, 'content': content,
                             'options': options, 'answer': answer})
    new_q_id += 1
add("INSERT INTO `question` (`id`, `course_id`, `chapter_id`, `type`, `content`, `options`, `answer`, `analysis`) VALUES")
add(",\n".join(f"({r[0]}, {r[1]}, {r[2]}, {r[3]}, {sql_str(r[4])}, {sql_str(r[5])}, {sql_str(r[6])}, {sql_str(r[7])})"
              for r in q_rows) + ";")
add("")

# ---------- 4. 新试卷（8 套，试卷总数达 10） ----------
add("-- ---------- 4. 新试卷（每门已上架课程均有测验） ----------")
exam_specs = [(1, 'Java 面向对象综合测验', 60), (2, 'Python 数据分析入门测验', 45),
              (4, 'MySQL 进阶测验', 45), (5, '数据结构期中测验', 60),
              (6, 'Linux 基础命令测验', 30), (7, '高数第一章测验', 40),
              (8, '线性代数基础测验', 30), (9, '英语四级词汇测验', 30), (10, 'UI 设计基础测验', 30)]
new_exam_id = max(e['id'] for e in table('exam')) + 1
new_exams = []
add("INSERT INTO `exam` (`id`, `course_id`, `title`, `duration`, `question_ids`, `status`, `create_time`) VALUES")
exam_sql = []
for cid, title, duration in exam_specs:
    qs = [q['id'] for q in q_by_course[cid] if q['type'] != 4]  # 模拟判分只放客观题
    new_exams.append({'id': new_exam_id, 'course_id': cid, 'questions': qs})
    exam_sql.append(f"({new_exam_id}, {cid}, {sql_str(title)}, {duration}, "
                    f"{sql_str(json.dumps(qs))}, 1, {sql_str(dt(random.randint(6, 12), 9))})")
    new_exam_id += 1
add(",\n".join(exam_sql) + ";")
add("")

# ---------- 5. 选课/学习补充（student1/2/3 的学习轨迹 + 兑换学生的资金积累） ----------
new_enroll_id = max(e['id'] for e in enrollments) + 1
new_lr_id = max(l['id'] for l in learning) + 1
extra_learning = []      # (id, student, video, pos, finished, time)
video_dur = {v['id']: v['duration'] for v in videos}
video_title = {v['id']: v['title'] for v in videos}

# student3 先补一条选课（init 中无；其视频学习记录统一在下方循环生成，避免重复）
u5 = students_init[2]
add("-- ---------- 5. 选课与学习补充 ----------")
add(f"INSERT INTO `course_enrollment` (`id`, `student_id`, `course_id`, `progress`, `create_time`) VALUES")
add(f"({new_enroll_id}, {u5}, 1, 0.00, {sql_str(dt(7, 19))});")
add("")
enrolled[u5].add(1)
new_enroll_id += 1
# student1/2/3 完成《Java 面向对象程序设计》部分视频（对应完课积分 +10/个）
for sid, vids in [(students_init[0], [1, 2, 3]), (students_init[1], [1, 2]), (u5, [1])]:
    for vid in vids:
        extra_learning.append((new_lr_id, sid, vid, int(video_dur[vid] * random.uniform(0.96, 1.0)), 1,
                               dt(random.randint(1, 5), random.randint(14, 22))))
        finished[sid].add(vid)
        learned_pairs.add((sid, vid))
        new_lr_id += 1


def add_finished_video(sid, day):
    """给学生补一条已完成的学习记录（只选已选课且未看过的视频），返回 video_id 或 None"""
    global new_lr_id
    cands = [vid for cid in enrolled[sid] for vid in videos_of_course[cid]
             if (sid, vid) not in learned_pairs]
    if not cands:
        return None
    vid = random.choice(cands)
    extra_learning.append((new_lr_id, sid, vid, int(video_dur[vid] * random.uniform(0.96, 1.0)), 1,
                           dt(day, random.randint(9, 22))))
    finished[sid].add(vid)
    learned_pairs.add((sid, vid))
    new_lr_id += 1
    return vid


# ---------- 记账（所有积分变动统一入账） ----------
base_balance = {sid: accounts[sid]['balance'] for sid in accounts}
earns = defaultdict(int)
acct_updates = defaultdict(lambda: [0, 0])  # sid -> [earned_delta, spent_delta]
points_rows = []  # (sid, type, change, desc, time)
new_pr_id = max(p['id'] for p in table('points_record')) + 1


def add_points(sid, ptype, change, desc, day, hour=None):
    global new_pr_id
    points_rows.append((new_pr_id, sid, ptype, change, desc, dt(day, hour if hour is not None else random.randint(9, 22))))
    new_pr_id += 1
    earns[sid] += change
    acct_updates[sid][0 if change > 0 else 1] += abs(change)


# ---------- 6. 考试记录与答题明细（分数与答题一致；及格 +20） ----------
def wrong_answer(q):
    if q['type'] == 3:
        return '错' if q['answer'] == '对' else '对'
    letters = [chr(65 + i) for i in range(len(json.loads(q['options'])))]
    ans = q['answer']
    if q['type'] == 2:
        if len(ans) > 1:
            return ans[:-1]
        extra = [l for l in letters if l not in ans]
        return ans + extra[0] if extra else ans
    others = [l for l in letters if l != ans]
    return random.choice(others) if others else ans


new_er_id = max(r['id'] for r in exam_records) + 1
new_ea_id = max(a['id'] for a in table('exam_answer')) + 1
exam_record_rows, exam_answer_rows = [], []
for exam in new_exams:
    # 参考人 = 该课程已选课的学生（course 2 的记录由兑换学生补充，见下）
    if exam['course_id'] == 2:
        continue
    # 按余额排序，前 6 名必参加（他们是后续课程兑换的主力，需攒足积分）；再随机补 1-2 人
    cands = sorted((sid for sid in students_demo if exam['course_id'] in enrolled[sid]),
                   key=lambda s: -base_balance.get(s, 0))
    if not cands:
        continue
    picked = cands[:6]
    rest = [s for s in cands[6:] if s not in picked]
    picked += random.sample(rest, min(len(rest), random.randint(1, 2))) if rest else []
    for sid in picked:
        qs = exam['questions']
        # 下限 = 及格所需题数（保证参与者均可获得及格奖励；题量少的卷子错 2 题就会挂）
        n_correct = random.randint(max(1, -(-len(qs) * 6 // 10)), len(qs))
        correct_set = set(random.sample(qs, n_correct))
        score = round(n_correct * 100.0 / len(qs), 1)
        day = random.randint(1, 5)
        exam_record_rows.append((new_er_id, exam['id'], sid, score, dt(day, random.randint(10, 22))))
        for qid in qs:
            q = next(x for c in [q_by_course[exam['course_id']]] for x in c if x['id'] == qid)
            ok = qid in correct_set
            exam_answer_rows.append((new_ea_id, new_er_id, qid, q['answer'] if ok else wrong_answer(q), 1 if ok else 0))
            new_ea_id += 1
        if score >= 60:
            add_points(sid, 3, 20, f"考试及格奖励（exam#{exam['id']}）", day)
            passed.add(sid)
        new_er_id += 1

# ---------- 7. AI 会话（student1/2/3，激活 ai_ask 任务） ----------
new_sess_id = max(s['id'] for s in sessions) + 1
new_msg_id = max(m['id'] for m in messages) + 1
ai_pairs = [
    (students_init[0], "Java 里多态到底怎么理解？",
     "多态是同一行为在不同对象上的不同表现。实现三要素：继承/实现、方法重写、父类引用指向子类对象。"
     "运行时 JVM 根据对象实际类型动态绑定对应方法，这就是动态绑定。建议结合《2.3 继承与多态》的示例代码理解。"),
    (students_init[1], "学完第二章后应该做什么练习？",
     "建议三步：先重写课程中的 Shape 体系示例体会重写与向上转型；再完成章节练习中的继承与多态题目；"
     "最后尝试用接口模拟一个简单支付场景。做题时先自己实现再对照解析。"),
    (u5, "封装的好处是什么？",
     "封装把数据和操作绑定并以访问控制隐藏实现细节，好处有三：降低耦合、便于维护内部实现、"
     "可通过 getter/setter 加入校验逻辑保护对象状态。可回看《2.2 封装与访问控制》巩固。"),
]
sess_rows, msg_rows = [], []
for sid, q, ans in ai_pairs:
    day = random.randint(1, 6)
    sess_rows.append((new_sess_id, sid, 1, q[:30], dt(day, random.randint(9, 21))))
    msg_rows.append((new_msg_id, new_sess_id, 'user', q, dt(day, random.randint(9, 21))))
    msg_rows.append((new_msg_id + 1, new_sess_id, 'assistant', ans, dt(day, random.randint(9, 21))))
    add_points(sid, 4, 2, "AI 答疑提问奖励", day)
    ai_students.add(sid)
    new_sess_id += 1
    new_msg_id += 2

# ---------- 8. 学习笔记（study_note） ----------
note_pool = {
    1: ["构造方法重载时 this() 必须放首行；this.属性 与局部变量同名时用 this 区分。",
        "继承里 super() 调父类构造要先于子类成员初始化，抽象类不能实例化但可以有构造器。"],
    4: ["索引像书的目录，B+ 树让范围查询变快；但索引不是越多越好，会影响写入性能。",
        "事务 ACID：原子性靠 undo log，持久性靠 redo log，隔离性靠锁与 MVCC。"],
    5: ["栈是先进后出，递归本质是函数调用栈；队列先进先出，BFS 用队列实现。",
        "二叉树遍历口诀：前序根左右、中序左根右、后序左右根；中序遍历 BST 得有序序列。"],
    6: ["pwd 显示当前目录，cd - 回上一次目录，cd ~ 回家目录。",
        "chmod 755 = rwxr-xr-x，数字是三组二进制：7=111，5=101。"],
    7: ["重要极限 lim(x→0) sinx/x = 1；用洛必达前先验证是不是 0/0 型。",
        "可导必连续，连续不一定可导，反例 |x| 在 0 处。"],
    8: ["矩阵可逆 ⇔ 行列式≠0；用初等行变换 (A|E)→(E|A⁻¹) 求逆。",
        "转置的转置等于自身，(AB)ᵀ=BᵀAᵀ，注意顺序反转。"],
}
note_rows = []
new_note_id = 1
for sid, vid in [(students_init[0], 1), (students_init[0], 2), (students_init[0], 3),
                 (students_init[1], 1), (students_init[1], 2), (u5, 1)]:
    note_rows.append((new_note_id, sid, vid, note_pool[1][new_note_id % 2], dt(random.randint(0, 5), random.randint(15, 22))))
    new_note_id += 1
# 演示学生的笔记：从有学习记录的课程中挑
for cid, pool in note_pool.items():
    studs = sorted({sid for sid in students_demo if cid in enrolled[sid]})[:3]
    for i, sid in enumerate(studs):
        vids = [v for v in videos_of_course[cid] if (sid, v) in learned_pairs]
        if not vids:
            continue
        note_rows.append((new_note_id, sid, random.choice(vids), pool[i % len(pool)],
                          dt(random.randint(0, 6), random.randint(10, 22))))
        new_note_id += 1

# ---------- 9. 积分活动领取（任务真实完成才可领，与 ActivityService 同口径） ----------
ACT_PROFILE, ACT_AI, ACT_CHAPTER, ACT_EXAM = 1, 2, 3, 4
claim_rows = []  # (id, user, activity, claim_date, reward, time)
new_claim_id = 1


def claim_task(sid, act_id, reward, title, day):
    global new_claim_id
    claim_rows.append((new_claim_id, sid, act_id, (NOW - timedelta(days=day)).strftime("%Y-%m-%d"),
                       reward, dt(day, random.randint(10, 21))))
    add_points(sid, 7, reward, title, day)
    new_claim_id += 1


for i, sid in enumerate(avatar_students):          # 完善资料（头像已 UPDATE）
    claim_task(sid, ACT_PROFILE, 10, '完善个人资料', 1 + i % 9)
ai_cands = sorted(ai_students & set(students_demo))[:9] + students_init
for i, sid in enumerate(ai_cands):                 # AI 答疑（有真实会话）
    claim_task(sid, ACT_AI, 5, '完成 AI 答疑', 1 + i % 9)
chapter_cands = [sid for sid in students_demo if has_finished_chapter(sid)][:10]
for i, sid in enumerate(chapter_cands):             # 章节学习（确实学完某章）
    claim_task(sid, ACT_CHAPTER, 15, '完成章节学习', 1 + i % 8)
exam_cands = sorted(passed & set(students_demo))[:10]
for i, sid in enumerate(exam_cands):                # 通过考试（确有及格记录）
    claim_task(sid, ACT_EXAM, 20, '通过一次考试', 1 + i % 8)

# ---------- 10. 新增优惠券活动（活动总数达 10） ----------
add("-- ---------- 6. 新增优惠券活动 ----------")
new_coupon_acts = [
    (8, '周末学习券', '周末兑换课程满 100 积分立减 20 积分', 'coupon', 2, None, 0, '周末学习券', 1, 20, 100, 10, 1, 8),
    (9, '进阶满减券', '兑换课程满 600 积分减 100 积分', 'coupon', 2, None, 0, '进阶满减券', 1, 100, 600, 30, 1, 9),
    (10, '会员折扣券', '兑换任意课程 9 折优惠', 'coupon', 2, None, 0, '会员折扣券', 2, 90, 0, 20, 1, 10),
]
add("INSERT INTO `points_activity` (`id`, `title`, `description`, `icon`, `activity_type`, `task_key`, `reward`, "
    "`coupon_name`, `coupon_type`, `coupon_value`, `coupon_threshold`, `coupon_expire_days`, `enabled`, `sort_order`) VALUES")
add(",\n".join("(" + ", ".join(
    "NULL" if v is None else (str(v) if isinstance(v, int) else sql_str(v)) for v in r) + ")" for r in new_coupon_acts) + ";")
add("")

# ---------- 11. 兑换计划（先选学生，再发券，最后核销） ----------
# 可用资金 = demo 余额 + 已累计新增收入
def avail(sid):
    return base_balance.get(sid, 0) + earns[sid]


exchange_students_pool = sorted(students_demo, key=avail, reverse=True)
# (course_id, pay, 活动id(无券为None))：折扣计算与 ExchangeService.calcDiscount 一致
plans = [
    (2, 700, 9),      # 进阶满减券（800≥600 门槛，减100）
    (5, 550, 6),      # 课程满减券（600≥500 门槛，减50）
    (5, 540, 10),     # 会员折扣券（600*0.9=540）
    (5, 510, 7),      # 全场折扣券（600*0.85=510）
    (9, 490, 5),      # 新人无门槛券（减10）
    (9, 480, 8),      # 周末学习券（500≥100 门槛，减20）
    (9, 500, None),   # 无券
]
chosen = []
used_sid = set()
for cid, pay, act in plans:
    for sid in exchange_students_pool:
        if sid in used_sid or cid in enrolled[sid] or (sid, cid) in exchanged_pairs:
            continue
        # 资金不足则补学习记录攒积分（最多补 6 个视频）
        tries = 0
        while avail(sid) < pay and tries < 6:
            if add_finished_video(sid, 1 + tries) is None:
                break
            tries += 1
        if avail(sid) >= pay:
            chosen.append((sid, cid, pay, act))
            used_sid.add(sid)
            break
assert len(chosen) == len(plans), f"兑换学生不足：{len(chosen)}/{len(plans)}"
# 兑换学生补考新试卷（Python 测验），及格再 +20（也满足"兑换后可考试"的真实流转）
for sid, cid, pay, act in chosen:
    if cid == 2:
        exam = next(e for e in new_exams if e['course_id'] == 2)
        qs = exam['questions']
        n_correct = random.randint(2, len(qs))
        correct_set = set(random.sample(qs, n_correct))
        score = round(n_correct * 100.0 / len(qs), 1)
        day = random.randint(0, 3)
        exam_record_rows.append((new_er_id, exam['id'], sid, score, dt(day, random.randint(10, 22))))
        for qid in qs:
            q = next(x for x in q_by_course[2] if x['id'] == qid)
            ok = qid in correct_set
            exam_answer_rows.append((new_ea_id, new_er_id, qid, q['answer'] if ok else wrong_answer(q), 1 if ok else 0))
            new_ea_id += 1
        if score >= 60:
            add_points(sid, 3, 20, f"考试及格奖励（exam#{exam['id']}）", day)
            passed.add(sid)
        new_er_id += 1

# ---------- 12. 优惠券（领券 → 5 张被兑换核销，2 张过期，其余未使用） ----------
coupon_owners = {(sid, cid, pay, act): act for (sid, cid, pay, act) in chosen if act}
used_coupon_map = {}  # (act_id) -> (owner_sid, use_day)
for (sid, cid, pay, act) in chosen:
    if act:
        used_coupon_map[act] = (sid, 1 + random.randint(0, 3))
ACT_INFO = {a['id']: a for a in activities}
ACT_INFO.update({r[0]: {'title': r[1], 'coupon_name': r[7], 'coupon_type': r[8], 'coupon_value': r[9],
                        'coupon_threshold': r[10], 'coupon_expire_days': r[11]} for r in new_coupon_acts})
new_coupon_id = 1
coupon_rows = []  # (id, user, act, name, type, value, threshold, status, expire, used_time, create_time)
coupon_claims = []  # (id, user, act, claim_date, reward=0, time)


def emit_coupon(sid, act_id, status, claim_day):
    """生成一条领券记录与券快照；返回券 id"""
    global new_coupon_id, new_claim_id
    a = ACT_INFO[act_id]
    expire_days = a['coupon_expire_days'] or 30
    claim_t = dt(claim_day, random.randint(10, 21))
    expire_t = (NOW - timedelta(days=claim_day) + timedelta(days=expire_days)).strftime("%Y-%m-%d %H:%M:%S")
    used_t = None
    if status == 1:  # 已使用：核销时间在领券之后、过期之前
        used_day = max(0, claim_day - random.randint(1, 3))
        used_t = dt(used_day, random.randint(10, 21))
    elif status == 2:  # 已过期：过期时间在过去
        expire_t = dt(random.randint(1, 3), 12)
    coupon_claims.append((new_claim_id, sid, act_id, (NOW - timedelta(days=claim_day)).strftime("%Y-%m-%d"), 0, claim_t))
    coupon_rows.append((new_coupon_id, sid, act_id, a['coupon_name'], a['coupon_type'], a['coupon_value'],
                        a['coupon_threshold'] or 0, status, expire_t, used_t, claim_t))
    cid_ = new_coupon_id
    new_coupon_id += 1
    new_claim_id += 1
    return cid_


# 兑换用券：归属对应兑换学生，核销时间与兑换时间一致
coupon_id_by_act = {}
for act_id, (owner, use_day) in used_coupon_map.items():
    a = ACT_INFO[act_id]
    expire_days = a['coupon_expire_days'] or 30
    claim_day = use_day + random.randint(1, 4)
    claim_t = dt(claim_day, random.randint(10, 21))
    expire_t = (NOW - timedelta(days=claim_day) + timedelta(days=expire_days)).strftime("%Y-%m-%d %H:%M:%S")
    coupon_claims.append((new_claim_id, owner, act_id, (NOW - timedelta(days=claim_day)).strftime("%Y-%m-%d"), 0, claim_t))
    coupon_rows.append((new_coupon_id, owner, act_id, a['coupon_name'], a['coupon_type'], a['coupon_value'],
                        a['coupon_threshold'] or 0, 1, expire_t, dt(use_day, random.randint(10, 21)), claim_t))
    coupon_id_by_act[act_id] = new_coupon_id
    new_coupon_id += 1
    new_claim_id += 1
# 其余学生领券：未使用/已过期混合
other_students = [s for s in students_demo if s not in used_sid][:12]
coupon_plan = [(5, 0), (5, 2), (6, 0), (6, 0), (7, 0), (7, 2), (8, 0), (9, 0), (10, 0), (5, 0), (6, 0), (10, 0)]
for (sid, (act_id, status)) in zip(other_students, coupon_plan):
    emit_coupon(sid, act_id, status, random.randint(2, 8))

# ---------- 13. 兑换执行（扣积分 + 自动选课 + 核销优惠券） ----------
new_ex_id = max((x['id'] for x in exchanges), default=0) + 1
exchange_rows = []
exchange_enrolls = []
for (sid, cid, pay, act) in chosen:
    day = 1 + random.randint(0, 3)
    coupon_id = coupon_id_by_act.get(act)
    discount = courses[cid]['points_price'] - pay
    exchange_rows.append((new_ex_id, sid, cid, pay, coupon_id, discount if discount > 0 else None, 1, dt(day, random.randint(10, 21))))
    exchange_enrolls.append((new_enroll_id, sid, cid, 0.00, dt(day, random.randint(10, 21))))
    enrolled[sid].add(cid)
    new_enroll_id += 1
    add_points(sid, 5, -pay, f"兑换课程《{courses[cid]['title']}》", day)
    new_ex_id += 1

# ---------- 14. 公告（总数达 10+） ----------
notices = [
    ('《Vue.js 3.0 组件化开发》上线', '王老师新课已提交审核，涵盖组合式 API 与实战项目，敬请期待。', 3, 0, 1, 2),
    ('周末双倍积分活动开启', '本周末完成视频学习与章节练习，任务奖励积分翻倍，快来参与。', 2, 1, 1, 4),
    ('题库全面更新', 'MySQL、数据结构、Linux 等课程新增练习题，章节练习已同步扩充。', 1, 0, 1, 5),
    ('《数据结构与算法》期中测验上线', '课程期中测验已发布，满分 100 分，及格可获积分奖励。', 3, 0, 1, 6),
    ('连续签到挑战', '连续签到 7 天累计可获 35 积分，坚持学习从今天开始。', 2, 0, 1, 8),
    ('考试周安排通知', '本周各课程测验集中开放，请合理安排复习时间，测验次数不限。', 1, 0, 1, 9),
    ('新券上线：进阶满减券', '兑换课程满 600 积分立减 100 积分，可在积分中心领取。', 2, 0, 1, 10),
    ('积分商城上新', '积分兑换课程新增《Python 网络爬虫实战》（暂已下架整理，重新上架后可兑换）。', 2, 0, 0, 11),
]

# ============================================================
# 三、输出 SQL（按依赖顺序）
# ============================================================
add("-- ---------- 7. 学习记录补充（含进度回填，口径与后端一致） ----------")
add("INSERT INTO `learning_record` (`id`, `student_id`, `video_id`, `position`, `finished`, `update_time`) VALUES")
add(",\n".join(f"({r[0]}, {r[1]}, {r[2]}, {r[3]}, {r[4]}, {sql_str(r[5])})" for r in extra_learning) + ";")
add("")
add("UPDATE `course_enrollment` e SET e.progress = (")
add("  SELECT ROUND(COUNT(*) * 100.0 / (")
add("    SELECT COUNT(v.id) FROM video v JOIN chapter ch ON v.chapter_id = ch.id WHERE ch.course_id = e.course_id")
add("  ), 2) FROM learning_record lr")
add("  JOIN video v ON lr.video_id = v.id JOIN chapter ch ON v.chapter_id = ch.id")
add("  WHERE lr.student_id = e.student_id AND ch.course_id = e.course_id AND lr.finished = 1")
add(");")
add("")
add("-- ---------- 8. 考试记录与答题明细（得分=答题正确率） ----------")
add("INSERT INTO `exam_record` (`id`, `exam_id`, `student_id`, `score`, `submit_time`) VALUES")
add(",\n".join(f"({r[0]}, {r[1]}, {r[2]}, {r[3]}, {sql_str(r[4])})" for r in exam_record_rows) + ";")
add("")
add("INSERT INTO `exam_answer` (`id`, `record_id`, `question_id`, `student_answer`, `correct`) VALUES")
add(",\n".join(f"({r[0]}, {r[1]}, {r[2]}, {sql_str(r[3])}, {r[4]})" for r in exam_answer_rows) + ";")
add("")
add("-- ---------- 9. AI 答疑会话（student1/2/3） ----------")
add("INSERT INTO `ai_chat_session` (`id`, `student_id`, `course_id`, `title`, `create_time`) VALUES")
add(",\n".join(f"({r[0]}, {r[1]}, {r[2]}, {sql_str(r[3])}, {sql_str(r[4])})" for r in sess_rows) + ";")
add("")
add("INSERT INTO `ai_chat_message` (`id`, `session_id`, `role`, `content`, `create_time`) VALUES")
add(",\n".join(f"({r[0]}, {r[1]}, {sql_str(r[2])}, {sql_str(r[3])}, {sql_str(r[4])})" for r in msg_rows) + ";")
add("")
add("-- ---------- 10. 学习笔记 ----------")
add("INSERT INTO `study_note` (`id`, `student_id`, `video_id`, `content`, `create_time`) VALUES")
add(",\n".join(f"({r[0]}, {r[1]}, {r[2]}, {sql_str(r[3])}, {sql_str(r[4])})" for r in note_rows) + ";")
add("")
add("-- ---------- 11. 积分活动领取（任务类，须真实完成） ----------")
add("INSERT INTO `points_activity_record` (`id`, `user_id`, `activity_id`, `claim_date`, `reward`, `create_time`) VALUES")
all_claims = claim_rows + coupon_claims
add(",\n".join(f"({r[0]}, {r[1]}, {r[2]}, {sql_str(r[3])}, {r[4]}, {sql_str(r[5])})" for r in all_claims) + ";")
add("")
add("-- ---------- 12. 用户优惠券（领取快照：未使用/已核销/已过期） ----------")
add("INSERT INTO `user_coupon` (`id`, `user_id`, `activity_id`, `name`, `type`, `value`, `threshold`, `status`, `expire_time`, `used_time`, `create_time`) VALUES")
add(",\n".join(f"({r[0]}, {r[1]}, {r[2]}, {sql_str(r[3])}, {r[4]}, {r[5]}, {r[6]}, {r[7]}, {sql_str(r[8])}, "
              f"{sql_str(r[9]) if r[9] else 'NULL'}, {sql_str(r[10])})" for r in coupon_rows) + ";")
add("")
add("-- ---------- 13. 课程兑换（扣积分 + 自动选课 + 券核销） ----------")
add("INSERT INTO `course_exchange_record` (`id`, `user_id`, `course_id`, `points_cost`, `coupon_id`, `discount`, `status`, `create_time`) VALUES")
add(",\n".join(f"({r[0]}, {r[1]}, {r[2]}, {r[3]}, {r[4] if r[4] else 'NULL'}, {r[5] if r[5] else 'NULL'}, {r[6]}, {sql_str(r[7])})"
              for r in exchange_rows) + ";")
add("")
add("INSERT INTO `course_enrollment` (`id`, `student_id`, `course_id`, `progress`, `create_time`) VALUES")
add(",\n".join(f"({r[0]}, {r[1]}, {r[2]}, {r[3]}, {sql_str(r[4])})" for r in exchange_enrolls) + ";")
add("")
add("-- ---------- 14. 积分明细（完课/考试/任务/兑换，账户按增量同步） ----------")
add("INSERT INTO `points_record` (`id`, `user_id`, `type`, `change_value`, `description`, `create_time`) VALUES")
add(",\n".join(f"({r[0]}, {r[1]}, {r[2]}, {r[3]}, {sql_str(r[4])}, {sql_str(r[5])})" for r in points_rows) + ";")
add("")
for sid, (ed, sd) in acct_updates.items():
    add(f"UPDATE `points_account` SET `balance` = `balance` + {ed - sd}, "
        f"`total_earned` = `total_earned` + {ed}, `total_spent` = `total_spent` + {sd} WHERE `user_id` = {sid};")
add("")
add("-- ---------- 15. 公告 ----------")
add("INSERT INTO `notice` (`title`, `content`, `type`, `top`, `status`, `create_time`) VALUES")
add(",\n".join(f"({sql_str(t)}, {sql_str(c)}, {ty}, {tp}, {st}, {sql_str(dt(d, 10))})"
              for (t, c, ty, tp, st, d) in notices) + ";")
add("")
add("-- 第三阶段演示数据导入完成")

out_path = os.path.join(SQL_DIR, 'rich_data.sql')
with open(out_path, 'w', encoding='utf-8') as f:
    f.write("\n".join(lines))

# ============================================================
# 四、一致性自检
# ============================================================
print("=== 一致性自检 ===")
fail = False
for sid in set(list(acct_updates) + students_demo + students_init):
    expect = base_balance.get(sid, 0) + earns.get(sid, 0)
    actual = base_balance.get(sid, 0) + acct_updates[sid][0] - acct_updates[sid][1]
    if expect != actual or actual < 0:
        fail = True
        print(f"  [FAIL] 用户 {sid} 余额异常：{expect} vs {actual}")
print(f"  账户余额核算（收入-支出，且≥0）：{'OK' if not fail else 'FAIL'}")
print(f"  补充题库 {len(q_rows)} 题 | 新试卷 {len(new_exams)} 套 | 考试记录 {len(exam_record_rows)} 条 | 答题明细 {len(exam_answer_rows)} 条")
print(f"  学习笔记 {len(note_rows)} 条 | 补充学习记录 {len(extra_learning)} 条 | AI 会话 {len(sess_rows)} 个")
print(f"  任务领取 {len(claim_rows)} 条 | 优惠券领取 {len(coupon_claims)} 条 | 优惠券 {len(coupon_rows)} 张（已核销 {sum(1 for c in coupon_rows if c[7] == 1)}）")
print(f"  新增兑换 {len(exchange_rows)} 条 | 积分明细 {len(points_rows)} 条 | 公告 {len(notices)} 条")
print(f"  新增课程 2 门（待审核 {c_pending} / 已下架 {c_offline}），课程 3 置为已驳回")
print(f"  输出：{out_path}")
