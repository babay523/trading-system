package com.trading.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 安全配置属性类
 * 用于从application.yml中读取安全相关配置
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "trading.security")
public class SecurityProperties {
    
    @Valid
    @NotNull
    private JwtConfig jwt = new JwtConfig();
    
    @Valid
    @NotNull
    private AuthenticationConfig authentication = new AuthenticationConfig();
    
    @Valid
    @NotNull
    private CorsConfig cors = new CorsConfig();
    
    @Valid
    @NotNull
    private EndpointsConfig endpoints = new EndpointsConfig();
    
    @Valid
    @NotNull
    private HeadersConfig headers = new HeadersConfig();
    
    @Valid
    @NotNull
    private AuditConfig audit = new AuditConfig();
    
    @Data
    public static class JwtConfig {
        private String secretKey;
        private Long expiration;
        private Long refreshExpiration;
        private String issuer;
        private String header;
        private String prefix;
        private String algorithm;
    }
    
    @Data
    public static class AuthenticationConfig {
        @NotNull
        private Boolean enabled = true;
        
        @Min(value = 1, message = "Max login attempts must be at least 1")
        @Max(value = 100, message = "Max login attempts cannot exceed 100")
        private Integer maxLoginAttempts = 5;
        
        @Min(value = 60000, message = "Lockout duration must be at least 1 minute")
        private Long lockoutDuration = 900000L; // 15 minutes
    }
    
    @Data
    public static class CorsConfig {
        @NotNull
        private Boolean enabled = true;
        private String allowedOrigins;
        private String allowedMethods;
        private String allowedHeaders;
        private String exposedHeaders;
        private Boolean allowCredentials = true;
        private Long maxAge = 3600L;
    }
    
    @Data
    public static class EndpointsConfig {
        private List<String> publicEndpoints;
        private List<String> protectedEndpoints;
    }
    
    @Data
    public static class HeadersConfig {
        private String frameOptions = "DENY";
        private String contentTypeOptions = "nosniff";
        private String xssProtection = "1; mode=block";
        private String referrerPolicy = "strict-origin-when-cross-origin";
    }
    
    @Data
    public static class AuditConfig {
        @NotNull
        private Boolean enabled = true;
        private Boolean logSuccessfulAuth = true;
        private Boolean logFailedAuth = true;
        private Boolean logAccessDenied = true;
    }
}