---
name: "fullstack-dev-standard"
description: "通用企业级全栈项目开发规范与流程（Spring Boot 3 + Vue3 前后端分离，与具体业务领域无关）。新项目从零搭建、功能开发、测试、文档到仓库同步时调用；编写/评审前后端代码、设计项目结构、规划路由时也适用。"
---

# 企业级全栈开发规范（Spring Boot + Vue3 前后端分离，通用型）

本规范与具体业务领域无关，适用于任何中后台管理系统 / 双端（管理端+用户端）项目。完整代码模板见本 skill 目录下 `templates/`（backend-template.md / frontend-template.md / workflow-template.md）。

## 〇、触发即遵守

无论任务是新建项目还是改造现有项目，接手后第一步先确认：
1. 本项目的**角色体系**是什么（按业务定义，如 客服/主管/管理员、买家/商家/平台 等）；
2. 任何界面开发前先确认：这条路由处于三级结构中的哪一级？
3. 任何列表接口/页面开发前先确认：用户看到的是名称还是 ID？

## 一、企业级核心原则（不可违反，与领域无关）

| # | 原则 | 说明 |
|---|------|------|
| 1 | 前后端分离 | 后端只出 REST API；前端独立工程、独立端口，只走相对路径 `/api`（经 Vite 代理转发） |
| 2 | 路由三级标准 | **一级=布局壳，二级=功能分类（分组菜单），三级=功能页面**（详见第二节） |
| 3 | 界面零技术标识 | 前端不显示数据库 ID、内部编码；列表/标签/筛选一律用名称（展示名优先，回退登录名） |
| 4 | 移动端一等公民 | 所有列表页 768px 断点双布局（桌面表格/移动卡片） |
| 5 | 数据一致性 | 删除主实体必须同事务级联清理关联数据；禁止孤儿记录 |
| 6 | 安全内建 | 鉴权在 service 入口；数据权限统一校验；敏感配置脱敏；危险操作二次确认 |
| 7 | 阶段化交付 + 测试收口 | 功能完成 ≠ 完成；三层测试通过才算完成 |
| 8 | 文档即代码 | 每阶段技术报告进 `docs/`，随代码同步仓库 |
| 9 | 参数编码兼容 | 前端 query 参数二次编码 + 后端容错解码，保证直连/代理两种路径中文参数行为一致 |

## 二、路由三级标准（强制结构）

**所有管理端功能页面必须遵循：一级路由为布局壳（MainLayout），二级路由为功能分类（分组菜单），三级路由为具体功能页面。**

### 2.1 结构定义（以通用"用户管理"为例）

```
/ (一级：MainLayout 布局壳：侧边栏 + 顶栏 + 内容区)
└── admin/user (二级：功能分类"用户管理"，RouteGroup 分组容器)
    ├── /admin/user/<typeA> (三级：功能页，如"会员管理")
    ├── /admin/user/<typeB> (三级：功能页，如"员工管理")
    └── /admin/user/<typeC> (三级：功能页，如"管理员账号")
```

> 示例中的"用户管理"是任意功能分类（订单管理、商品管理、内容审核……同理）。

### 2.2 路由代码标准

```js
// router/index.js —— 二级分类 + 三级功能页的标准写法
{
  path: 'admin/user',                                            // 二级：功能分类
  component: () => import('../layouts/RouteGroup.vue'),   // 分组容器（子路由出口）
  children: [
    { path: 'member',  name: 'user-member',  component: () => import('../views/admin/user/UserMemberView.vue') },
    { path: 'staff',  name: 'user-staff',   component: () => import('../views/admin/user/UserStaffView.vue') },
    { path: 'manager', name: 'user-manager', component: () => import('../views/admin/user/ManagerAccountView.vue') },
    { path: '', redirect: { name: 'user-member' } }   // 分类默认跳第一个子页
  ]
}
```

### 2.3 菜单与路由对齐标准

```js
// layouts/MainLayout.vue —— 菜单 key 与路由 name 严格一致
{ key: 'user-group', label: '用户管理', icon: TeamOutlined, children: [
  { key: 'user-member', label: '会员管理' },
  { key: 'user-staff', label: '员工管理' },
  { key: 'user-manager', label: '管理员账号' }
] }
// developedRoutes 白名单控制菜单可见性（未开发的入口不上线）
const developedRoutes = ['Dashboard', 'user-member', 'user-staff', 'user-manager', 'settings']
```

### 2.4 页面复用标准

**同一功能分类下的多个三级页面，若 UI 逻辑相同仅数据域不同，必须抽参数化共用组件**：

```
views/admin/user/
├── UserList.vue          # 共用组件：接收 type prop（区分数据域）
├── UserMemberView.vue    # 薄壳：<UserList :type="1" title="会员管理" />
├── UserStaffView.vue     # 薄壳：<UserList :type="2" title="员工管理" />
└── ManagerAccountView.vue # 薄壳：<UserList :type="3" title="管理员账号" />
```

### 2.5 新增功能时的路由决策

| 场景 | 决策 |
|------|------|
| 全新功能域（如新增"营销管理"） | 新建二级分类（分组菜单）+ 若干三级页 |
| 已有分类下加页面（用户管理加"供应商账号"） | 该分类 children 加一条三级路由 + 薄壳视图 |
| 单一页面功能（如"系统设置"） | 可直接二级路由挂功能页，不必强行分组 |
| 同类多功能同构（按类型/角色/状态区分） | 三级页面 + 参数化共用组件（禁止复制多份页面代码） |

## 三、标准开发流程（每次需求必走）

1. **需求拆解**：拆成可测试要点表（# / 要点 / 实现方式），建 todo 清单，完成一项勾一项；
2. **开发顺序**：数据库（`sql/init.sql`）→ 后端（entity → mapper → service → controller）→ 前端（api 封装 → 共用组件 → 薄壳视图 → 路由 → 菜单）→ 浏览器联调；
3. **三层测试**：接口测试（含权限边界 403 用例）→ 浏览器测试（真实用户路径）→ 回归测试（重跑受影响模块）；
4. **文档归档**：写《阶段N技术报告-主题.md》进 `docs/`（结构见 workflow-template.md）；
5. **仓库同步**：`sync.sh`（rsync 排除构建产物 + 条件 commit + push）。

## 四、后端规范（Spring Boot 3 + MyBatis-Plus + Sa-Token）

完整代码模板见 `templates/backend-template.md`，核心红线：

- 包结构：`common/`（Result/BizException/GlobalExceptionHandler/UserContext）+ `config/` + `module/<领域>/controller|service|mapper|entity|dto`；
- Controller 按角色拆分（角色名按业务定义，如 AdminXxx / OperatorXxx / MemberXxx）；业务逻辑只在 service；
- **鉴权在 service 入口第一行**：`UserContext.checkRole(...)`；数据权限抽 `checkXxxAccess()`（资源归属者只能操作自己的资源，管理员放行）；
- 统一响应：成功 `Result.ok(data)`；失败抛 `BizException("面向用户的提示")`；
- 关联名称批量 `selectBatchIds` 建 Map（禁止 N+1）；**列表接口返回名称不返回裸 ID**；
- 多表写必须 `@Transactional`；删除级联清理；不许删除/禁用当前登录账号自己；
- 敏感配置（密钥/外部服务参数）存 `system_config`：回显脱敏、留空跳过、指纹缓存热更新；默认值放 yml 用 `${KEY:默认}` 环境变量占位；
- 外部服务集成（AI/支付/短信等任意第三方）：统一入口检查开关+配置；429 指数退避（最多 2 次）；超时放宽；降级给业务提示不裸抛堆栈；
- 建表 SQL 全进 `init.sql`，字段必带 COMMENT，必备 `id/create_time`，唯一键 `uk_*` 索引 `idx_*`。

## 五、前端规范（Vue 3 + Vite 多端工程）

完整代码模板见 `templates/frontend-template.md`，核心红线：

- 按用户群拆独立工程（UI 库可按端选择，如管理端 Ant Design Vue、用户端 Naive UI），各工程固定独立端口；
- axios 统一封装：baseURL `/api`、token 拦截器、响应拦截解包 `{code,message,data}`、401 自动登出、参数二次编码（`%`→`%25`）；页面里不直接写 axios；
- **路由三级标准**（见第二节，最高优先级）；
- **移动端强制适配**：`isMobile = window.innerWidth <= 768` + resize 监听（onUnmounted 移除）；桌面表格/移动卡片双布局；弹窗 `:width="isMobile ? '92%' : 520"`；模板中不直接访问 `window`；
- UI 准则：不显示 ID 一律名称；筛选是名称文本搜索框不是 ID 数字框；删除二次确认；当前账号行无操作按钮；列表标配 spin/empty/分页；操作即时 message 反馈。

## 六、Bug 修复标准动作

复现 → 定位（看后端日志确认请求是否到达，区分前端/代理/网关/后端）→ 根因 → 最小改动 + 兼容新旧路径 → 回归（直连/代理、中文/ASCII、普通/上传组合）→ 记入阶段报告。

## 七、环境纪律

- 后端从工程根目录启动 jar（静态资源/相对路径依赖工作目录）；
- 受限网络环境出网给 JVM 加 `-Dhttp(s).proxyHost/Port`；
- 排障先看日志再改代码，不凭猜测。
