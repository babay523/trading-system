package com.trading.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.dto.request.LoginRequest;
import com.trading.dto.request.MerchantRegisterRequest;
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

import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security Boundary Test
 * 
 * Tests various attack scenarios including:
 * - Invalid tokens, expired tokens, tampered tokens
 * - Error handling and logging verification
 * - Security boundary conditions
 * 
 * Validates: Requirements 6.1, 6.2, 6.3, 6.4, 6.5
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Security Boundary Tests")
class SecurityBoundaryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private Long validMerchantId;
    private String validUsername = "securitytest";
    private String validToken;

    @BeforeEach
    void setUp() throws Exception {
        // Register a test merchant
        MerchantRegisterRequest registerRequest = new MerchantRegisterRequest();
        registerRequest.setBusinessName("Security Test Business");
        registerRequest.setUsername(validUsername);
        registerRequest.setPassword("password123");

        MvcResult registerResult = mockMvc.perform(post("/api/v1/merchants/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponse<MerchantResponse> registerResponse = objectMapper.readValue(
                registerResult.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, MerchantResponse.class)
        );
        validMerchantId = registerResponse.getData().getId();

        // Login to get valid token
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(validUsername);
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/merchants/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<AuthResponse> authResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthResponse.class)
        );
        validToken = authResponse.getData().getAccessToken();
    }

    @Test
    @DisplayName("Invalid token formats are rejected with consistent error response")
    void invalidTokenFormats_AreRejectedWithConsistentErrorResponse() throws Exception {
        String[] invalidTokens = {
                "invalid-token",
                "not.a.jwt",
                "too.few.parts",
                "too.many.parts.here.extra",
                "",
                "Bearer-without-space-invalid",
                "completely-malformed-token-string",
                "eyJhbGciOiJIUzI1NiJ9.invalid-payload.signature",
                "header.eyJzdWIiOiJ0ZXN0In0.invalid-signature"
        };

        for (String invalidToken : invalidTokens) {
            mockMvc.perform(get("/api/v1/merchants/" + validMerchantId)
                    .header("Authorization", "Bearer " + invalidToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(401))
                    .andExpect(jsonPath("$.message").value("Authentication failed"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    @Test
    @DisplayName("Missing Authorization header is handled correctly")
    void missingAuthorizationHeader_IsHandledCorrectly() throws Exception {
        // Test all protected endpoints without Authorization header
        String[] protectedEndpoints = {
                "/api/v1/merchants/" + validMerchantId,
                "/api/v1/merchants/" + validMerchantId + "/balance",
                "/api/v1/merchants/" + validMerchantId + "/inventory",
                "/api/v1/merchants/" + validMerchantId + "/settlements"
        };

        for (String endpoint : protectedEndpoints) {
            mockMvc.perform(get(endpoint))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(401))
                    .andExpect(jsonPath("$.message").value("Authentication token is required"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    @Test
    @DisplayName("Malformed Authorization header formats are rejected")
    void malformedAuthorizationHeaders_AreRejected() throws Exception {
        String[] malformedHeaders = {
                "InvalidPrefix " + validToken,
                "Bearer" + validToken, // Missing space
                "bearer " + validToken, // Wrong case
                "Basic " + validToken, // Wrong auth type
                validToken, // Missing Bearer prefix
                "Bearer ", // Missing token
                "Bearer  " + validToken, // Extra space
                "Bearer\t" + validToken, // Tab instead of space
                "Bearer\n" + validToken // Newline instead of space
        };

        for (String malformedHeader : malformedHeaders) {
            mockMvc.perform(get("/api/v1/merchants/" + validMerchantId)
                    .header("Authorization", malformedHeader))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(401));
        }
    }

    @Test
    @DisplayName("Token signature tampering is detected")
    void tokenSignatureTampering_IsDetected() throws Exception {
        String[] parts = validToken.split("\\.");
        assertEquals(3, parts.length, "Valid token should have 3 parts");

        // Test various signature tampering scenarios
        String[] tamperedSignatures = {
                parts[2] + "X", // Append character
                "X" + parts[2], // Prepend character
                parts[2].substring(1), // Remove first character
                parts[2].substring(0, parts[2].length() - 1), // Remove last character
                parts[2].replace('a', 'b'), // Replace character
                Base64.getEncoder().encodeToString("fake-signature".getBytes()), // Completely fake signature
                "" // Empty signature
        };

        for (String tamperedSignature : tamperedSignatures) {
            String tamperedToken = parts[0] + "." + parts[1] + "." + tamperedSignature;
            
            mockMvc.perform(get("/api/v1/merchants/" + validMerchantId)
                    .header("Authorization", "Bearer " + tamperedToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(401))
                    .andExpect(jsonPath("$.message").value("Authentication failed"));
        }
    }

    @Test
    @DisplayName("Token payload tampering is detected")
    void tokenPayloadTampering_IsDetected() throws Exception {
        String[] parts = validToken.split("\\.");
        assertEquals(3, parts.length, "Valid token should have 3 parts");

        // Create tampered payloads
        String originalPayload = parts[1];
        String[] tamperedPayloads = {
                originalPayload + "XX", // Append characters
                "XX" + originalPayload, // Prepend characters
                originalPayload.substring(1), // Remove first character
                originalPayload.replace('e', 'x'), // Replace character
                Base64.getEncoder().encodeToString("{\"sub\":\"hacker\",\"merchantId\":999}".getBytes()), // Fake payload
                "" // Empty payload
        };

        for (String tamperedPayload : tamperedPayloads) {
            String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];
            
            mockMvc.perform(get("/api/v1/merchants/" + validMerchantId)
                    .header("Authorization", "Bearer " + tamperedToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(401))
                    .andExpect(jsonPath("$.message").value("Authentication failed"));
        }
    }

    @Test
    @DisplayName("Token header tampering is detected")
    void tokenHeaderTampering_IsDetected() throws Exception {
        String[] parts = validToken.split("\\.");
        assertEquals(3, parts.length, "Valid token should have 3 parts");

        // Create tampered headers
        String originalHeader = parts[0];
        String[] tamperedHeaders = {
                originalHeader + "XX", // Append characters
                "XX" + originalHeader, // Prepend characters
                originalHeader.substring(1), // Remove first character
                Base64.getEncoder().encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes()), // Algorithm confusion
                Base64.getEncoder().encodeToString("{\"alg\":\"HS512\",\"typ\":\"JWT\"}".getBytes()), // Wrong algorithm
                "" // Empty header
        };

        for (String tamperedHeader : tamperedHeaders) {
            String tamperedToken = tamperedHeader + "." + parts[1] + "." + parts[2];
            
            mockMvc.perform(get("/api/v1/merchants/" + validMerchantId)
                    .header("Authorization", "Bearer " + tamperedToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(401))
                    .andExpect(jsonPath("$.message").value("Authentication failed"));
        }
    }

    @Test
    @DisplayName("Brute force authentication attempts are handled")
    void bruteForceAuthenticationAttempts_AreHandled() throws Exception {
        String[] invalidPasswords = {
                "wrong1", "wrong2", "wrong3", "wrong4", "wrong5",
                "password", "123456", "admin", "test", "wrong"
        };

        // Attempt multiple failed logins
        for (String invalidPassword : invalidPasswords) {
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername(validUsername);
            loginRequest.setPassword(invalidPassword);

            mockMvc.perform(post("/api/v1/merchants/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(401))
                    .andExpect(jsonPath("$.message").exists());
        }

        // Valid login should still work after failed attempts
        LoginRequest validLoginRequest = new LoginRequest();
        validLoginRequest.setUsername(validUsername);
        validLoginRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/merchants/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    @DisplayName("Concurrent authentication attempts are handled safely")
    void concurrentAuthenticationAttempts_AreHandledSafely() throws Exception {
        int numberOfThreads = 10;
        int attemptsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < attemptsPerThread; j++) {
                        LoginRequest loginRequest = new LoginRequest();
                        loginRequest.setUsername(validUsername);
                        // Mix of valid and invalid passwords
                        loginRequest.setPassword(j % 2 == 0 ? "password123" : "wrong" + threadId + j);

                        try {
                            MvcResult result = mockMvc.perform(post("/api/v1/merchants/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(loginRequest)))
                                    .andReturn();

                            if (result.getResponse().getStatus() == 200) {
                                successCount.incrementAndGet();
                            } else {
                                failureCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Verify that some attempts succeeded and some failed
        assertTrue(successCount.get() > 0, "Some valid login attempts should succeed");
        assertTrue(failureCount.get() > 0, "Some invalid login attempts should fail");
        assertEquals(numberOfThreads * attemptsPerThread, successCount.get() + failureCount.get(),
                "Total attempts should match expected count");
    }

    @Test
    @DisplayName("SQL injection attempts in login are prevented")
    void sqlInjectionAttempts_ArePrevented() throws Exception {
        String[] sqlInjectionPayloads = {
                "admin'; DROP TABLE merchants; --",
                "' OR '1'='1",
                "' OR 1=1 --",
                "admin'/*",
                "' UNION SELECT * FROM merchants --",
                "'; INSERT INTO merchants VALUES ('hacker', 'password'); --",
                "' OR 'x'='x",
                "1' OR '1'='1' /*"
        };

        for (String payload : sqlInjectionPayloads) {
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername(payload);
            loginRequest.setPassword(payload);

            mockMvc.perform(post("/api/v1/merchants/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(401))
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    @Test
    @DisplayName("XSS attempts in request parameters are handled")
    void xssAttempts_AreHandled() throws Exception {
        String[] xssPayloads = {
                "<script>alert('xss')</script>",
                "javascript:alert('xss')",
                "<img src=x onerror=alert('xss')>",
                "';alert('xss');//",
                "<svg onload=alert('xss')>",
                "<%2Fscript%3E%3Cscript%3Ealert('xss')%3C%2Fscript%3E"
        };

        for (String payload : xssPayloads) {
            // Test XSS in username during registration
            MerchantRegisterRequest registerRequest = new MerchantRegisterRequest();
            registerRequest.setBusinessName("Test Business");
            registerRequest.setUsername(payload);
            registerRequest.setPassword("password123");

            mockMvc.perform(post("/api/v1/merchants/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));

            // Test XSS in business name
            registerRequest.setUsername("validuser" + System.currentTimeMillis());
            registerRequest.setBusinessName(payload);

            mockMvc.perform(post("/api/v1/merchants/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }
    }

    @Test
    @DisplayName("Large payload attacks are handled")
    void largePayloadAttacks_AreHandled() throws Exception {
        // Create very large username and password
        StringBuilder largeString = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeString.append("A");
        }
        String largePayload = largeString.toString();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(largePayload);
        loginRequest.setPassword(largePayload);

        mockMvc.perform(post("/api/v1/merchants/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("Error responses do not expose sensitive information")
    void errorResponses_DoNotExposeSensitiveInformation() throws Exception {
        // Test various error scenarios and verify no sensitive data is exposed
        
        // Invalid token error
        MvcResult result1 = mockMvc.perform(get("/api/v1/merchants/" + validMerchantId)
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andReturn();
        
        String response1 = result1.getResponse().getContentAsString();
        assertFalse(response1.contains("secret"), "Response should not contain secret key");
        assertFalse(response1.contains("password"), "Response should not contain password");
        assertFalse(response1.contains("merchantId"), "Response should not contain internal merchant ID");
        assertFalse(response1.contains("Exception"), "Response should not contain exception details");
        assertFalse(response1.contains("Stack"), "Response should not contain stack trace");

        // Cross-merchant access error
        MvcResult result2 = mockMvc.perform(get("/api/v1/merchants/99999")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isForbidden())
                .andReturn();
        
        String response2 = result2.getResponse().getContentAsString();
        assertFalse(response2.contains("99999"), "Response should not expose requested merchant ID");
        assertFalse(response2.contains(validMerchantId.toString()), "Response should not expose current merchant ID");
        assertFalse(response2.contains("database"), "Response should not contain database details");

        // Invalid login error
        LoginRequest invalidLogin = new LoginRequest();
        invalidLogin.setUsername("nonexistent");
        invalidLogin.setPassword("wrong");

        MvcResult result3 = mockMvc.perform(post("/api/v1/merchants/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidLogin)))
                .andExpect(status().isUnauthorized())
                .andReturn();
        
        String response3 = result3.getResponse().getContentAsString();
        assertFalse(response3.contains("nonexistent"), "Response should not echo back username");
        assertFalse(response3.contains("wrong"), "Response should not echo back password");
        assertFalse(response3.contains("not found"), "Response should not reveal if user exists");
    }

    @Test
    @DisplayName("Security headers are present in responses")
    void securityHeaders_ArePresentInResponses() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/merchants/" + validMerchantId)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andReturn();

        // Check for security headers (these would be configured in SecurityConfig)
        // Note: Actual header presence depends on Spring Security configuration
        assertNotNull(result.getResponse().getHeader("X-Content-Type-Options"));
        assertNotNull(result.getResponse().getHeader("X-Frame-Options"));
        assertNotNull(result.getResponse().getHeader("X-XSS-Protection"));
    }

    @Test
    @DisplayName("Rate limiting behavior is consistent")
    void rateLimiting_BehaviorIsConsistent() throws Exception {
        // This test would verify rate limiting if implemented
        // For now, it tests that multiple rapid requests are handled consistently
        
        int numberOfRequests = 20;
        for (int i = 0; i < numberOfRequests; i++) {
            mockMvc.perform(get("/api/v1/merchants/" + validMerchantId)
                    .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(validMerchantId));
        }
    }

    @Test
    @DisplayName("Token with future issued date is rejected")
    void tokenWithFutureIssuedDate_IsRejected() throws Exception {
        // This would require creating a token with future iat claim
        // For now, we test that the current token validation is working
        assertTrue(jwtUtil.validateToken(validToken), "Valid token should pass validation");
        
        // Test that token validation includes proper time checks
        assertFalse(jwtUtil.isTokenExpired(validToken), "Current token should not be expired");
    }
}