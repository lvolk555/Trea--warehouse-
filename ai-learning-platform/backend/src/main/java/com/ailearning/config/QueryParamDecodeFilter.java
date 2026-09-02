package com.ailearning.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 查询参数容错解码过滤器
 *
 * 前端为兼容反向代理（如沙箱预览代理）会对 query string 预先解码一次的行为，
 * 对参数值做了二次编码（% → %25）。Spring/Tomcat 解码一次后参数值中若仍含
 * %XX 转义序列，则在此再解码一次，保证：
 *   - 直连访问（单层编码 + 本过滤器解码）与
 *   - 经代理访问（代理解码一层 + Tomcat 解码一层）
 * 两种路径下中文关键字等参数均正确还原。
 */
@Component
@Order(1)
public class QueryParamDecodeFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // 文件上传请求跳过，避免提前触发 multipart 解析
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith("multipart/")) {
            chain.doFilter(request, response);
            return;
        }
        Map<String, String[]> raw = request.getParameterMap();
        Map<String, String[]> decoded = new HashMap<>();
        boolean changed = false;
        for (Map.Entry<String, String[]> entry : raw.entrySet()) {
            String[] values = entry.getValue().clone();
            for (int i = 0; i < values.length; i++) {
                String value = percentDecode(values[i]);
                if (value != null) {
                    values[i] = value;
                    changed = true;
                }
            }
            decoded.put(entry.getKey(), values);
        }
        chain.doFilter(changed ? new DecodedParamRequest(request, decoded) : request, response);
    }

    /**
     * 对包含合法 %XX 序列且可解码为有效 UTF-8 的字符串做一次百分号解码；
     * 不满足条件返回 null（保持原值，不影响普通参数）。
     */
    static String percentDecode(String value) {
        if (value == null || value.indexOf('%') < 0) {
            return null;
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        boolean hasEscape = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '%' && i + 2 < value.length()) {
                int high = Character.digit(value.charAt(i + 1), 16);
                int low = Character.digit(value.charAt(i + 2), 16);
                if (high >= 0 && low >= 0) {
                    bytes.write((byte) ((high << 4) | low));
                    hasEscape = true;
                    i += 2;
                    continue;
                }
            }
            // 其余字符按 UTF-8 原样写入
            byte[] charBytes = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
            bytes.write(charBytes, 0, charBytes.length);
        }
        if (!hasEscape) {
            return null;
        }
        try {
            return bytes.toString(StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return null;
        }
    }

    /** 用解码后的参数表替换原请求参数 */
    private static class DecodedParamRequest extends HttpServletRequestWrapper {

        private final Map<String, String[]> params;

        DecodedParamRequest(HttpServletRequest request, Map<String, String[]> params) {
            super(request);
            this.params = params;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return params;
        }

        @Override
        public String getParameter(String name) {
            String[] values = params.get(name);
            return values == null || values.length == 0 ? null : values[0];
        }

        @Override
        public String[] getParameterValues(String name) {
            return params.get(name);
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return Collections.enumeration(params.keySet());
        }
    }
}
