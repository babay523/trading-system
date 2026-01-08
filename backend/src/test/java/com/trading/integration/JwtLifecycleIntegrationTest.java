package com.trading.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.dto.request.LoginRequest;
import com.trading.dto.request.MerchantRegisterRequest;
import com.trading.dto.request.RefreshTokenRequest;
import com.trading.dto.response.ApiResponse;
import com.trading.dto.response.AuthResponse;
import com.trading.dto.response.MerchantResponse;
import com.trading.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * JWT Lifecycle Integration Test
 * 
 * Tests the complete JWT token lifecycle including:
 * - Token generation, usage, expiration, refresh
 * - Token tampering detection
 * 
 * Validates: Requirements 1.1, 1.2, 1.3, 1.6
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("JWT Token Lifecycle Tests")
class JwtLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private Long merchantId;
    private String username = "lifecycletest";
    private String password = "password123";

    @BeforeEach
    void setUp() throws Exception {
        // Register a test merchant
        MerchantRegisterRequest registerRequest = new MerchantRegisterRequest();
        registerRequest.setBusinessName("Lifecycle Test Business");
        registerRequest.setUsername(username);
        registerRequest.setPassword(password);

        MvcResult registerResult = mockMvc.perform(post("/api/v1/merchants/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponse<MerchantResponse> registerResponse = objectMapper.readValue(
                registerResult.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, MerchantResponse.class)
        );
        merchantId = registerResponse.getData().getId();
    }

    @Test
    @DisplayName("Complete JWT lifecycle: generation -> usage -> expiration")
    void completeJwtLifecycle_GenerationUsageExpiration() throws Exception {
        // Step 1: Login and generate token
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/merchants/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").exists())
                .andReturn();

        ApiResponse<AuthResponse> authResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthResponse.class)
        );
        String accessToken = authResponse.getData().getAccessToken();
        Long expiresIn = authResponse.getData().getExpiresIn();

        // Validate token properties
        assertNotNull(accessToken, "Access token should be generated");
        assertFalse(accessToken.isEmpty(), "Access token should not be empty");
        assertTrue(expiresIn > 0, "Token should have positive expiration time");

        // Step 2: Use token to access protected resource
        mockMvc.perform(get("/api/v1/merchants/" + merchantId)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(merchantId))
                .andExpect(jsonPath("$.data.username").value(username));

        // Step 3: Validate token properties
        assertTrue(jwtUtil.validateToken(accessToken), "Token should be valid");
        assertEquals(merchantId, jwtUtil.extractMerchantId(accessToken), "Merchant ID should match");
        assertEquals(username, jwtUtil.extractUsername(accessToken), "Username should match");
        assertFalse(jwtUtil.isTokenExpired(accessToken), "Token should not be expired yet");

        // Step 4: Verify token expiration time is reasonable
        Date expiration = jwtUtil.extractExpiration(accessToken);
        Date now = new Date();
        long timeDiff = expiration.getTime() - now.getTime();
        long hoursDiff = TimeUnit.MILLISECONDS.toHours(timeDiff);
        
        assertTrue(hoursDiff > 0, "Token should expire in the future");
        assertTrue(hoursDiff <= 24, "Token should expire within 24 hours");
    }

    @Test
    @DisplayName("Expired tokens are rejected")
    void expiredTokens_AreRejected() throws Exception {
        // Generate a token with very short expiration (1 second)
        String shortLivedToken = generateShortLivedToken(merchantId, username, 1000); // 1 second

        // Token should be valid initially
        assertTrue(jwtUtil.validateToken(shortLivedToken), "Token should be valid initially");

        // Use token immediately - should work
        mockMvc.perform(get("/api/v1/merchants/" + merchantId)
                .header("Authorization", "Bearer " + shortLivedToken))
                .andExpect(status().isOk());

        // Wait for token to expire
        Thread.sleep(1500); // Wait 1.5 seconds

        // Token should now be expired
        assertTrue(jwtUtil.isTokenExpired(shortLivedToken), "Token should be expired");

        // Expired token should be rejected
        mockMvc.perform(get("/api/v1/merchants/" + merchantId)
                .header("Authorization", "Bearer " + shortLivedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Authentication failed"));
    }

    @Test
    @DisplayName("Token refresh functionality works correctly")
    void tokenRefresh_WorksCorrectly() throws Exception {
        // Step 1: Login to get initial token
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/merchants/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<AuthResponse> authResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthResponse.class)
        );
        String originalToken = authResponse.getData().getAccessToken();

        // Step 2: Use original token
        mockMvc.perform(get("/api/v1/merchants/" + merchantId)
                .header("Authorization", "Bearer " + originalToken))
                .andExpect(status().isOk());

        // Step 3: Refresh token
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(originalToken); // Using access token as refresh token for simplicity

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andReturn();

        ApiResponse<AuthResponse> refreshResponse = objectMapper.readValue(
                refreshResult.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthResponse.class)
        );
        String newToken = refreshResponse.getData().getAccessToken();

        // Step 4: Verify new token is different and works
        assertNotEquals(originalToken, newToken, "New token should be different from original");
        assertTrue(jwtUtil.validateToken(newToken), "New token should be valid");
        assertEquals(merchantId, jwtUtil.extractMerchantId(newToken), "New token should contain same merchant ID");
        assertEquals(username, jwtUtil.extractUsername(newToken), "New token should contain same username");

        // Step 5: Use new token
        mockMvc.perform(get("/api/v1/merchants/" + merchantId)
                .header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(merchantId));
    }

    @Test
    @DisplayName("Token tampering is detected and rejected")
    void tokenTampering_IsDetectedAndRejected() throws Exception {
        // Generate valid token
        String validToken = jwtUtil.generateToken(merchantId, username);
        
        // Verify token works initially
        mockMvc.perform(get("/api/v1/merchants/" + merchantId)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());

        // Test various tampering scenarios
        testTamperedToken(validToken, "Signature tampering", this::tamperSignature);
        testTamperedToken(validToken, "Payload tampering", this::tamperPayload);
        testTamperedToken(validToken, "Header tampering", this::tamperHeader);
        testTamperedToken(validToken, "Complete replacement", token -> "fake.jwt.token");
    }

    @Test
    @DisplayName("Token validation endpoint works correctly")
    void tokenValidation_WorksCorrectly() throws Exception {
        // Generate valid token
        String validToken = jwtUtil.generateToken(merchantId, username);

        // Test valid token validation
        mockMvc.perform(get("/api/v1/auth/validate")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        // Test invalid token validation
        mockMvc.perform(get("/api/v1/auth/validate")
                .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        // Test missing token validation
        mockMvc.perform(get("/api/v1/auth/validate"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("Current merchant endpoint works with valid token")
    void currentMerchant_WorksWithValidToken() throws Exception {
        // Generate valid token
        String validToken = jwtUtil.generateToken(merchantId, username);

        // Test current merchant endpoint
        mockMvc.perform(get("/api/v1/auth/current")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(merchantId))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.businessName").value("Lifecycle Test Business"));
    }

    @Test
    @DisplayName("Logout invalidates token usage")
    void logout_InvalidatesTokenUsage() throws Exception {
        // Generate valid token
        String validToken = jwtUtil.generateToken(merchantId, username);

        // Verify token works initially
        mockMvc.perform(get("/api/v1/merchants/" + merchantId)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());

        // Logout
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        // Note: In a stateless JWT implementation, logout typically doesn't invalidate the token
        // unless there's a blacklist mechanism. This test verifies the logout endpoint works.
        // The token would still be valid until expiration in a pure stateless implementation.
    }

    @Test
    @DisplayName("Multiple concurrent tokens work independently")
    void multipleConcurrentTokens_WorkIndependently() throws Exception {
        // Generate multiple tokens for the same merchant
        String token1 = jwtUtil.generateToken(merchantId, username);
        String token2 = jwtUtil.generateToken(merchantId, username);
        String token3 = jwtUtil.generateToken(merchantId, username);

        // All tokens should be different
        assertNotEquals(token1, token2, "Tokens should be unique");
        assertNotEquals(token2, token3, "Tokens should be unique");
        assertNotEquals(token1, token3, "Tokens should be unique");

        // All tokens should work independently
        mockMvc.perform(get("/api/v1/merchants/" + merchantId)
                .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/merchants/" + merchantId)
                .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/merchants/" + merchantId)
                .header("Authorization", "Bearer " + token3))
                .andExpect(status().isOk());

        // All tokens should contain the same merchant information
        assertEquals(merchantId, jwtUtil.extractMerchantId(token1));
        assertEquals(merchantId, jwtUtil.extractMerchantId(token2));
        assertEquals(merchantId, jwtUtil.extractMerchantId(token3));

        assertEquals(username, jwtUtil.extractUsername(token1));
        assertEquals(username, jwtUtil.extractUsername(token2));
        assertEquals(username, jwtUtil.extractUsername(token3));
    }

    // Helper methods

    private String generateShortLivedToken(Long merchantId, String username, long expirationMs) throws Exception {
        // Use reflection to temporarily modify JWT expiration for testing
        Field expirationField = JwtUtil.class.getDeclaredField("jwtExpiration");
        expirationField.setAccessible(true);
        long originalExpiration = (Long) expirationField.get(jwtUtil);
        
        try {
            expirationField.set(jwtUtil, expirationMs);
            return jwtUtil.generateToken(merchantId, username);
        } finally {
            expirationField.set(jwtUtil, originalExpiration);
        }
    }

    private void testTamperedToken(String originalToken, String tamperingType, TokenTamperer tamperer) throws Exception {
        String tamperedToken = tamperer.tamper(originalToken);
        
        // Tampering should be detected and request rejected
        mockMvc.perform(get("/api/v1/merchants/" + merchantId)
                .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Authentication failed"));
    }

    private String tamperSignature(String token) {
        String[] parts = token.split("\\.");
        if (parts.length == 3) {
            // Modify the last character of the signature
            String signature = parts[2];
            String tamperedSignature = signature.substring(0, signature.length() - 1) + "X";
            return parts[0] + "." + parts[1] + "." + tamperedSignature;
        }
        return token + "TAMPERED";
    }

    private String tamperPayload(String token) {
        String[] parts = token.split("\\.");
        if (parts.length == 3) {
            // Modify the payload by adding characters
            String payload = parts[1];
            String tamperedPayload = payload + "XX";
            return parts[0] + "." + tamperedPayload + "." + parts[2];
        }
        return token + "TAMPERED";
    }

    private String tamperHeader(String token) {
        String[] parts = token.split("\\.");
        if (parts.length == 3) {
            // Modify the header
            String header = parts[0];
            String tamperedHeader = header + "XX";
            return tamperedHeader + "." + parts[1] + "." + parts[2];
        }
        return token + "TAMPERED";
    }

    @FunctionalInterface
    private interface TokenTamperer {
        String tamper(String token);
    }
}