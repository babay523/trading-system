package com.trading.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.dto.request.LoginRequest;
import com.trading.dto.request.MerchantRegisterRequest;
import com.trading.dto.response.ApiResponse;
import com.trading.dto.response.AuthResponse;
import com.trading.dto.response.MerchantResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 商家授权控制集成测试
 * 验证商家只能访问自己的资源
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
public class MerchantAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String merchant1Token;
    private String merchant2Token;
    private Long merchant1Id;
    private Long merchant2Id;

    @BeforeEach
    void setUp() throws Exception {
        // 注册两个商家
        MerchantRegisterRequest merchant1Request = new MerchantRegisterRequest();
        merchant1Request.setBusinessName("Test Business 1");
        merchant1Request.setUsername("merchant1");
        merchant1Request.setPassword("password123");

        MerchantRegisterRequest merchant2Request = new MerchantRegisterRequest();
        merchant2Request.setBusinessName("Test Business 2");
        merchant2Request.setUsername("merchant2");
        merchant2Request.setPassword("password123");

        // 注册商家1
        MvcResult result1 = mockMvc.perform(post("/api/v1/merchants/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(merchant1Request)))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponse<MerchantResponse> response1 = objectMapper.readValue(
                result1.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, MerchantResponse.class)
        );
        merchant1Id = response1.getData().getId();

        // 注册商家2
        MvcResult result2 = mockMvc.perform(post("/api/v1/merchants/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(merchant2Request)))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponse<MerchantResponse> response2 = objectMapper.readValue(
                result2.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, MerchantResponse.class)
        );
        merchant2Id = response2.getData().getId();

        // 登录商家1获取token
        LoginRequest loginRequest1 = new LoginRequest();
        loginRequest1.setUsername("merchant1");
        loginRequest1.setPassword("password123");

        MvcResult loginResult1 = mockMvc.perform(post("/api/v1/merchants/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest1)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<AuthResponse> authResponse1 = objectMapper.readValue(
                loginResult1.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthResponse.class)
        );
        merchant1Token = authResponse1.getData().getAccessToken();

        // 登录商家2获取token
        LoginRequest loginRequest2 = new LoginRequest();
        loginRequest2.setUsername("merchant2");
        loginRequest2.setPassword("password123");

        MvcResult loginResult2 = mockMvc.perform(post("/api/v1/merchants/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest2)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<AuthResponse> authResponse2 = objectMapper.readValue(
                loginResult2.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthResponse.class)
        );
        merchant2Token = authResponse2.getData().getAccessToken();
    }

    @Test
    void testMerchantCanAccessOwnResources() throws Exception {
        // 商家1应该能够访问自己的资源
        mockMvc.perform(get("/api/v1/merchants/" + merchant1Id)
                .header("Authorization", "Bearer " + merchant1Token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/merchants/" + merchant1Id + "/balance")
                .header("Authorization", "Bearer " + merchant1Token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/merchants/" + merchant1Id + "/inventory")
                .header("Authorization", "Bearer " + merchant1Token))
                .andExpect(status().isOk());
    }

    @Test
    void testMerchantCannotAccessOtherMerchantResources() throws Exception {
        // 商家1不应该能够访问商家2的资源
        mockMvc.perform(get("/api/v1/merchants/" + merchant2Id)
                .header("Authorization", "Bearer " + merchant1Token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/merchants/" + merchant2Id + "/balance")
                .header("Authorization", "Bearer " + merchant1Token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/merchants/" + merchant2Id + "/inventory")
                .header("Authorization", "Bearer " + merchant1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUnauthorizedAccessWithoutToken() throws Exception {
        // 没有token的请求应该被拒绝
        mockMvc.perform(get("/api/v1/merchants/" + merchant1Id))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/merchants/" + merchant1Id + "/balance"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/merchants/" + merchant1Id + "/inventory"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testUnauthorizedAccessWithInvalidToken() throws Exception {
        // 无效token的请求应该被拒绝
        mockMvc.perform(get("/api/v1/merchants/" + merchant1Id)
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/merchants/" + merchant1Id + "/balance")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }
}