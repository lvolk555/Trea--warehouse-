#!/usr/bin/env python3
"""AI 辅助在线学习平台 - 接口功能与权限测试脚本
覆盖：认证/课程/学习/练习/考试/积分/统计 核心链路 + 边界与权限用例
运行：python3 api_test.py （需后端运行在 localhost:8080，数据库为初始化种子数据）
"""
import json
import urllib.request
import urllib.error

BASE = "http://localhost:8080/api"
PASS, FAIL = 0, 0
FAILURES = []


def call(method, path, token=None, body=None):
    """发起请求，返回 (http_status, 响应json)"""
    url = BASE + path
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read().decode())
        except Exception:
            return e.code, {}


def check(name, cond, detail=""):
    global PASS, FAIL
    if cond:
        PASS += 1
        print(f"  [PASS] {name}")
    else:
        FAIL += 1
        FAILURES.append(f"{name} {detail}")
        print(f"  [FAIL] {name} {detail}")


def login(username, password="123456"):
    s, r = call("POST", "/auth/login", body={"username": username, "password": password})
    return r.get("data", {}).get("token") if s == 200 and r.get("code") == 200 else None


# ==================== 1. 认证模块 ====================
def test_auth():
    print("\n== 1. 认证模块 ==")
    s, r = call("POST", "/auth/login", body={"username": "student1", "password": "123456"})
    check("正确账号登录成功", s == 200 and r["code"] == 200 and r["data"]["token"])
    s, r = call("POST", "/auth/login", body={"username": "student1", "password": "wrong"})
    check("错误密码登录被拒", r["code"] != 200)
    s, r = call("POST", "/auth/login", body={"username": "", "password": ""})
    check("空参数登录触发校验", r["code"] != 200)
    s, r = call("POST", "/auth/login", body={"username": "no_such_user_xyz", "password": "123456"})
    check("不存在用户登录被拒", r["code"] != 200)
    # 注册
    s, r = call("POST", "/auth/register", body={"username": "test_new_user", "password": "abc12345", "nickname": "测试新人"})
    check("新用户注册成功", r["code"] == 200, str(r))
    s, r = call("POST", "/auth/register", body={"username": "test_new_user", "password": "abc12345", "nickname": "重复"})
    check("重复用户名注册被拒", r["code"] != 200)
    # 注册赠送积分
    tk = login("test_new_user", "abc12345")
    s, r = call("GET", "/points/account", token=tk)
    check("注册赠送 100 积分到账", r["code"] == 200 and r["data"]["balance"] == 100, str(r.get("data")))


# ==================== 2. 课程模块 ====================
def test_course(tk_student):
    print("\n== 2. 课程模块 ==")
    # 课程广场已改为 POST + JSON 参数（中文分类经请求体传输，避免 URL 编码 400）
    s, r = call("POST", "/course/square", token=tk_student, body={"page": 1, "size": 8})
    check("课程广场返回已上架课程", r["code"] == 200 and isinstance(r["data"], dict) and "records" in r["data"])
    s, r = call("GET", "/course/1", token=tk_student)
    check("课程详情含章节视频", r["code"] == 200 and r["data"]["title"])
    s, r = call("GET", "/course/99999", token=tk_student)
    check("不存在的课程返回错误", r["code"] != 200)
    # student2 已选课程1；重复选课幂等返回已有记录（不新建）
    tk2 = login("student2")
    s, r = call("POST", "/course/enroll/1", token=tk2)
    check("重复选课幂等返回已有记录", r["code"] == 200 and r["data"]["studentId"] == 4, str(r))
    # 免费课程选课成功（student3 未选课程1）
    tk3 = login("student3")
    s, r = call("POST", "/course/enroll/1", token=tk3)
    check("免费课程选课成功", r["code"] == 200, str(r))
    # 未上架课程不可选（课程3 status=0）
    s, r = call("POST", "/course/enroll/3", token=tk3)
    check("未上架课程不可选", r["code"] != 200)
    # 积分课程直接选课被拒（课程2 需兑换）
    s, r = call("POST", "/course/enroll/2", token=tk3)
    check("积分课程不可直接选课", r["code"] != 200, str(r))
    s, r = call("GET", "/course/my", token=tk_student)
    check("我的课程列表", r["code"] == 200)


# ==================== 3. 学习进度 ====================
def test_study(tk_student):
    print("\n== 3. 学习进度 ==")
    # 上报不存在的视频
    s, r = call("POST", "/study/progress", token=tk_student, body={"videoId": 99999, "position": 10})
    check("不存在视频上报被拒", r["code"] != 200)
    # 参数校验
    s, r = call("POST", "/study/progress", token=tk_student, body={"position": 10})
    check("缺 videoId 触发校验", r["code"] != 200)
    # 正常上报（视频1 时长 10 秒，position=5 未达 95% 阈值，不算完课）
    s, r = call("POST", "/study/progress", token=tk_student, body={"videoId": 1, "position": 5})
    check("播放进度上报成功", r["code"] == 200, str(r))
    # 断点续播
    s, r = call("GET", "/study/resume/1", token=tk_student)
    check("断点续播返回位置", r["code"] == 200 and r["data"]["position"] == 5, str(r.get("data")))
    # 完课上报（视频1 时长内 → position 达 95%）
    s, r = call("POST", "/study/progress", token=tk_student, body={"videoId": 1, "position": 9999, "finished": True})
    check("完课上报成功", r["code"] == 200)
    s, r = call("GET", "/study/resume/1", token=tk_student)
    check("已完课视频断点归零", r["code"] == 200 and r["data"]["position"] == 0, str(r.get("data")))


# ==================== 4. 练习与错题本 ====================
def test_practice(tk_student):
    print("\n== 4. 练习与错题本 ==")
    s, r = call("GET", "/student/practice/questions?courseId=1&chapterId=1", token=tk_student)
    check("按章节拉取练习题", r["code"] == 200 and len(r["data"]) > 0)
    # 提交正确答案（题1 答案 B）
    s, r = call("POST", "/student/practice/submit", token=tk_student, body={"questionId": 1, "studentAnswer": "B"})
    check("答对判定正确", r["code"] == 200 and r["data"]["correct"] is True, str(r.get("data")))
    # 提交错误答案（题3 答案 C，答 A）
    s, r = call("POST", "/student/practice/submit", token=tk_student, body={"questionId": 3, "studentAnswer": "A"})
    check("答错判定正确", r["code"] == 200 and r["data"]["correct"] is False)
    # 空答案校验
    s, r = call("POST", "/student/practice/submit", token=tk_student, body={"questionId": 1, "studentAnswer": ""})
    check("空答案触发校验", r["code"] != 200)
    # 错题本应包含题3（返回结构 {items, total}）
    s, r = call("GET", "/student/practice/error-book", token=tk_student)
    items = r.get("data", {}).get("items", [])
    ids = [item["questionId"] for item in items]
    check("错题本收录答错题目", r["code"] == 200 and 3 in ids, str(ids))
    # 标记掌握
    rec_id = next((item["recordId"] for item in items if item["questionId"] == 3), None)
    if rec_id:
        s, r = call("POST", f"/student/practice/mastered/{rec_id}", token=tk_student)
        check("错题标记掌握成功", r["code"] == 200)


# ==================== 5. 考试链路（教师组卷→发布→学生作答→出分） ====================
def test_exam(tk_teacher, tk_student):
    print("\n== 5. 考试链路 ==")
    # 教师组卷（仅客观题 1-4，避免 AI 批改依赖）
    s, r = call("POST", "/teacher/exam/save", token=tk_teacher,
                body={"courseId": 1, "title": "阶段测验", "duration": 60, "questionIds": [1, 2, 3, 4], "status": 1})
    check("教师组卷并发布", r["code"] == 200, str(r))
    exam_id = r["data"]["id"]
    # 组卷校验：空题目
    s, r = call("POST", "/teacher/exam/save", token=tk_teacher,
                body={"courseId": 1, "title": "空卷", "duration": 60, "questionIds": []})
    check("空试卷被拒", r["code"] != 200)
    # 学生考试列表
    s, r = call("GET", "/student/exam/list", token=tk_student)
    check("学生可见已发布考试", r["code"] == 200 and any(e["id"] == exam_id for e in r["data"]))
    # 开始考试
    s, r = call("GET", f"/student/exam/start/{exam_id}", token=tk_student)
    check("开始考试返回题目", r["code"] == 200)
    # 交卷（全对：q1=B q2=错 q3=C q4=ABC）
    s, r = call("POST", "/student/exam/submit", token=tk_student,
                body={"examId": exam_id, "answers": {"1": "B", "2": "错", "3": "C", "4": "ABC"}})
    check("交卷成功且满分", r["code"] == 200 and float(r["data"]["score"]) == 100, str(r.get("data")))
    # 重复交卷被拒
    s, r = call("POST", "/student/exam/submit", token=tk_student,
                body={"examId": exam_id, "answers": {"1": "B"}})
    check("重复交卷被拒", r["code"] != 200, str(r))
    # 成绩列表
    s, r = call("GET", "/student/exam/scores", token=tk_student)
    check("成绩列表含本次记录", r["code"] == 200 and len(r["data"]) > 0)
    return exam_id


# ==================== 6. 积分体系 ====================
def test_points(tk_student, tk_admin):
    print("\n== 6. 积分体系 ==")
    s, r = call("GET", "/points/account", token=tk_student)
    check("积分账户查询", r["code"] == 200 and "balance" in r["data"])
    # 签到
    s, r = call("POST", "/points/sign", token=tk_student)
    check("每日签到成功", r["code"] == 200, str(r))
    s, r = call("POST", "/points/sign", token=tk_student)
    check("当日重复签到被拒", r["code"] != 200)
    s, r = call("GET", "/points/sign/month", token=tk_student)
    check("签到月历查询", r["code"] == 200)
    # 明细
    s, r = call("GET", "/points/records", token=tk_student)
    check("积分明细查询", r["code"] == 200)
    # 余额不足兑换（课程2 需 800，student1 余额远不足）
    s, r = call("POST", "/points/exchange/2", token=tk_student)
    check("余额不足兑换被拒", r["code"] != 200, str(r))
    # 兑换不存在课程
    s, r = call("POST", "/points/exchange/99999", token=tk_student)
    check("兑换不存在课程被拒", r["code"] != 200)
    # 给 student2 充值 900 后兑换成功（测试事务：扣分+自动选课）
    import subprocess
    subprocess.run(["mysql", "-uroot", "-proot", "ai_learning", "-e",
                    "UPDATE points_account SET balance=900 WHERE user_id=4"], capture_output=True)
    tk2 = login("student2")
    s, r = call("POST", "/points/exchange/2", token=tk2)
    check("积分兑换课程成功", r["code"] == 200, str(r))
    s, r = call("GET", "/points/account", token=tk2)
    check("兑换后余额扣减 800", r["data"]["balance"] == 100, str(r.get("data")))
    s, r = call("GET", "/course/my", token=tk2)
    enrolled = [c.get("courseId") or c.get("id") for c in r["data"]]
    check("兑换后自动选课", 2 in enrolled, str(enrolled))
    # 重复兑换被拒
    s, r = call("POST", "/points/exchange/2", token=tk2)
    check("重复兑换被拒", r["code"] != 200, str(r))
    # 管理端：积分规则与兑换记录
    s, r = call("GET", "/admin/points/rules", token=tk_admin)
    check("管理端查询积分规则", r["code"] == 200 and len(r["data"]) > 0)
    s, r = call("GET", "/admin/points/exchanges", token=tk_admin)
    check("管理端查询兑换记录", r["code"] == 200)


# ==================== 7. 统计看板 ====================
def test_stats(tk_student, tk_teacher, tk_admin):
    print("\n== 7. 统计看板 ==")
    s, r = call("GET", "/stats/student", token=tk_student)
    check("学生看板数据", r["code"] == 200 and "pointsTrend" in r["data"])
    check("学生积分趋势已补零为14天", len(r["data"]["pointsTrend"]) == 14)
    s, r = call("GET", "/stats/teacher", token=tk_teacher)
    check("教师看板数据", r["code"] == 200 and "chapterCompletion" in r["data"])
    s, r = call("GET", "/stats/admin", token=tk_admin)
    check("管理看板数据", r["code"] == 200 and "userGrowth" in r["data"])
    check("管理看板用户增长补零14天", len(r["data"]["userGrowth"]) == 14)


# ==================== 8. 权限与越权 ====================
def test_permission(tk_student, tk_teacher, tk_admin):
    print("\n== 8. 权限与越权 ==")
    # 未登录（统一异常处理：HTTP 200 + body code 401）
    s, r = call("GET", "/user/me")
    check("未登录访问被拒(401)", r.get("code") == 401, f"body={r}")
    # 学生越权
    s, r = call("GET", "/stats/admin", token=tk_student)
    check("学生访问管理看板被拒", r["code"] == 403, str(r))
    s, r = call("GET", "/admin/points/rules", token=tk_student)
    check("学生访问积分规则被拒", r["code"] == 403)
    s, r = call("POST", "/teacher/exam/save", token=tk_student,
                body={"courseId": 1, "title": "x", "duration": 60, "questionIds": [1]})
    check("学生调用教师组卷被拒", r["code"] == 403)
    # 教师越权
    s, r = call("GET", "/stats/admin", token=tk_teacher)
    check("教师访问管理看板被拒", r["code"] == 403)
    s, r = call("GET", "/admin/course/pending", token=tk_teacher)
    check("教师访问课程审核被拒", r["code"] == 403)
    # 管理员访问教师看板
    s, r = call("GET", "/stats/teacher", token=tk_admin)
    check("管理员访问教师看板被拒", r["code"] == 403)
    # 教师数据隔离：教师只能看自己课程（种子数据课程均属 teacher1，验证结构存在即可）
    s, r = call("GET", "/teacher/course/list", token=tk_teacher)
    check("教师课程列表", r["code"] == 200)


# ==================== 9. 并发防超扣 ====================
def test_concurrent_exchange():
    print("\n== 9. 并发防超扣 ==")
    import subprocess
    import threading
    # student3 余额设为 900（课程2 需 800），10 线程并发兑换，只应成功 1 次
    subprocess.run(["mysql", "-uroot", "-proot", "ai_learning", "-e",
                    "UPDATE points_account SET balance=900 WHERE user_id=5"], capture_output=True)
    tk = login("student3")
    results = []

    def try_exchange():
        s, r = call("POST", "/points/exchange/2", token=tk)
        results.append(r["code"] == 200)

    threads = [threading.Thread(target=try_exchange) for _ in range(10)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    success_count = sum(results)
    check("10 并发兑换仅 1 次成功", success_count == 1, f"成功 {success_count} 次")
    s, r = call("GET", "/points/account", token=tk)
    check("并发后余额不为负", r["data"]["balance"] >= 0, str(r.get("data")))
    check("并发后余额恰好扣 800", r["data"]["balance"] == 100, str(r.get("data")))


if __name__ == "__main__":
    tk_student = login("student1")
    tk_teacher = login("teacher1")
    tk_admin = login("admin")
    if not (tk_student and tk_teacher and tk_admin):
        print("基础账号登录失败，终止测试")
        raise SystemExit(1)

    test_auth()
    test_course(tk_student)
    test_study(tk_student)
    test_practice(tk_student)
    test_exam(tk_teacher, tk_student)
    test_points(tk_student, tk_admin)
    test_stats(tk_student, tk_teacher, tk_admin)
    test_permission(tk_student, tk_teacher, tk_admin)
    test_concurrent_exchange()

    print(f"\n{'='*40}\n总计: {PASS + FAIL}  通过: {PASS}  失败: {FAIL}")
    if FAILURES:
        print("失败用例:")
        for f in FAILURES:
            print("  -", f)
