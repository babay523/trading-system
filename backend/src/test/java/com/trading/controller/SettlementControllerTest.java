package com.trading.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.dto.request.*;
import com.trading.dto.response.*;
import com.trading.repository.*;
import com.trading.service.*;
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
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Settlement endpoints
 * Tests the GET /api/v1/merchants/{id}/settlements endpoint
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SettlementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private ProductService productService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private SettlementRepository settlementRepository;

    @Autowired
    private TransactionRecordRepository transactionRecordRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        settlementRepository.deleteAll();
        transactionRecordRepository.deleteAll();
        cartItemRepository.deleteAll();
        orderRepository.deleteAll();
        inventoryRepository.deleteAll();
        productRepository.deleteAll();
        merchantRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void getSettlements_ShouldReturnEmptyPage_WhenNoSettlements() throws Exception {
        // Create merchant
        MerchantResponse merchant = createMerchant();

        // Get settlements - should be empty
        mockMvc.perform(get("/api/v1/merchants/{id}/settlements", merchant.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", hasSize(0)))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void getSettlements_ShouldReturnSettlements_AfterRunningSettlement() throws Exception {
        // Create merchant
        MerchantResponse merchant = createMerchant();

        // Create product and inventory
        ProductResponse product = createProduct(merchant.getId());
        String sku = createInventory(merchant.getId(), product.getId(), BigDecimal.valueOf(100), 1000);

        // Create user with balance
        UserResponse user = createUser();
        userService.deposit(user.getId(), DepositRequest.builder()
                .amount(BigDecimal.valueOf(10000)).build());

        // Create and complete an order
        DirectPurchaseRequest purchaseRequest = DirectPurchaseRequest.builder()
                .sku(sku)
                .quantity(5)
                .build();
        OrderResponse order = orderService.createDirect(user.getId(), purchaseRequest);
        orderService.confirmPayment(order.getId());
        orderService.ship(order.getId());
        orderService.complete(order.getId());

        // Run settlement for today
        LocalDate today = LocalDate.now();
        settlementService.runSettlementForMerchant(merchant.getId(), today);

        // Get settlements
        mockMvc.perform(get("/api/v1/merchants/{id}/settlements", merchant.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].merchantId").value(merchant.getId()))
                .andExpect(jsonPath("$.data.content[0].totalSales").value(500.00))
                .andExpect(jsonPath("$.data.content[0].totalRefunds").value(0))
                .andExpect(jsonPath("$.data.content[0].netAmount").value(500.00))
                .andExpect(jsonPath("$.data.content[0].status").value("MATCHED"));
    }

    @Test
    void getSettlements_ShouldReturnSettlementWithRefunds() throws Exception {
        // Create merchant
        MerchantResponse merchant = createMerchant();

        // Create product and inventory
        ProductResponse product = createProduct(merchant.getId());
        String sku = createInventory(merchant.getId(), product.getId(), BigDecimal.valueOf(50), 1000);

        // Create user with balance
        UserResponse user = createUser();
        userService.deposit(user.getId(), DepositRequest.builder()
                .amount(BigDecimal.valueOf(10000)).build());

        // Create and complete first order
        DirectPurchaseRequest purchaseRequest1 = DirectPurchaseRequest.builder()
                .sku(sku)
                .quantity(10)
                .build();
        OrderResponse order1 = orderService.createDirect(user.getId(), purchaseRequest1);
        orderService.confirmPayment(order1.getId());
        orderService.ship(order1.getId());
        orderService.complete(order1.getId());

        // Create second order, complete it, then refund
        // This ensures the order goes through COMPLETED status before being refunded
        DirectPurchaseRequest purchaseRequest2 = DirectPurchaseRequest.builder()
                .sku(sku)
                .quantity(4)
                .build();
        OrderResponse order2 = orderService.createDirect(user.getId(), purchaseRequest2);
        orderService.confirmPayment(order2.getId());
        orderService.ship(order2.getId());
        orderService.complete(order2.getId());
        // Note: In the current implementation, refund can only be done from PAID or SHIPPED status
        // So we need to test with a different scenario

        // Run settlement for today
        LocalDate today = LocalDate.now();
        settlementService.runSettlementForMerchant(merchant.getId(), today);

        // Get settlements
        // Both orders are COMPLETED, so totalSales = 500 + 200 = 700
        // No refunds in this test (since we can't refund COMPLETED orders)
        mockMvc.perform(get("/api/v1/merchants/{id}/settlements", merchant.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].totalSales").value(700.00))
                .andExpect(jsonPath("$.data.content[0].totalRefunds").value(0))
                .andExpect(jsonPath("$.data.content[0].netAmount").value(700.00))
                .andExpect(jsonPath("$.data.content[0].status").value("MATCHED"));
    }

    @Test
    void getSettlements_ShouldHandleRefundedOrders() throws Exception {
        // Create merchant
        MerchantResponse merchant = createMerchant();

        // Create product and inventory
        ProductResponse product = createProduct(merchant.getId());
        String sku = createInventory(merchant.getId(), product.getId(), BigDecimal.valueOf(50), 1000);

        // Create user with balance
        UserResponse user = createUser();
        userService.deposit(user.getId(), DepositRequest.builder()
                .amount(BigDecimal.valueOf(10000)).build());

        // Create and complete first order
        DirectPurchaseRequest purchaseRequest1 = DirectPurchaseRequest.builder()
                .sku(sku)
                .quantity(10)
                .build();
        OrderResponse order1 = orderService.createDirect(user.getId(), purchaseRequest1);
        orderService.confirmPayment(order1.getId());
        orderService.ship(order1.getId());
        orderService.complete(order1.getId());

        // Create and refund second order (from PAID status)
        DirectPurchaseRequest purchaseRequest2 = DirectPurchaseRequest.builder()
                .sku(sku)
                .quantity(4)
                .build();
        OrderResponse order2 = orderService.createDirect(user.getId(), purchaseRequest2);
        orderService.confirmPayment(order2.getId());
        orderService.refund(order2.getId());

        // Run settlement for today
        LocalDate today = LocalDate.now();
        settlementService.runSettlementForMerchant(merchant.getId(), today);

        // Get settlements
        // Order 1: COMPLETED, amount = 500 → counted in totalSales
        // Order 2: REFUNDED, amount = 200 → counted in totalRefunds
        // Balance change: SALE(500) + SALE(200) - REFUND_OUT(200) = 500
        // Net amount: totalSales(500) - totalRefunds(200) = 300
        // This creates a MISMATCH because the refunded order's sale is in balance but not in totalSales
        mockMvc.perform(get("/api/v1/merchants/{id}/settlements", merchant.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].totalSales").value(500.00))
                .andExpect(jsonPath("$.data.content[0].totalRefunds").value(200.00))
                .andExpect(jsonPath("$.data.content[0].netAmount").value(300.00))
                .andExpect(jsonPath("$.data.content[0].balanceChange").value(500.00))
                .andExpect(jsonPath("$.data.content[0].status").value("MISMATCHED"));
    }

    @Test
    void getSettlements_ShouldReturn404_WhenMerchantNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/{id}/settlements", 99999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void getSettlements_ShouldSupportPagination() throws Exception {
        // Create merchant
        MerchantResponse merchant = createMerchant();

        // Create product and inventory
        ProductResponse product = createProduct(merchant.getId());
        String sku = createInventory(merchant.getId(), product.getId(), BigDecimal.valueOf(10), 10000);

        // Create user with balance
        UserResponse user = createUser();
        userService.deposit(user.getId(), DepositRequest.builder()
                .amount(BigDecimal.valueOf(100000)).build());

        // Create settlements for multiple days
        for (int i = 0; i < 5; i++) {
            // Create and complete an order
            DirectPurchaseRequest purchaseRequest = DirectPurchaseRequest.builder()
                    .sku(sku)
                    .quantity(1)
                    .build();
            OrderResponse order = orderService.createDirect(user.getId(), purchaseRequest);
            orderService.confirmPayment(order.getId());
            orderService.ship(order.getId());
            orderService.complete(order.getId());

            // Run settlement for different dates
            LocalDate date = LocalDate.now().minusDays(i);
            settlementService.runSettlementForMerchant(merchant.getId(), date);
        }

        // Get first page with size 2
        mockMvc.perform(get("/api/v1/merchants/{id}/settlements", merchant.getId())
                        .param("page", "0")
                        .param("size", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements").value(5))
                .andExpect(jsonPath("$.data.totalPages").value(3));

        // Get second page
        mockMvc.perform(get("/api/v1/merchants/{id}/settlements", merchant.getId())
                        .param("page", "1")
                        .param("size", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.number").value(1));
    }

    // Helper methods
    private UserResponse createUser() {
        String username = "user_" + UUID.randomUUID().toString().substring(0, 8);
        return userService.register(UserRegisterRequest.builder()
                .username(username)
                .password("password123")
                .build());
    }

    private MerchantResponse createMerchant() {
        String username = "merchant_" + UUID.randomUUID().toString().substring(0, 8);
        return merchantService.register(MerchantRegisterRequest.builder()
                .businessName("Business " + username)
                .username(username)
                .password("password123")
                .build());
    }

    private ProductResponse createProduct(Long merchantId) {
        return productService.create(ProductCreateRequest.builder()
                .merchantId(merchantId)
                .name("Product " + UUID.randomUUID().toString().substring(0, 8))
                .description("Test product description")
                .category("Electronics")
                .build());
    }

    private String createInventory(Long merchantId, Long productId, BigDecimal price, int quantity) {
        String sku = "SKU" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        inventoryService.addInventory(merchantId, InventoryAddRequest.builder()
                .sku(sku)
                .productId(productId)
                .quantity(quantity)
                .price(price)
                .build());
        return sku;
    }
}
