package com.trading.controller;

import com.trading.dto.request.LoginRequest;
import com.trading.dto.request.RefreshTokenRequest;
import com.trading.dto.response.ApiResponse;
import com.trading.dto.response.AuthResponse;
import com.trading.dto.response.MerchantResponse;
import com.trading.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 提供商家身份验证相关的API端点
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthenticationService authenticationService;
    
    /**
     * 商家登录
     * POST /api/v1/auth/login
     * 
     * @param request 登录请求
     * @return 认证响应，包含JWT令牌和商家信息
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        log.debug("Processing login request for username: {}", request.getUsername());
        
        AuthResponse authResponse = authenticationService.login(request);
        
        log.info("Login successful for username: {}", request.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }
    
    /**
     * 刷新JWT令牌
     * POST /api/v1/auth/refresh
     * 
     * @param request 刷新令牌请求
     * @return 新的认证响应
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.debug("Processing refresh token request");
        
        AuthResponse authResponse = authenticationService.refreshToken(request.getRefreshToken());
        
        log.info("Token refresh successful");
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", authResponse));
    }
    
    /**
     * 登出
     * POST /api/v1/auth/logout
     * 
     * 注意：由于JWT是无状态的，登出主要是客户端删除令牌
     * 服务端记录登出事件用于审计
     * 
     * @param request HTTP请求，用于获取令牌信息
     * @return 登出确认响应
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        log.debug("Processing logout request");
        
        // 从请求头中提取令牌信息用于日志记录
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // 记录登出事件（用于审计）
                Long merchantId = authenticationService.getCurrentMerchantId();
                log.info("Merchant logged out: id={}", merchantId);
            } catch (Exception e) {
                log.debug("Could not extract merchant info during logout: {}", e.getMessage());
            }
        }
        
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }
    
    /**
     * 验证JWT令牌
     * GET /api/v1/auth/validate
     * 
     * @param request HTTP请求，用于获取Authorization header中的令牌
     * @return 令牌验证结果
     */
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateToken(HttpServletRequest request) {
        log.debug("Processing token validation request");
        
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No valid Authorization header found");
            return ResponseEntity.ok(ApiResponse.success("Token validation result", false));
        }
        
        String token = authHeader.substring(7);
        boolean isValid = authenticationService.validateToken(token);
        
        log.debug("Token validation result: {}", isValid);
        return ResponseEntity.ok(ApiResponse.success("Token validation result", isValid));
    }
    
    /**
     * 获取当前认证的商家信息
     * GET /api/v1/auth/current
     * 
     * 需要有效的JWT令牌
     * 
     * @return 当前商家信息
     */
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<MerchantResponse>> getCurrentMerchant() {
        log.debug("Processing get current merchant request");
        
        MerchantResponse merchant = authenticationService.getCurrentMerchant();
        
        log.debug("Retrieved current merchant: id={}, username={}", 
                merchant.getId(), merchant.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Current merchant information", merchant));
    }
}