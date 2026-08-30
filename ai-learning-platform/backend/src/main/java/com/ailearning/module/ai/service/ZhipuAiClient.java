package com.ailearning.module.ai.service;

import com.ailearning.common.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 智谱 GLM 客户端：OpenAI 兼容协议（/chat/completions）
 * 支持流式（SSE）与非流式两种调用；密钥从环境变量 ZHIPU_API_KEY 注入，禁止硬编码
 */
@Slf4j
@Service
public class ZhipuAiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final String apiKey;

    public ZhipuAiClient(@Value("${ai.zhipu.base-url}") String baseUrl,
                         @Value("${ai.zhipu.api-key}") String apiKey,
                         @Value("${ai.zhipu.model}") String model,
                         ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                // 大模型输出较长，放宽响应体大小限制
                .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                .build();
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
        ObjectNode body = buildRequestBody(systemPrompt, messages, true);
        return webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body.toString())
                .retrieve()
                .bodyToFlux(new org.springframework.core.ParameterizedTypeReference<ServerSentEvent<String>>() {})
                // 过滤 [DONE] 与空块，解析 delta.content
                .mapNotNull(sse -> extractDelta(sse.data()))
                .filter(text -> !text.isEmpty())
                .timeout(Duration.ofSeconds(60));
    }

    /**
     * 非流式对话：一次性返回完整输出（AI 出题/批改使用，需要完整 JSON）
     */
    public Mono<String> chat(String systemPrompt, List<Map<String, String>> messages) {
        ObjectNode body = buildRequestBody(systemPrompt, messages, false);
        return webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body.toString())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(90))
                .map(this::extractContent);
    }

    /** 组装 OpenAI 格式请求体 */
    private ObjectNode buildRequestBody(String systemPrompt, List<Map<String, String>> messages, boolean stream) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("stream", stream);
        body.put("temperature", 0.7);
        ArrayNode messagesNode = body.putArray("messages");
        ObjectNode system = messagesNode.addObject();
        system.put("role", "system");
        system.put("content", systemPrompt);
        for (Map<String, String> m : messages) {
            ObjectNode node = messagesNode.addObject();
            node.put("role", m.get("role"));
            node.put("content", m.get("content"));
        }
        return body;
    }

    /** 解析流式块中的 choices[0].delta.content */
    private String extractDelta(String data) {
        if (data == null || data.isBlank() || "[DONE]".equals(data.trim())) {
            return "";
        }
        try {
            JsonNode node = objectMapper.readTree(data);
            JsonNode delta = node.path("choices").path(0).path("delta").path("content");
            return delta.isMissingNode() ? "" : delta.asText();
        } catch (Exception e) {
            return "";
        }
    }

    /** 解析非流式响应中的 choices[0].message.content */
    private String extractContent(String responseBody) {
        try {
            JsonNode node = objectMapper.readTree(responseBody);
            JsonNode content = node.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode()) {
                throw new BizException("AI 服务返回异常");
            }
            return content.asText();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析 AI 响应失败", e);
            throw new BizException("AI 服务返回格式异常");
        }
    }
}
