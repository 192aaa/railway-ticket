package com.example.demo.dto;

import java.util.List;
import java.util.Map;

/** AI 对话请求：messages 与 tools 结构对齐 OpenAI chat completions，由服务端透传。 */
public class AiChatRequest {

    private List<Map<String, Object>> messages;
    private List<Map<String, Object>> tools;
    private String model;
    private Double temperature;
    /** 是否流式；流式由 Controller 以 SSE 返回 */
    private Boolean stream = false;

    public List<Map<String, Object>> getMessages() { return messages; }
    public void setMessages(List<Map<String, Object>> messages) { this.messages = messages; }
    public List<Map<String, Object>> getTools() { return tools; }
    public void setTools(List<Map<String, Object>> tools) { this.tools = tools; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Boolean getStream() { return stream; }
    public void setStream(Boolean stream) { this.stream = stream; }
}
