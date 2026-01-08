package com.trading.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.dto.response.ApiResponse;
import com.trading.exception.InvalidTokenException;
import com.trading.exception.MissingTokenException;
import com.trading.exception.TokenExpiredException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * JWT认证过滤器
 * 继承OncePerRequestFilter确保每个请求只执行一次过滤
 * 负责从请求中提取JWT令牌，验证令牌，并设置安全上下文
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    
    // 不需要认证的公开端点
    private static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(
        "/api/v1/merchants/register",
        "/api/v1/merchants/login",
        "/api/v1/auth/login",
        "/api/v1/auth/refresh",
        "/api/v1/auth/validate",
        "/api/v1/auth/logout",
        "/api/v1/products",
        "/api/v1/users/register",
        "/api/v1/users/login",
        "/h2-console",
        "/actuator/health",
        "/error"
    );
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // 检查是否为公开端点
            if (shouldSkipAuthentication(request.getRequestURI())) {
                log.debug("Skipping authentication for public endpoint: {}", request.getRequestURI());
                
                // 对于公开端点，尝试提取并验证token（如果存在），但不强制要求
                String token = extractTokenFromRequest(request);
                if (token != null) {
                    try {
                        // 如果token存在且有效，设置认证上下文
                        if (jwtUtil.validateToken(token)) {
                            setAuthenticationContext(token, request);
                            logSuccessfulAuthentication(request, jwtUtil.extractUsername(token), jwtUtil.extractMerchantId(token));
                        }
                    } catch (Exception e) {
                        // 对于公开端点，token验证失败不影响访问，只记录日志
                        log.debug("Token validation failed for public endpoint, continuing without authentication: {}", e.getMessage());
                    }
                }
                
                // 无论token是否有效，都允许访问公开端点
                filterChain.doFilter(request, response);
                return;
            }
            
            // 对于非公开端点，必须有有效的token
            String token = extractTokenFromRequest(request);
            
            // 如果没有令牌，抛出缺失令牌异常
            if (token == null) {
                throw new MissingTokenException();
            }
            
            // 验证令牌并设置认证上下文
            if (jwtUtil.validateToken(token)) {
                setAuthenticationContext(token, request);
                // 记录成功的认证事件
                logSuccessfulAuthentication(request, jwtUtil.extractUsername(token), jwtUtil.extractMerchantId(token));
            }
            
            // 继续过滤器链
            filterChain.doFilter(request, response);
            
        } catch (MissingTokenException | InvalidTokenException | TokenExpiredException e) {
            // 记录详细的认证失败信息
            logAuthenticationFailure(request, e);
            handleAuthenticationFailure(response, e);
        } catch (Exception e) {
            log.error("Unexpected error during authentication for request {}: {}", request.getRequestURI(), e.getMessage(), e);
            // 记录意外的认证错误
            logAuthenticationFailure(request, new InvalidTokenException("Authentication failed"));
            handleAuthenticationFailure(response, new InvalidTokenException("Authentication failed"));
        }
    }
    
    /**
     * 从请求的Authorization header中提取JWT令牌
     * 
     * @param request HTTP请求
     * @return JWT令牌字符串，如果不存在则返回null
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            log.debug("Extracted JWT token from Authorization header");
            return token;
        }
        
        log.debug("No valid Authorization header found in request");
        return null;
    }
    
    /**
     * 设置Spring Security认证上下文
     * 
     * @param token JWT令牌
     * @param request HTTP请求
     */
    private void setAuthenticationContext(String token, HttpServletRequest request) {
        try {
            // 从令牌中提取用户信息
            String username = jwtUtil.extractUsername(token);
            Long merchantId = jwtUtil.extractMerchantId(token);
            String role = jwtUtil.extractRole(token);
            
            log.debug("Setting authentication context for user: {} (ID: {}, Role: {})", username, merchantId, role);
            
            // 创建UserDetails对象
            UserDetails userDetails = User.builder()
                .username(username)
                .password("") // JWT认证不需要密码
                .authorities(new ArrayList<>()) // 暂时不设置权限，后续可扩展
                .build();
            
            // 创建认证令牌
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            
            // 设置请求详情
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            // 在认证令牌中添加用户ID信息，供后续使用（支持用户和商家）
            authToken.setDetails(new MerchantAuthenticationDetails(merchantId, username, role, request));
            
            // 设置到安全上下文
            SecurityContextHolder.getContext().setAuthentication(authToken);
            
            log.debug("Successfully set authentication context for user: {}", username);
            
        } catch (Exception e) {
            log.error("Failed to set authentication context: {}", e.getMessage(), e);
            throw new InvalidTokenException("Failed to process authentication token");
        }
    }
    
    /**
     * 检查请求路径是否应该跳过认证
     * 
     * @param requestPath 请求路径
     * @return 是否跳过认证
     */
    private boolean shouldSkipAuthentication(String requestPath) {
        return PUBLIC_ENDPOINTS.stream()
            .anyMatch(endpoint -> requestPath.startsWith(endpoint));
    }
    
    /**
     * 处理认证失败情况
     * 返回适当的HTTP状态码和错误消息
     * 
     * @param response HTTP响应
     * @param exception 认证异常
     * @throws IOException IO异常
     */
    private void handleAuthenticationFailure(HttpServletResponse response, Exception exception) throws IOException {
        // 清除安全上下文
        SecurityContextHolder.clearContext();
        
        // 设置响应状态和内容类型
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        // 创建错误响应
        ApiResponse<Object> errorResponse;
        
        if (exception instanceof MissingTokenException) {
            errorResponse = ApiResponse.error(401, "Authentication token is required");
        } else if (exception instanceof TokenExpiredException) {
            errorResponse = ApiResponse.error(401, "Token has expired");
        } else if (exception instanceof InvalidTokenException) {
            errorResponse = ApiResponse.error(401, "Invalid authentication token");
        } else {
            errorResponse = ApiResponse.error(401, "Authentication failed");
        }
        
        // 写入响应
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
        
        log.debug("Sent authentication failure response: {}", errorResponse.getMessage());
    }
    
    /**
     * 记录认证失败事件
     * 包含详细的安全信息用于审计
     * 
     * @param request HTTP请求
     * @param exception 认证异常
     */
    private void logAuthenticationFailure(HttpServletRequest request, Exception exception) {
        org.slf4j.Logger securityLogger = org.slf4j.LoggerFactory.getLogger("SECURITY");
        
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        String eventType = "AUTHENTICATION_FAILURE";
        
        if (exception instanceof MissingTokenException) {
            eventType = "MISSING_TOKEN";
        } else if (exception instanceof TokenExpiredException) {
            eventType = "EXPIRED_TOKEN";
        } else if (exception instanceof InvalidTokenException) {
            eventType = "INVALID_TOKEN";
        }
        
        securityLogger.warn("Security Event: {} | Method: {} | URI: {} | IP: {} | Details: {} | UserAgent: {} | Timestamp: {}", 
            eventType, method, requestUri, clientIp, exception.getMessage(), userAgent, java.time.LocalDateTime.now());
    }
    
    /**
     * 获取客户端真实IP地址
     * 考虑代理和负载均衡器的情况
     * 
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * 记录成功的认证事件
     * 
     * @param request HTTP请求
     * @param username 用户名
     * @param merchantId 商家ID
     */
    private void logSuccessfulAuthentication(HttpServletRequest request, String username, Long merchantId) {
        org.slf4j.Logger securityLogger = org.slf4j.LoggerFactory.getLogger("SECURITY");
        
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        
        securityLogger.info("Security Event: AUTHENTICATION_SUCCESS | Method: {} | URI: {} | Merchant: {} ({}) | IP: {} | UserAgent: {} | Timestamp: {}", 
            method, requestUri, username, merchantId, clientIp, userAgent, java.time.LocalDateTime.now());
    }
    
    /**
     * 商家认证详情类
     * 扩展WebAuthenticationDetails以包含商家特定信息
     */
    public static class MerchantAuthenticationDetails extends WebAuthenticationDetailsSource {
        private final Long merchantId;
        private final String username;
        private final String role;
        private final String remoteAddress;
        private final String sessionId;
        
        public MerchantAuthenticationDetails(Long merchantId, String username, String role, HttpServletRequest request) {
            this.merchantId = merchantId;
            this.username = username;
            this.role = role != null ? role : "MERCHANT";
            this.remoteAddress = request.getRemoteAddr();
            this.sessionId = request.getSession(false) != null ? request.getSession().getId() : null;
        }
        
        public Long getMerchantId() {
            return merchantId;
        }
        
        public String getUsername() {
            return username;
        }
        
        public String getRole() {
            return role;
        }
        
        public boolean isAdmin() {
            return "ADMIN".equals(role);
        }
        
        public String getRemoteAddress() {
            return remoteAddress;
        }
        
        public String getSessionId() {
            return sessionId;
        }
        
        @Override
        public String toString() {
            return "MerchantAuthenticationDetails{" +
                "merchantId=" + merchantId +
                ", username='" + username + '\'' +
                ", role='" + role + '\'' +
                ", remoteAddress='" + remoteAddress + '\'' +
                ", sessionId='" + sessionId + '\'' +
                '}';
        }
    }
}