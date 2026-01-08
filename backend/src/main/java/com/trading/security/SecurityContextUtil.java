package com.trading.security;

import com.trading.exception.UnauthorizedAccessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 安全上下文工具类
 * 提供从Spring Security上下文中提取当前认证用户信息的便捷方法
 */
@Slf4j
public class SecurityContextUtil {
    
    /**
     * 获取当前认证的商家ID
     * 
     * @return 当前商家ID
     * @throws UnauthorizedAccessException 如果用户未认证或无法获取商家ID
     */
    public static Long getCurrentMerchantId() {
        Authentication authentication = getCurrentAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedAccessException("User not authenticated");
        }
        
        Object details = authentication.getDetails();
        if (details instanceof JwtAuthenticationFilter.MerchantAuthenticationDetails) {
            JwtAuthenticationFilter.MerchantAuthenticationDetails merchantDetails = 
                (JwtAuthenticationFilter.MerchantAuthenticationDetails) details;
            return merchantDetails.getMerchantId();
        }
        
        throw new UnauthorizedAccessException("Merchant information not available in security context");
    }
    
    /**
     * 获取当前认证的用户ID（如果是普通用户，返回正数；如果是商家，返回商家ID）
     * 
     * @return 当前用户ID
     * @throws UnauthorizedAccessException 如果用户未认证
     */
    public static Long getCurrentUserId() {
        Authentication authentication = getCurrentAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedAccessException("User not authenticated");
        }
        
        Object details = authentication.getDetails();
        if (details instanceof JwtAuthenticationFilter.MerchantAuthenticationDetails) {
            JwtAuthenticationFilter.MerchantAuthenticationDetails authDetails = 
                (JwtAuthenticationFilter.MerchantAuthenticationDetails) details;
            Long id = authDetails.getMerchantId();
            // 如果是负数，说明是普通用户，返回正数
            return id < 0 ? -id : id;
        }
        
        throw new UnauthorizedAccessException("User information not available in security context");
    }
    
    /**
     * 检查当前用户是否是普通用户（非商家）
     * 
     * @return 是否是普通用户
     */
    public static boolean isUser() {
        try {
            Authentication authentication = getCurrentAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return false;
            }
            
            Object details = authentication.getDetails();
            if (details instanceof JwtAuthenticationFilter.MerchantAuthenticationDetails) {
                JwtAuthenticationFilter.MerchantAuthenticationDetails authDetails = 
                    (JwtAuthenticationFilter.MerchantAuthenticationDetails) details;
                return "USER".equals(authDetails.getRole());
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取当前认证的用户名
     * 
     * @return 当前用户名
     * @throws UnauthorizedAccessException 如果用户未认证
     */
    public static String getCurrentUsername() {
        Authentication authentication = getCurrentAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedAccessException("User not authenticated");
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            return (String) principal;
        }
        
        throw new UnauthorizedAccessException("Username not available in security context");
    }
    
    /**
     * 检查当前用户是否已认证
     * 
     * @return 是否已认证
     */
    public static boolean isAuthenticated() {
        Authentication authentication = getCurrentAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
    
    /**
     * 获取当前认证对象
     * 
     * @return 当前认证对象，如果未认证则返回null
     */
    public static Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
    
    /**
     * 验证当前认证的商家是否与指定的商家ID匹配
     * 
     * @param merchantId 要验证的商家ID
     * @return 是否匹配
     */
    public static boolean isCurrentMerchant(Long merchantId) {
        try {
            // 管理员可以访问所有商家资源
            if (isAdmin()) {
                return true;
            }
            Long currentMerchantId = getCurrentMerchantId();
            return currentMerchantId != null && currentMerchantId.equals(merchantId);
        } catch (UnauthorizedAccessException e) {
            log.debug("Unable to verify merchant identity: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查当前用户是否是管理员
     * 
     * @return 是否是管理员
     */
    public static boolean isAdmin() {
        Authentication authentication = getCurrentAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        Object details = authentication.getDetails();
        if (details instanceof JwtAuthenticationFilter.MerchantAuthenticationDetails) {
            JwtAuthenticationFilter.MerchantAuthenticationDetails merchantDetails = 
                (JwtAuthenticationFilter.MerchantAuthenticationDetails) details;
            return "ADMIN".equals(merchantDetails.getRole());
        }
        
        return false;
    }
    
    /**
     * 获取当前用户角色
     * 
     * @return 角色字符串
     */
    public static String getCurrentRole() {
        Authentication authentication = getCurrentAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        Object details = authentication.getDetails();
        if (details instanceof JwtAuthenticationFilter.MerchantAuthenticationDetails) {
            JwtAuthenticationFilter.MerchantAuthenticationDetails merchantDetails = 
                (JwtAuthenticationFilter.MerchantAuthenticationDetails) details;
            return merchantDetails.getRole();
        }
        
        return "MERCHANT";
    }
    
    /**
     * 验证当前认证的商家是否有权访问指定的商家资源
     * 如果没有权限，抛出异常
     * 
     * @param merchantId 要访问的商家ID
     * @throws UnauthorizedAccessException 如果没有权限访问
     */
    public static void validateMerchantAccess(Long merchantId) {
        if (!isCurrentMerchant(merchantId)) {
            Long currentMerchantId = getCurrentMerchantId();
            log.warn("Merchant {} attempted to access resources of merchant {}", currentMerchantId, merchantId);
            throw new UnauthorizedAccessException("Access denied to merchant resources");
        }
    }
    
    /**
     * 清除当前安全上下文
     */
    public static void clearContext() {
        SecurityContextHolder.clearContext();
        log.debug("Security context cleared");
    }
}