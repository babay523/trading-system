package com.trading.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.dto.request.LoginRequest;
import com.trading.dto.request.MerchantRegisterRequest;
import com.trading.dto.request.RefreshTokenRequest;
import com.trading.repository.MerchantRepository;
import com.trading.service.MerchantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController集成测试
 * 测试认证相关的API端点
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private MerchantRepository merchantRepository;

    private String testMerchantUsername = "testmerchant";
    private String testMerchantPassword = "password123";

    @BeforeEach
    void setUp() {
        // 清理测试数据
        merchantRepository.deleteAll();
        
        // 创建测试商家
        MerchantRegisterRequest registerRequest = MerchantRegisterRequest.builder()
                .businessName("Test Business")
                .username(testMerchantUsername)
                .password(testMerchantPassword)
                .build();
        
        merchantService.register(registerRequest);
    }

    @Test
    void testLogin_Success() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username(testMerchantUsername)
                .password(testMerchantPassword)
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").isNumber())
                .andExpect(jsonPath("$.data.merchant.username").value(testMerchantUsername))
                .andExpect(jsonPath("$.data.merchant.businessName").value("Test Business"));
    }

    @Test
    void testLogin_InvalidCredentials() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username(testMerchantUsername)
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void testLogin_NonExistentUser() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("nonexistent")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void testRefreshToken_Success() throws Exception {
        // 首先登录获取令牌
        LoginRequest loginRequest = LoginRequest.builder()
                .username(testMerchantUsername)
                .password(testMerchantPassword)
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        JsonNode responseNode = objectMapper.readTree(loginResponse);
        String accessToken = responseNode.get("data").get("accessToken").asText();

        // 使用访问令牌作为刷新令牌（在实际应用中，刷新令牌应该是不同的）
        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken(accessToken)
                .build();

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.merchant.username").value(testMerchantUsername));
    }

    @Test
    void testRefreshToken_InvalidToken() throws Exception {
        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken("invalid-token")
                .build();

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void testValidateToken_ValidToken() throws Exception {
        // 首先登录获取令牌
        LoginRequest loginRequest = LoginRequest.builder()
                .username(testMerchantUsername)
                .password(testMerchantPassword)
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        JsonNode responseNode = objectMapper.readTree(loginResponse);
        String accessToken = responseNode.get("data").get("accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/validate")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Token validation result"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void testValidateToken_InvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/validate")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Token validation result"))
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    void testValidateToken_NoToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Token validation result"))
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    void testGetCurrentMerchant_Success() throws Exception {
        // 首先登录获取令牌
        LoginRequest loginRequest = LoginRequest.builder()
                .username(testMerchantUsername)
                .password(testMerchantPassword)
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        JsonNode responseNode = objectMapper.readTree(loginResponse);
        String accessToken = responseNode.get("data").get("accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/current")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Current merchant information"))
                .andExpect(jsonPath("$.data.username").value(testMerchantUsername))
                .andExpect(jsonPath("$.data.businessName").value("Test Business"));
    }

    @Test
    void testGetCurrentMerchant_NoToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/current"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLogout_Success() throws Exception {
        // 首先登录获取令牌
        LoginRequest loginRequest = LoginRequest.builder()
                .username(testMerchantUsername)
                .password(testMerchantPassword)
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        JsonNode responseNode = objectMapper.readTree(loginResponse);
        String accessToken = responseNode.get("data").get("accessToken").asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Logout successful"));
    }

    @Test
    void testLogout_NoToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Logout successful"));
    }
}