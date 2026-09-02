# 流程模板（需求 → 交付全链路，通用型）

> 与业务领域无关。每个需求/阶段按此剧本执行，不跳步。

## 一、需求拆解表模板

收到需求后第一件事，产出拆解表（放进当阶段报告第一节）：

```markdown
| # | 要点 | 实现方式 |
|---|------|----------|
| 1 | 用户提出的功能 A | 后端：xxx 接口；前端：xxx 页面 |
| 2 | 用户提出的功能 B | 配置项/权限/适配处理方式 |
| 3 | 隐含期望（如界面不显示 ID、手机能正常用） | 显式化为要点，按规范处理 |
```

同时建 todo 清单（每要点一条），随开发即时更新。

## 二、开发顺序模板

```
1. 数据库     改 sql/init.sql（建表/加字段/初始数据）→ 本地执行
2. 后端       entity → mapper → service（鉴权+事务）→ controller → VO/DTO
3. 前端       api 封装 → 共用组件 → 薄壳视图 → 路由（三级结构）→ 菜单
4. 联调       启动后端 + 前端 dev，浏览器走完整用户路径
```

## 三、三层测试模板

### 3.1 接口测试（Python 脚本模式）

```python
import requests

BASE = 'http://localhost:8080/api'

def req(method, path, token=None, body=None, params=None):
    headers = {'Authorization': f'Bearer {token}'} if token else {}
    r = requests.request(method, BASE + path, json=body, params=params, headers=headers)
    return r.json()

def check(name, resp, expect_code=200):
    status = '通过' if resp.get('code') == expect_code else '失败'
    print(f'[{status}] {name}: {resp.get("message", "")[:50]}')

# 登录拿 token（角色按项目业务定义）
admin = req('POST', '/auth/login', body={'username': 'admin', 'password': '***'})['data']['token']
owner = req('POST', '/auth/login', body={'username': 'owner1', 'password': '***'})['data']['token']
user  = req('POST', '/auth/login', body={'username': 'user1', 'password': '***'})['data']['token']

# 正常路径
check('列表查询', req('GET', '/admin/ops/users?page=1&size=10', token=admin))
# 权限边界（必测）
check('普通用户访问管理接口应被拒', req('GET', '/admin/ops/users', token=user), expect_code=403)
check('资源归属者越权他人资源应被拒', req('GET', '/product/999/items', token=owner), expect_code=403)
```

### 3.2 浏览器测试（验收标准）

走真实用户路径：登录 → 目标页面 → 操作 → 断言：

- 界面显示的内容（列表字段、按钮文案）；
- 无红色错误提示；
- 操作后的界面反馈（成功提示/列表刷新）；
- 移动端至少做逻辑验证（断点变量 + 布局代码审查）。

### 3.3 回归测试清单

后端重新打包后必须重跑：

```markdown
| 场景 | 确认点 |
|------|--------|
| 受影响模块的既有接口 | 结果与改动前一致 |
| 权限边界用例 | 403 行为不变 |
| 中文/ASCII 关键字搜索 | 直连/代理两种路径均正常 |
| 文件上传（若加了过滤器） | multipart 不受影响 |
| 之前修过的 Bug 场景 | 不复发 |
```

## 四、阶段技术报告模板

```markdown
# 阶段N技术报告：<主题>
> 日期：YYYY-MM-DD
> 范围：<涉及工程>
> 目标：<一句话说明本阶段做了什么>

## 一、任务背景与要点拆解
（拆解表）

## 二~N、各功能实现方案
（每功能一节：设计取舍 + 关键代码片段；Bug 修复记录"现象→排查→根因→方案"）

## N+1、数据库变更
（建表/改表 SQL + 初始数据）

## N+2、回归测试
（接口测试表 + 浏览器测试表，逐项列出结果）

## N+3、遗留说明
（已知问题、外部依赖、边界情况）

## N+4、本阶段新增/修改文件清单
（按 backend / 各前端工程分组，标注关键改动）
```

## 五、仓库同步脚本模板（sync.sh）

```bash
#!/bin/bash
set -e
REPO=/path/to/local/repo
cd "$REPO"

mkdir -p <project>/docs
cp -f /workspace/docs/*.md <project>/docs/ 2>/dev/null || true

EXCLUDES="--exclude=node_modules --exclude=target --exclude=dist --exclude=.m2 --exclude=*.log"
rsync -a --delete $EXCLUDES /workspace/backend/ <project>/backend/ || true
rsync -a --delete $EXCLUDES /workspace/<admin-web>/ <project>/<admin-web>/ || true
rsync -a --delete $EXCLUDES /workspace/<user-web>/ <project>/<user-web>/ || true

if [ -n "$(git status --porcelain)" ]; then
  git add <project>
  git -c user.name="<name>" -c user.email="<email>" \
    commit -m "自动更新：同步项目代码与文档 $(date '+%Y-%m-%d %H:%M')" >/dev/null
  git push origin main
  echo "SYNCED: 已推送更新"
else
  echo "NO_CHANGE: 无变更，跳过"
fi
```

要点：rsync `--delete` 保持一致；排除构建产物；无变更不空提交；可配每小时定时任务自动执行。

## 六、Bug 修复剧本

```
1. 复现    用最小步骤稳定复现
2. 定位    看后端日志确认请求是否到达 → 区分前端/代理/网关/后端
3. 根因    写清楚为什么会发生（不是猜）
4. 修复    最小改动 + 兼容新旧两种路径（直连/代理等组合）
5. 回归    修复场景 + 相邻场景（中文/ASCII、普通请求/上传）
6. 记录    写入当阶段报告（现象→排查→根因→方案）
```

## 七、阶段划分参考（按项目规模裁剪）

| 阶段 | 主题 | 产出 |
|------|------|------|
| 一 | 环境搭建与基础骨架 | 工程初始化、登录鉴权、数据库连通 |
| 二 | 核心业务主流程 | 最主要领域模型与 CRUD |
| 三 | 扩展业务模块 | 依赖主流程的次级模块 |
| 四 | 亮点/集成功能 | 第三方服务、AI 等差异化能力 |
| 五~六 | 运营与数据 | 积分/活动/统计分析等运营体系 |
| 七 | 功能与接口测试 | 全量测试 + Bug 修复 |
| 八+ | 深度优化 | 性能、状态机、资源本地化、体验打磨 |
