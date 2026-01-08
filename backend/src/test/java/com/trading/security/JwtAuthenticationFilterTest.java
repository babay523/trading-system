package com.trading.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.config.JwtProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * JWT认证过滤器测试
 * 测试JWT令牌提取、验证和安全上下文设置功能
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    
    @Mock
    private JwtUtil jwtUtil;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;
    
    private ObjectMapper objectMapper;
    private JwtAuthenticationFilter filter;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // This will register JSR310 module for LocalDateTime
        filter = new JwtAuthenticationFilter(jwtUtil, objectMapper);
        SecurityContextHolder.clearContext();
    }
    
    @Test
    void shouldSkipAuthenticationForPublicEndpoints() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/merchants/login");
        
        // When
        filter.doFilterInternal(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtil);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
    
    @Test
    void shouldReturnUnauthorizedWhenTokenIsMissing() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/merchants/123/balance");
        when(request.getHeader("Authorization")).thenReturn(null);
        
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);
        
        // When
        filter.doFilterInternal(request, response, filterChain);
        
        // Then
        verify(response).setStatus(401);
        verify(response).setContentType("application/json");
        verifyNoInteractions(filterChain);
        assertTrue(stringWriter.toString().contains("Authentication token is required"));
    }
    
    @Test
    void shouldReturnUnauthorizedWhenTokenIsInvalid() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/merchants/123/balance");
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(jwtUtil.validateToken("invalid-token")).thenThrow(new com.trading.exception.InvalidTokenException("Invalid token"));
        
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);
        
        // When
        filter.doFilterInternal(request, response, filterChain);
        
        // Then
        verify(response).setStatus(401);
        verify(response).setContentType("application/json");
        verifyNoInteractions(filterChain);
        assertTrue(stringWriter.toString().contains("Invalid authentication token"));
    }
    
    @Test
    void shouldSetAuthenticationContextWhenTokenIsValid() throws Exception {
        // Given
        String validToken = "valid-jwt-token";
        when(request.getRequestURI()).thenReturn("/api/v1/merchants/123/balance");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.extractUsername(validToken)).thenReturn("merchant123");
        when(jwtUtil.extractMerchantId(validToken)).thenReturn(123L);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getSession(false)).thenReturn(null);
        
        // When
        filter.doFilterInternal(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
    }
    
    @Test
    void shouldExtractTokenFromAuthorizationHeader() throws Exception {
        // Given
        String token = "test-token";
        when(request.getRequestURI()).thenReturn("/api/v1/merchants/123/balance");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.extractUsername(token)).thenReturn("merchant123");
        when(jwtUtil.extractMerchantId(token)).thenReturn(123L);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getSession(false)).thenReturn(null);
        
        // When
        filter.doFilterInternal(request, response, filterChain);
        
        // Then
        verify(jwtUtil).validateToken(token);
        verify(jwtUtil, times(2)).extractUsername(token); // Called once in setAuthenticationContext and once in logSuccessfulAuthentication
        verify(jwtUtil, times(2)).extractMerchantId(token); // Called once in setAuthenticationContext and once in logSuccessfulAuthentication
    }
    
    @Test
    void shouldHandleTokenExpiredException() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/merchants/123/balance");
        when(request.getHeader("Authorization")).thenReturn("Bearer expired-token");
        when(jwtUtil.validateToken("expired-token")).thenThrow(new com.trading.exception.TokenExpiredException());
        
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);
        
        // When
        filter.doFilterInternal(request, response, filterChain);
        
        // Then
        verify(response).setStatus(401);
        verify(response).setContentType("application/json");
        verifyNoInteractions(filterChain);
        assertTrue(stringWriter.toString().contains("Token has expired"));
    }
}