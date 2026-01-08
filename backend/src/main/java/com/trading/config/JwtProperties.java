package com.trading.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * JWT配置属性类
 * 用于从application.yml中读取JWT相关配置
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "trading.security.jwt")
public class JwtProperties {
    
    /**
     * JWT签名密钥
     * 必须至少32个字符长度以确保安全性
     */
    @NotBlank(message = "JWT secret key cannot be blank")
    private String secretKey = "your-256-bit-secret-key-here-must-be-at-least-32-characters-long";
    
    /**
     * JWT令牌过期时间（毫秒）
     * 默认24小时，最小5分钟
     */
    @NotNull(message = "JWT expiration cannot be null")
    @Min(value = 300000, message = "JWT expiration must be at least 5 minutes (300000ms)")
    private Long expiration = 86400000L;
    
    /**
     * 刷新令牌过期时间（毫秒）
     * 默认7天
     */
    @NotNull(message = "JWT refresh expiration cannot be null")
    @Min(value = 3600000, message = "JWT refresh expiration must be at least 1 hour (3600000ms)")
    private Long refreshExpiration = 604800000L;
    
    /**
     * JWT发行者
     */
    @NotBlank(message = "JWT issuer cannot be blank")
    private String issuer = "trading-system";
    
    /**
     * Authorization header名称
     */
    @NotBlank(message = "JWT header cannot be blank")
    private String header = "Authorization";
    
    /**
     * JWT令牌前缀
     */
    @NotBlank(message = "JWT prefix cannot be blank")
    private String prefix = "Bearer ";
    
    /**
     * JWT签名算法
     */
    @NotBlank(message = "JWT algorithm cannot be blank")
    @Pattern(regexp = "^(HS256|HS384|HS512)$", message = "JWT algorithm must be one of: HS256, HS384, HS512")
    private String algorithm = "HS256";
    
    /**
     * 验证密钥长度是否足够安全
     */
    public boolean isSecretKeyValid() {
        return secretKey != null && 
               !secretKey.trim().isEmpty() && 
               secretKey.length() >= 32 &&
               !secretKey.contains("your-256-bit-secret-key") && // 不能使用默认值
               !secretKey.contains("dev-secret-key") && // 生产环境不能使用开发密钥
               !secretKey.contains("test-secret-key"); // 生产环境不能使用测试密钥
    }
    
    /**
     * 检查是否为生产环境安全配置
     */
    public boolean isProductionReady() {
        return isSecretKeyValid() && 
               expiration <= 86400000L && // 生产环境令牌不应超过24小时
               !secretKey.contains("development") &&
               !secretKey.contains("testing");
    }
}