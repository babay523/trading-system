package com.trading.config;

import com.trading.security.JwtAuthenticationEntryPoint;
import com.trading.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security配置类
 * 配置JWT认证、CORS、端点访问控制等安全设置
 * 
 * Requirements: 4.6, 8.2, 1.4
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final SecurityProperties securityProperties;
    
    /**
     * 配置安全过滤器链
     * 定义公开和受保护的端点，添加JWT认证过滤器
     * 
     * @param http HttpSecurity配置对象
     * @return SecurityFilterChain
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("Configuring security filter chain...");
        
        // 调试：打印SecurityProperties的配置
        log.debug("SecurityProperties - Authentication enabled: {}", securityProperties.getAuthentication().getEnabled());
        log.debug("SecurityProperties - CORS enabled: {}", securityProperties.getCors().getEnabled());
        log.debug("SecurityProperties - Public endpoints: {}", securityProperties.getEndpoints().getPublicEndpoints());
        log.debug("SecurityProperties - Protected endpoints: {}", securityProperties.getEndpoints().getProtectedEndpoints());
        
        http
            // 禁用CSRF，因为使用JWT令牌
            .csrf(AbstractHttpConfigurer::disable)
            
            // 配置CORS
            .cors(cors -> {
                if (securityProperties.getCors().getEnabled()) {
                    cors.configurationSource(corsConfigurationSource());
                    log.debug("CORS enabled with custom configuration");
                } else {
                    cors.disable();
                    log.debug("CORS disabled");
                }
            })
            
            // 配置会话管理为无状态
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 配置异常处理
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            )
            
            // 配置安全头
            .headers(headers -> {
                configureSecurityHeaders(headers);
            })
            
            // 配置请求授权
            .authorizeHttpRequests(authz -> {
                configureEndpointSecurity(authz);
            })
            
            // 添加JWT认证过滤器（如果认证启用）
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        log.info("Security filter chain configured successfully");
        return http.build();
    }
    
    /**
     * 配置安全头
     */
    private void configureSecurityHeaders(org.springframework.security.config.annotation.web.configurers.HeadersConfigurer<HttpSecurity> headers) {
        SecurityProperties.HeadersConfig headersConfig = securityProperties.getHeaders();
        
        // Frame Options
        if ("DENY".equalsIgnoreCase(headersConfig.getFrameOptions())) {
            headers.frameOptions().deny();
        } else if ("SAMEORIGIN".equalsIgnoreCase(headersConfig.getFrameOptions())) {
            headers.frameOptions().sameOrigin();
        }
        
        // Content Type Options
        if ("nosniff".equalsIgnoreCase(headersConfig.getContentTypeOptions())) {
            headers.contentTypeOptions();
        }
        
        // Referrer Policy
        if (headersConfig.getReferrerPolicy() != null) {
            try {
                ReferrerPolicyHeaderWriter.ReferrerPolicy policy = 
                    ReferrerPolicyHeaderWriter.ReferrerPolicy.valueOf(
                        headersConfig.getReferrerPolicy().toUpperCase().replace("-", "_")
                    );
                headers.referrerPolicy(policy);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid referrer policy: {}", headersConfig.getReferrerPolicy());
            }
        }
        
        log.debug("Security headers configured");
    }
    
    /**
     * 配置端点安全
     */
    private void configureEndpointSecurity(org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz) {
        List<String> publicEndpoints = securityProperties.getEndpoints().getPublicEndpoints();
        List<String> protectedEndpoints = securityProperties.getEndpoints().getProtectedEndpoints();
        
        log.debug("Public endpoints from config: {}", publicEndpoints);
        log.debug("Protected endpoints from config: {}", protectedEndpoints);
        
        // 配置公开端点
        if (publicEndpoints != null && !publicEndpoints.isEmpty()) {
            String[] publicArray = publicEndpoints.toArray(new String[0]);
            authz.requestMatchers(publicArray).permitAll();
            log.info("Configured {} public endpoints: {}", publicEndpoints.size(), publicEndpoints);
        } else {
            // 默认公开端点
            authz.requestMatchers(
                "/api/v1/merchants/register",
                "/api/v1/merchants/login",
                "/api/v1/auth/login",
                "/api/v1/auth/refresh",
                "/api/v1/auth/validate",
                "/api/v1/auth/logout",
                "/api/v1/products/**",
                "/api/v1/users/**",
                "/api/v1/cart/**",
                "/api/v1/orders/{id}/**",
                "/api/v1/users/{userId}/orders/**",
                "/h2-console/**",
                "/actuator/health",
                "/error"
            ).permitAll();
            log.info("Using default public endpoints configuration");
        }
        
        // 配置受保护端点
        if (protectedEndpoints != null && !protectedEndpoints.isEmpty()) {
            for (String endpoint : protectedEndpoints) {
                authz.requestMatchers(endpoint).authenticated();
            }
            log.info("Configured {} protected endpoints: {}", protectedEndpoints.size(), protectedEndpoints);
        } else {
            // 默认受保护端点
            authz.requestMatchers(
                "/api/v1/merchants/{id}/**",
                "/api/v1/auth/current",
                "/api/v1/auth/logout",
                "/api/v1/auth/validate"
            ).authenticated();
            log.info("Using default protected endpoints configuration");
        }
        
        // 检查认证是否启用
        if (securityProperties.getAuthentication().getEnabled()) {
            authz.anyRequest().authenticated();
            log.debug("Authentication enabled - all other requests require authentication");
        } else {
            authz.anyRequest().permitAll();
            log.warn("Authentication disabled - all requests are permitted");
        }
    }
    
    /**
     * 配置认证管理器
     * 
     * @param config 认证配置
     * @return AuthenticationManager
     * @throws Exception 配置异常
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    /**
     * 配置密码编码器
     * 使用BCrypt算法进行密码加密
     * 
     * @return PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * 配置CORS设置
     * 允许前端应用跨域访问API
     * 
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        SecurityProperties.CorsConfig corsConfig = securityProperties.getCors();
        
        // 配置允许的源
        if (corsConfig.getAllowedOrigins() != null && !corsConfig.getAllowedOrigins().isEmpty()) {
            String[] origins = corsConfig.getAllowedOrigins().split(",");
            configuration.setAllowedOriginPatterns(Arrays.asList(origins));
            log.debug("CORS allowed origins: {}", Arrays.toString(origins));
        } else {
            // 默认允许的源
            configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:8080",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:8080"
            ));
            log.debug("Using default CORS allowed origins");
        }
        
        // 配置允许的HTTP方法
        if (corsConfig.getAllowedMethods() != null && !corsConfig.getAllowedMethods().isEmpty()) {
            String[] methods = corsConfig.getAllowedMethods().split(",");
            configuration.setAllowedMethods(Arrays.asList(methods));
            log.debug("CORS allowed methods: {}", Arrays.toString(methods));
        } else {
            configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
            ));
        }
        
        // 配置允许的请求头
        if (corsConfig.getAllowedHeaders() != null && !corsConfig.getAllowedHeaders().isEmpty()) {
            if ("*".equals(corsConfig.getAllowedHeaders().trim())) {
                configuration.setAllowedHeaders(Arrays.asList("*"));
            } else {
                String[] headers = corsConfig.getAllowedHeaders().split(",");
                configuration.setAllowedHeaders(Arrays.asList(headers));
            }
        } else {
            configuration.setAllowedHeaders(Arrays.asList("*"));
        }
        
        // 配置暴露的响应头
        if (corsConfig.getExposedHeaders() != null && !corsConfig.getExposedHeaders().isEmpty()) {
            String[] exposedHeaders = corsConfig.getExposedHeaders().split(",");
            configuration.setExposedHeaders(Arrays.asList(exposedHeaders));
        } else {
            configuration.setExposedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"
            ));
        }
        
        // 配置其他CORS设置
        configuration.setAllowCredentials(corsConfig.getAllowCredentials());
        configuration.setMaxAge(corsConfig.getMaxAge());
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        log.info("CORS configuration applied successfully");
        return source;
    }
}