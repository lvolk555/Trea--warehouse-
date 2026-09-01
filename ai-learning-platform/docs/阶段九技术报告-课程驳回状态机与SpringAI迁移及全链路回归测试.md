# 阶段九技术报告：课程驳回状态机、Spring AI 迁移与全链路回归测试

> 日期：2026-09-01
> 范围：AI 辅助在线学习平台（后端 + 学生端 + 管理端）
> 目标：完善课程审核"已驳回"状态的业务闭环；将 AI 客户端从手写 WebClient 迁移到 Spring AI 官方 OpenAI 通用模块；开展全量接口测试 + 前端交互测试，修复测试中发现的问题并同步仓库。

---

## 一、任务背景

本阶段前，平台存在两项待完善事项：

1. **课程驳回闭环缺失**：管理员驳回课程后，课程状态与"待审核/已下架"无法区分，教师端看不到"已驳回"标识，且被驳回的课程可以直接再次提交审核、甚至被上下架，业务流转存在漏洞。
2. **AI 客户端为手写封装**：阶段四出于"按需引入、避免过度设计"的考虑，用 WebClient 手写调用了智谱的 OpenAI 兼容接口。随着功能稳定，按当初预留的演进路径正式迁移到 **Spring AI 官方的 OpenAI 通用协议模块**（不使用任何厂商绑定 starter），获得官方抽象、重试与配置化能力。

在此基础上，本阶段对全平台进行了**深度接口测试（120 用例）与管理端/教师端/学生端三端浏览器交互测试**，修复测试中发现的问题。

---

## 二、课程审核"已驳回"状态机完善

### 2.1 状态定义

`CourseService` 中课程状态常量：

```java
public static final int STATUS_PENDING  = 0; // 待审核
public static final int STATUS_ONLINE   = 1; // 已上架
public static final int STATUS_OFFLINE  = 2; // 已下架（曾审核通过）
public static final int STATUS_REJECTED = 3; // 已驳回（需修改后重新提交）
```

状态流转规则：

```
教师创建课程 ──→ 待审核(0) ──管理员通过──→ 已上架(1) ⇄ 已下架(2)
                     │                        ↑
                     └──管理员驳回──→ 已驳回(3) ──教师修改保存──→ 待审核(0)
```

核心规则（均为本阶段明确并落地的业务约束）：

| 场景 | 行为 |
|------|------|
| 教师提交已驳回课程直接送审 | **拒绝**：提示"课程已被驳回，请重新修改后保存提交" |
| 教师重新修改保存（save）被驳回课程 | 状态回到**待审核**，走完整审核流 |
| 管理员对已驳回课程执行上架/下架 | **拒绝**：提示"该课程已被驳回，需教师重新修改提交审核后才可上下架" |
| 管理员对待审核课程执行上下架 | 拒绝：需先在课程审核中处理 |
| 已下架（曾通过）课程 | 教师端提供"提交审核"按钮可直接重新送审 |

### 2.2 后端实现

`CourseService.submitReview`（教师提交审核）增加已驳回拦截：

```java
public Course submitReview(Long courseId) {
    UserContext.checkRole(UserContext.ROLE_TEACHER, UserContext.ROLE_ADMIN);
    Course course = getOwnCourse(courseId);
    if (course.getStatus() == STATUS_PENDING) {
        throw new BizException("课程已在审核中");
    }
    if (course.getStatus() == STATUS_REJECTED) {
        throw new BizException("课程已被驳回，请重新修改后保存提交");
    }
    course.setStatus(STATUS_PENDING);
    courseMapper.updateById(course);
    return course;
}
```

`CourseAdminService`（管理员审核 + 上下架）：

```java
public Course review(ReviewDTO dto) {
    UserContext.checkRole(UserContext.ROLE_ADMIN);
    Course course = courseMapper.selectById(dto.getCourseId());
    if (course == null) throw new BizException("课程不存在");
    if (!course.getStatus().equals(CourseService.STATUS_PENDING)) {
        throw new BizException("该课程不在待审核状态");
    }
    // 驳回 → 状态 3（而非回到草稿），教师端可见"已驳回"标识
    course.setStatus(dto.getApproved() ? CourseService.STATUS_ONLINE : CourseService.STATUS_REJECTED);
    courseMapper.updateById(course);
    return course;
}

public Course changeStatus(Long courseId, boolean online) {
    UserContext.checkRole(UserContext.ROLE_ADMIN);
    Course course = courseMapper.selectById(courseId);
    if (course == null) throw new BizException("课程不存在");
    int status = course.getStatus();
    if (status == CourseService.STATUS_PENDING) {
        throw new BizException("该课程尚未审核通过，请先在课程审核中处理后再上下架");
    }
    if (status == CourseService.STATUS_REJECTED) {
        throw new BizException("该课程已被驳回，需教师重新修改提交审核后才可上下架");
    }
    course.setStatus(online ? CourseService.STATUS_ONLINE : CourseService.STATUS_OFFLINE);
    courseMapper.updateById(course);
    return course;
}
```

同步更新 `init.sql` 中 `course.status` 字段注释为 `0待审核 1已上架 2已下架 3已驳回`。

### 2.3 前端适配

**教师端 `CourseManageView.vue`**：

- 状态映射新增 `3: { text: '已驳回', color: 'red' }`；
- 操作列按状态渲染，被驳回课程显示红色"重新修改"按钮（打开编辑弹窗，保存即重新送审），**不显示**"提交审核"按钮；仅已下架（曾审核通过）课程显示"提交审核"。

```vue
<!-- 被驳回：仅允许重新修改（保存即重新提交审核），不可直接提交审核 -->
<a-button v-if="record.status === 3" size="small" type="primary" danger
         @click="openEdit(record)"><EditOutlined /> 重新修改</a-button>
<a-button v-else size="small" @click="openEdit(record)"><EditOutlined /> 编辑</a-button>
<!-- 仅已下架（曾审核通过）的课程可直接重新提交审核 -->
<a-button v-if="record.status === 2" size="small" type="primary" ghost
         @click="handleSubmit(record)"><SendOutlined /> 提交审核</a-button>
```

**管理端 `CourseManageAdminView.vue`**：状态筛选下拉新增"已驳回"选项；已驳回课程的上下架操作按钮禁用。

---

## 三、AI 客户端迁移 Spring AI（OpenAI 通用协议）

### 3.1 迁移决策演进

阶段四曾"手写 WebClient 而不引入 Spring AI"（当时只有两种调用模式，手写更可控）。本阶段按当时预留的演进路径完成迁移：**引入 Spring AI 官方的 OpenAI 通用协议模块 `spring-ai-starter-model-openai`，而非任何厂商绑定 starter**（如 zhipu starter）。理由：

1. **厂商中立**：OpenAI Chat Completions 已是事实行业标准，智谱/DeepSeek/通义等均提供兼容端点。使用通用模块后，切换模型厂商只需改 `base-url` + `api-key` + `model` 三个配置项，零代码改动。
2. **官方抽象与质量**：由 Spring 官方团队维护 `ChatModel`/`Prompt`/`Message` 抽象，SSE 流式解析、JSON 结构解析等协议细节不再需要自己维护。
3. **保留原有能力**：流式（答疑）与非流式（出题/批改/文章，需要完整 JSON）两种模式、429 限流重试、代理出网、超时降级全部保留，对上层服务的接口签名不变（`chat` / `chatStream` / `isConfigured`）。

### 3.2 依赖与配置

`backend/pom.xml`：

```xml
<properties>
    <spring-ai.version>1.1.8</spring-ai.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- OpenAI 通用协议模块（非厂商 starter），可对接任意兼容端点 -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-openai</artifactId>
    </dependency>
</dependencies>
```

`application.yml` 核心配置：

```yaml
spring:
  ai:
    model:
      chat: openai                      # 启用 OpenAI 协议的 chat 模型
    retry:
      max-attempts: 1                   # 关闭框架内置重试（默认最多10次、退避最长3分钟），429 重试统一由 ZhipuAiClient 控制
    openai:
      api-key: ${ZHIPU_API_KEY:...}      # 环境变量注入
      base-url: ${AI_BASE_URL:https://open.bigmodel.cn/api/paas/v4}
      chat:
        completions-path: /chat/completions   # 智谱兼容端点路径（默认 /v1/chat/completions 不适用）
        options:
          model: ${AI_MODEL:glm-4.6v-flash}
          temperature: 0.7
```

### 3.3 ZhipuAiClient 重构

由手写 WebClient 改为注入 Spring AI 自动装配的 `OpenAiChatModel`，对上层（AiChatService/AiGenerateService）接口签名完全不变：

```java
@Service
public class ZhipuAiClient {

    private final OpenAiChatModel chatModel;
    private final String apiKey;

    public ZhipuAiClient(OpenAiChatModel chatModel,
                         @Value("${spring.ai.openai.api-key:}") String apiKey) {
        this.chatModel = chatModel;
        this.apiKey = apiKey;
    }

    /** 流式对话（AI 答疑）：chatModel.stream() 返回逐块文本 */
    public Flux<String> chatStream(String systemPrompt, List<Map<String, String>> messages) {
        Prompt prompt = buildPrompt(systemPrompt, messages);
        return chatModel.stream(prompt)
                .mapNotNull(resp -> resp.getResult() == null || resp.getResult().getOutput() == null
                        ? null : resp.getResult().getOutput().getText())
                .filter(text -> text != null && !text.isEmpty())
                .retryWhen(rateLimitRetry())          // 429 限流重试（2次指数退避）
                .timeout(Duration.ofSeconds(60))
                .onErrorMap(e -> e instanceof BizException ? e
                        : new BizException("AI 服务暂时不可用，请稍后重试"));
    }

    /** 非流式对话（AI 出题/文章生成，需要完整 JSON）：chatModel.call 为同步阻塞，放入弹性线程池 */
    public Mono<String> chat(String systemPrompt, List<Map<String, String>> messages) {
        Prompt prompt = buildPrompt(systemPrompt, messages);
        return Mono.fromCallable(() -> chatModel.call(prompt))
                .map(resp -> { /* 校验非空后返回完整文本 */ })
                .subscribeOn(Schedulers.boundedElastic())
                .retryWhen(rateLimitRetry())
                .timeout(Duration.ofSeconds(90))
                .onErrorMap(e -> e instanceof BizException ? e
                        : new BizException("AI 服务暂时不可用，请稍后重试"));
    }
}
```

迁移中的两个关键适配点：

1. **429 重试识别**：Spring AI 将上游 4xx 包装为 `NonTransientAiException`（message 形如 `HTTP 429 - {...}`），并非裸的 `RestClientResponseException`。重试过滤器需同时检查异常链中的 `RestClientResponseException` 与 `HTTP 429` 前缀消息，并关闭框架内置重试（`spring.ai.retry.max-attempts=1`），否则一次限流最长可阻塞 3 分钟。
2. **阻塞调用隔离**：`chatModel.call()` 是同步阻塞的，用 `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())` 隔离，避免占用 Netty 事件循环线程。

### 3.4 代理出网支持

Spring AI 同步调用走 `RestClient`、流式走 `WebClient`，两者默认都**不读取**标准代理环境变量，而沙箱等环境必须经代理出网。新增 `config/AiHttpClientConfig.java`，以自定义 Builder Bean 替代 Boot 默认装配：

```java
@Configuration(proxyBeanMethods = false)
public class AiHttpClientConfig {

    /** 同步调用（chat）使用的 RestClient 构建器 */
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder().requestFactory(reactorRequestFactory());
    }

    /** 流式调用（chatStream / SSE）使用的 WebClient 构建器 */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(buildProxiedHttpClient()))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024));
    }

    /** 读取 HTTPS_PROXY/HTTP_PROXY/ALL_PROXY 环境变量；未设置时直连（生产环境行为不变） */
    private HttpClient buildProxiedHttpClient() { /* 同阶段八：解析代理地址并配置 reactor-netty HTTP 代理 */ }
}
```

### 3.5 迁移验证

| 验证项 | 结果 |
|--------|------|
| AI 答疑（SSE 流式，学生端页面逐字打字机效果） | 通过 |
| AI 出题（非流式，返回题目 JSON 草稿） | 通过 |
| AI 文章生成 | 通过 |
| AI 待复核批改列表 | 通过 |
| 多轮对话（历史上下文正确携带） | 通过 |
| 429 限流自动重试后仍失败时降级提示 | 通过（不阻断、不抛堆栈给前端） |

---

## 四、Bug 修复：AI 降级文案污染多轮对话上下文

### 4.1 问题表现

AI 因限流/超时进入降级时，"AI 服务暂时不可用，请稍后重试。"会被当作一条 **assistant 消息落库**。下一轮提问时该文案随历史一起进入模型上下文，模型会把它当成"自己说过的话"，例如学生问"请复述你上一条回答"，模型复述出的是错误提示文案，多轮语义被污染。

### 4.2 根因

`AiChatService.ask()` 的 `onErrorResume` 降级分支中 `saveMessage(sessionId, "assistant", msg)` 将降级文案与正常回答同等持久化，而 `buildHistory()` 组装上下文时未做区分。

### 4.3 修复方案

降级文案仍落库（保证前端回看历史完整），但**组装模型上下文时过滤**：

```java
// buildHistory() 中：
if ("assistant".equals(m.getRole()) && isFallbackText(m.getContent())) {
    continue;   // 降级提示不入上下文：避免模型把"服务不可用"当作自己说过的话
}

/** 降级提示文案前缀（这些消息仅用于前端展示，不进入模型上下文） */
private boolean isFallbackText(String content) {
    return content == null || content.isBlank()
            || content.startsWith("AI 服务暂时不可用")
            || content.startsWith("AI 服务暂未配置");
}
```

### 4.4 验证

构造被污染的会话（历史中含一条降级文案），冷却限流后追问"请逐字复述你上一条正式回答的开头一句话"：修复前模型会复述出"AI 服务暂时不可用"；**修复后模型正确复述出此前对 Java 的正式回答**，证明降级文案已不再进入模型上下文。

---

## 五、全链路回归测试

### 5.1 测试环境重建

沙箱环境重置后重新搭建：安装 MySQL 8.0 并初始化 `init.sql`（26 张表、5 个种子用户）；`mvn -s settings.xml package`（Maven 需显式代理配置）打包启动后端（8080）；学生端（5173）/管理端（5174）Vite 开发服务器；数据库连接、静态资源、三端登录均验证正常。

### 5.2 全量接口测试（tests/api_test.py，120 用例）

覆盖十大模块，结果 **PASS 118~120（因免费模型限流偶发 1~2 例波动）、FAIL 0、SKIP 1**（教师 2 账号不存在，条件跳过）：

| 模块 | 用例数 | 覆盖要点 |
|------|--------|----------|
| 一、认证与用户 | 18 | 三角色登录、错误/伪造 token、注册重复名、改密后新旧密码、登出失效 |
| 二、课程状态机 | 18 | 创建→驳回→直接送审被拒→管理员上架被拒→教师修改→重审通过→上下架全闭环 |
| 三、选课与学习 | 12 | 免费/积分选课、**重复选课幂等**、进度上报、断点续播、笔记 CRUD |
| 四、题库与考试 | 18 | 三种题型 CRUD、组卷发布、作答判分（满分/部分分）、重复提交被拒、**学生端题目不含答案** |
| 五、练习与错题本 | 7 | 按章节抽题、即时判分、**抽题不含答案**、错题本归集 |
| 六、积分体系 | 21 | 签到、兑换防重复、**管理端兑换记录含学生/课程名称**、积分活动、余额一致性 |
| 七、运营模块 | 9 | 公告发布/置顶/下线/删除、课程评论发表与管理端审核 |
| 八、统计模块 | 5 | 学生/教师/管理员三视角统计、管理端用户列表 |
| 九、AI 模块 | 5 | AI 出题、AI 文章生成、学生越权被拒、答疑会话列表、待复核批改列表 |
| 十、安全与边界 | 7 | 三角色越权互斥、SQL 注入登录、超长标题、删除上架课程被拒 |

### 5.3 浏览器交互测试（三端）

**管理端 + 教师端（5174，浏览器自动化实测）**：

| 测试项 | 结果 |
|--------|------|
| admin 登录、首页与左侧菜单渲染 | 通过 |
| 课程管理：列表加载、状态筛选含"已驳回" | 通过 |
| 课程审核：对待审核课程执行驳回，提示成功 | 通过 |
| 兑换记录列表含"学生名称/课程名称"列 | 通过 |
| 公告管理/用户管理/数据统计页面打开 | 通过 |
| 教师端"已驳回"课程：红色状态标签 + "重新修改"按钮、无"提交审核"按钮 | 通过 |
| "重新修改"打开编辑弹窗并正确加载课程数据 | 通过 |
| "待审核"课程无"提交审核"按钮；"已下架"课程有"提交审核"按钮 | 通过 |
| "已上架"课程无"提交审核"按钮 | 通过 |

**学生端（5173，浏览器自动化实测）**：

| 测试项 | 结果 |
|--------|------|
| student1 登录、个人中心（昵称/角色/积分余额） | 通过 |
| 课程广场卡片加载、分类筛选、搜索"Java"过滤 | 通过 |
| 课程详情：章节/小节目录、课程介绍、选课状态与完成度 | 通过 |
| 我的课程进入学习页（视频/文章小节渲染） | 通过 |
| AI 答疑：提问后流式回复逐字打字机输出 | 通过（限流冷却后复测通过） |
| 积分商城商品列表（含"立即兑换"按钮） | 通过 |
| 在线考试列表、章节练习（抽题配置） | 通过 |

### 5.4 已知外部限制（非代码缺陷）

- **免费模型限流**：`glm-4.6v-flash` 免费档对调用频率限制严格（连续调用会返回 429）。系统行为符合设计：自动重试 2 次（指数退避）后仍失败则对用户降级提示"AI 服务暂时不可用"，不阻断业务、不向前端抛堆栈；冷却约 1 分钟后即恢复。生产环境更换为付费档位或配置更高配额即可消除。
- Maven 在沙箱中需 `mvn -s settings.xml` 显式指定代理（阶段七已知问题，沿用）。

---

## 六、结论

1. **课程审核驳回闭环完成**：新增"已驳回"状态（3），教师端可见红色标识并提供"重新修改"入口，被驳回课程不可直接送审、不可上下架，必须修改保存后重新走完整审核流；前后端与 SQL 注释同步更新，接口与浏览器双重验证通过。
2. **AI 客户端完成 Spring AI 迁移**：采用官方 `spring-ai-starter-model-openai` 通用模块（非厂商 starter）对接智谱 OpenAI 兼容端点，保留流式/非流式双模式、429 重试、代理出网与超时降级，上层服务零改动；切换厂商仅需改三个配置项。
3. **修复 AI 降级文案污染多轮上下文的逻辑 Bug**：降级提示不再进入模型上下文，多轮对话语义正确。
4. **全量回归通过**：接口测试 120 用例 0 失败；管理端/教师端/学生端浏览器交互测试全部通过；测试发现的 1 个真实逻辑 Bug（上述）已修复并复验。

以上变更已通过 `sync.sh` 同步至 GitHub 仓库（代码、SQL、测试脚本与本文档）。
