package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** AI 助手服务端的模型配置；密钥只存在于服务端，不下发到浏览器。 */
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /** OpenAI/DeepSeek 兼容的 chat completions 地址 */
    private String endpoint = "https://api.deepseek.com/v1/chat/completions";
    /** 模型密钥；为空时服务端返回“未配置”，前端降级为模拟回复 */
    private String apiKey = "";
    /** 默认模型名 */
    private String model = "deepseek-chat";
    /** 默认温度 */
    private Double temperature = 0.7;
    /** 调用超时（秒） */
    private Integer timeoutSeconds = 30;

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
