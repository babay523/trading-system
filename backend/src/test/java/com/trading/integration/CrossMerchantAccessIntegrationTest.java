package com.trading.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.dto.request.*;
import com.trading.dto.response.*;
import com.trading.entity.Inventory;
import com.trading.entity.Product;
import com.trading.entity.Settlement;
import com.trading.enums.SettlementStatus;
import com.trading.repository.*;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Cross-Merchant Access Integration Test
 * 
 * Tests that merchants cannot access other merchants' resources including:
 * - Inventory, balance, settlement records
 * - All protected endpoints access control
 * 
 * Validates: Requirements 2.4, 3.1, 3.2, 3.3, 3.4
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Cross-Merchant Access Prevention Tests")
class CrossMerchantAccessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private SettlementRepository settlementRepository;

    private String merchantAToken;
    private String merchantBToken;
    private Long merchantAId;
    private Long merchantBId;
    private String testSkuA = "MERCHANT-A-SKU";
    private String testSkuB = "MERCHANT-B-SKU";

    @BeforeEach
    void setUp() throws Exception {
        // Clean up data
        settlementRepository.deleteAll();
        inventoryRepository.deleteAll();
        productRepository.deleteAll();
        merchantRepository.deleteAll();

        // Register Merchant A
        MerchantRegisterRequest merchantARequest = new MerchantRegisterRequest();
        merchantARequest.setBusinessName("Merchant A Business");
        merchantARequest.setUsername("merchantA");
        merchantARequest.setPassword("password123");

        MvcResult resultA = mockMvc.perform(post("/api/v1/merchants/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(merchantARequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponse<MerchantResponse> responseA = objectMapper.readValue(
                resultA.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, MerchantResponse.class)
        );
        merchantAId = responseA.getData().getId();

        // Register Merchant B
        MerchantRegisterRequest merchantBRequest = new MerchantRegisterRequest();
        merchantBRequest.setBusinessName("Merchant B Business");
        merchantBRequest.setUsername("merchantB");
        merchantBRequest.setPassword("password123");

        MvcResult resultB = mockMvc.perform(post("/api/v1/merchants/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(merchantBRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponse<MerchantResponse> responseB = objectMapper.readValue(
                resultB.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, MerchantResponse.class)
        );
        merchantBId = responseB.getData().getId();

        // Login Merchant A
        LoginRequest loginRequestA = new LoginRequest();
        loginRequestA.setUsername("merchantA");
        loginRequestA.setPassword("password123");

        MvcResult loginResultA = mockMvc.perform(post("/api/v1/merchants/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequestA)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<AuthResponse> authResponseA = objectMapper.readValue(
                loginResultA.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthResponse.class)
        );
        merchantAToken = authResponseA.getData().getAccessToken();

        // Login Merchant B
        LoginRequest loginRequestB = new LoginRequest();
        loginRequestB.setUsername("merchantB");
        loginRequestB.setPassword("password123");

        MvcResult loginResultB = mockMvc.perform(post("/api/v1/merchants/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequestB)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<AuthResponse> authResponseB = objectMapper.readValue(
                loginResultB.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthResponse.class)
        );
        merchantBToken = authResponseB.getData().getAccessToken();

        // Create test data for both merchants
        setupTestData();
    }

    private void setupTestData() {
        // Create products for both merchants
        Product productA = Product.builder()
                .name("Merchant A Product")
                .description("Product owned by Merchant A")
                .category("Electronics")
                .merchantId(merchantAId)
                .build();
        productA = productRepository.save(productA);

        Product productB = Product.builder()
                .name("Merchant B Product")
                .description("Product owned by Merchant B")
                .category("Electronics")
                .merchantId(merchantBId)
                .build();
        productB = productRepository.save(productB);

        // Create inventory for both merchants
        Inventory inventoryA = Inventory.builder()
                .sku(testSkuA)
                .productId(productA.getId())
                .merchantId(merchantAId)
                .quantity(100)
                .price(new BigDecimal("29.99"))
                .build();
        inventoryRepository.save(inventoryA);

        Inventory inventoryB = Inventory.builder()
                .sku(testSkuB)
                .productId(productB.getId())
                .merchantId(merchantBId)
                .quantity(50)
                .price(new BigDecimal("39.99"))
                .build();
        inventoryRepository.save(inventoryB);

        // Create settlement records for both merchants
        Settlement settlementA = Settlement.builder()
                .merchantId(merchantAId)
                .settlementDate(LocalDate.now().minusDays(1))
                .totalSales(new BigDecimal("500.00"))
                .totalRefunds(BigDecimal.ZERO)
                .netAmount(new BigDecimal("500.00"))
                .balanceChange(new BigDecimal("500.00"))
                .status(SettlementStatus.MATCHED)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
        settlementRepository.save(settlementA);

        Settlement settlementB = Settlement.builder()
                .merchantId(merchantBId)
                .settlementDate(LocalDate.now().minusDays(1))
                .totalSales(new BigDecimal("750.00"))
                .totalRefunds(BigDecimal.ZERO)
                .netAmount(new BigDecimal("750.00"))
                .balanceChange(new BigDecimal("750.00"))
                .status(SettlementStatus.MATCHED)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
        settlementRepository.save(settlementB);
    }

    @Test
    @DisplayName("Merchant A cannot access Merchant B's profile")
    void merchantA_CannotAccessMerchantB_Profile() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/" + merchantBId)
                .header("Authorization", "Bearer " + merchantAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @DisplayName("Merchant A cannot access Merchant B's balance")
    void merchantA_CannotAccessMerchantB_Balance() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/" + merchantBId + "/balance")
                .header("Authorization", "Bearer " + merchantAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @DisplayName("Merchant A cannot access Merchant B's inventory")
    void merchantA_CannotAccessMerchantB_Inventory() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/" + merchantBId + "/inventory")
                .header("Authorization", "Bearer " + merchantAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @DisplayName("Merchant A cannot add inventory for Merchant B")
    void merchantA_CannotAddInventoryFor_MerchantB() throws Exception {
        InventoryAddRequest request = new InventoryAddRequest();
        request.setSku("UNAUTHORIZED-SKU");
        request.setProductId(1L);
        request.setQuantity(10);
        request.setPrice(new BigDecimal("99.99"));

        mockMvc.perform(post("/api/v1/merchants/" + merchantBId + "/inventory")
                .header("Authorization", "Bearer " + merchantAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @DisplayName("Merchant A cannot update Merchant B's inventory prices")
    void merchantA_CannotUpdateMerchantB_InventoryPrices() throws Exception {
        PriceUpdateRequest request = new PriceUpdateRequest();
        request.setPrice(new BigDecimal("999.99"));

        mockMvc.perform(put("/api/v1/merchants/" + merchantBId + "/inventory/" + testSkuB + "/price")
                .header("Authorization", "Bearer " + merchantAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @DisplayName("Merchant A cannot access Merchant B's settlement records")
    void merchantA_CannotAccessMerchantB_SettlementRecords() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/" + merchantBId + "/settlements")
                .header("Authorization", "Bearer " + merchantAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @DisplayName("Merchant A cannot run settlement for Merchant B")
    void merchantA_CannotRunSettlementFor_MerchantB() throws Exception {
        mockMvc.perform(post("/api/v1/merchants/" + merchantBId + "/settlements/run")
                .header("Authorization", "Bearer " + merchantAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @DisplayName("Merchant B cannot access Merchant A's resources")
    void merchantB_CannotAccessMerchantA_Resources() throws Exception {
        // Test profile access
        mockMvc.perform(get("/api/v1/merchants/" + merchantAId)
                .header("Authorization", "Bearer " + merchantBToken))
                .andExpect(status().isForbidden());

        // Test balance access
        mockMvc.perform(get("/api/v1/merchants/" + merchantAId + "/balance")
                .header("Authorization", "Bearer " + merchantBToken))
                .andExpect(status().isForbidden());

        // Test inventory access
        mockMvc.perform(get("/api/v1/merchants/" + merchantAId + "/inventory")
                .header("Authorization", "Bearer " + merchantBToken))
                .andExpect(status().isForbidden());

        // Test settlement access
        mockMvc.perform(get("/api/v1/merchants/" + merchantAId + "/settlements")
                .header("Authorization", "Bearer " + merchantBToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Merchants can access their own resources successfully")
    void merchants_CanAccessOwnResources_Successfully() throws Exception {
        // Merchant A can access own resources
        mockMvc.perform(get("/api/v1/merchants/" + merchantAId)
                .header("Authorization", "Bearer " + merchantAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(merchantAId))
                .andExpect(jsonPath("$.data.businessName").value("Merchant A Business"));

        mockMvc.perform(get("/api/v1/merchants/" + merchantAId + "/balance")
                .header("Authorization", "Bearer " + merchantAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.merchantId").value(merchantAId));

        mockMvc.perform(get("/api/v1/merchants/" + merchantAId + "/inventory")
                .header("Authorization", "Bearer " + merchantAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());

        mockMvc.perform(get("/api/v1/merchants/" + merchantAId + "/settlements")
                .header("Authorization", "Bearer " + merchantAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());

        // Merchant B can access own resources
        mockMvc.perform(get("/api/v1/merchants/" + merchantBId)
                .header("Authorization", "Bearer " + merchantBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(merchantBId))
                .andExpect(jsonPath("$.data.businessName").value("Merchant B Business"));

        mockMvc.perform(get("/api/v1/merchants/" + merchantBId + "/balance")
                .header("Authorization", "Bearer " + merchantBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.merchantId").value(merchantBId));

        mockMvc.perform(get("/api/v1/merchants/" + merchantBId + "/inventory")
                .header("Authorization", "Bearer " + merchantBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());

        mockMvc.perform(get("/api/v1/merchants/" + merchantBId + "/settlements")
                .header("Authorization", "Bearer " + merchantBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("Requests without authentication tokens are rejected")
    void requestsWithoutTokens_AreRejected() throws Exception {
        // Test all protected endpoints without tokens
        mockMvc.perform(get("/api/v1/merchants/" + merchantAId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/api/v1/merchants/" + merchantAId + "/balance"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/api/v1/merchants/" + merchantAId + "/inventory"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/api/v1/merchants/" + merchantAId + "/settlements"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(post("/api/v1/merchants/" + merchantAId + "/inventory")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(put("/api/v1/merchants/" + merchantAId + "/inventory/test-sku/price")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(post("/api/v1/merchants/" + merchantAId + "/settlements/run"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("Requests with invalid tokens are rejected")
    void requestsWithInvalidTokens_AreRejected() throws Exception {
        String invalidToken = "invalid.jwt.token";

        mockMvc.perform(get("/api/v1/merchants/" + merchantAId)
                .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/api/v1/merchants/" + merchantAId + "/balance")
                .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/api/v1/merchants/" + merchantAId + "/inventory")
                .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("Data isolation is maintained - merchants only see their own data")
    void dataIsolation_IsMaintained() throws Exception {
        // Merchant A should only see their own inventory
        mockMvc.perform(get("/api/v1/merchants/" + merchantAId + "/inventory")
                .header("Authorization", "Bearer " + merchantAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[?(@.sku == '" + testSkuA + "')]").exists())
                .andExpect(jsonPath("$.data.content[?(@.sku == '" + testSkuB + "')]").doesNotExist());

        // Merchant B should only see their own inventory
        mockMvc.perform(get("/api/v1/merchants/" + merchantBId + "/inventory")
                .header("Authorization", "Bearer " + merchantBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[?(@.sku == '" + testSkuB + "')]").exists())
                .andExpect(jsonPath("$.data.content[?(@.sku == '" + testSkuA + "')]").doesNotExist());

        // Merchant A should only see their own settlements
        mockMvc.perform(get("/api/v1/merchants/" + merchantAId + "/settlements")
                .header("Authorization", "Bearer " + merchantAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[?(@.merchantId == " + merchantAId + ")]").exists())
                .andExpect(jsonPath("$.data.content[?(@.merchantId == " + merchantBId + ")]").doesNotExist());

        // Merchant B should only see their own settlements
        mockMvc.perform(get("/api/v1/merchants/" + merchantBId + "/settlements")
                .header("Authorization", "Bearer " + merchantBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[?(@.merchantId == " + merchantBId + ")]").exists())
                .andExpect(jsonPath("$.data.content[?(@.merchantId == " + merchantAId + ")]").doesNotExist());
    }
}