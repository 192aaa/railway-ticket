package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.AiChatRequest;
import com.example.demo.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** AI 助手代理接口：浏览器只与本接口通信，模型密钥由服务端持有。 */
@Tag(name = "AI 助手接口")
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @Operation(summary = "查询 AI 助手配置状态（不含密钥）")
    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        return Result.success(Map.of(
                "configured", aiService.isConfigured(),
                "model", aiService.defaultModel()
        ));
    }

    @Operation(summary = "AI 对话代理（服务端注入密钥，原样返回上游响应）")
    @PostMapping(value = "/chat", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> chat(@RequestBody AiChatRequest req) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(aiService.chat(req));
    }
}
