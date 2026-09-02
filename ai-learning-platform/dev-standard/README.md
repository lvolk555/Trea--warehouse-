# 全栈项目开发规范与流程（dev-standard，通用型）

> 通用企业级开发标准，与具体业务领域无关（Spring Boot 3 + Vue3 前后端分离）。
> 沉淀自真实项目全流程实践：阶段化交付、三层测试收口、文档即代码、路由三级标准、界面零技术标识、移动端适配。
> 新项目直接按本目录内容执行；在 TRAE 中使用时由 skill `fullstack-dev-standard` 自动加载。

## 文件索引

| 文件 | 内容 | 使用时机 |
|------|------|----------|
| [SKILL.md](./SKILL.md) | 主规范：核心原则 + 路由三级标准 + 前后端红线 + 流程 | 所有开发任务的总纲 |
| [templates/backend-template.md](./templates/backend-template.md) | 后端全套代码模板（分层/鉴权/事务/级联/敏感配置/外部服务） | 编写后端代码时照抄替换 |
| [templates/frontend-template.md](./templates/frontend-template.md) | 前端全套代码模板（axios 封装/三级路由/共用组件/移动端适配） | 编写前端代码时照抄替换 |
| [templates/workflow-template.md](./templates/workflow-template.md) | 流程剧本（拆解表/三层测试/阶段报告/sync.sh/Bug 修复） | 每个需求/阶段执行时 |

## 核心理念（一分钟版）

1. **路由三级标准**：一级=布局壳，二级=功能分类（分组菜单），三级=功能页面；同类多页抽参数化共用组件；
2. **界面零技术标识**：前端不显示数据库 ID；列表/筛选一律名称（展示名优先）；
3. **移动端一等公民**：所有列表页 768px 断点双布局（桌面表格/移动卡片）；
4. **阶段化交付**：大需求拆阶段，每阶段产出可运行版本 + 技术报告；
5. **测试先行收口**：接口（含 403 权限用例）+ 浏览器 + 回归三层全过才算完成；
6. **数据一致性**：删除级联清理无孤儿；敏感配置脱敏回显、留空不修改；
7. **持续同步**：sync.sh 一键 rsync + commit + push，可配定时任务。

## 快速开始（新项目）

```bash
# 1. 按 templates 中的结构初始化目录（backend + 各前端工程 + docs + tests）
# 2. 复制 workflow-template.md 中的 sync.sh，替换 REPO 路径与项目子目录名
# 3. 按 SKILL.md 规范开发，按 workflow-template.md 流程推进阶段
# 4. 每阶段结束：三层测试 → 写阶段技术报告 → bash sync.sh
```
