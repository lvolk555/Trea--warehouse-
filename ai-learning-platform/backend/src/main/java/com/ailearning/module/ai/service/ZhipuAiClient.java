package com.ailearning.module.ai.service;

import com.ailearning.common.BizException;
import com.ailearning.module.ops.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 对话客户端：基于 Spring AI 的 OpenAI 通用协议实现
 *
 * 接入参数（密钥 / 端点 / 模型）支持两级配置：
 * 1. 管理端「系统设置」动态配置（system_config 表，优先级高，修改后即时生效）；
 * 2. application.yml / 环境变量（默认值）。
 * 运行时按配置指纹缓存模型客户端，配置变化自动重建，无需重启。
 *
 * - chatStream：流式对话（SSE），用于 AI 答疑
 * - chat：一次性返回完整输出，用于 AI 出题/批改（需要完整 JSON）
 * 两者均内置 429 限流自动重试（指数退避）。
 *
 * 管理员可在系统设置中关闭 ai_enabled 总开关，关闭后所有 AI 功能降级提示，不发起调用。
 */
@Slf4j
@Service
public class ZhipuAiClient {

    /** 智谱 OpenAI 兼容端点的补全路径（与 application.yml 保持一致） */
    private static final String COMPLETIONS_PATH = "/chat/completions";
    private static final double DEFAULT_TEMPERATURE = 0.7;

    private final SystemConfigService systemConfigService;

    /** yml / 环境变量默认配置（系统设置未覆盖时使用） */
    private final String defaultApiKey;
    private final String defaultBaseUrl;
    private final String defaultModel;

    /** 动态模型客户端缓存（配置指纹变化时重建） */
    private volatile OpenAiChatModel cachedModel;
    private volatile String cachedFingerprint = "";

    public ZhipuAiClient(SystemConfigService systemConfigService,
                         @Value("${spring.ai.openai.api-key:}") String defaultApiKey,
                         @Value("${spring.ai.openai.base-url:}") String defaultBaseUrl,
                         @Value("${spring.ai.openai.chat.options.model:}") String defaultModel) {
        this.systemConfigService = systemConfigService;
        this.defaultApiKey = defaultApiKey;
        this.defaultBaseUrl = defaultBaseUrl;
        this.defaultModel = defaultModel;
    }

    /** 生效的 API Key（系统设置优先，回退 yml 默认） */
    private String effectiveApiKey() {
        return systemConfigService.getValueOrDefault("ai_api_key", defaultApiKey);
    }

    /** API Key 是否已配置（未配置时各功能走降级提示） */
    public boolean isConfigured() {
        String key = effectiveApiKey();
        return StringUtils.hasText(key);
    }

    /** AI 功能是否可用：开关开启且密钥已配置 */
    public boolean isAvailable() {
        return systemConfigService.isEnabled("ai_enabled") && isConfigured();
    }

    /** 统一入口校验：开关关闭或密钥缺失时直接降级提示 */
    private void checkAvailable() {
        if (!systemConfigService.isEnabled("ai_enabled")) {
            throw new BizException("AI 功能已被管理员关闭");
        }
        if (!isConfigured()) {
            throw new BizException("AI 服务未配置，请联系管理员");
        }
    }

    /**
     * 解析当前配置的模型客户端：密钥/端点/模型任一变化即重建。
     * 使用配置指纹对比缓存，避免每次调用重复构建。
     */
    private OpenAiChatModel resolveModel() {
        String apiKey = effectiveApiKey();
        String baseUrl = systemConfigService.getValueOrDefault("ai_base_url", defaultBaseUrl);
        String model = systemConfigService.getValueOrDefault("ai_model", defaultModel);

        String fingerprint = apiKey + "|" + baseUrl + "|" + model;
        if (cachedModel == null || !fingerprint.equals(cachedFingerprint)) {
            // 出网代理跟随 JVM 系统参数（-Dhttps.proxyHost 等）；未设置时不启用，兼容直连环境
            HttpClient httpClient = HttpClient.create().proxyWithSystemProperties();
            OpenAiApi api = OpenAiApi.builder()
                    .apiKey(apiKey)
                    .baseUrl(baseUrl)
                    .completionsPath(COMPLETIONS_PATH)
                    .restClientBuilder(RestClient.builder()
                            .requestFactory(new ReactorClientHttpRequestFactory(httpClient)))
                    .webClientBuilder(WebClient.builder()
                            .clientConnector(new ReactorClientHttpConnector(httpClient)))
                    .build();
            this.cachedModel = OpenAiChatModel.builder()
                    .openAiApi(api)
                    .defaultOptions(OpenAiChatOptions.builder()
                            .model(model)
                            .temperature(DEFAULT_TEMPERATURE)
                            .build())
                    .build();
            this.cachedFingerprint = fingerprint;
            log.info("AI 模型配置已加载/刷新: model={}, baseUrl={}", model, baseUrl);
        }
        return cachedModel;
    }

    /**
     * 流式对话：逐块返回模型输出文本
     *
     * @param systemPrompt 系统提示词
     * @param messages     多轮历史 [{role, content}]，不含 system
     */
    public Flux<String> chatStream(String systemPrompt, List<Map<String, String>> messages) {
        checkAvailable();
        Prompt prompt = buildPrompt(systemPrompt, messages);
        return resolveModel().stream(prompt)
                .mapNotNull(resp -> resp.getResult() == null || resp.getResult().getOutput() == null
                        ? null : resp.getResult().getOutput().getText())
                .filter(text -> text != null && !text.isEmpty())
                // 限流自动重试（最多 2 次，指数退避），避免瞬时高峰导致失败
                .retryWhen(rateLimitRetry())
                // 免费/低配模型高峰期首 token 可达 60s+，放宽到 120s
                .timeout(Duration.ofSeconds(120))
                .onErrorMap(e -> e instanceof BizException ? e
                        : new BizException("AI 服务暂时不可用，请稍后重试"));
    }

    /**
     * 非流式对话：一次性返回完整输出（AI 出题/文章生成使用，需要完整 JSON/Markdown）
     */
    public Mono<String> chat(String systemPrompt, List<Map<String, String>> messages) {
        checkAvailable();
        Prompt prompt = buildPrompt(systemPrompt, messages);
        return Mono.fromCallable(() -> resolveModel().call(prompt))
                .map(resp -> {
                    String text = resp.getResult() == null || resp.getResult().getOutput() == null
                            ? null : resp.getResult().getOutput().getText();
                    if (text == null || text.isBlank()) {
                        throw new BizException("AI 服务返回异常");
                    }
                    return text;
                })
                // chatModel.call 为同步阻塞调用，放入弹性线程池，避免占用 Netty 事件循环线程
                .subscribeOn(Schedulers.boundedElastic())
                // 限流自动重试（最多 2 次，指数退避），避免瞬时高峰导致失败
                .retryWhen(rateLimitRetry())
                // 出题/批改需要完整输出，高峰期耗时更长，放宽到 150s
                .timeout(Duration.ofSeconds(150))
                .onErrorMap(e -> e instanceof BizException ? e
                        : new BizException("AI 服务暂时不可用，请稍后重试"));
    }

    /** 限流自动重试策略：仅针对 429，最多 2 次，指数退避 */
    private Retry rateLimitRetry() {
        return Retry.backoff(2, Duration.ofSeconds(2))
                .filter(this::isRateLimit)
                .onRetryExhaustedThrow((spec, signal) -> new BizException("AI 服务限流，请稍后重试"));
    }

    /** 识别 429 限流（含被包装的异常链） */
    private boolean isRateLimit(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof RestClientResponseException ex && ex.getStatusCode().value() == 429) {
                return true;
            }
            // Spring AI 会将上游错误包装为 AiException，message 形如 "429 - {...}" 或 "HTTP 429 - {...}"
            String msg = t.getMessage();
            if (msg != null && (msg.startsWith("429") || msg.startsWith("HTTP 429"))) {
                return true;
            }
        }
        return false;
    }

    /** 组装 Prompt：系统提示词 + 多轮历史 */
    private Prompt buildPrompt(String systemPrompt, List<Map<String, String>> messages) {
        List<Message> list = new ArrayList<>();
        list.add(new SystemMessage(systemPrompt));
        for (Map<String, String> m : messages) {
            String content = m.get("content");
            if (content == null) {
                continue;
            }
            list.add("assistant".equals(m.get("role")) ? new AssistantMessage(content) : new UserMessage(content));
        }
        return new Prompt(list);
    }
}
