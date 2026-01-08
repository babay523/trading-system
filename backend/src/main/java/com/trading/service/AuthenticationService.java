package com.trading.service;

import com.trading.dto.request.LoginRequest;
import com.trading.dto.response.AuthResponse;
import com.trading.dto.response.MerchantResponse;

/**
 * 认证服务接口
 * 提供商家身份验证和授权相关功能
 */
public interface AuthenticationService {
    
    /**
     * 商家登录认证
     * 验证凭据并生成JWT令牌
     * 
     * @param request 登录请求
     * @return 认证响应，包含JWT令牌和商家信息
     */
    AuthResponse login(LoginRequest request);
    
    /**
     * 刷新JWT令牌
     * 
     * @param refreshToken 刷新令牌
     * @return 新的认证响应
     */
    AuthResponse refreshToken(String refreshToken);
    
    /**
     * 验证JWT令牌
     * 
     * @param token JWT令牌
     * @return 令牌是否有效
     */
    boolean validateToken(String token);
    
    /**
     * 获取当前认证的商家信息
     * 
     * @return 当前商家信息
     */
    MerchantResponse getCurrentMerchant();
    
    /**
     * 获取当前认证的商家ID
     * 
     * @return 当前商家ID
     */
    Long getCurrentMerchantId();
    
    /**
     * 检查指定商家ID是否为当前认证的商家
     * 
     * @param merchantId 要检查的商家ID
     * @return 是否为当前商家
     */
    boolean isCurrentMerchant(Long merchantId);
    
    /**
     * 验证当前商家是否有权访问指定商家的资源
     * 如果没有权限，抛出异常
     * 
     * @param merchantId 要访问的商家ID
     */
    void validateMerchantAccess(Long merchantId);
}