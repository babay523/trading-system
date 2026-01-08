package com.trading.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.dto.request.*;
import com.trading.entity.Inventory;
import com.trading.entity.Product;
import com.trading.enums.OrderStatus;
import com.trading.repository.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OrderController
 * Validates: Requirements 11.2
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TransactionRecordRepository transactionRecordRepository;

    private Long userId;
    private Long merchantId;
    private Long productId;
    private static final String TEST_SKU = "ORDER-TEST-SKU";
    private static final BigDecimal UNIT_PRICE = new BigDecimal("50.00");

    @BeforeEach
    void setUp() throws Exception {
        transactionRecordRepository.deleteAll();
        cartItemRepository.deleteAll();
        orderRepository.deleteAll();
        inventoryRepository.deleteAll();
        productRepository.deleteAll();
        merchantRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user with balance
        UserRegisterRequest userRequest = UserRegisterRequest.builder()
                .username("orderuser")
                .password("password123")
                .build();

        String userResponse = mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        userId = objectMapper.readTree(userResponse).get("data").get("id").asLong();

        // Deposit money to user
        DepositRequest depositRequest = DepositRequest.builder()
                .amount(new BigDecimal("1000.00"))
                .build();

        mockMvc.perform(post("/api/v1/users/" + userId + "/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isOk());

        // Create test merchant
        MerchantRegisterRequest merchantRequest = MerchantRegisterRequest.builder()
                .businessName("Test Store")
                .username("ordermerchant")
                .password("password123")
                .build();

        String merchantResponse = mockMvc.perform(post("/api/v1/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(merchantRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        merchantId = objectMapper.readTree(merchantResponse).get("data").get("id").asLong();

        // Create test product
        Product product = Product.builder()
                .name("Test Product")
                .description("Test Description")
                .category("Electronics")
                .merchantId(merchantId)
                .build();
        product = productRepository.save(product);
        productId = product.getId();

        // Create test inventory
        Inventory inventory = Inventory.builder()
                .sku(TEST_SKU)
                .productId(productId)
                .merchantId(merchantId)
                .quantity(100)
                .price(UNIT_PRICE)
                .build();
        inventoryRepository.save(inventory);
    }

    @Test
    void createDirectOrder_WithValidRequest_ShouldCreateOrder() throws Exception {
        DirectPurchaseRequest request = DirectPurchaseRequest.builder()
                .sku(TEST_SKU)
                .quantity(2)
                .build();

        mockMvc.perform(post("/api/v1/users/" + userId + "/orders/direct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.merchantId").value(merchantId))
                .andExpect(jsonPath("$.data.totalAmount").value(100.00))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].sku").value(TEST_SKU))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2));
    }

    @Test
    void createFromCart_WithItemsInCart_ShouldCreateOrder() throws Exception {
        // Add item to cart
        CartAddRequest cartRequest = CartAddRequest.builder()
                .sku(TEST_SKU)
                .quantity(3)
                .build();

        mockMvc.perform(post("/api/v1/users/" + userId + "/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest)))
                .andExpect(status().isCreated());

        // Create order from cart
        mockMvc.perform(post("/api/v1/users/" + userId + "/orders/from-cart"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.totalAmount").value(150.00))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.items", hasSize(1)));
    }

    @Test
    void confirmPayment_WithValidOrder_ShouldProcessPayment() throws Exception {
        // Create order
        DirectPurchaseRequest request = DirectPurchaseRequest.builder()
                .sku(TEST_SKU)
                .quantity(2)
                .build();

        String orderResponse = mockMvc.perform(post("/api/v1/users/" + userId + "/orders/direct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(orderResponse).get("data").get("id").asLong();

        // Confirm payment
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("PAID"));

        // Verify user balance decreased
        mockMvc.perform(get("/api/v1/users/" + userId + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(900.00));

        // Verify merchant balance increased
        mockMvc.perform(get("/api/v1/merchants/" + merchantId + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(100.00));
    }

    @Test
    void confirmPayment_WithInsufficientBalance_ShouldReject() throws Exception {
        // Create order with amount exceeding balance
        DirectPurchaseRequest request = DirectPurchaseRequest.builder()
                .sku(TEST_SKU)
                .quantity(25) // 25 * 50 = 1250 > 1000 balance
                .build();

        String orderResponse = mockMvc.perform(post("/api/v1/users/" + userId + "/orders/direct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(orderResponse).get("data").get("id").asLong();

        // Confirm payment should fail
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/pay"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shipOrder_WithPaidOrder_ShouldUpdateStatus() throws Exception {
        // Create and pay order
        DirectPurchaseRequest request = DirectPurchaseRequest.builder()
                .sku(TEST_SKU)
                .quantity(1)
                .build();

        String orderResponse = mockMvc.perform(post("/api/v1/users/" + userId + "/orders/direct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(orderResponse).get("data").get("id").asLong();

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/pay"))
                .andExpect(status().isOk());

        // Ship order
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/ship"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHIPPED"));
    }

    @Test
    void completeOrder_WithShippedOrder_ShouldUpdateStatus() throws Exception {
        // Create, pay, and ship order
        DirectPurchaseRequest request = DirectPurchaseRequest.builder()
                .sku(TEST_SKU)
                .quantity(1)
                .build();

        String orderResponse = mockMvc.perform(post("/api/v1/users/" + userId + "/orders/direct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(orderResponse).get("data").get("id").asLong();

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/pay"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/ship"))
                .andExpect(status().isOk());

        // Complete order
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void cancelOrder_WithPendingOrder_ShouldCancel() throws Exception {
        // Create order
        DirectPurchaseRequest request = DirectPurchaseRequest.builder()
                .sku(TEST_SKU)
                .quantity(1)
                .build();

        String orderResponse = mockMvc.perform(post("/api/v1/users/" + userId + "/orders/direct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(orderResponse).get("data").get("id").asLong();

        // Cancel order
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void refundOrder_WithPaidOrder_ShouldRefund() throws Exception {
        // Create and pay order
        DirectPurchaseRequest request = DirectPurchaseRequest.builder()
                .sku(TEST_SKU)
                .quantity(2)
                .build();

        String orderResponse = mockMvc.perform(post("/api/v1/users/" + userId + "/orders/direct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(orderResponse).get("data").get("id").asLong();

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/pay"))
                .andExpect(status().isOk());

        // Verify balance after payment
        mockMvc.perform(get("/api/v1/users/" + userId + "/balance"))
                .andExpect(jsonPath("$.data.balance").value(900.00));

        // Refund order
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/refund"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REFUNDED"));

        // Verify user balance restored
        mockMvc.perform(get("/api/v1/users/" + userId + "/balance"))
                .andExpect(jsonPath("$.data.balance").value(1000.00));

        // Verify merchant balance deducted
        mockMvc.perform(get("/api/v1/merchants/" + merchantId + "/balance"))
                .andExpect(jsonPath("$.data.balance").value(0.00));
    }

    @Test
    void getOrder_WithValidId_ShouldReturnOrder() throws Exception {
        // Create order
        DirectPurchaseRequest request = DirectPurchaseRequest.builder()
                .sku(TEST_SKU)
                .quantity(1)
                .build();

        String orderResponse = mockMvc.perform(post("/api/v1/users/" + userId + "/orders/direct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(orderResponse).get("data").get("id").asLong();

        // Get order
        mockMvc.perform(get("/api/v1/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(orderId))
                .andExpect(jsonPath("$.data.userId").value(userId));
    }

    @Test
    void getOrder_WithInvalidId_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/orders/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void getUserOrders_ShouldReturnPaginatedOrders() throws Exception {
        // Create multiple orders
        for (int i = 0; i < 3; i++) {
            DirectPurchaseRequest request = DirectPurchaseRequest.builder()
                    .sku(TEST_SKU)
                    .quantity(1)
                    .build();

            mockMvc.perform(post("/api/v1/users/" + userId + "/orders/direct")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Get user orders
        mockMvc.perform(get("/api/v1/users/" + userId + "/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content", hasSize(3)))
                .andExpect(jsonPath("$.data.totalElements").value(3));
    }

    /**
     * Integration test for concurrent purchases
     * Tests that multiple users purchasing the same limited inventory
     * does not result in overselling
     * Validates: Requirements 11.3
     */
    @Test
    void concurrentPurchases_ShouldNotOversellInventory() throws Exception {
        // Setup: Create inventory with limited stock
        final int INVENTORY_QUANTITY = 10;
        final int NUM_CONCURRENT_USERS = 20;
        final String CONCURRENT_SKU = "CONCURRENT-TEST-SKU";

        // Create inventory with limited stock
        Inventory concurrentInventory = Inventory.builder()
                .sku(CONCURRENT_SKU)
                .productId(productId)
                .merchantId(merchantId)
                .quantity(INVENTORY_QUANTITY)
                .price(new BigDecimal("10.00"))
                .build();
        inventoryRepository.save(concurrentInventory);

        // Create multiple users with sufficient balance
        List<Long> userIds = new ArrayList<>();
        for (int i = 0; i < NUM_CONCURRENT_USERS; i++) {
            UserRegisterRequest userRequest = UserRegisterRequest.builder()
                    .username("concurrentuser" + i)
                    .password("password123")
                    .build();

            String userResponse = mockMvc.perform(post("/api/v1/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userRequest)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            Long newUserId = objectMapper.readTree(userResponse).get("data").get("id").asLong();

            // Deposit money
            DepositRequest depositRequest = DepositRequest.builder()
                    .amount(new BigDecimal("100.00"))
                    .build();

            mockMvc.perform(post("/api/v1/users/" + newUserId + "/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(depositRequest)))
                    .andExpect(status().isOk());

            userIds.add(newUserId);
        }

        // Track results
        AtomicInteger successfulPurchases = new AtomicInteger(0);
        AtomicInteger failedPurchases = new AtomicInteger(0);

        // Use CountDownLatch to synchronize concurrent execution
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(NUM_CONCURRENT_USERS);

        ExecutorService executor = Executors.newFixedThreadPool(NUM_CONCURRENT_USERS);

        // Submit concurrent purchase tasks
        for (Long uid : userIds) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    // Create order
                    DirectPurchaseRequest request = DirectPurchaseRequest.builder()
                            .sku(CONCURRENT_SKU)
                            .quantity(1)
                            .build();

                    MvcResult createResult = mockMvc.perform(post("/api/v1/users/" + uid + "/orders/direct")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andReturn();

                    if (createResult.getResponse().getStatus() == 201) {
                        Long orderId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                                .get("data").get("id").asLong();

                        // Try to pay
                        MvcResult payResult = mockMvc.perform(post("/api/v1/orders/" + orderId + "/pay"))
                                .andReturn();

                        if (payResult.getResponse().getStatus() == 200) {
                            successfulPurchases.incrementAndGet();
                        } else {
                            failedPurchases.incrementAndGet();
                        }
                    } else {
                        failedPurchases.incrementAndGet();
                    }
                } catch (Exception e) {
                    failedPurchases.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for all threads to complete
        doneLatch.await();
        executor.shutdown();

        // Verify: successful purchases should not exceed inventory
        assertThat(successfulPurchases.get())
                .as("Successful purchases should not exceed inventory quantity")
                .isLessThanOrEqualTo(INVENTORY_QUANTITY);

        // Verify: final inventory should be non-negative
        Inventory finalInventory = inventoryRepository.findBySku(CONCURRENT_SKU).orElseThrow();
        assertThat(finalInventory.getQuantity())
                .as("Final inventory should be non-negative")
                .isGreaterThanOrEqualTo(0);

        // Verify: inventory consistency
        assertThat(INVENTORY_QUANTITY - successfulPurchases.get())
                .as("Inventory should be consistent with successful purchases")
                .isEqualTo(finalInventory.getQuantity());
    }
}
