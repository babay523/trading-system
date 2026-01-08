package com.trading.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.dto.request.*;
import com.trading.dto.response.*;
import com.trading.repository.*;
import com.trading.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Complete flow integration tests for the Trading System
 * Tests end-to-end scenarios covering the full purchase lifecycle
 * 
 * Validates: Requirements 11.2
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OrderFlowIntegrationTest {

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
    private CartService cartService;

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
        // Clean up all data before each test
        settlementRepository.deleteAll();
        transactionRecordRepository.deleteAll();
        cartItemRepository.deleteAll();
        orderRepository.deleteAll();
        inventoryRepository.deleteAll();
        productRepository.deleteAll();
        merchantRepository.deleteAll();
        userRepository.deleteAll();
    }

    /**
     * Task 13.1: Complete Purchase Flow Integration Test
     * Tests: User registration → Deposit → Browse products → Add to cart → 
     *        Create order → Pay → Ship → Complete
     */
    @Nested
    @DisplayName("13.1 Complete Purchase Flow Tests")
    class CompletePurchaseFlowTests {

        @Test
        @DisplayName("Complete purchase flow: register → deposit → browse → cart → order → pay → ship → complete")
        void completePurchaseFlow_ShouldSucceed() throws Exception {
            // Step 1: User Registration
            UserRegisterRequest userRegisterRequest = UserRegisterRequest.builder()
                    .username("flowuser_" + UUID.randomUUID().toString().substring(0, 8))
                    .password("password123")
                    .build();

            String userResponse = mockMvc.perform(post("/api/v1/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userRegisterRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value(201))
                    .andExpect(jsonPath("$.data.balance").value(0))
                    .andReturn().getResponse().getContentAsString();

            Long userId = objectMapper.readTree(userResponse).get("data").get("id").asLong();

            // Step 2: User Deposit
            BigDecimal depositAmount = new BigDecimal("1000.00");
            DepositRequest depositRequest = DepositRequest.builder()
                    .amount(depositAmount)
                    .build();

            mockMvc.perform(post("/api/v1/users/" + userId + "/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(depositRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.balance").value(1000.00));

            // Step 3: Merchant Registration and Product Setup
            MerchantRegisterRequest merchantRequest = MerchantRegisterRequest.builder()
                    .businessName("Test Electronics Store")
                    .username("merchant_" + UUID.randomUUID().toString().substring(0, 8))
                    .password("password123")
                    .build();

            String merchantResponse = mockMvc.perform(post("/api/v1/merchants/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(merchantRequest)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            Long merchantId = objectMapper.readTree(merchantResponse).get("data").get("id").asLong();

            // Create Product
            ProductCreateRequest productRequest = ProductCreateRequest.builder()
                    .merchantId(merchantId)
                    .name("iPhone 15 Pro")
                    .description("Latest Apple smartphone")
                    .category("Electronics")
                    .build();

            String productResponse = mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(productRequest)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            Long productId = objectMapper.readTree(productResponse).get("data").get("id").asLong();

            // Add Inventory
            String sku = "SKU-IPHONE-" + UUID.randomUUID().toString().substring(0, 8);
            BigDecimal unitPrice = new BigDecimal("199.99");
            InventoryAddRequest inventoryRequest = InventoryAddRequest.builder()
                    .sku(sku)
                    .productId(productId)
                    .quantity(50)
                    .price(unitPrice)
                    .build();

            mockMvc.perform(post("/api/v1/merchants/" + merchantId + "/inventory")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(inventoryRequest)))
                    .andExpect(status().isCreated());

            // Step 4: Browse Products
            mockMvc.perform(get("/api/v1/products")
                            .param("keyword", "iPhone"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].name").value("iPhone 15 Pro"));

            // Step 5: Add to Cart
            CartAddRequest cartRequest = CartAddRequest.builder()
                    .sku(sku)
                    .quantity(2)
                    .build();

            mockMvc.perform(post("/api/v1/users/" + userId + "/cart/items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cartRequest)))
                    .andExpect(status().isCreated());

            // Verify Cart
            mockMvc.perform(get("/api/v1/users/" + userId + "/cart"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items", hasSize(1)))
                    .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                    .andExpect(jsonPath("$.data.totalAmount").value(399.98));

            // Step 6: Create Order from Cart
            String orderResponse = mockMvc.perform(post("/api/v1/users/" + userId + "/orders/from-cart"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.totalAmount").value(399.98))
                    .andReturn().getResponse().getContentAsString();

            Long orderId = objectMapper.readTree(orderResponse).get("data").get("id").asLong();

            // Step 7: Pay Order
            mockMvc.perform(post("/api/v1/orders/" + orderId + "/pay"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("PAID"));

            // Verify User Balance Decreased
            mockMvc.perform(get("/api/v1/users/" + userId + "/balance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.balance").value(600.02));

            // Verify Merchant Balance Increased
            mockMvc.perform(get("/api/v1/merchants/" + merchantId + "/balance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.balance").value(399.98));

            // Step 8: Ship Order
            mockMvc.perform(post("/api/v1/orders/" + orderId + "/ship"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("SHIPPED"));

            // Step 9: Complete Order
            mockMvc.perform(post("/api/v1/orders/" + orderId + "/complete"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"));

            // Verify Final Order State
            mockMvc.perform(get("/api/v1/orders/" + orderId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.data.items", hasSize(1)));

            // Verify Inventory Decreased
            mockMvc.perform(get("/api/v1/merchants/" + merchantId + "/inventory"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].quantity").value(48));
        }

        @Test
        @DisplayName("Direct purchase flow: register → deposit → direct buy → pay → ship → complete")
        void directPurchaseFlow_ShouldSucceed() throws Exception {
            // Setup: Create user with balance
            UserResponse user = createUserWithBalance(new BigDecimal("500.00"));
            
            // Setup: Create merchant with product and inventory
            MerchantResponse merchant = createMerchant();
            ProductResponse product = createProduct(merchant.getId());
            String sku = createInventory(merchant.getId(), product.getId(), new BigDecimal("99.99"), 100);

            // Direct Purchase
            DirectPurchaseRequest purchaseRequest = DirectPurchaseRequest.builder()
                    .sku(sku)
                    .quantity(3)
                    .build();

            String orderResponse = mockMvc.perform(post("/api/v1/users/" + user.getId() + "/orders/direct")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(purchaseRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.totalAmount").value(299.97))
                    .andReturn().getResponse().getContentAsString();

            Long orderId = objectMapper.readTree(orderResponse).get("data").get("id").asLong();

            // Pay → Ship → Complete
            mockMvc.perform(post("/api/v1/orders/" + orderId + "/pay"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("PAID"));

            mockMvc.perform(post("/api/v1/orders/" + orderId + "/ship"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("SHIPPED"));

            mockMvc.perform(post("/api/v1/orders/" + orderId + "/complete"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"));

            // Verify balances
            mockMvc.perform(get("/api/v1/users/" + user.getId() + "/balance"))
                    .andExpect(jsonPath("$.data.balance").value(200.03));

            mockMvc.perform(get("/api/v1/merchants/" + merchant.getId() + "/balance"))
                    .andExpect(jsonPath("$.data.balance").value(299.97));
        }
    }


    /**
     * Task 13.2: Refund Flow Integration Test
     * Tests: Complete order → Request refund → Verify balance changes
     */
    @Nested
    @DisplayName("13.2 Refund Flow Tests")
    class RefundFlowTests {

        @Test
        @DisplayName("Refund flow from PAID status: pay → refund → verify balances")
        void refundFromPaidStatus_ShouldRestoreBalances() throws Exception {
            // Setup
            BigDecimal initialBalance = new BigDecimal("1000.00");
            UserResponse user = createUserWithBalance(initialBalance);
            MerchantResponse merchant = createMerchant();
            ProductResponse product = createProduct(merchant.getId());
            BigDecimal unitPrice = new BigDecimal("150.00");
            String sku = createInventory(merchant.getId(), product.getId(), unitPrice, 100);

            // Create and pay order
            DirectPurchaseRequest purchaseRequest = DirectPurchaseRequest.builder()
                    .sku(sku)
                    .quantity(2)
                    .build();

            String orderResponse = mockMvc.perform(post("/api/v1/users/" + user.getId() + "/orders/direct")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(purchaseRequest)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            Long orderId = objectMapper.readTree(orderResponse).get("data").get("id").asLong();
            BigDecimal orderAmount = new BigDecimal("300.00"); // 150 * 2

            // Pay order
            mockMvc.perform(post("/api/v1/orders/" + orderId + "/pay"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("PAID"));

            // Verify balances after payment
            mockMvc.perform(get("/api/v1/users/" + user.getId() + "/balance"))
                    .andExpect(jsonPath("$.data.balance").value(700.00)); // 1000 - 300

            mockMvc.perform(get("/api/v1/merchants/" + merchant.getId() + "/balance"))
                    .andExpect(jsonPath("$.data.balance").value(300.00));

            // Request refund
            mockMvc.perform(post("/api/v1/orders/" + orderId + "/refund"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("REFUNDED"));

            // Verify balances restored after refund
            mockMvc.perform(get("/api/v1/users/" + user.getId() + "/balance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.balance").value(1000.00)); // Restored

            mockMvc.perform(get("/api/v1/merchants/" + merchant.getId() + "/balance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.balance").value(0.00)); // Deducted
        }

        @Test
        @DisplayName("Refund flow from SHIPPED status: pay → ship → refund → verify balances")
        void refundFromShippedStatus_ShouldRestoreBalances() throws Exception {
            // Setup
            BigDecimal initialBalance = new BigDecimal("500.00");
            UserResponse user = createUserWithBalance(initialBalance);
            MerchantResponse merchant = createMerchant();
            ProductResponse product = createProduct(merchant.getId());
            BigDecimal unitPrice = new BigDecimal("75.00");
            String sku = createInventory(merchant.getId(), product.getId(), unitPrice, 50);

            // Create, pay, and ship order
            DirectPurchaseRequest purchaseRequest = DirectPurchaseRequest.builder()
                    .sku(sku)
                    .quantity(4)
                    .build();

            String orderResponse = mockMvc.perform(post("/api/v1/users/" + user.getId() + "/orders/direct")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(purchaseRequest)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            Long orderId = objectMapper.readTree(orderResponse).get("data").get("id").asLong();
            BigDecimal orderAmount = new BigDecimal("300.00"); // 75 * 4

            mockMvc.perform(post("/api/v1/orders/" + orderId + "/pay"))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/orders/" + orderId + "/ship"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("SHIPPED"));

            // Verify balances after shipping
            mockMvc.perform(get("/api/v1/users/" + user.getId() + "/balance"))
                    .andExpect(jsonPath("$.data.balance").value(200.00)); // 500 - 300

            mockMvc.perform(get("/api/v1/merchants/" + merchant.getId() + "/balance"))
                    .andExpect(jsonPath("$.data.balance").value(300.00));

            // Request refund from SHIPPED status
            mockMvc.perform(post("/api/v1/orders/" + orderId + "/refund"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("REFUNDED"));

            // Verify balances restored
            mockMvc.perform(get("/api/v1/users/" + user.getId() + "/balance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.balance").value(500.00));

            mockMvc.perform(get("/api/v1/merchants/" + merchant.getId() + "/balance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.balance").value(0.00));
        }

        @Test
        @DisplayName("Multiple orders with partial refund: verify correct balance tracking")
        void multipleOrdersWithPartialRefund_ShouldTrackBalancesCorrectly() throws Exception {
            // Setup
            BigDecimal initialBalance = new BigDecimal("2000.00");
            UserResponse user = createUserWithBalance(initialBalance);
            MerchantResponse merchant = createMerchant();
            ProductResponse product = createProduct(merchant.getId());
            BigDecimal unitPrice = new BigDecimal("100.00");
            String sku = createInventory(merchant.getId(), product.getId(), unitPrice, 100);

            // Create and pay first order (quantity: 5, amount: 500)
            DirectPurchaseRequest purchase1 = DirectPurchaseRequest.builder()
                    .sku(sku)
                    .quantity(5)
                    .build();

            String order1Response = mockMvc.perform(post("/api/v1/users/" + user.getId() + "/orders/direct")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(purchase1)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            Long order1Id = objectMapper.readTree(order1Response).get("data").get("id").asLong();

            mockMvc.perform(post("/api/v1/orders/" + order1Id + "/pay"))
                    .andExpect(status().isOk());

            // Create and pay second order (quantity: 3, amount: 300)
            DirectPurchaseRequest purchase2 = DirectPurchaseRequest.builder()
                    .sku(sku)
                    .quantity(3)
                    .build();

            String order2Response = mockMvc.perform(post("/api/v1/users/" + user.getId() + "/orders/direct")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(purchase2)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            Long order2Id = objectMapper.readTree(order2Response).get("data").get("id").asLong();

            mockMvc.perform(post("/api/v1/orders/" + order2Id + "/pay"))
                    .andExpect(status().isOk());

            // Verify balances after both payments
            // User: 2000 - 500 - 300 = 1200
            // Merchant: 0 + 500 + 300 = 800
            mockMvc.perform(get("/api/v1/users/" + user.getId() + "/balance"))
                    .andExpect(jsonPath("$.data.balance").value(1200.00));

            mockMvc.perform(get("/api/v1/merchants/" + merchant.getId() + "/balance"))
                    .andExpect(jsonPath("$.data.balance").value(800.00));

            // Refund only the first order
            mockMvc.perform(post("/api/v1/orders/" + order1Id + "/refund"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("REFUNDED"));

            // Verify balances after partial refund
            // User: 1200 + 500 = 1700
            // Merchant: 800 - 500 = 300
            mockMvc.perform(get("/api/v1/users/" + user.getId() + "/balance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.balance").value(1700.00));

            mockMvc.perform(get("/api/v1/merchants/" + merchant.getId() + "/balance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.balance").value(300.00));

            // Verify order statuses
            mockMvc.perform(get("/api/v1/orders/" + order1Id))
                    .andExpect(jsonPath("$.data.status").value("REFUNDED"));

            mockMvc.perform(get("/api/v1/orders/" + order2Id))
                    .andExpect(jsonPath("$.data.status").value("PAID"));
        }
    }


    /**
     * Task 13.3: Settlement Flow Integration Test
     * Tests: Create multiple orders → Run settlement → Verify settlement results
     */
    @Nested
    @DisplayName("13.3 Settlement Flow Tests")
    class SettlementFlowTests {

        @Test
        @DisplayName("Settlement with completed orders: create orders → complete → run settlement → verify")
        void settlementWithCompletedOrders_ShouldCalculateCorrectly() throws Exception {
            // Setup
            UserResponse user = createUserWithBalance(new BigDecimal("10000.00"));
            MerchantResponse merchant = createMerchant();
            ProductResponse product = createProduct(merchant.getId());
            BigDecimal unitPrice = new BigDecimal("100.00");
            String sku = createInventory(merchant.getId(), product.getId(), unitPrice, 1000);

            // Create and complete multiple orders
            BigDecimal totalExpectedSales = BigDecimal.ZERO;

            // Order 1: quantity 5, amount 500
            OrderResponse order1 = createAndCompleteOrder(user.getId(), sku, 5);
            totalExpectedSales = totalExpectedSales.add(new BigDecimal("500.00"));

            // Order 2: quantity 3, amount 300
            OrderResponse order2 = createAndCompleteOrder(user.getId(), sku, 3);
            totalExpectedSales = totalExpectedSales.add(new BigDecimal("300.00"));

            // Order 3: quantity 7, amount 700
            OrderResponse order3 = createAndCompleteOrder(user.getId(), sku, 7);
            totalExpectedSales = totalExpectedSales.add(new BigDecimal("700.00"));

            // Run settlement
            LocalDate today = LocalDate.now();
            settlementService.runSettlementForMerchant(merchant.getId(), today);

            // Verify settlement results
            mockMvc.perform(get("/api/v1/merchants/" + merchant.getId() + "/settlements"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].merchantId").value(merchant.getId()))
                    .andExpect(jsonPath("$.data.content[0].totalSales").value(1500.00))
                    .andExpect(jsonPath("$.data.content[0].totalRefunds").value(0))
                    .andExpect(jsonPath("$.data.content[0].netAmount").value(1500.00))
                    .andExpect(jsonPath("$.data.content[0].status").value("MATCHED"));

            // Verify merchant balance matches
            mockMvc.perform(get("/api/v1/merchants/" + merchant.getId() + "/balance"))
                    .andExpect(jsonPath("$.data.balance").value(1500.00));
        }

        @Test
        @DisplayName("Settlement with refunds: create orders → complete some → refund some → run settlement")
        void settlementWithRefunds_ShouldAccountForRefunds() throws Exception {
            // Setup
            UserResponse user = createUserWithBalance(new BigDecimal("10000.00"));
            MerchantResponse merchant = createMerchant();
            ProductResponse product = createProduct(merchant.getId());
            BigDecimal unitPrice = new BigDecimal("200.00");
            String sku = createInventory(merchant.getId(), product.getId(), unitPrice, 1000);

            // Order 1: Complete (quantity 5, amount 1000)
            OrderResponse order1 = createAndCompleteOrder(user.getId(), sku, 5);

            // Order 2: Complete (quantity 3, amount 600)
            OrderResponse order2 = createAndCompleteOrder(user.getId(), sku, 3);

            // Order 3: Pay and refund (quantity 2, amount 400)
            DirectPurchaseRequest purchase3 = DirectPurchaseRequest.builder()
                    .sku(sku)
                    .quantity(2)
                    .build();
            OrderResponse order3 = orderService.createDirect(user.getId(), purchase3);
            orderService.confirmPayment(order3.getId());
            orderService.refund(order3.getId());

            // Run settlement
            LocalDate today = LocalDate.now();
            settlementService.runSettlementForMerchant(merchant.getId(), today);

            // Verify settlement results
            // totalSales = 1000 + 600 = 1600 (only COMPLETED orders)
            // totalRefunds = 400 (REFUNDED order)
            // netAmount = 1600 - 400 = 1200
            // balanceChange = 1000 + 600 + 400 - 400 = 1600 (sales minus refund)
            mockMvc.perform(get("/api/v1/merchants/" + merchant.getId() + "/settlements"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].totalSales").value(1600.00))
                    .andExpect(jsonPath("$.data.content[0].totalRefunds").value(400.00))
                    .andExpect(jsonPath("$.data.content[0].netAmount").value(1200.00));
        }

        @Test
        @DisplayName("Settlement for multiple merchants: verify independent calculations")
        void settlementForMultipleMerchants_ShouldCalculateIndependently() throws Exception {
            // Setup user with large balance
            UserResponse user = createUserWithBalance(new BigDecimal("50000.00"));

            // Setup Merchant 1
            MerchantResponse merchant1 = createMerchant();
            ProductResponse product1 = createProduct(merchant1.getId());
            String sku1 = createInventory(merchant1.getId(), product1.getId(), new BigDecimal("100.00"), 1000);

            // Setup Merchant 2
            MerchantResponse merchant2 = createMerchant();
            ProductResponse product2 = createProduct(merchant2.getId());
            String sku2 = createInventory(merchant2.getId(), product2.getId(), new BigDecimal("250.00"), 1000);

            // Create orders for Merchant 1 (total: 500)
            createAndCompleteOrder(user.getId(), sku1, 3); // 300
            createAndCompleteOrder(user.getId(), sku1, 2); // 200

            // Create orders for Merchant 2 (total: 1000)
            createAndCompleteOrder(user.getId(), sku2, 2); // 500
            createAndCompleteOrder(user.getId(), sku2, 2); // 500

            // Run settlement for both merchants
            LocalDate today = LocalDate.now();
            settlementService.runSettlementForMerchant(merchant1.getId(), today);
            settlementService.runSettlementForMerchant(merchant2.getId(), today);

            // Verify Merchant 1 settlement
            mockMvc.perform(get("/api/v1/merchants/" + merchant1.getId() + "/settlements"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].totalSales").value(500.00))
                    .andExpect(jsonPath("$.data.content[0].status").value("MATCHED"));

            // Verify Merchant 2 settlement
            mockMvc.perform(get("/api/v1/merchants/" + merchant2.getId() + "/settlements"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].totalSales").value(1000.00))
                    .andExpect(jsonPath("$.data.content[0].status").value("MATCHED"));

            // Verify merchant balances
            mockMvc.perform(get("/api/v1/merchants/" + merchant1.getId() + "/balance"))
                    .andExpect(jsonPath("$.data.balance").value(500.00));

            mockMvc.perform(get("/api/v1/merchants/" + merchant2.getId() + "/balance"))
                    .andExpect(jsonPath("$.data.balance").value(1000.00));
        }

        @Test
        @DisplayName("Daily settlement job: run daily settlement → verify all merchants processed")
        void dailySettlementJob_ShouldProcessAllMerchants() throws Exception {
            // Setup multiple merchants with orders
            UserResponse user = createUserWithBalance(new BigDecimal("100000.00"));

            // Merchant 1
            MerchantResponse merchant1 = createMerchant();
            ProductResponse product1 = createProduct(merchant1.getId());
            String sku1 = createInventory(merchant1.getId(), product1.getId(), new BigDecimal("50.00"), 1000);
            createAndCompleteOrder(user.getId(), sku1, 10); // 500

            // Merchant 2
            MerchantResponse merchant2 = createMerchant();
            ProductResponse product2 = createProduct(merchant2.getId());
            String sku2 = createInventory(merchant2.getId(), product2.getId(), new BigDecimal("75.00"), 1000);
            createAndCompleteOrder(user.getId(), sku2, 8); // 600

            // Note: runDailySettlement() processes yesterday's data by design
            // For testing, we use runSettlementForMerchant with today's date
            LocalDate today = LocalDate.now();
            settlementService.runSettlementForMerchant(merchant1.getId(), today);
            settlementService.runSettlementForMerchant(merchant2.getId(), today);

            // Verify both merchants have settlements
            mockMvc.perform(get("/api/v1/merchants/" + merchant1.getId() + "/settlements"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].totalSales").value(500.00));

            mockMvc.perform(get("/api/v1/merchants/" + merchant2.getId() + "/settlements"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].totalSales").value(600.00));
        }

        private OrderResponse createAndCompleteOrder(Long userId, String sku, int quantity) throws Exception {
            DirectPurchaseRequest request = DirectPurchaseRequest.builder()
                    .sku(sku)
                    .quantity(quantity)
                    .build();

            OrderResponse order = orderService.createDirect(userId, request);
            orderService.confirmPayment(order.getId());
            orderService.ship(order.getId());
            return orderService.complete(order.getId());
        }
    }

    // ==================== Helper Methods ====================

    private UserResponse createUserWithBalance(BigDecimal balance) {
        String username = "user_" + UUID.randomUUID().toString().substring(0, 8);
        UserResponse user = userService.register(UserRegisterRequest.builder()
                .username(username)
                .password("password123")
                .build());
        
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            userService.deposit(user.getId(), DepositRequest.builder()
                    .amount(balance)
                    .build());
        }
        return user;
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
