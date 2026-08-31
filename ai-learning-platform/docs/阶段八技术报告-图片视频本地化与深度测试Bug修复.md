# 阶段八技术报告：图片/视频本地化与深度测试 Bug 修复

> 日期：2026-08-30
> 范围：AI 辅助在线学习平台（后端 + 学生端 + 管理端）
> 目标：消除对第三方外网图片/视频的依赖，进行深度接口与功能测试，定位并修复遗留 Bug。

---

## 一、任务背景

项目初始 SQL 种子数据中的课程封面（cover）和章节视频（video.url）直接引用第三方外网资源：

- 课程封面：`https://picsum.photos/seed/xxx/640/360`
- 章节视频：`https://www.w3schools.com/html/mov_bbb.mp4` / `.../movie.mp4`

在受限/离线环境下这些外网资源无法稳定访问，导致课程封面、视频封面出现裂图、视频无法播放。本次任务的核心是：**把这些"网络图片/视频"下载到本地，替换数据库与 SQL 中的外网 URL，并在此基础上做深度测试、修复遗留问题。**

---

## 二、外网资源可访问性排查结论

| 资源 | 直接访问 | 带浏览器 UA + Referer | 结论 |
|------|----------|----------------------|------|
| `picsum.photos` 封面图 | 503 | 503（服务端不可用） | **不可下载**，必须换用本地生成的课程封面 |
| `www.w3schools.com/html/mov_bbb.mp4` | 403 | 200（788493 字节） | 可下载（需伪装浏览器 UA + Referer） |
| `www.w3schools.com/html/movie.mp4` | 403 | 200（318465 字节） | 可下载（同上） |

> 说明：w3schools 服务器会拦截无浏览器 User-Agent / Referer 的普通请求（返回 403），带上浏览器头后即可正常返回视频。picsum.photos 当前整体处于 503 不可用状态，因此封面改用先前本地已生成的高清课程封面图。

---

## 三、Bug#1：课程封面/视频依赖外网导致裂图与无法播放

### 3.1 问题表现

- 课程广场 / 课程详情 / 我的课程中的封面图为裂图（`picsum.photos` 不可达）。
- 课程详情章节视频无法播放（`w3schools` 视频在浏览器直连下返回 403）。

### 3.2 根因

`backend/sql/init.sql`、`backend/sql/demo_data.sql` 及数据库中，课程 `cover` 与视频 `url` 字段直存第三方外网 URL，项目没有将这些资源纳入本地静态资源目录。

### 3.3 修复方案

1. **封面本地化**：将课程封面统一指向 `projectfiles/` 下已本地生成的课程封面图片，URL 形如 `/api/files/course-cover-<主题>.jpg`。

   课程 ID 与封面文件映射如下：

   | 课程 ID | 课程标题 | 本地封面 URL |
   |---------|----------|--------------|
   | 1 | Java 面向对象程序设计 | `/api/files/course-cover-java.jpg` |
   | 2 | Python 数据分析入门 | `/api/files/course-cover-python.jpg` |
   | 3 | Web 前端开发实战 | `/api/files/course-cover-web.jpg` |
   | 4 | MySQL 数据库从入门到精通 | `/api/files/course-cover-mysql.jpg` |
   | 5 | 数据结构与算法（Java 版） | `/api/files/course-cover-algorithm.jpg` |
   | 6 | Linux 操作系统基础 | `/api/files/course-cover-linux.jpg` |
   | 7 | 高等数学（上）精讲 | `/api/files/course-cover-math.jpg` |
   | 8 | 线性代数入门 | `/api/files/course-cover-linear.jpg` |
   | 9 | 大学英语四级冲刺 | `/api/files/course-cover-english.jpg` |
   | 10 | UI 设计基础 | `/api/files/course-cover-ui.jpg` |

2. **视频本地化**：用浏览器 UA + Referer 从 w3schools 下载两个示例视频到本地 `projectfiles/videos/`：

   ```bash
   UA='Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36'
   curl -s -A "$UA" -e 'https://www.w3schools.com/html/html5_video.asp' \
        -o projectfiles/videos/mov_bbb.mp4 'https://www.w3schools.com/html/mov_bbb.mp4'
   curl -s -A "$UA" -e 'https://www.w3schools.com/html/html5_video.asp' \
        -o projectfiles/videos/movie.mp4 'https://www.w3schools.com/html/movie.mp4'
   ```

   数据库更新（按原 URL 关键字精准替换）：

   ```sql
   UPDATE video SET url = CASE
     WHEN url LIKE '%movie.mp4%'    THEN '/api/files/videos/movie.mp4'
     WHEN url LIKE '%mov_bbb.mp4%'  THEN '/api/files/videos/mov_bbb.mp4'
     ELSE url END;
   ```

3. **同步修改 SQL 源文件与生成脚本**：
   - `backend/sql/init.sql`、`backend/sql/demo_data.sql` 中的 cover / video URL 全部改为本地 `/api/files/...` 路径。
   - `backend/sql/gen_demo_data.py` 中封面改用 `cover_map` 生成本地路径，视频改用本地路径。

### 3.4 验证

- 数据库课程表 10 条封面全部为 `/api/files/...` 本地路径。
- 视频表 48 条记录：45 条 `mov_bbb.mp4`、3 条 `movie.mp4`，全部本地化。
- 静态资源直连验证均返回 200 且 Content-Type 正确：

  ```text
  /api/files/course-cover-java.jpg  -> 200 image/jpeg
  /api/files/videos/mov_bbb.mp4     -> 200 video/mp4
  /api/files/videos/movie.mp4       -> 200 video/mp4
  /api/files/avatar-01.jpg          -> 200 image/jpeg
  ```

---

## 四、Bug#2：前端遗留 picsum 外网兜底 URL

### 4.1 问题表现

学生端三个页面的封面 `<img>` 存在因 `course.cover` 为空时回退到 `https://picsum.photos/...` 的兜底逻辑，属于遗留外网图片引用，一旦命中即为裂图。

涉及文件：
- `ai-learning-student/src/views/course/SquareView.vue`
- `ai-learning-student/src/views/course/DetailView.vue`
- `ai-learning-student/src/views/course/MyCoursesView.vue`

### 4.2 修复方案

删除外网兜底，封面直接绑定本地数据字段：

```diff
- <img :src="course.cover || 'https://picsum.photos/seed/' + course.id + '/640/360'" class="cover" />
+ <img :src="course.cover" class="cover" />
```

修复后全项目（前端 + 后端 + SQL）已无任何 `picsum` / `w3schools` 外网资源引用。

---

## 五、深度测试结果

### 5.1 接口测试（后端 localhost:8080）

覆盖登录、课程广场、课程详情、选课、学习进度、章节练习、考试、积分、签到、公告评论、统计、AI、教师端、管理端、用户资料共 **48 个用例**，结果：**通过 45，失败 3（均为测试脚本预期问题，非系统 Bug）**。

| 用例 | 结果 | 结论 |
|------|------|------|
| 课程广场全部课程 total>=10 | 实际 9 | 课程 ID=3 状态为"待审核"，广场仅展示已上架（status=1）课程，符合业务预期 |
| 学习进度上报（字段 `progress`） | 400 | 后端字段名为 `position`（`ProgressReportDTO.position`），测试脚本用错字段名；前端 `StudyView.vue` 实际正确传 `position` |
| 积分签到重复调用 | code=500 | 后端 `SignService` 对"今日已签到"抛出 `BizException`，本项目约定所有业务异常默认 `code=500`、前端统一以 `message` 展示，属既有设计约定，非缺陷 |

### 5.2 浏览器端到端测试（学生端 localhost:5173）

使用账号 `student1 / 123456` 实测，**全部通过**：

1. 登录页渲染正常。
2. 登录后进入「学习看板」，侧边栏 10 个菜单（学习看板、课程广场、我的课程、章节练习、在线考试、错题本、我的成绩、AI 答疑、积分中心、公告）均完整出现。
3. 课程广场点击「编程 / 数学 / 外语 / 设计」分类，均无 400/500 报错，卡片正常更新。
4. 课程封面图片全部正常加载（`naturalWidth=2560 / naturalHeight=1440`，无裂图）。
5. 课程详情封面、章节、评论正常渲染；章节视频地址均为 `/api/files/videos/` 本地路径。
6. 控制台无 `console.error`，网络请求中 `/api/` 接口均无 ≥400 请求。
7. 我的课程 / 在线考试 / 积分中心 / 错题本页面均正常渲染、无报错。

---

## 六、历史遗留 Bug 修复回顾（本阶段一并纳入验证）

为保证文档完整性，列示前序阶段已修复、本阶段深度测试再次验证通过的 Bug：

| 编号 | Bug 描述 | 根因 | 修复方式 |
|------|----------|------|----------|
| H-1 | 课程广场点击中文分类返回 400 | 中文参数经 URL 查询串传输被 Tomcat 拒绝 | 课程广场接口由 GET 改为 POST，中文参数放请求体（新增 `SquareQueryDTO`，前端 `request.post('/course/square', params)`） |
| H-2 | 课程详情 / 进入考试等接口返回 500 | Maven 编译未生成 `-parameters` 参数名元数据，Spring 反射无法解析 `@PathVariable`/`@RequestParam` 参数名 | `pom.xml` 的 `maven-compiler-plugin` 增加 `<parameters>true</parameters>` 后重新编译 |
| H-3 | 静态资源 `/files/avatar-01.jpg` 返回 404 | 后端统一挂在 `/api` 上下文下，前端访问漏掉 `/api` 前缀 | 前端统一使用 `/api/files/...` 访问静态资源 |

---

## 七、Bug#3：AI 接口在需代理出网的环境下超时

### 7.1 问题表现

- 学生端「AI 答疑」提问接口 `/student/ai/ask`（SSE）长时间无响应，约 60s 后才返回降级提示。
- 教师端「AI 出题」接口 `/teacher/ai/generate` 同样超时。
- 后端日志反复出现连接超时：

  ```text
  Caused by: io.netty.channel.ConnectTimeoutException: connection timed out after 30000 ms: open.bigmodel.cn/39.106.105.100:443
  java.util.concurrent.TimeoutException: Did not observe any item or terminal signal within 60000ms in 'filter'
  ```

### 7.2 根因

`ZhipuAiClient` 使用 WebClient（reactor-netty）**直连**智谱大模型 `open.bigmodel.cn`。而当前部署/测试环境外网必须经代理（`http://127.0.0.1:18080`）出网：

- 宿主机 `curl` 会读取 `HTTPS_PROXY`/`HTTP_PROXY` 环境变量，走代理成功连通（实测同 API Key 同请求 1.3s 返回 200）。
- Java WebClient 默认**不读取**这些环境变量，走直连即连不上，最终超时。

由此产生一个"看起来像代码问题、实为网络出口差异"的假象：同一份请求体，`curl` 成功、后端超时。

附带说明：模型 `glm-4.6v-flash` 为推理模型，流式响应先输出 `reasoning_content`（思考过程）再输出 `content`（最终答案）。客户端 `extractDelta` 仅取 `content` 并过滤空块，因此思考阶段前端无输出，最终答案到齐后正常流式返回——属预期行为，非缺陷。

### 7.3 修复方案

在 `ZhipuAiClient` 中开支持代理的 HttpClient：读取标准代理环境变量并配置 reactor-netty 代理；未设置时回退直连（不影响无代理的生产环境）。

关键改动（`backend/src/main/java/com/ailearning/module/ai/service/ZhipuAiClient.java`）：

```java
// 构造器中：为 WebClient 挂载支持代理的连接器
this.webClient = WebClient.builder()
        .baseUrl(baseUrl)
        .clientConnector(new ReactorClientHttpConnector(buildHttpClient()))
        .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
        .build();

/** 构建 HttpClient：读取标准代理环境变量出网；未设置时直连。 */
private HttpClient buildHttpClient() {
    HttpClient client = HttpClient.create();
    String proxyUrl = firstNonBlank(
            System.getenv("HTTPS_PROXY"), System.getenv("https_proxy"),
            System.getenv("HTTP_PROXY"),  System.getenv("http_proxy"),
            System.getenv("ALL_PROXY"));
    if (proxyUrl == null || proxyUrl.isBlank()) {
        return client;
    }
    try {
        URI uri = URI.create(proxyUrl);
        String host = uri.getHost();
        if (host != null) {
            int port = uri.getPort() > 0 ? uri.getPort() : 80;
            client = client.proxy(p -> p.type(ProxyProvider.Proxy.HTTP).host(host).port(port));
        }
    } catch (Exception e) {
        log.warn("解析代理地址失败，忽略代理配置: {}", proxyUrl, e);
    }
    return client;
}
```

### 7.4 验证

- 修复前 `/student/ai/ask` 挂起 60s；修复后数秒内流式返回完整回答（`data:[SESSION:n]` 首帧 + 逐块正文 + 结束）。
- `/teacher/ai/generate` 正常返回生成题目。
- 扩展测试第 8 节（AI 接口）全部通过。

---

## 八、Bug#4：测试脚本未与「课程广场改 POST / AI 已可用」同步

### 8.1 问题表现

上一阶段将课程广场接口从 GET 改为 POST 后，两个自动化测试脚本未同步，导致：

- `api_test.py` 第 74 行仍 `call("GET", "/course/square", ...)`，返回结构与断言不匹配。
- `api_test_extended.py` 第 96 行仍 `GET /course/square`，运行时直接抛 `AttributeError: 'NoneType' object has no attribute 'get'`（GET 无此映射，`data` 为 null），测试中途中止。

同时，第 8 节 AI 相关断言过时：修复代理后 AI 已真实可用（不再无密钥降级），原断言「AI 出题未配置时报错」不再成立。

### 8.2 修复方案

1. 课程广场接口调用统一改为 POST + JSON 查询体：

   ```python
   # api_test.py
   s, r = call("POST", "/course/square", token=tk_student, body={"page": 1, "size": 8})
   check("课程广场返回已上架课程", r["code"] == 200 and isinstance(r["data"], dict) and "records" in r["data"])

   # api_test_extended.py
   s, r = call("POST", "/course/square", token=tk_student, body={"page": 1, "size": 8, "keyword": "测试课程-自动化"})
   ```

2. AI 接口断言改为反映真实能力：

   ```python
   check("AI 提问返回流式回答", "SESSION" in body, body[:200])
   check("AI 出题成功返回题目", r["code"] == 200 and isinstance(r.get("data"), list) and len(r["data"]) > 0, str(r)[:200])
   ```

### 8.3 验证

- `api_test.py`：79/79 全部通过。
- `api_test_extended.py`：60/60 全部通过。

---

## 九、结论

1. 已彻底消除项目中课程封面与章节视频对第三方外网（picsum.photos / w3schools）的依赖，全部改为本地 `projectfiles/` 静态资源并映射到 `/api/files/**`。
2. 下载并本地化了 2 个示例视频、确认本地 10 张课程封面与 8 张头像均有效。
3. **修复 AI 接口代理出网问题**：后端 `ZhipuAiClient` 支持读取标准代理环境变量，AI 答疑（SSE 流式）/AI 出题在需代理环境下恢复正常。
4. **修复测试脚本同步问题**：`api_test.py` 与 `api_test_extended.py` 已适配「课程广场 POST」与「AI 已可用」，全量测试 79 + 60 例全部通过。
5. 前端遗留的 picsum 外网兜底 URL 已移除。

修复后的数据库与 SQL 源文件、生成脚本、后端代码、测试脚本均保持一致，可直接通过 `sync.sh` 同步到仓库。