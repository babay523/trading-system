package com.trading.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 安全配置验证器
 * 在应用启动时验证关键安全配置的有效性
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityConfigurationValidator {
    
    private final JwtProperties jwtProperties;
    private final SecurityProperties securityProperties;
    private final Environment environment;
    
    /**
     * 应用启动完成后验证配置
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateConfiguration() {
        log.info("Starting security configuration validation...");
        
        try {
            validateJwtConfiguration();
            validateEnvironmentSpecificConfiguration();
            validateCorsConfiguration();
            validateEndpointsConfiguration();
            
            log.info("Security configuration validation completed successfully");
        } catch (SecurityConfigurationException e) {
            log.error("Security configuration validation failed: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 验证JWT配置
     */
    private void validateJwtConfiguration() {
        log.debug("Validating JWT configuration...");
        
        // 验证密钥基本要求
        String secretKey = jwtProperties.getSecretKey();
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new SecurityConfigurationException(
                "JWT secret key cannot be null or empty. Please set JWT_SECRET_KEY environment variable."
            );
        }
        
        if (secretKey.length() < 32) {
            throw new SecurityConfigurationException(
                String.format("JWT secret key is too short (%d characters). It must be at least 32 characters long for security.", 
                    secretKey.length())
            );
        }
        
        // 检查是否使用了不安全的默认值
        if (secretKey.contains("your-256-bit-secret-key")) {
            throw new SecurityConfigurationException(
                "JWT secret key is using the default placeholder value. Please set a secure JWT_SECRET_KEY environment variable."
            );
        }
        
        // 验证过期时间
        if (jwtProperties.getExpiration() == null || jwtProperties.getExpiration() < 300000) {
            throw new SecurityConfigurationException(
                "JWT expiration time must be at least 5 minutes (300000ms)"
            );
        }
        
        // 验证刷新令牌过期时间
        if (jwtProperties.getRefreshExpiration() == null || jwtProperties.getRefreshExpiration() < 3600000) {
            throw new SecurityConfigurationException(
                "JWT refresh expiration time must be at least 1 hour (3600000ms)"
            );
        }
        
        // 验证刷新令牌过期时间应该大于访问令牌过期时间
        if (jwtProperties.getRefreshExpiration() <= jwtProperties.getExpiration()) {
            throw new SecurityConfigurationException(
                "JWT refresh expiration time must be greater than access token expiration time"
            );
        }
        
        log.debug("JWT configuration validation passed");
    }
    
    /**
     * 验证环境特定配置
     */
    private void validateEnvironmentSpecificConfiguration() {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isProduction = Arrays.asList(activeProfiles).contains("prod");
        
        log.debug("Validating environment-specific configuration for profiles: {}", Arrays.toString(activeProfiles));
        
        if (isProduction) {
            validateProductionConfiguration();
        } else if (Arrays.asList(activeProfiles).contains("test")) {
            validateTestConfiguration();
        } else {
            validateDevelopmentConfiguration();
        }
        
        log.debug("Environment-specific configuration validation passed");
    }
    
    /**
     * 验证生产环境配置
     */
    private void validateProductionConfiguration() {
        log.debug("Validating production configuration...");
        
        // 生产环境不能使用开发或测试密钥
        String secretKey = jwtProperties.getSecretKey();
        if (secretKey.contains("dev-secret-key") || 
            secretKey.contains("test-secret-key") || 
            secretKey.contains("development") || 
            secretKey.contains("testing")) {
            throw new SecurityConfigurationException(
                "Production environment detected but JWT secret key contains development/test keywords. " +
                "Please set a secure production JWT_SECRET_KEY environment variable."
            );
        }
        
        // 验证生产环境必须启用认证
        if (!securityProperties.getAuthentication().getEnabled()) {
            throw new SecurityConfigurationException(
                "Authentication cannot be disabled in production environment"
            );
        }
        
        // 验证生产环境的登录尝试限制
        if (securityProperties.getAuthentication().getMaxLoginAttempts() > 5) {
            log.warn("Production environment has high max login attempts ({}). Consider reducing for better security.", 
                securityProperties.getAuthentication().getMaxLoginAttempts());
        }
        
        // 验证CORS配置
        String allowedOrigins = securityProperties.getCors().getAllowedOrigins();
        if (allowedOrigins != null && allowedOrigins.contains("*")) {
            throw new SecurityConfigurationException(
                "CORS cannot allow all origins (*) in production environment. Please specify exact allowed origins."
            );
        }
    }
    
    /**
     * 验证测试环境配置
     */
    private void validateTestConfiguration() {
        log.debug("Validating test configuration...");
        
        // 测试环境可以禁用认证，但要记录警告
        if (!securityProperties.getAuthentication().getEnabled()) {
            log.warn("Authentication is disabled in test environment");
        }
    }
    
    /**
     * 验证开发环境配置
     */
    private void validateDevelopmentConfiguration() {
        log.debug("Validating development configuration...");
        
        // 开发环境的警告
        if (jwtProperties.getSecretKey().contains("dev-secret-key")) {
            log.warn("Using development JWT secret key. This should not be used in production.");
        }
        
        if (jwtProperties.getExpiration() < 3600000) {
            log.info("JWT expiration is set to less than 1 hour in development environment");
        }
    }
    
    /**
     * 验证CORS配置
     */
    private void validateCorsConfiguration() {
        log.debug("Validating CORS configuration...");
        
        if (!securityProperties.getCors().getEnabled()) {
            log.warn("CORS is disabled. This may cause issues with frontend applications.");
            return;
        }
        
        String allowedOrigins = securityProperties.getCors().getAllowedOrigins();
        if (allowedOrigins == null || allowedOrigins.trim().isEmpty()) {
            throw new SecurityConfigurationException(
                "CORS is enabled but no allowed origins are specified"
            );
        }
        
        log.debug("CORS configuration validation passed");
    }
    
    /**
     * 验证端点配置
     */
    private void validateEndpointsConfiguration() {
        log.debug("Validating endpoints configuration...");
        
        List<String> publicEndpoints = securityProperties.getEndpoints().getPublicEndpoints();
        List<String> protectedEndpoints = securityProperties.getEndpoints().getProtectedEndpoints();
        
        if (publicEndpoints == null || publicEndpoints.isEmpty()) {
            log.warn("No public endpoints configured. This may cause issues with authentication endpoints.");
        }
        
        if (protectedEndpoints == null || protectedEndpoints.isEmpty()) {
            log.warn("No protected endpoints configured. All endpoints may be publicly accessible.");
        }
        
        // 检查是否有重叠的端点配置
        if (publicEndpoints != null && protectedEndpoints != null) {
            for (String publicEndpoint : publicEndpoints) {
                for (String protectedEndpoint : protectedEndpoints) {
                    if (publicEndpoint.equals(protectedEndpoint)) {
                        throw new SecurityConfigurationException(
                            String.format("Endpoint '%s' is configured as both public and protected", publicEndpoint)
                        );
                    }
                }
            }
        }
        
        log.debug("Endpoints configuration validation passed");
    }
    
    /**
     * 安全配置异常
     */
    public static class SecurityConfigurationException extends RuntimeException {
        public SecurityConfigurationException(String message) {
            super(message);
        }
        
        public SecurityConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}