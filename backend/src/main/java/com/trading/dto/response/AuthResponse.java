package com.trading.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 认证响应DTO
 * 包含JWT令牌和商家信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    
    /**
     * JWT访问令牌
     */
    private String accessToken;
    
    /**
     * 令牌类型，固定为"Bearer"
     */
    @Builder.Default
    private String tokenType = "Bearer";
    
    /**
     * 令牌过期时间（秒）
     */
    private Long expiresIn;
    
    /**
     * 认证的商家信息
     */
    private MerchantResponse merchant;
    
    /**
     * 令牌签发时间
     */
    private LocalDateTime issuedAt;
}