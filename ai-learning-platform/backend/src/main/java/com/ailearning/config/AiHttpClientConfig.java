package com.ailearning.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import java.net.URI;

/**
 * Spring AI HTTP 客户端代理配置
 *
 * Spring AI 的 OpenAI 客户端（同步走 RestClient、流式走 WebClient）不会读取标准代理环境变量，
 * 此处提供代理感知的 Builder Bean（替代 Boot 默认装配），保证需要代理出网的环境（如沙箱）
 * 也能访问 AI 服务；未设置代理变量时直连，行为与直连环境一致。
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class AiHttpClientConfig {

    /** Spring AI 同步调用（chat）使用的 RestClient 构建器 */
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder()
                .requestFactory(reactorRequestFactory());
    }

    /** Spring AI 流式调用（chatStream / SSE）使用的 WebClient 构建器 */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(buildProxiedHttpClient()))
                // 大模型输出较长，放宽响应体大小限制
                .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024));
    }

    private ClientHttpRequestFactory reactorRequestFactory() {
        return new ReactorClientHttpRequestFactory(buildProxiedHttpClient());
    }

    /** 读取标准代理环境变量（HTTPS_PROXY/HTTP_PROXY/ALL_PROXY）构建 HttpClient；未设置时直连 */
    private HttpClient buildProxiedHttpClient() {
        HttpClient client = HttpClient.create();
        String proxyUrl = firstNonBlank(
                System.getenv("HTTPS_PROXY"),
                System.getenv("https_proxy"),
                System.getenv("HTTP_PROXY"),
                System.getenv("http_proxy"),
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

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
