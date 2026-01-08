package com.trading.service.impl;

import com.trading.config.JwtProperties;
import com.trading.dto.request.LoginRequest;
import com.trading.dto.response.AuthResponse;
import com.trading.dto.response.MerchantResponse;
import com.trading.entity.Merchant;
import com.trading.exception.BusinessException;
import com.trading.exception.InvalidTokenException;
import com.trading.exception.UnauthorizedAccessException;
import com.trading.repository.MerchantRepository;
import com.trading.security.JwtUtil;
import com.trading.security.SecurityContextUtil;
import com.trading.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 认证服务实现类
 * 提供商家身份验证和授权功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    
    private final MerchantRepository merchantRepository;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.debug("处理用户名登录请求: {}", request.getUsername());
        
        // 查找商家
        Merchant merchant = merchantRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("尝试使用不存在的用户名登录: {}", request.getUsername());
                    return new BusinessException(401, "Invalid credentials");
                });
        
        // 验证密码 - 使用BCrypt密码编码器进行验证
        if (!passwordEncoder.matches(request.getPassword(), merchant.getPassword())) {
            log.warn("用户名密码错误的登录尝试: {}", request.getUsername());
            throw new BusinessException(401, "Invalid credentials");
        }
        
        // 生成JWT令牌（包含角色信息）
        String token = jwtUtil.generateToken(merchant.getId(), merchant.getUsername(), merchant.getRole());
        
        // 构建响应
        AuthResponse response = AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpiration() / 1000) // 转换为秒
                .merchant(MerchantResponse.fromEntity(merchant))
                .issuedAt(LocalDateTime.now())
                .build();
        
        log.info("商家登录成功: id={}, username={}", 
                merchant.getId(), merchant.getUsername());
        
        return response;
    }
    
    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        log.debug("处理刷新令牌请求");
        
        try {
            // 验证刷新令牌
            if (!jwtUtil.validateToken(refreshToken)) {
                throw new InvalidTokenException("Invalid refresh token");
            }
            
            // 从令牌中提取商家信息
            Long merchantId = jwtUtil.extractMerchantId(refreshToken);
            String username = jwtUtil.extractUsername(refreshToken);
            
            // 查找商家确保仍然存在
            Merchant merchant = merchantRepository.findById(merchantId)
                    .orElseThrow(() -> new BusinessException(401, "Merchant not found"));
            
            // 生成新的JWT令牌（包含角色信息）
            String newToken = jwtUtil.generateToken(merchant.getId(), merchant.getUsername(), merchant.getRole());
            
            // 构建响应
            AuthResponse response = AuthResponse.builder()
                    .accessToken(newToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtProperties.getExpiration() / 1000)
                    .merchant(MerchantResponse.fromEntity(merchant))
                    .issuedAt(LocalDateTime.now())
                    .build();
            
            log.info("令牌刷新成功，商家: id={}, username={}", 
                    merchant.getId(), merchant.getUsername());
            
            return response;
            
        } catch (Exception e) {
            log.warn("刷新令牌失败: {}", e.getMessage());
            throw new InvalidTokenException("Failed to refresh token");
        }
    }
    
    @Override
    public boolean validateToken(String token) {
        try {
            return jwtUtil.validateToken(token);
        } catch (Exception e) {
            log.debug("令牌验证失败: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public MerchantResponse getCurrentMerchant() {
        Long merchantId = getCurrentMerchantId();
        if (merchantId == null) {
            throw new UnauthorizedAccessException("No authenticated merchant found");
        }
        
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(404, "Merchant not found"));
        
        return MerchantResponse.fromEntity(merchant);
    }
    
    @Override
    public Long getCurrentMerchantId() {
        return SecurityContextUtil.getCurrentMerchantId();
    }
    
    @Override
    public boolean isCurrentMerchant(Long merchantId) {
        return SecurityContextUtil.isCurrentMerchant(merchantId);
    }
    
    @Override
    public void validateMerchantAccess(Long merchantId) {
        SecurityContextUtil.validateMerchantAccess(merchantId);
    }
}