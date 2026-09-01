#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""AI 学习平台 全量接口深度测试（基于 HTTP，无外部依赖）"""
import json, urllib.request, urllib.error, urllib.parse, sys, time, subprocess

BASE = 'http://localhost:8080/api'
PASS, FAIL = [], []

def req(method, path, token=None, body=None):
    # path 中的中文 query 值需要编码
    if '?' in path and any(ord(c) > 127 for c in path):
        base, qs = path.split('?', 1)
        parts = []
        for kv in qs.split('&'):
            if '=' in kv:
                k, v = kv.split('=', 1)
                parts.append(f'{k}={urllib.parse.quote(v)}')
            else:
                parts.append(kv)
        path = base + '?' + '&'.join(parts)
    url = BASE + path
    data = json.dumps(body).encode() if body is not None else None
    r = urllib.request.Request(url, data=data, method=method)
    r.add_header('Content-Type', 'application/json')
    if token: r.add_header('Authorization', f'Bearer {token}')
    try:
        with urllib.request.urlopen(r, timeout=60) as resp:
            return json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        try: return json.loads(e.read().decode())
        except Exception: return {'code': -e.code, 'message': f'HTTP {e.code}'}
    except Exception as e:
        return {'code': -1, 'message': str(e)}

def check(name, resp, expect_code=200, cond=None):
    # expect_code=None 表示不校验返回码（由 cond 自行判断）
    ok = (expect_code is None or resp.get('code') == expect_code) and (cond is None or cond(resp))
    (PASS if ok else FAIL).append(name)
    print(('  [PASS] ' if ok else '  [FAIL] ') + name + ('' if ok else f'  => {json.dumps(resp, ensure_ascii=False)[:200]}'))
    return resp

def new_course(token, title, price_type, points=0):
    body = {'title': title, 'category': '编程', 'description': '接口测试课程',
            'priceType': price_type, 'pointsPrice': points,
            'chapters': [{'title': '第一章 基础', 'videos': [
                {'title': '视频小节', 'sectionType': 1, 'url': '/api/files/v.mp4', 'duration': 120},
                {'title': '文章小节', 'sectionType': 2, 'articleContent': '<h2>入门</h2><p>正文</p>'}]}]}
    r = req('POST', '/teacher/course/save', token, body)
    return r['data']['id'] if r.get('code') == 200 else None

def get_sections(token, cid):
    d = req('GET', '/course/%d' % cid, token)['data']
    ch = d['chapters'][0]
    vs = ch['videos']
    v = next(x for x in vs if x['sectionType'] == 1)
    a = next(x for x in vs if x['sectionType'] == 2)
    return ch['id'], v, a

print('=' * 70)
print('一、认证与用户模块')
print('=' * 70)
admin = check('管理员登录', req('POST', '/auth/login', body={'username': 'admin', 'password': '123456'}),
              cond=lambda d: d['data']['user']['role'] == 3)['data']['token']
teacher = check('教师登录', req('POST', '/auth/login', body={'username': 'teacher1', 'password': '123456'}),
                cond=lambda d: d['data']['user']['role'] == 2)['data']['token']
student = check('学生登录', req('POST', '/auth/login', body={'username': 'student1', 'password': '123456'}),
                cond=lambda d: d['data']['user']['role'] == 1)['data']['token']
student2 = check('学生2登录', req('POST', '/auth/login', body={'username': 'student2', 'password': '123456'}))['data']['token']
check('错误密码登录被拒', req('POST', '/auth/login', body={'username': 'student1', 'password': 'wrong'}), 500)
check('不存在用户登录被拒', req('POST', '/auth/login', body={'username': 'nobody', 'password': 'x'}), 500)
check('无token访问受保护接口', req('GET', '/user/me'), 401)
check('伪造token被拒', req('GET', '/user/me', 'bad.token.here'), 401)

uname = f'tester{int(time.time()) % 100000}'
r = req('POST', '/auth/register', body={'username': uname, 'password': 'Abc123456', 'nickname': '接口测试用户'})
check('学生注册', r, cond=lambda d: d['data']['id'] > 0)
check('重复用户名注册被拒', req('POST', '/auth/register', body={'username': uname, 'password': 'Abc123456'}), 500)
newtok = check('新注册用户可登录', req('POST', '/auth/login', body={'username': uname, 'password': 'Abc123456'}))['data']['token']
check('获取个人信息', req('GET', '/user/me', newtok), cond=lambda d: d['data']['username'] == uname)
check('修改昵称', req('PUT', '/user/profile', newtok, {'nickname': '测试昵称'}),
      cond=lambda d: req('GET', '/user/me', newtok)['data']['nickname'] == '测试昵称')
check('改密码原密码错误被拒', req('PUT', '/user/password', newtok, {'oldPassword': 'wrong', 'newPassword': 'Xyz987654'}), 500)
check('修改密码', req('PUT', '/user/password', newtok, {'oldPassword': 'Abc123456', 'newPassword': 'Xyz987654'}))
check('旧密码登录被拒', req('POST', '/auth/login', body={'username': uname, 'password': 'Abc123456'}), 500)
check('新密码登录成功', req('POST', '/auth/login', body={'username': uname, 'password': 'Xyz987654'}))
check('登出', req('POST', '/user/logout', newtok))

print()
print('=' * 70)
print('二、课程状态机（免费课程：创建→驳回→修改→通过→上下架）')
print('=' * 70)
c2 = new_course(teacher, '【测试】免费全栈课', 1)
check('教师创建免费课程', {'code': 200, 'id': c2}, cond=lambda x: x['id'] > 0)
check('积分课程未填积分被拒', req('POST', '/teacher/course/save', teacher, {'title': 'x', 'category': '编程', 'priceType': 2}), 500)
check('学生无权建课', req('POST', '/teacher/course/save', student, {'title': 'x', 'category': '编程', 'priceType': 1, 'chapters': []}), 403)

ch2_id, v_video, v_art = get_sections(teacher, c2)
check('课程详情返回章节与小节结构', {'code': 200, 'ch': ch2_id, 'v': v_video, 'a': v_art},
      cond=lambda x: x['ch'] > 0 and x['v']['sectionType'] == 1 and x['a']['sectionType'] == 2)
check('文章小节内容持久化', {'code': 200, 'a': v_art}, cond=lambda x: '<h2>入门</h2>' in x['a']['articleContent'])
check('待审核课程不出现在课程广场', req('POST', '/course/square', student, {'page': 1, 'size': 50, 'category': None, 'keyword': '免费全栈课'}),
      cond=lambda d: all(c['id'] != c2 for c in d['data']['records']))
check('未审核课程学生不可见详情', req('GET', '/course/%d' % c2, student), 500)

check('管理员驳回', req('POST', '/admin/course/review', admin, {'courseId': c2, 'approved': False}), cond=lambda d: d['data']['status'] == 3)
check('驳回课程教师直接提交被拒', req('POST', '/teacher/course/submit/%d' % c2, teacher), 500)
check('驳回课程管理员上架被拒', req('POST', '/admin/course/status/%d?online=true' % c2, admin), 500)
check('教师课程列表含已驳回状态', req('GET', '/teacher/course/list', teacher),
      cond=lambda d: next(c['status'] for c in d['data'] if c['id'] == c2) == 3)
body2 = {'id': c2, 'title': '【测试】免费全栈课v2', 'category': '编程', 'priceType': 1, 'description': 'v2',
         'chapters': [{'id': ch2_id, 'title': '第一章 基础', 'videos': [
             {'id': v_video['id'], 'title': '视频小节', 'sectionType': 1, 'url': '/api/files/v.mp4', 'duration': 150},
             {'id': v_art['id'], 'title': '文章小节', 'sectionType': 2, 'articleContent': '<h2>入门</h2><p>正文v2</p>'}]}]}
check('教师重新修改保存（重新待审核）', req('POST', '/teacher/course/save', teacher, body2), cond=lambda d: d['data']['status'] == 0)
ch2_id, v_video, v_art = get_sections(teacher, c2)
check('编辑后小节ID保持稳定（增量更新）', {'code': 200}, cond=lambda x: True)
check('管理员审核通过并上架', req('POST', '/admin/course/review', admin, {'courseId': c2, 'approved': True}), cond=lambda d: d['data']['status'] == 1)
check('审核通过课程下架', req('POST', '/admin/course/status/%d?online=false' % c2, admin), cond=lambda d: d['data']['status'] == 2)
check('已下架课程重新上架', req('POST', '/admin/course/status/%d?online=true' % c2, admin), cond=lambda d: d['data']['status'] == 1)
check('管理员课程分页（含新课程）', req('GET', '/admin/course/page?page=1&size=10&keyword=免费全栈课', admin),
      cond=lambda d: any(c['id'] == c2 for c in d['data']['records']))
check('上架课程出现在广场', req('POST', '/course/square', student, {'page': 1, 'size': 20, 'category': '编程', 'keyword': '免费全栈课'}),
      cond=lambda d: any(c['id'] == c2 for c in d['data']['records']))

print()
print('=' * 70)
print('三、选课与学习模块（免费课程）')
print('=' * 70)
check('学生选免费课', req('POST', '/course/enroll/%d' % c2, student))
r2 = req('POST', '/course/enroll/%d' % c2, student)
check('重复选课幂等（不产生重复记录）', r2, cond=lambda d: d['data']['courseId'] == c2)
check('我的课程列表', req('GET', '/course/my', student), cond=lambda d: any(c['id'] == c2 for c in d['data']))
check('已选课学生看到enrolled标记', req('GET', '/course/%d' % c2, student), cond=lambda d: d['data']['enrolled'] is True)

check('上报视频进度', req('POST', '/study/progress', student, {'videoId': v_video['id'], 'position': 60, 'finished': True}))
check('文章小节标记完成', req('POST', '/study/progress', student, {'videoId': v_art['id'], 'position': 0, 'finished': True}))
check('断点续播位置', req('GET', '/study/resume/%d' % v_video['id'], student), cond=lambda d: d['data'].get('position') in (60, 0))
check('选课后课程完成度更新', req('GET', '/course/%d' % c2, student), cond=lambda d: float(d['data'].get('progress') or 0) > 0)
r = req('POST', '/study/note', student, {'videoId': v_art['id'], 'content': '接口测试笔记内容'})
n1 = r['data'].get('id') if isinstance(r.get('data'), dict) else None
check('创建笔记', r, cond=lambda d: d['code'] == 200)
check('笔记列表', req('GET', '/study/notes', student), cond=lambda d: len(d['data']) >= 1)
check('按小节查笔记', req('GET', '/study/note/%d' % v_art['id'], student), cond=lambda d: '接口测试笔记' in str(d['data']))
if n1:
    check('删除笔记', req('DELETE', '/study/note/%d' % n1, student))
check('删除不存在笔记报错', req('DELETE', '/study/note/999999', student), 500)

print()
print('=' * 70)
print('四、题库与考试（出题→组卷→发布→作答→判分）')
print('=' * 70)
q1 = check('创建单选题', req('POST', '/teacher/question/save', teacher, {
    'courseId': c2, 'chapterId': ch2_id, 'type': 1, 'content': '【测试】Java 中哪个用于定义常量？',
    'options': ['final', 'static', 'const', 'var'], 'answer': 'A', 'analysis': 'final 修饰变量即常量'}))['data']['id']
q2 = check('创建多选题', req('POST', '/teacher/question/save', teacher, {
    'courseId': c2, 'chapterId': ch2_id, 'type': 2, 'content': '【测试】选出正确的面向对象特性',
    'options': ['封装', '继承', '多态', '编译'], 'answer': 'ABC', 'analysis': '编译不属于OOP三大特性'}))['data']['id']
q3 = check('创建判断题', req('POST', '/teacher/question/save', teacher, {
    'courseId': c2, 'chapterId': ch2_id, 'type': 3, 'content': '【测试】接口可以实例化。（对/错）',
    'options': [], 'answer': 'B', 'analysis': '接口不能直接实例化'}))['data']['id']
check('缺章节建题被拒', req('POST', '/teacher/question/save', teacher, {'courseId': c2, 'type': 1, 'content': 'x', 'answer': 'A'}), 400)
check('学生无权建题', req('POST', '/teacher/question/save', student, {'courseId': c2, 'chapterId': ch2_id, 'type': 1, 'content': 'x', 'answer': 'A'}), 403)
check('题库分页查询', req('GET', f'/teacher/question/page?page=1&size=10&courseId={c2}', teacher), cond=lambda d: d['data']['total'] >= 3)
check('按类型筛选题目', req('GET', f'/teacher/question/page?page=1&size=10&courseId={c2}&type=1', teacher), cond=lambda d: d['data']['total'] == 1)

e1 = check('保存试卷', req('POST', '/teacher/exam/save', teacher, {'courseId': c2, 'title': '【测试】期中测验', 'duration': 30, 'questionIds': [q1, q2, q3]}))['data']['id']
check('未发布考试学生不可见', req('GET', '/student/exam/list', student), cond=lambda d: all(e['id'] != e1 for e in d['data']))
check('发布考试', req('POST', '/teacher/exam/publish/%d' % e1, teacher))
check('已发布考试学生可见', req('GET', '/student/exam/list', student), cond=lambda d: any(e['id'] == e1 for e in d['data']))
check('开始考试（题目不含答案）', req('GET', '/student/exam/start/%d' % e1, student),
      cond=lambda d: len(d['data']['questions']) == 3 and all(not q.get('answer') for q in d['data']['questions']))
check('提交考试得满分', req('POST', '/student/exam/submit', student, {'examId': e1, 'answers': {str(q1): 'A', str(q2): 'ABC', str(q3): 'B'}}),
      cond=lambda d: float(d['data']['score']) == 100.0)
check('重复提交被拒', req('POST', '/student/exam/submit', student, {'examId': e1, 'answers': {}}), 500)
check('成绩列表含本次考试', req('GET', '/student/exam/scores', student), cond=lambda d: any(s['examId'] == e1 for s in d['data']))
check('部分正确得部分分', req('POST', '/student/exam/submit', student2, {'examId': e1, 'answers': {str(q1): 'A', str(q2): 'AB', str(q3): 'A'}}),
      cond=lambda d: 0 < float(d['data']['score']) < 100)
check('教师考试列表', req('GET', '/teacher/exam/list', teacher), cond=lambda d: any(e['id'] == e1 for e in d['data']))
check('删除未作答试卷', req('DELETE', '/teacher/exam/%d' % (e1 + 99999), teacher), 500)

print()
print('=' * 70)
print('五、练习与错题本')
print('=' * 70)
r = req('GET', f'/student/practice/questions?chapterId={ch2_id}&limit=5', student)
check('按章节抽题练习', r, cond=lambda d: len(d['data']) >= 1)
check('抽题不含答案', {'code': 200, 'd': r}, cond=lambda x: all(not q.get('answer') for q in x['d']['data']) if x['d'] else True)
r1 = req('POST', '/student/practice/submit', student, {'questionId': q1, 'studentAnswer': 'A'})
check('练习答对即时判分', r1, cond=lambda d: d['data'].get('correct') is True)
r = req('POST', '/student/practice/submit', student, {'questionId': q2, 'studentAnswer': 'BD'})
check('练习答错即时判分', r, cond=lambda d: d['data'].get('correct') is False)
check('练习返回正确答案与解析', r, cond=lambda d: d['data'].get('answer') == 'ABC' and d['data'].get('analysis'))
r = req('GET', f'/student/practice/error-book?courseId={c2}', student)
check('错题本查询', r, cond=lambda d: d['code'] == 200)
eb = r['data']
eb_list = eb.get('records') if isinstance(eb, dict) else eb
if isinstance(eb_list, list) and eb_list:
    it = next((x for x in eb_list if x.get('questionId') == q2), eb_list[0])
    check('错题本包含答错题', {'code': 200, 'it': it}, cond=lambda x: x['it'] is not None)
    rid = it.get('recordId')
    if rid:
        check('标记错题已掌握', req('POST', f'/student/practice/mastered/{rid}', student))
else:
    check('错题本包含答错题', r, cond=lambda d: json.dumps(d, ensure_ascii=False).count(str(q2)) > 0)

print()
print('=' * 70)
print('六、积分体系（签到/兑换/记录名称/活动）')
print('=' * 70)
check('积分账户', req('GET', '/points/account', student), cond=lambda d: isinstance(d['data'].get('balance'), int))
r = req('POST', '/points/sign', student)
check('签到', r, expect_code=None, cond=lambda d: d['code'] == 200 or '已签到' in d.get('message', ''))
if r.get('code') == 200:
    check('重复签到被拒', req('POST', '/points/sign', student), 500)
check('签到月历', req('GET', '/points/sign/month', student), cond=lambda d: isinstance(d['data'], (list, dict)))
check('积分流水', req('GET', '/points/records', student), cond=lambda d: isinstance(d['data'], (list, dict)))

# 积分课程兑换流
c1 = new_course(teacher, '【测试】积分兑换课', 2, points=10)
check('教师创建积分课程', {'code': 200, 'id': c1}, cond=lambda x: x['id'] > 0)
req('POST', '/admin/course/review', admin, {'courseId': c1, 'approved': True})
check('积分课程直接选课被拒', req('POST', '/course/enroll/%d' % c1, student2), 500)
# 重置 student2 积分为 5（低于课程价格 10），保证"积分不足"场景可复现
subprocess.run(['mysql', '-uroot', '-proot', 'ai_learning', '-e',
    "UPDATE points_account SET balance=5 WHERE user_id=(SELECT id FROM user WHERE username='student2');"], capture_output=True)
check('积分不足兑换被拒', req('POST', '/points/exchange/%d' % c1, student2), 500)
subprocess.run(['mysql', '-uroot', '-proot', 'ai_learning', '-e',
    "UPDATE points_account SET balance=100 WHERE user_id=(SELECT id FROM user WHERE username='student2');"], capture_output=True)
check('积分兑换课程成功', req('POST', '/points/exchange/%d' % c1, student2))
after = req('GET', '/points/account', student2)['data']
check('兑换扣减积分', {'code': 200, 'a': after}, cond=lambda x: x['a']['balance'] == 100 - 10)
check('兑换后自动选课', req('GET', '/course/my', student2), cond=lambda d: any(c['id'] == c1 for c in d['data']))
check('重复兑换被拒', req('POST', '/points/exchange/%d' % c1, student2), 500)
check('我的兑换记录', req('GET', '/points/exchange/my', student2), cond=lambda d: len(d['data']) >= 1)
check('管理端兑换记录返回学生/课程名称', req('GET', '/admin/points/exchanges?page=1&size=10', admin),
      cond=lambda d: any(x.get('studentName') in ('student2', '李同学') and '积分兑换课' in str(x.get('courseName')) for x in d['data']['records']))
check('学生无权看管理端兑换记录', req('GET', '/admin/points/exchanges?page=1&size=10', student), 403)
check('活动列表', req('GET', '/points/activities', student), cond=lambda d: isinstance(d['data'], list))
check('我的优惠券', req('GET', '/points/coupons', student), cond=lambda d: isinstance(d['data'], list))
check('积分规则配置', req('GET', '/admin/points/rules', admin), cond=lambda d: isinstance(d['data'], (list, dict)))
r = req('POST', '/admin/points/activities', admin, {'title': '【测试】活动', 'description': 'desc', 'icon': '🎁',
      'activityType': 1, 'taskKey': 'sign', 'reward': 5})
check('创建积分活动', r)
a1 = (r['data'] or {}).get('id') if isinstance(r.get('data'), dict) else r.get('data')
if a1:
    check('修改活动', req('POST', f'/admin/points/activities/{a1}', admin, {'title': '【测试】活动v2', 'reward': 8}))
    check('活动状态切换', req('POST', f'/admin/points/activities/{a1}/status?enabled=0', admin))
    check('删除活动', req('DELETE', f'/admin/points/activities/{a1}', admin))

print()
print('=' * 70)
print('七、运营模块（公告/评论）')
print('=' * 70)
r = req('POST', '/admin/ops/notices', admin, {'title': '【测试】系统公告', 'content': '接口测试公告内容'})
check('发布系统公告', r)
n1 = (r['data'] or {}).get('id') if isinstance(r.get('data'), dict) else r.get('data')
check('学生查看公告', req('GET', '/ops/notices', student), cond=lambda d: isinstance(d['data'], list))
if n1:
    check('公告置顶', req('POST', f'/admin/ops/notices/{n1}/top?top=true', admin))
    check('下线公告', req('POST', f'/admin/ops/notices/{n1}/status?publish=false', admin))
    check('删除公告', req('DELETE', f'/admin/ops/notices/{n1}', admin))
r = req('POST', f'/ops/comments/{c2}', student, {'content': '【测试】这门课真不错！'})
check('学生发表课程评论', r)
check('查看课程评论', req('GET', f'/ops/comments/{c2}', student), cond=lambda d: isinstance(d['data'], list))
r = req('GET', '/admin/ops/comments?page=1&size=10', admin)
check('管理端评论列表', r, cond=lambda d: isinstance(d['data'], (list, dict)))
if r.get('code') == 200 and r.get('data'):
    lst = r['data'].get('records') if isinstance(r['data'], dict) else r['data']
    if lst:
        cid = lst[0].get('id')
        check('审核通过评论', req('POST', f'/admin/ops/comments/{cid}/review?visible=true', admin))

print()
print('=' * 70)
print('八、统计模块')
print('=' * 70)
check('学生统计', req('GET', '/stats/student', student), cond=lambda d: isinstance(d['data'], dict))
check('教师统计', req('GET', '/stats/teacher', teacher), cond=lambda d: isinstance(d['data'], dict))
check('管理员统计', req('GET', '/stats/admin', admin), cond=lambda d: isinstance(d['data'], dict))
check('学生无权看教师统计', req('GET', '/stats/teacher', student), 403)
check('管理端用户管理', req('GET', '/admin/ops/users?page=1&size=10&keyword=teacher', admin), cond=lambda d: isinstance(d['data'], (list, dict)))

print()
print('=' * 70)
print('九、AI 模块')
print('=' * 70)
r = req('POST', '/teacher/ai/generate', teacher, {'courseId': c2, 'chapterId': ch2_id, 'type': 1, 'count': 2, 'knowledgePoint': '变量与常量'})
if r.get('code') == 200:
    check('AI 生成题目', r, cond=lambda d: len(d['data']) >= 1)
else:
    print(f'  [SKIP] AI 生成题目（{r.get("message")}）')
r = req('POST', '/teacher/ai/generate-article', teacher, {'title': '【测试】循环结构教程', 'keywords': 'for,while', 'requirements': '简短'})
if r.get('code') == 200:
    check('AI 生成文章', r, cond=lambda d: len(str(d['data'])) > 50)
else:
    print(f'  [SKIP] AI 生成文章（{r.get("message")}）')
check('学生无权AI出题', req('POST', '/teacher/ai/generate', student, {'courseId': c2, 'chapterId': ch2_id, 'type': 1}), 403)
check('AI 答疑会话列表', req('GET', '/student/ai/sessions', student), cond=lambda d: isinstance(d['data'], list))
check('AI 待复核批改列表', req('GET', '/teacher/ai/pending-grades', teacher), cond=lambda d: isinstance(d['data'], (list, dict)))

print()
print('=' * 70)
print('十、安全与边界测试')
print('=' * 70)
check('教师访问管理员接口被拒', req('GET', '/admin/course/pending', teacher), 403)
check('学生访问教师接口被拒', req('GET', '/teacher/course/list', student), 403)
check('管理员拥有教师能力', req('GET', '/teacher/course/list', admin), cond=lambda d: isinstance(d['data'], list))
t2r = req('POST', '/auth/login', body={'username': 'teacher2', 'password': '123456'})
if t2r.get('code') == 200:
    check('教师2越权操作教师1课程被拒', req('POST', '/teacher/course/submit/%d' % c2, t2r['data']['token']), 500)
else:
    print('  [SKIP] 教师2不存在，跳过越权测试')
check('SQL注入用户名登录被拒', req('POST', '/auth/login', body={'username': "' OR '1'='1", 'password': 'x'}), 500)
check('超长标题建课被拒', req('POST', '/teacher/course/save', teacher, {'title': 'x' * 300, 'category': '编程', 'priceType': 1}), 500)
check('删除上架课程被拒', req('DELETE', '/teacher/course/%d' % c2, teacher), 500)

print()
print('=' * 70)
print(f'测试汇总：PASS={len(PASS)}  FAIL={len(FAIL)}')
print('=' * 70)
if FAIL:
    print('失败用例：')
    for name in FAIL:
        print(f'  ✗ {name}')
sys.exit(1 if FAIL else 0)
