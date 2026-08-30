#!/usr/bin/env python3
"""AI 辅助在线学习平台 - 扩展功能测试（第二轮）
覆盖：教师课程管理+审核链路、题库CRUD、公告管理、评论审核、用户管理、积分规则配置、
      个人资料/登出、AI 接口降级
运行：先执行 init.sql 重置数据，再运行本脚本（依赖后端运行在 localhost:8080）
"""
import json
import urllib.request
import urllib.error
import urllib.parse

BASE = "http://localhost:8080/api"
PASS, FAIL = 0, 0
FAILURES = []


def call(method, path, token=None, body=None, params=None):
    url = BASE + path
    if params:
        url += "?" + urllib.parse.urlencode(params)
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


# ==================== 1. 教师课程管理 + 审核链路 ====================
def test_course_lifecycle(tk_teacher, tk_admin, tk_student):
    print("\n== 1. 教师课程管理 + 管理审核链路 ==")
    # 创建课程（含章节视频）
    course_body = {
        "title": "测试课程-自动化",
        "category": "编程",
        "description": "自动化测试创建",
        "priceType": 1,
        "chapters": [{
            "title": "第一章",
            "sortOrder": 1,
            "videos": [{"title": "视频1", "url": "http://example.com/v.mp4", "duration": 600, "sortOrder": 1}]
        }]
    }
    s, r = call("POST", "/teacher/course/save", token=tk_teacher, body=course_body)
    check("教师创建课程成功", r["code"] == 200 and r["data"]["title"] == "测试课程-自动化", str(r))
    new_course_id = r["data"]["id"]
    check("新课程初始状态为待审核(0)", r["data"]["status"] == 0, f"status={r['data']['status']}")

    # 重复提交审核被拒（已是待审核）
    s, r = call("POST", f"/teacher/course/submit/{new_course_id}", token=tk_teacher)
    check("已在审核中重复提交被拒", r["code"] != 200, str(r))

    # 管理员查看待审核列表
    s, r = call("GET", "/admin/course/pending", token=tk_admin)
    pending_ids = [c["id"] for c in r.get("data", [])]
    check("待审核列表包含新课程", new_course_id in pending_ids, str(pending_ids))

    # 审核通过
    s, r = call("POST", "/admin/course/review", token=tk_admin,
                body={"courseId": new_course_id, "approved": True})
    check("审核通过→上架", r["code"] == 200 and r["data"]["status"] == 1, str(r.get("data", {}).get("status")))

    # 审核已上架课程被拒（不在待审核状态）
    s, r = call("POST", "/admin/course/review", token=tk_admin,
                body={"courseId": new_course_id, "approved": False})
    check("非待审核状态审核被拒", r["code"] != 200)

    # 下架
    s, r = call("POST", f"/admin/course/status/{new_course_id}", token=tk_admin, params={"online": "false"})
    check("管理员下架课程", r["code"] == 200 and r["data"]["status"] == 2, str(r.get("data", {}).get("status")))

    # 下架后课程广场不可见
    s, r = call("GET", "/course/square", token=tk_student, params={"keyword": "测试课程-自动化"})
    records = r.get("data", {}).get("records", [])
    check("下架课程不在广场", all(c["id"] != new_course_id for c in records))

    # 重新上架
    s, r = call("POST", f"/admin/course/status/{new_course_id}", token=tk_admin, params={"online": "true"})
    check("管理员重新上架", r["code"] == 200 and r["data"]["status"] == 1)

    # 教师编辑已上架课程 → 回到待审核
    s, r = call("POST", "/teacher/course/save", token=tk_teacher,
                body={"id": new_course_id, "title": "测试课程-已编辑", "priceType": 1})
    check("编辑已上架课程回到待审核", r["code"] == 200 and r["data"]["status"] == 0, str(r.get("data", {}).get("status")))

    # 驳回
    s, r = call("POST", "/admin/course/review", token=tk_admin,
                body={"courseId": new_course_id, "approved": False, "reason": "内容不符"})
    check("审核驳回→下架", r["code"] == 200 and r["data"]["status"] == 2)

    # 删除课程（无人选课，状态为下架）
    s, r = call("DELETE", f"/teacher/course/{new_course_id}", token=tk_teacher)
    check("删除无人选课的下架课程", r["code"] == 200, str(r))

    # 课程管理分页
    s, r = call("GET", "/admin/course/page", token=tk_admin, params={"page": 1, "size": 10})
    check("管理端课程分页", r["code"] == 200 and "records" in r["data"])

    # 参数校验：缺 title
    s, r = call("POST", "/teacher/course/save", token=tk_teacher, body={"priceType": 1})
    check("缺课程名触发校验", r["code"] != 200)


# ==================== 2. 题库 CRUD ====================
def test_question_crud(tk_teacher):
    print("\n== 2. 题库 CRUD ==")
    # 新增单选题
    q_body = {
        "courseId": 1, "chapterId": 1, "type": 1,
        "content": "自动化测试题目：1+1等于？",
        "options": ["1", "2", "3", "4"],
        "answer": "B", "analysis": "基础数学"
    }
    s, r = call("POST", "/teacher/question/save", token=tk_teacher, body=q_body)
    check("新增题目成功", r["code"] == 200, str(r))
    qid = r["data"]["id"]

    # 编辑题目
    s, r = call("POST", "/teacher/question/save", token=tk_teacher,
                body={"id": qid, "courseId": 1, "chapterId": 1, "type": 1,
                      "content": "自动化测试题目（已编辑）", "options": ["1", "2", "3", "4"],
                      "answer": "B", "analysis": "已编辑"})
    check("编辑题目成功", r["code"] == 200 and r["data"]["content"] == "自动化测试题目（已编辑）")

    # 分页查询（按课程筛选）
    s, r = call("GET", "/teacher/question/page", token=tk_teacher, params={"courseId": 1, "pageNum": 1, "pageSize": 50})
    check("题库分页查询", r["code"] == 200 and r["data"]["total"] >= 1)

    # 参数校验：缺题干
    s, r = call("POST", "/teacher/question/save", token=tk_teacher,
                body={"courseId": 1, "chapterId": 1, "type": 1, "answer": "A"})
    check("缺题干触发校验", r["code"] != 200)

    # 删除题目
    s, r = call("DELETE", f"/teacher/question/{qid}", token=tk_teacher)
    check("删除题目成功", r["code"] == 200)

    # 删除不存在的题目
    s, r = call("DELETE", "/teacher/question/99999", token=tk_teacher)
    check("删除不存在题目被拒", r["code"] != 200)


# ==================== 3. 公告管理 ====================
def test_notice_mgmt(tk_admin, tk_student):
    print("\n== 3. 公告管理 ==")
    # 新建公告（产品设计：新建即发布）
    s, r = call("POST", "/admin/ops/notices", token=tk_admin,
                body={"title": "自动化测试公告", "content": "测试内容", "type": 1})
    check("新建公告成功且默认发布", r["code"] == 200 and r["data"]["status"] == 1, str(r))
    notice_id = r["data"]["id"]

    # 学生端可见（已发布）
    s, r = call("GET", "/ops/notices", token=tk_student)
    check("新公告学生立即可见", any(n["id"] == notice_id for n in r.get("data", [])))

    # 发布（幂等）
    s, r = call("POST", f"/admin/ops/notices/{notice_id}/status", token=tk_admin, params={"publish": "true"})
    check("发布公告成功", r["code"] == 200 and r["data"]["status"] == 1)

    # 置顶
    s, r = call("POST", f"/admin/ops/notices/{notice_id}/top", token=tk_admin, params={"top": "true"})
    check("置顶公告", r["code"] == 200 and r["data"]["top"] == 1)

    # 撤回
    s, r = call("POST", f"/admin/ops/notices/{notice_id}/status", token=tk_admin, params={"publish": "false"})
    check("撤回公告", r["code"] == 200 and r["data"]["status"] == 0)

    # 删除
    s, r = call("DELETE", f"/admin/ops/notices/{notice_id}", token=tk_admin)
    check("删除公告", r["code"] == 200)

    # 参数校验：缺标题
    s, r = call("POST", "/admin/ops/notices", token=tk_admin, body={"content": "无标题"})
    check("缺标题触发校验", r["code"] != 200)

    # 管理端分页
    s, r = call("GET", "/admin/ops/notices", token=tk_admin, params={"page": 1, "size": 10})
    check("管理端公告分页", r["code"] == 200 and "records" in r["data"])


# ==================== 4. 评论审核 ====================
def test_comment_review(tk_student, tk_admin):
    print("\n== 4. 评论审核 ==")
    # 学生发评论（进入待审核）
    s, r = call("POST", "/ops/comments/1", token=tk_student, body={"content": "自动化测试评论"})
    check("学生发评论成功", r["code"] == 200, str(r))
    comment_id = r["data"]["id"]
    check("新评论状态为待审核(0)", r["data"]["status"] == 0)

    # 待审核评论不在学生可见列表
    s, r = call("GET", "/ops/comments/1", token=tk_student)
    check("待审核评论学生不可见", all(c.get("id") != comment_id for c in r.get("data", [])))

    # 空评论被拒
    s, r = call("POST", "/ops/comments/1", token=tk_student, body={"content": ""})
    check("空评论被拒", r["code"] != 200)

    # 管理员审核通过
    s, r = call("POST", f"/admin/ops/comments/{comment_id}/review", token=tk_admin, params={"visible": "true"})
    check("审核通过评论", r["code"] == 200 and r["data"]["status"] == 1)

    # 学生可见
    s, r = call("GET", "/ops/comments/1", token=tk_student)
    check("审核后学生可见", any(c.get("id") == comment_id for c in r.get("data", [])))

    # 隐藏
    s, r = call("POST", f"/admin/ops/comments/{comment_id}/review", token=tk_admin, params={"visible": "false"})
    check("隐藏评论", r["code"] == 200 and r["data"]["status"] == 2)

    # 管理端评论分页
    s, r = call("GET", "/admin/ops/comments", token=tk_admin, params={"page": 1, "size": 10})
    check("管理端评论分页", r["code"] == 200 and "records" in r["data"])

    # 删除评论
    s, r = call("DELETE", f"/admin/ops/comments/{comment_id}", token=tk_admin)
    check("删除评论", r["code"] == 200)


# ==================== 5. 用户管理 ====================
def test_user_mgmt(tk_admin):
    print("\n== 5. 用户管理 ==")
    # 分页
    s, r = call("GET", "/admin/ops/users", token=tk_admin, params={"page": 1, "size": 10})
    check("用户分页查询", r["code"] == 200 and r["data"]["total"] >= 5)

    # 按角色筛选
    s, r = call("GET", "/admin/ops/users", token=tk_admin, params={"role": 1, "page": 1, "size": 10})
    check("按角色筛选学生", r["code"] == 200 and all(u["role"] == 1 for u in r["data"]["records"]))

    # 禁用用户（student3 id=5）
    s, r = call("POST", "/admin/ops/users/5/status", token=tk_admin, params={"enable": "false"})
    check("禁用用户", r["code"] == 200 and r["data"]["status"] == 0)

    # 被禁用用户无法登录
    s, r = call("POST", "/auth/login", body={"username": "student3", "password": "123456"})
    check("禁用用户登录被拒", r["code"] != 200, str(r))

    # 重新启用
    s, r = call("POST", "/admin/ops/users/5/status", token=tk_admin, params={"enable": "true"})
    check("启用用户", r["code"] == 200 and r["data"]["status"] == 1)

    # 启用后可登录
    s, r = call("POST", "/auth/login", body={"username": "student3", "password": "123456"})
    check("启用后登录成功", r["code"] == 200)

    # 调整角色（把 student3 改为教师）
    s, r = call("POST", "/admin/ops/users/5/role", token=tk_admin, params={"role": 2})
    check("调整用户角色", r["code"] == 200 and r["data"]["role"] == 2)

    # 改回学生
    s, r = call("POST", "/admin/ops/users/5/role", token=tk_admin, params={"role": 1})
    check("恢复用户角色", r["code"] == 200 and r["data"]["role"] == 1)


# ==================== 6. 积分规则配置 ====================
def test_points_rule_config(tk_admin):
    print("\n== 6. 积分规则配置 ==")
    # 查询规则
    s, r = call("GET", "/admin/points/rules", token=tk_admin)
    check("查询积分规则", r["code"] == 200 and len(r["data"]) >= 5)
    rules = {rule["ruleKey"]: rule for rule in r["data"]}

    # 修改签到积分值
    sign_rule = rules.get("daily_sign")
    if sign_rule:
        rid = sign_rule["id"]
        s, r = call("POST", f"/admin/points/rules/{rid}", token=tk_admin, params={"ruleValue": 20})
        check("修改签到积分值", r["code"] == 200 and r["data"]["ruleValue"] == 20)
        # 改回
        call("POST", f"/admin/points/rules/{rid}", token=tk_admin, params={"ruleValue": 10})

    # 禁用某规则
    video_rule = rules.get("video_finish")
    if video_rule:
        rid = video_rule["id"]
        s, r = call("POST", f"/admin/points/rules/{rid}", token=tk_admin, params={"enabled": 0})
        check("禁用积分规则", r["code"] == 200 and r["data"]["enabled"] == 0)
        # 恢复
        call("POST", f"/admin/points/rules/{rid}", token=tk_admin, params={"enabled": 1})

    # 修改不存在的规则
    s, r = call("POST", "/admin/points/rules/99999", token=tk_admin, params={"ruleValue": 1})
    check("修改不存在规则被拒", r["code"] != 200)


# ==================== 7. 个人资料与登出 ====================
def test_profile_logout(tk_student):
    print("\n== 7. 个人资料与登出 ==")
    # 获取当前用户
    s, r = call("GET", "/user/me", token=tk_student)
    check("获取当前用户信息", r["code"] == 200 and r["data"]["username"] == "student1")

    # 更新昵称
    s, r = call("PUT", "/user/profile", token=tk_student, body={"nickname": "自动化昵称"})
    check("更新昵称成功", r["code"] == 200 and r["data"]["nickname"] == "自动化昵称")

    # 更新头像
    s, r = call("PUT", "/user/profile", token=tk_student, body={"avatar": "http://example.com/avatar.png"})
    check("更新头像成功", r["code"] == 200 and r["data"]["avatar"] == "http://example.com/avatar.png")

    # 登出
    s, r = call("POST", "/user/logout", token=tk_student)
    check("登出成功", r["code"] == 200)

    # 登出后 token 失效
    s, r = call("GET", "/user/me", token=tk_student)
    check("登出后 token 失效", r.get("code") == 401, str(r))


# ==================== 8. AI 接口降级 ====================
def test_ai_degraded(tk_student, tk_teacher):
    print("\n== 8. AI 接口降级（未配置 API Key） ==")
    # 会话列表
    s, r = call("GET", "/student/ai/sessions", token=tk_student)
    check("AI 会话列表", r["code"] == 200)

    # 提问（SSE 降级）：无 API Key 时返回降级提示
    import urllib.request as ur
    req = ur.Request(BASE + "/student/ai/ask",
                     data=json.dumps({"question": "什么是机器学习？"}).encode(),
                     method="POST")
    req.add_header("Content-Type", "application/json")
    req.add_header("Authorization", "Bearer " + tk_student)
    req.add_header("Accept", "text/event-stream")
    try:
        with ur.urlopen(req, timeout=10) as resp:
            body = resp.read().decode()
            check("AI 提问返回降级提示", "暂未配置" in body or "SESSION" in body, body[:200])
    except Exception as e:
        check("AI 提问返回降级提示", False, str(e))

    # 会话列表应有新会话
    s, r = call("GET", "/student/ai/sessions", token=tk_student)
    check("提问后会话已创建", r["code"] == 200 and len(r["data"]) >= 1)

    # 删除会话
    if r["data"]:
        sid = r["data"][0]["id"]
        s, r = call("DELETE", f"/student/ai/sessions/{sid}", token=tk_student)
        check("删除 AI 会话", r["code"] == 200)

    # 教师 AI 出题（无 API Key → 报错）
    s, r = call("POST", "/teacher/ai/generate", token=tk_teacher,
                body={"courseId": 1, "chapterId": 1, "type": 1, "count": 2})
    check("AI 出题未配置时报错", r["code"] != 200, str(r))

    # 待批改列表（无简答题提交，应为空）
    s, r = call("GET", "/teacher/ai/pending-grades", token=tk_teacher)
    check("待批改列表查询", r["code"] == 200)


if __name__ == "__main__":
    tk_student = login("student1")
    tk_teacher = login("teacher1")
    tk_admin = login("admin")
    if not (tk_student and tk_teacher and tk_admin):
        print("基础账号登录失败，终止测试")
        raise SystemExit(1)

    test_course_lifecycle(tk_teacher, tk_admin, tk_student)
    test_question_crud(tk_teacher)
    test_notice_mgmt(tk_admin, tk_student)
    test_comment_review(tk_student, tk_admin)
    test_user_mgmt(tk_admin)
    test_points_rule_config(tk_admin)
    # 重新登录 student1（上面 profile 测试会登出）
    tk_student2 = login("student1")
    test_profile_logout(tk_student2)
    # AI 测试用 student2（避免 token 失效影响）
    tk_student3 = login("student2")
    test_ai_degraded(tk_student3, tk_teacher)

    print(f"\n{'='*40}\n总计: {PASS + FAIL}  通过: {PASS}  失败: {FAIL}")
    if FAILURES:
        print("失败用例:")
        for f in FAILURES:
            print("  -", f)
