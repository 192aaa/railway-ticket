package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    /** Redis 模式下返回；无 Redis 环境为 null，前端仍按原有 token 字段登录。 */
    private String refreshToken;
    private Long userId;
    private String username;
    private String realName;
}
