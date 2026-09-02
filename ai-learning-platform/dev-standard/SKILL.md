---
name: "fullstack-dev-standard"
description: "企业级全栈项目开发规范与流程（Spring Boot 3 + Vue3 前后端分离）。新项目从零搭建、功能开发、测试、文档到仓库同步时调用；编写/评审前后端代码、设计项目结构、规划路由时也适用。"
---

# 企业级全栈开发规范（Spring Boot + Vue3 前后端分离）

本 skill 是可直接执行的企业级开发标准。完整代码模板见本 skill 目录下 `templates/`（backend-template.md / frontend-template.md / workflow-template.md）。执行时严格按以下规范。

## 〇、触发即遵守

无论任务是新建项目还是改造现有项目，接手后第一步先读本规范的"路由三级标准"与"开发顺序"，再开始动手。任何界面开发前先确认：这条路由处于三级结构中的哪一级？

## 一、企业级核心原则（不可违反）

| # | 原则 | 说明 |
|---|------|------|
| 1 | 前后端分离 | 后端只出 REST API；前端独立工程、独立端口，只走相对路径 `/api` |
| 2 | 路由三级标准 | **一级=布局壳，二级=功能分类（分组菜单），三级=功能页面**（详见第二节） |
| 3 | 界面零技术标识 | 前端不显示数据库 ID；列表/标签/筛选一律用名称（昵称优先） |
| 4 | 移动端一等公民 | 所有列表页 768px 断点双布局（桌面表格/移动卡片） |
| 5 | 数据一致性 | 删除主实体必须同事务级联清理关联数据；禁止孤儿记录 |
| 6 | 安全内建 | 鉴权在 service 入口；敏感配置脱敏；危险操作二次确认 |
| 7 | 阶段化交付 + 测试收口 | 功能完成 ≠ 完成；三层测试通过才算完成 |
| 8 | 文档即代码 | 每阶段技术报告进 `docs/`，随代码同步仓库 |

## 二、路由三级标准（强制结构）

**所有管理端功能页面必须遵循：一级路由为布局壳（MainLayout），二级路由为功能分类（分组菜单），三级路由为具体功能页面。**

### 2.1 结构定义

```
/ (一级：MainLayout 布局壳：侧边栏 + 顶栏 + 内容区)
└── admin/user (二级：功能分类"用户管理"，RouteGroup 分组容器)
    ├── /admin/user/student (三级：功能页"学生管理")
    ├── /admin/user/teacher (三级：功能页"教师管理")
    └── /admin/user/manager (三级：功能页"管理员账号")
```

### 2.2 路由代码标准

```js
// router/index.js —— 二级分类 + 三级功能页的标准写法
{
  path: 'admin/user',
  component: () => import('../layouts/RouteGroup.vue'),   // 分组容器（子路由出口）
  children: [
    { path: 'student', name: 'user-student', component: () => import('../views/admin/user/UserStudentView.vue') },
    { path: 'teacher', name: 'user-teacher', component: () => import('../views/admin/user/UserTeacherView.vue') },
    { path: 'manager', name: 'user-manager', component: () => import('../views/admin/user/UserManagerAccountView.vue') },
    { path: '', redirect: { name: 'user-student' } }     // 分类默认跳第一个子页
  ]
}
```

### 2.3 菜单与路由对齐标准

```js
// layouts/MainLayout.vue —— 菜单 key 与路由 name 严格一致
{ key: 'user-group', label: '用户管理', icon: TeamOutlined, children: [
  { key: 'user-student', label: '学生管理' },
  { key: 'user-teacher', label: '教师管理' },
  { key: 'user-manager', label: '管理员账号' }
] }
// developedRoutes 白名单控制菜单可见性（未开发的入口不上线）
const developedRoutes = ['Dashboard', 'user-student', 'user-teacher', 'user-manager', 'settings']
```

### 2.4 页面复用标准

**同一功能分类下的多个三级页面，若 UI 逻辑相同仅数据域不同，必须抽参数化共用组件**：

```
views/admin/user/
├── UserList.vue          # 共用组件：接收 role prop（1学生 2教师 3管理员）
├── UserStudentView.vue   # 薄壳：<UserList :role="1" title="学生管理" />
├── UserTeacherView.vue   # 薄壳：<UserList :role="2" title="教师管理" />
└── UserManagerAccountView.vue  # 薄壳：<UserList :role="3" title="管理员账号" />
```

### 2.5 新增功能时的路由决策

| 场景 | 决策 |
|------|------|
| 全新功能域（如"积分管理"） | 新建二级分类（分组菜单）+ 若干三级页 |
| 已有分类下加页面（用户管理加"家长账号"） | 在该分类 children 里加一条三级路由 + 薄壳视图 |
| 单一页面功能（如"系统设置"） | 可直接二级路由挂功能页，不必强行分组 |
| 同类多功能同构（按角色/类型） | 三级页面 + 参数化共用组件（禁止复制三份页面代码） |

## 三、标准开发流程（每次需求必走）

1. **需求拆解**：拆成可测试要点表（# / 要点 / 实现方式），建 todo 清单，完成一项勾一项；
2. **开发顺序**：数据库（`sql/init.sql`）→ 后端（entity → mapper → service → controller）→ 前端（api 封装 → 共用组件 → 薄壳视图 → 路由 → 菜单）→ 浏览器联调；
3. **三层测试**：接口测试（含权限边界 403 用例）→ 浏览器测试（真实用户路径）→ 回归测试（重跑受影响模块）；
4. **文档归档**：写《阶段N技术报告-主题.md》进 `docs/`（结构见 workflow-template.md）；
5. **仓库同步**：`sync.sh`（rsync 排除构建产物 + 条件 commit + push）。

## 四、后端规范（Spring Boot 3 + MyBatis-Plus + Sa-Token）

完整代码模板见 `templates/backend-template.md`，核心红线：

- 包结构：`common/`（Result/BizException/GlobalExceptionHandler/UserContext）+ `config/` + `module/<领域>/controller|service|mapper|entity|dto`；
- Controller 按角色拆分：AdminXxx / TeacherXxx / StudentXxx；业务逻辑只在 service；
- **鉴权在 service 入口第一行**：`UserContext.checkRole(...)`；数据权限抽 `checkXxxAccess()`；
- 统一响应：成功 `Result.ok(data)`；失败抛 `BizException("面向用户的提示")`；
- 关联名称批量 `selectBatchIds` 建 Map（禁止 N+1）；**列表接口返回名称不返回裸 ID**；
- 多表写必须 `@Transactional`；删除级联清理；不许删除当前登录账号；
- 敏感配置存 `system_config`：回显脱敏、留空跳过、指纹缓存热更新；
- 外部服务：开关校验 + 429 指数退避（最多 2 次）+ 超时放宽 + 降级提示；
- 建表 SQL 全进 `init.sql`，字段必带 COMMENT，必备 `id/create_time`，唯一键 `uk_*` 索引 `idx_*`。

## 五、前端规范（Vue 3 + Vite 双端）

完整代码模板见 `templates/frontend-template.md`，核心红线：

- 双工程：管理端（Ant Design Vue，5174）+ 用户端（Naive UI，5173）；
- axios 统一封装：baseURL `/api`、token 拦截器、响应拦截解包 `{code,message,data}`、401 自动登出、参数二次编码（`%`→`%25`）；页面里不直接写 axios；
- **路由三级标准**（见第二节，最高优先级）；
- **移动端强制适配**：`isMobile = window.innerWidth <= 768` + resize 监听（onUnmounted 移除）；桌面表格/移动卡片双布局；弹窗 `:width="isMobile ? '92%' : 520"`；模板中不直接访问 `window`；
- UI 准则：不显示 ID 一律名称；删除二次确认；当前账号行无操作按钮；列表标配 spin/empty/分页；操作即时 message 反馈。

## 六、Bug 修复标准动作

复现 → 定位（看后端日志确认请求是否到达，区分前端/代理/网关/后端）→ 根因 → 最小改动 + 兼容新旧路径 → 回归（直连/代理、中文/ASCII、普通/上传组合）→ 记入阶段报告。

## 七、环境纪律

- 后端从 `backend/` 目录启动 jar（静态资源依赖工作目录）；
- 沙箱出网给 JVM 加 `-Dhttp(s).proxyHost/Port`；
- 排障先看日志再改代码，不凭猜测。
