---
name: "fullstack-dev-standard"
description: "全栈项目开发规范与流程（Spring Boot + Vue3 前后端分离）。新项目从零搭建、功能开发、测试、文档到仓库同步时调用；编写/评审前后端代码、设计项目结构时也适用。"
---

# 全栈项目开发规范与流程

按本规范执行全栈项目的开发、测试、文档与交付。详细版见 `dev-standard/` 目录。

## 一、开发流程（每次需求必走）

1. **需求拆解**：需求拆成可测试要点表（# / 要点 / 实现方式），建立 todo 清单，完成一项勾一项；
2. **开发顺序**：数据库（改 `sql/init.sql`）→ 后端（entity → mapper → service → controller）→ 前端（api 封装 → 页面 → 路由 → 菜单）→ 浏览器联调；
3. **阶段化交付**：大项目按阶段推进（骨架 → 主流程 → 扩展 → 亮点 → 运营 → 测试 → 深度优化），每阶段产出可运行版本；
4. **三层测试**：接口测试（Python/curl，含权限边界 403 用例）→ 浏览器测试（真实用户路径）→ 回归测试（后端改动重跑受影响模块）；
5. **文档归档**：测试通过后写《阶段N技术报告-主题.md》进 `docs/`，固定结构：要点拆解 → 实现方案（含代码与设计取舍）→ 数据库变更 → 回归测试结果 → 遗留说明 → 文件清单；
6. **仓库同步**：`sync.sh`（rsync 排除 node_modules/target/dist + 条件 commit + push），commit 格式 `自动更新：同步项目代码与文档 <时间>`。

## 二、后端规范（Spring Boot 3 + MyBatis-Plus + Sa-Token）

- 包结构：`common/`（Result/BizException/GlobalExceptionHandler/UserContext/FileUploadController）+ `config/` + `module/<领域>/controller|service|mapper|entity|dto`；
- controller 按角色拆分：AdminXxx / TeacherXxx / StudentXxx；业务逻辑只在 service；
- **鉴权在 service 入口第一行**：`UserContext.checkRole(UserContext.ROLE_XX)`；
- 数据权限抽 `checkXxxAccess()` 统一校验（教师只能操作自己的资源，管理员放行）；
- 统一响应：成功 `Result.ok(data)`；失败抛 `BizException("面向用户的提示")`；
- 查询用 LambdaQueryWrapper + 分页 Page；关联名称**批量 selectBatchIds 建 Map**，禁止循环单查（N+1）；
- 多表写必须 `@Transactional(rollbackFor = Exception.class)`；删除主实体时同事务级联清理全部关联数据（无孤儿记录）；不许删除当前登录账号自己；
- **列表接口返回名称不返回裸 ID**：VO 加 `studentName`/`courseName`（昵称优先回退用户名，缺失给兜底文案）；筛选入参用 `keyword` 按名称模糊匹配（先命中 ID 集合再过滤，无命中返回空页）；
- 敏感配置存 `system_config` 表：回显脱敏（前4后4打码）、保存留空跳过（不覆盖）、支持运行时热更新（配置指纹缓存，改完即生效）；默认值放 yml 用 `${KEY:默认}` 环境变量占位；
- 外部服务（AI 等）：统一入口检查开关+配置；429 指数退避重试最多 2 次；流式超时 120s；降级给业务提示不裸抛堆栈；
- 建表 SQL 全进 `backend/sql/init.sql`，表字段必带 COMMENT，必备 `id/create_time`，唯一键 `uk_*` 索引 `idx_*`；
- 文件上传 UUID 重命名；全局过滤器跳过 multipart 请求。

## 三、前端规范（Vue 3 + Vite 双端）

- 双工程：管理端（Ant Design Vue，5174）+ 用户端（Naive UI，5173），结构 `api/ views/ components/ layouts/ router/ stores/ utils/`；
- axios 统一封装：baseURL `/api`、token 拦截器（Bearer，localStorage 键按端隔离）、响应拦截解包 `{code,message,data}`、401 自动登出；**参数二次编码**（`encodeURIComponent` 后 `%`→`%25`，兼容代理预解码，配合后端容错解码过滤器）；页面里不直接写 axios；
- **路由三级结构**：一级布局 → 二级分类（分组菜单）→ 三级功能页；同类多功能用参数化共用组件（`<UserList :role="1" title="学生管理">`）；
- **移动端强制适配**（所有列表页）：`isMobile = window.innerWidth <= 768` + resize 监听（onUnmounted 移除）；桌面表格 / 移动卡片双布局；弹窗 `:width="isMobile ? '92%' : 520"`、抽屉移动端 100%；模板中不直接访问 `window`；
- UI 准则：
  - **不显示任何 ID**，一律名称（昵称优先）；筛选是名称文本搜索框不是 ID 数字框；
  - 删除/移除 `Modal.confirm` 二次确认，文案说明后果；
  - 当前账号行显示"当前账号"且无编辑/删除按钮；
  - 列表标配：spin 加载态、empty 空态、分页（pageSize 10 + 总数）；
  - 成功 success / 失败 error(e.message) 即时反馈；
- Vite：固定端口 + `/api` 代理到 8080，前端只请求相对路径。

## 四、Bug 修复标准动作

复现 → 定位（看后端日志确认请求是否到达，区分前端/代理/网关/后端）→ 根因（写清为什么）→ 最小改动修复 + 兼容新旧路径 → 回归（直连/代理、中文/ASCII、普通/上传等组合）→ 记入阶段报告。

## 五、环境纪律

- 后端从 `backend/` 目录启动 jar（静态资源相对路径依赖工作目录）；
- 沙箱出网给 JVM 加 `-Dhttp(s).proxyHost/Port`；
- 排障先看日志再改代码，不凭猜测。
