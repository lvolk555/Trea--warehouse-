package com.ailearning.module.ai.service;

import com.ailearning.common.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 对话客户端：基于 Spring AI 的 OpenAI 通用协议实现
 *
 * 通过 spring-ai-starter-model-openai 自动装配 OpenAiChatModel，以标准 OpenAI 协议
 * 对接任意兼容端点（当前为智谱 GLM，可通过配置无缝切换 DeepSeek/OpenAI 等，不绑定厂商 starter）。
 * 密钥从环境变量 ZHIPU_API_KEY 注入，禁止硬编码。
 *
 * - chatStream：流式对话（SSE），用于 AI 答疑
 * - chat：一次性返回完整输出，用于 AI 出题/批改（需要完整 JSON）
 * 两者均内置 429 限流自动重试（指数退避）。
 */
@Slf4j
@Service
public class ZhipuAiClient {

    private final OpenAiChatModel chatModel;
    private final String apiKey;

    public ZhipuAiClient(OpenAiChatModel chatModel,
                         @Value("${spring.ai.openai.api-key:}") String apiKey) {
        this.chatModel = chatModel;
        this.apiKey = apiKey;
    }

    /** API Key 是否已配置（未配置时各功能走降级提示） */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * 流式对话：逐块返回模型输出文本
     *
     * @param systemPrompt 系统提示词
     * @param messages     多轮历史 [{role, content}]，不含 system
     */
    public Flux<String> chatStream(String systemPrompt, List<Map<String, String>> messages) {
        Prompt prompt = buildPrompt(systemPrompt, messages);
        return chatModel.stream(prompt)
                .mapNotNull(resp -> resp.getResult() == null || resp.getResult().getOutput() == null
                        ? null : resp.getResult().getOutput().getText())
                .filter(text -> text != null && !text.isEmpty())
                // 限流自动重试（最多 2 次，指数退避），避免瞬时高峰导致失败
                .retryWhen(rateLimitRetry())
                .timeout(Duration.ofSeconds(60))
                .onErrorMap(e -> e instanceof BizException ? e
                        : new BizException("AI 服务暂时不可用，请稍后重试"));
    }

    /**
     * 非流式对话：一次性返回完整输出（AI 出题/文章生成使用，需要完整 JSON/Markdown）
     */
    public Mono<String> chat(String systemPrompt, List<Map<String, String>> messages) {
        Prompt prompt = buildPrompt(systemPrompt, messages);
        return Mono.fromCallable(() -> chatModel.call(prompt))
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
                .timeout(Duration.ofSeconds(90))
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
            // Spring AI 会将上游 4xx 包装为 AiException，message 形如 "HTTP 429 - {...}"
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("HTTP 429")) {
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
