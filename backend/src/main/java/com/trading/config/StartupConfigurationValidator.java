package com.trading.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 启动时配置验证器
 * 在应用启动时验证关键配置并提供清晰的错误消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupConfigurationValidator implements CommandLineRunner {
    
    private final JwtProperties jwtProperties;
    private final SecurityProperties securityProperties;
    private final Environment environment;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("=== Trading System Security Configuration Validation ===");
        
        try {
            validateRequiredConfiguration();
            validateEnvironmentConfiguration();
            logConfigurationSummary();
            
            log.info("=== Configuration validation completed successfully ===");
        } catch (Exception e) {
            log.error("=== Configuration validation failed ===");
            log.error("Error: {}", e.getMessage());
            log.error("Please fix the configuration issues before starting the application.");
            throw e;
        }
    }
    
    /**
     * 验证必需的配置
     */
    private void validateRequiredConfiguration() {
        log.info("Validating required configuration...");
        
        // 验证JWT密钥
        validateJwtSecretKey();
        
        // 验证JWT过期时间
        validateJwtExpiration();
        
        // 验证基本安全设置
        validateBasicSecuritySettings();
        
        log.info("✓ Required configuration validation passed");
    }
    
    /**
     * 验证JWT密钥
     */
    private void validateJwtSecretKey() {
        String secretKey = jwtProperties.getSecretKey();
        
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new ConfigurationValidationException(
                "JWT secret key is not configured. Please set JWT_SECRET_KEY environment variable."
            );
        }
        
        if (secretKey.length() < 32) {
            throw new ConfigurationValidationException(
                String.format("JWT secret key is too short (%d characters). It must be at least 32 characters long for security.", 
                    secretKey.length())
            );
        }
        
        // 检查是否使用了默认值
        if (secretKey.contains("your-256-bit-secret-key") || 
            secretKey.contains("default") || 
            secretKey.contains("changeme")) {
            throw new ConfigurationValidationException(
                "JWT secret key appears to be using a default value. Please set a secure, unique secret key."
            );
        }
        
        log.debug("✓ JWT secret key validation passed");
    }
    
    /**
     * 验证JWT过期时间
     */
    private void validateJwtExpiration() {
        Long expiration = jwtProperties.getExpiration();
        Long refreshExpiration = jwtProperties.getRefreshExpiration();
        
        if (expiration == null || expiration < 300000) { // 5 minutes
            throw new ConfigurationValidationException(
                "JWT expiration time must be at least 5 minutes (300000ms). Current value: " + expiration
            );
        }
        
        if (refreshExpiration == null || refreshExpiration < 3600000) { // 1 hour
            throw new ConfigurationValidationException(
                "JWT refresh expiration time must be at least 1 hour (3600000ms). Current value: " + refreshExpiration
            );
        }
        
        if (refreshExpiration <= expiration) {
            throw new ConfigurationValidationException(
                String.format("JWT refresh expiration (%dms) must be greater than access token expiration (%dms)", 
                    refreshExpiration, expiration)
            );
        }
        
        log.debug("✓ JWT expiration validation passed");
    }
    
    /**
     * 验证基本安全设置
     */
    private void validateBasicSecuritySettings() {
        // 验证认证设置
        if (securityProperties.getAuthentication().getMaxLoginAttempts() == null ||
            securityProperties.getAuthentication().getMaxLoginAttempts() < 1) {
            throw new ConfigurationValidationException(
                "Max login attempts must be at least 1"
            );
        }
        
        if (securityProperties.getAuthentication().getLockoutDuration() == null ||
            securityProperties.getAuthentication().getLockoutDuration() < 60000) {
            throw new ConfigurationValidationException(
                "Lockout duration must be at least 1 minute (60000ms)"
            );
        }
        
        log.debug("✓ Basic security settings validation passed");
    }
    
    /**
     * 验证环境特定配置
     */
    private void validateEnvironmentConfiguration() {
        String[] activeProfiles = environment.getActiveProfiles();
        log.info("Validating configuration for active profiles: {}", Arrays.toString(activeProfiles));
        
        if (activeProfiles.length == 0) {
            log.warn("No active profiles detected. Using default configuration.");
            validateDefaultConfiguration();
        } else if (Arrays.asList(activeProfiles).contains("prod")) {
            validateProductionConfiguration();
        } else if (Arrays.asList(activeProfiles).contains("test")) {
            validateTestConfiguration();
        } else {
            validateDevelopmentConfiguration();
        }
        
        log.info("✓ Environment-specific configuration validation passed");
    }
    
    /**
     * 验证默认配置
     */
    private void validateDefaultConfiguration() {
        log.info("Validating default configuration...");
        
        if (jwtProperties.getSecretKey().contains("your-256-bit-secret-key")) {
            log.warn("⚠ Using default JWT secret key. Please set JWT_SECRET_KEY environment variable for production use.");
        }
    }
    
    /**
     * 验证生产环境配置
     */
    private void validateProductionConfiguration() {
        log.info("Validating production configuration...");
        
        // 生产环境不能使用开发或测试密钥
        String secretKey = jwtProperties.getSecretKey();
        if (secretKey.contains("dev-secret-key") || 
            secretKey.contains("test-secret-key") || 
            secretKey.contains("development") || 
            secretKey.contains("testing")) {
            throw new ConfigurationValidationException(
                "Production environment detected but JWT secret key contains development/test keywords. " +
                "Please set a secure production JWT_SECRET_KEY environment variable."
            );
        }
        
        // 生产环境必须启用认证
        if (!securityProperties.getAuthentication().getEnabled()) {
            throw new ConfigurationValidationException(
                "Authentication cannot be disabled in production environment"
            );
        }
        
        // 检查CORS配置
        String allowedOrigins = securityProperties.getCors().getAllowedOrigins();
        if (allowedOrigins != null && allowedOrigins.contains("*")) {
            throw new ConfigurationValidationException(
                "CORS cannot allow all origins (*) in production environment. " +
                "Please specify exact allowed origins using CORS_ORIGINS environment variable."
            );
        }
        
        // 检查JWT过期时间不应过长
        if (jwtProperties.getExpiration() > 86400000) { // 24 hours
            log.warn("⚠ JWT expiration time is longer than 24 hours in production. Consider reducing for better security.");
        }
        
        log.info("✓ Production configuration validation passed");
    }
    
    /**
     * 验证测试环境配置
     */
    private void validateTestConfiguration() {
        log.info("Validating test configuration...");
        
        if (!securityProperties.getAuthentication().getEnabled()) {
            log.info("ℹ Authentication is disabled in test environment");
        }
        
        if (jwtProperties.getSecretKey().contains("test-secret-key")) {
            log.info("ℹ Using test JWT secret key");
        }
        
        log.info("✓ Test configuration validation passed");
    }
    
    /**
     * 验证开发环境配置
     */
    private void validateDevelopmentConfiguration() {
        log.info("Validating development configuration...");
        
        if (jwtProperties.getSecretKey().contains("dev-secret-key")) {
            log.info("ℹ Using development JWT secret key");
        }
        
        if (jwtProperties.getExpiration() < 3600000) { // 1 hour
            log.info("ℹ JWT expiration is set to less than 1 hour for development convenience");
        }
        
        log.info("✓ Development configuration validation passed");
    }
    
    /**
     * 记录配置摘要
     */
    private void logConfigurationSummary() {
        log.info("=== Configuration Summary ===");
        log.info("JWT Issuer: {}", jwtProperties.getIssuer());
        log.info("JWT Expiration: {} ms ({} hours)", 
            jwtProperties.getExpiration(), 
            jwtProperties.getExpiration() / 3600000.0);
        log.info("JWT Refresh Expiration: {} ms ({} hours)", 
            jwtProperties.getRefreshExpiration(), 
            jwtProperties.getRefreshExpiration() / 3600000.0);
        log.info("Authentication Enabled: {}", securityProperties.getAuthentication().getEnabled());
        log.info("Max Login Attempts: {}", securityProperties.getAuthentication().getMaxLoginAttempts());
        log.info("CORS Enabled: {}", securityProperties.getCors().getEnabled());
        log.info("Audit Logging Enabled: {}", securityProperties.getAudit().getEnabled());
        
        if (securityProperties.getEndpoints().getPublicEndpoints() != null) {
            log.info("Public Endpoints: {}", securityProperties.getEndpoints().getPublicEndpoints().size());
        }
        if (securityProperties.getEndpoints().getProtectedEndpoints() != null) {
            log.info("Protected Endpoints: {}", securityProperties.getEndpoints().getProtectedEndpoints().size());
        }
        
        log.info("=== End Configuration Summary ===");
    }
    
    /**
     * 配置验证异常
     */
    public static class ConfigurationValidationException extends RuntimeException {
        public ConfigurationValidationException(String message) {
            super(message);
        }
        
        public ConfigurationValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}