package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendMessageRequest {

    @NotNull(message = "收件人 ID 不能为空")
    private Long receiverId;

    @NotNull(message = "消息内容不能为空")
    @Size(max = 500, message = "消息内容不能超过 500 字")
    private String content;
}