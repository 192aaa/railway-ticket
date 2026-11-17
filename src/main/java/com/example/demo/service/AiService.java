package com.example.demo.service;

import com.example.demo.common.BusinessException;
import com.example.demo.config.AiProperties;
import com.example.demo.dto.AiChatRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 助手服务端代理：模型密钥只保存在服务端，由本类注入到对上游模型的请求中。
 * 浏览器永远拿不到密钥，只与后端的 /api/ai/** 交互。
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final AiProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    public AiService(AiProperties props) {
        this.props = props;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(5, props.getTimeoutSeconds())))
                .build();
    }

    public boolean isConfigured() {
        return props.isConfigured();
    }

    public String defaultModel() {
        return props.getModel();
    }

    /** 非流式对话：返回上游响应 JSON 字符串（含 choices/message/tool_calls）。 */
    public String chat(AiChatRequest req) {
        if (!props.isConfigured()) {
            throw new BusinessException("AI 模型未配置");
        }
        byte[] body = serializeBody(req, false);
        HttpRequest httpReq = HttpRequest.newBuilder(URI.create(props.getEndpoint()))
                .timeout(Duration.ofSeconds(Math.max(5, props.getTimeoutSeconds())))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + props.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        try {
            HttpResponse<String> resp = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                log.warn("AI 上游返回 HTTP {}: {}", resp.statusCode(), abbreviate(resp.body()));
                throw new BusinessException("AI 服务暂时不可用（HTTP " + resp.statusCode() + "）");
            }
            return resp.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("AI 请求被中断");
        } catch (java.io.IOException e) {
            log.error("AI 上游请求失败", e);
            throw new BusinessException("AI 服务连接失败");
        }
    }

    /**
     * 构造上游请求体：messages / tools 原样透传，模型与温度由服务端配置兜底，
     * 密钥通过 HTTP 头注入，绝不写进 body 下发给浏览器。
     */
    private byte[] serializeBody(AiChatRequest req, boolean stream) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", (req.getModel() != null && !req.getModel().isBlank())
                ? req.getModel() : props.getModel());
        body.put("messages", req.getMessages() == null ? List.of() : req.getMessages());
        if (req.getTools() != null && !req.getTools().isEmpty()) {
            body.put("tools", req.getTools());
            body.put("tool_choice", "auto");
        }
        body.put("temperature", req.getTemperature() != null ? req.getTemperature() : props.getTemperature());
        body.put("stream", stream);
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new BusinessException("AI 请求参数序列化失败");
        }
    }

    private static String abbreviate(String s) {
        if (s == null) return "";
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }
}
