package com.trading.security;

import com.trading.config.JwtProperties;
import com.trading.exception.InvalidTokenException;
import com.trading.exception.TokenExpiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil单元测试
 */
@SpringBootTest
@ActiveProfiles("test")
class JwtUtilTest {
    
    private JwtUtil jwtUtil;
    private JwtProperties jwtProperties;
    
    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecretKey("test-secret-key-must-be-at-least-32-characters-long");
        jwtProperties.setExpiration(3600000L); // 1 hour
        jwtProperties.setIssuer("test-trading-system");
        
        jwtUtil = new JwtUtil(jwtProperties);
    }
    
    @Test
    void testGenerateToken() {
        // Given
        Long merchantId = 123L;
        String username = "testmerchant";
        
        // When
        String token = jwtUtil.generateToken(merchantId, username);
        
        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains("."));
    }
    
    @Test
    void testValidateValidToken() {
        // Given
        Long merchantId = 123L;
        String username = "testmerchant";
        String token = jwtUtil.generateToken(merchantId, username);
        
        // When & Then
        assertTrue(jwtUtil.validateToken(token));
    }
    
    @Test
    void testValidateInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";
        
        // When & Then
        assertThrows(InvalidTokenException.class, () -> jwtUtil.validateToken(invalidToken));
    }
    
    @Test
    void testExtractMerchantId() {
        // Given
        Long merchantId = 123L;
        String username = "testmerchant";
        String token = jwtUtil.generateToken(merchantId, username);
        
        // When
        Long extractedMerchantId = jwtUtil.extractMerchantId(token);
        
        // Then
        assertEquals(merchantId, extractedMerchantId);
    }
    
    @Test
    void testExtractUsername() {
        // Given
        Long merchantId = 123L;
        String username = "testmerchant";
        String token = jwtUtil.generateToken(merchantId, username);
        
        // When
        String extractedUsername = jwtUtil.extractUsername(token);
        
        // Then
        assertEquals(username, extractedUsername);
    }
    
    @Test
    void testExtractExpiration() {
        // Given
        Long merchantId = 123L;
        String username = "testmerchant";
        String token = jwtUtil.generateToken(merchantId, username);
        
        // When
        Date expiration = jwtUtil.extractExpiration(token);
        
        // Then
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }
    
    @Test
    void testIsTokenExpired() {
        // Given
        Long merchantId = 123L;
        String username = "testmerchant";
        String token = jwtUtil.generateToken(merchantId, username);
        
        // When & Then
        assertFalse(jwtUtil.isTokenExpired(token));
    }
    
    @Test
    void testTokenWithExpiredTime() {
        // Given - Create a token with very short expiration
        jwtProperties.setExpiration(1L); // 1 millisecond
        JwtUtil shortExpirationJwtUtil = new JwtUtil(jwtProperties);
        
        Long merchantId = 123L;
        String username = "testmerchant";
        String token = shortExpirationJwtUtil.generateToken(merchantId, username);
        
        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // When & Then
        assertTrue(shortExpirationJwtUtil.isTokenExpired(token));
        assertThrows(TokenExpiredException.class, () -> shortExpirationJwtUtil.validateToken(token));
    }
}