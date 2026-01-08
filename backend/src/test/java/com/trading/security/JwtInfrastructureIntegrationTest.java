package com.trading.security;

import com.trading.config.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT基础设施集成测试
 * 验证JWT工具类、配置属性和认证过滤器的集成工作
 */
@SpringBootTest
@ActiveProfiles("test")
class JwtInfrastructureIntegrationTest {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private JwtProperties jwtProperties;
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Test
    void contextLoads() {
        // 验证所有JWT相关组件都能正确加载
        assertNotNull(jwtUtil, "JwtUtil should be loaded");
        assertNotNull(jwtProperties, "JwtProperties should be loaded");
        assertNotNull(jwtAuthenticationFilter, "JwtAuthenticationFilter should be loaded");
    }
    
    @Test
    void jwtPropertiesAreConfiguredCorrectly() {
        // 验证JWT配置属性正确加载
        assertNotNull(jwtProperties.getSecretKey(), "Secret key should be configured");
        assertTrue(jwtProperties.getSecretKey().length() >= 32, "Secret key should be at least 32 characters");
        assertTrue(jwtProperties.getExpiration() > 0, "Expiration should be positive");
        assertNotNull(jwtProperties.getIssuer(), "Issuer should be configured");
        assertEquals("Authorization", jwtProperties.getHeader(), "Header should be Authorization");
        assertEquals("Bearer ", jwtProperties.getPrefix(), "Prefix should be 'Bearer '");
    }
    
    @Test
    void jwtTokenGenerationAndValidationWorksTogether() {
        // 测试JWT令牌生成和验证的完整流程
        Long merchantId = 123L;
        String username = "testmerchant";
        
        // 生成令牌
        String token = jwtUtil.generateToken(merchantId, username);
        assertNotNull(token, "Token should be generated");
        assertFalse(token.isEmpty(), "Token should not be empty");
        
        // 验证令牌
        assertTrue(jwtUtil.validateToken(token), "Token should be valid");
        
        // 提取信息
        assertEquals(merchantId, jwtUtil.extractMerchantId(token), "Merchant ID should match");
        assertEquals(username, jwtUtil.extractUsername(token), "Username should match");
        assertNotNull(jwtUtil.extractExpiration(token), "Expiration should be present");
        assertFalse(jwtUtil.isTokenExpired(token), "Token should not be expired");
    }
    
    @Test
    void jwtTokenContainsCorrectClaims() {
        // 验证JWT令牌包含正确的声明
        Long merchantId = 456L;
        String username = "merchant456";
        
        String token = jwtUtil.generateToken(merchantId, username);
        
        // 验证令牌结构和内容
        String[] tokenParts = token.split("\\.");
        assertEquals(3, tokenParts.length, "JWT should have 3 parts (header.payload.signature)");
        
        // 验证提取的信息
        assertEquals(merchantId, jwtUtil.extractMerchantId(token));
        assertEquals(username, jwtUtil.extractUsername(token));
        
        // 验证令牌未过期
        assertFalse(jwtUtil.isTokenExpired(token));
    }
    
    @Test
    void invalidTokensAreRejected() {
        // 测试各种无效令牌的处理
        
        // 测试格式错误的令牌
        assertThrows(Exception.class, () -> jwtUtil.validateToken("invalid.token.format"));
        
        // 测试空令牌
        assertThrows(Exception.class, () -> jwtUtil.validateToken(""));
        
        // 测试null令牌
        assertThrows(Exception.class, () -> jwtUtil.validateToken(null));
        
        // 测试篡改的令牌
        String validToken = jwtUtil.generateToken(123L, "test");
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "XXXXX";
        assertThrows(Exception.class, () -> jwtUtil.validateToken(tamperedToken));
    }
}