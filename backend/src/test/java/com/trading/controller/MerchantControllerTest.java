package com.trading.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.dto.request.InventoryAddRequest;
import com.trading.dto.request.LoginRequest;
import com.trading.dto.request.MerchantRegisterRequest;
import com.trading.dto.request.PriceUpdateRequest;
import com.trading.dto.request.ProductCreateRequest;
import com.trading.dto.response.ApiResponse;
import com.trading.dto.response.AuthResponse;
import com.trading.dto.response.MerchantResponse;
import com.trading.dto.response.ProductResponse;
import com.trading.repository.InventoryRepository;
import com.trading.repository.MerchantRepository;
import com.trading.repository.ProductRepository;
import com.trading.service.MerchantService;
import com.trading.service.ProductService;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class MerchantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private ProductService productService;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
        productRepository.deleteAll();
        merchantRepository.deleteAll();
    }

    @Test
    void registerMerchant_Success() throws Exception {
        MerchantRegisterRequest request = MerchantRegisterRequest.builder()
                .businessName("Test Electronics Store")
                .username("testmerchant")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.businessName").value("Test Electronics Store"))
                .andExpect(jsonPath("$.data.username").value("testmerchant"))
                .andExpect(jsonPath("$.data.balance").value(0));
    }

    @Test
    void registerMerchant_DuplicateUsername() throws Exception {
        // First registration
        MerchantRegisterRequest request = MerchantRegisterRequest.builder()
                .businessName("Test Store")
                .username("duplicatemerchant")
                .password("password123")
                .build();
        merchantService.register(request);

        // Second registration with same username
        mockMvc.perform(post("/api/v1/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void loginMerchant_Success() throws Exception {
        // Register first
        MerchantRegisterRequest registerRequest = MerchantRegisterRequest.builder()
                .businessName("Test Store")
                .username("loginmerchant")
                .password("password123")
                .build();
        merchantService.register(registerRequest);

        // Login
        LoginRequest loginRequest = LoginRequest.builder()
                .username("loginmerchant")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/merchants/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.merchant.username").value("loginmerchant"));
    }

    @Test
    void loginMerchant_InvalidCredentials() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("nonexistent")
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/api/v1/merchants/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void getMerchantBalance_Success() throws Exception {
        MerchantRegisterRequest request = MerchantRegisterRequest.builder()
                .businessName("Test Store")
                .username("balancemerchant")
                .password("password123")
                .build();
        MerchantResponse merchant = merchantService.register(request);

        // Login to get token
        LoginRequest loginRequest = LoginRequest.builder()
                .username("balancemerchant")
                .password("password123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/merchants/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<AuthResponse> authResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthResponse.class)
        );
        String token = authResponse.getData().getAccessToken();

        mockMvc.perform(get("/api/v1/merchants/{id}/balance", merchant.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.merchantId").value(merchant.getId()))
                .andExpect(jsonPath("$.data.balance").value(0));
    }

    @Test
    void addInventory_Success() throws Exception {
        // Create merchant
        MerchantRegisterRequest merchantRequest = MerchantRegisterRequest.builder()
                .businessName("Test Store")
                .username("inventorymerchant")
                .password("password123")
                .build();
        MerchantResponse merchant = merchantService.register(merchantRequest);

        // Login to get token
        LoginRequest loginRequest = LoginRequest.builder()
                .username("inventorymerchant")
                .password("password123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/merchants/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<AuthResponse> authResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthResponse.class)
        );
        String token = authResponse.getData().getAccessToken();

        // Create product
        ProductCreateRequest productRequest = ProductCreateRequest.builder()
                .merchantId(merchant.getId())
                .name("Test Phone")
                .description("A great phone")
                .category("Electronics")
                .build();
        ProductResponse product = productService.create(productRequest);

        // Add inventory
        InventoryAddRequest inventoryRequest = InventoryAddRequest.builder()
                .sku("SKU-001")
                .productId(product.getId())
                .quantity(100)
                .price(new BigDecimal("999.99"))
                .build();

        mockMvc.perform(post("/api/v1/merchants/{id}/inventory", merchant.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inventoryRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.sku").value("SKU-001"))
                .andExpect(jsonPath("$.data.quantity").value(100))
                .andExpect(jsonPath("$.data.price").value(999.99));
    }

    @Test
    void updateInventoryPrice_Success() throws Exception {
        // Create merchant
        MerchantRegisterRequest merchantRequest = MerchantRegisterRequest.builder()
                .businessName("Test Store")
                .username("pricemerchant")
                .password("password123")
                .build();
        MerchantResponse merchant = merchantService.register(merchantRequest);

        // Login to get token
        LoginRequest loginRequest = LoginRequest.builder()
                .username("pricemerchant")
                .password("password123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/merchants/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<AuthResponse> authResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthResponse.class)
        );
        String token = authResponse.getData().getAccessToken();

        // Create product
        ProductCreateRequest productRequest = ProductCreateRequest.builder()
                .merchantId(merchant.getId())
                .name("Test Laptop")
                .description("A great laptop")
                .category("Computers")
                .build();
        ProductResponse product = productService.create(productRequest);

        // Add inventory
        InventoryAddRequest inventoryRequest = InventoryAddRequest.builder()
                .sku("SKU-LAPTOP-001")
                .productId(product.getId())
                .quantity(50)
                .price(new BigDecimal("1499.99"))
                .build();

        mockMvc.perform(post("/api/v1/merchants/{id}/inventory", merchant.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inventoryRequest)))
                .andExpect(status().isCreated());

        // Update price
        PriceUpdateRequest priceRequest = PriceUpdateRequest.builder()
                .price(new BigDecimal("1299.99"))
                .build();

        mockMvc.perform(put("/api/v1/merchants/{id}/inventory/{sku}/price", merchant.getId(), "SKU-LAPTOP-001")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(priceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.price").value(1299.99));
    }

    @Test
    void getMerchantInventory_Success() throws Exception {
        // Create merchant
        MerchantRegisterRequest merchantRequest = MerchantRegisterRequest.builder()
                .businessName("Test Store")
                .username("listmerchant")
                .password("password123")
                .build();
        MerchantResponse merchant = merchantService.register(merchantRequest);

        // Login to get token
        LoginRequest loginRequest = LoginRequest.builder()
                .username("listmerchant")
                .password("password123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/merchants/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<AuthResponse> authResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthResponse.class)
        );
        String token = authResponse.getData().getAccessToken();

        // Create product
        ProductCreateRequest productRequest = ProductCreateRequest.builder()
                .merchantId(merchant.getId())
                .name("Test Product")
                .description("A test product")
                .category("Electronics")
                .build();
        ProductResponse product = productService.create(productRequest);

        // Add multiple inventory items
        for (int i = 1; i <= 3; i++) {
            InventoryAddRequest inventoryRequest = InventoryAddRequest.builder()
                    .sku("SKU-LIST-" + i)
                    .productId(product.getId())
                    .quantity(10 * i)
                    .price(new BigDecimal("99.99"))
                    .build();

            mockMvc.perform(post("/api/v1/merchants/{id}/inventory", merchant.getId())
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(inventoryRequest)))
                    .andExpect(status().isCreated());
        }

        // Get inventory list
        mockMvc.perform(get("/api/v1/merchants/{id}/inventory", merchant.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content", hasSize(3)));
    }
}
