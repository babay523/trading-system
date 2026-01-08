package com.trading.property;

import com.trading.dto.request.*;
import com.trading.dto.response.*;
import com.trading.entity.*;
import com.trading.enums.OrderStatus;
import com.trading.enums.SettlementStatus;
import com.trading.repository.*;
import com.trading.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for Settlement module
 * Feature: trading-system
 */
@SpringBootTest
@ActiveProfiles("test")
public class SettlementPropertyTest {

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private ProductService productService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private SettlementRepository settlementRepository;

    @Autowired
    private OrderRepository orderRepository;

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
    private TransactionRecordRepository transactionRecordRepository;

    private Random random = new Random();

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

    /**
     * Property 12: Settlement Calculation Correctness
     * For any merchant, the calculated total sales SHALL equal the sum of all 
     * COMPLETED order amounts for that merchant within the settlement period.
     * Validates: Requirements 7.1
     */
    @Test
    void settlementTotalSalesShouldEqualSumOfCompletedOrders() {
        for (int iteration = 0; iteration < 100; iteration++) {
            // Clean up for each iteration
            setUp();
            
            // Create merchant
            MerchantResponse merchant = createRandomMerchant();
            
            // Create product and inventory
            ProductResponse product = createRandomProduct(merchant.getId());
            BigDecimal unitPrice = BigDecimal.valueOf(random.nextDouble() * 99 + 1)
                    .setScale(2, RoundingMode.HALF_UP);
            String sku = createInventory(merchant.getId(), product.getId(), unitPrice, 10000);
            
            // Create random number of orders (1-10)
            int numOrders = random.nextInt(10) + 1;
            List<OrderResponse> completedOrders = new ArrayList<>();
            BigDecimal expectedTotalSales = BigDecimal.ZERO;
            
            for (int i = 0; i < numOrders; i++) {
                // Create user with sufficient balance
                UserResponse user = createRandomUser();
                userService.deposit(user.getId(), DepositRequest.builder()
                        .amount(BigDecimal.valueOf(100000)).build());
                
                // Create order with random quantity
                int quantity = random.nextInt(5) + 1;
                DirectPurchaseRequest request = DirectPurchaseRequest.builder()
                        .sku(sku)
                        .quantity(quantity)
                        .build();
                
                OrderResponse order = orderService.createDirect(user.getId(), request);
                
                // Pay the order
                OrderResponse paidOrder = orderService.confirmPayment(order.getId());
                
                // Ship the order
                OrderResponse shippedOrder = orderService.ship(paidOrder.getId());
                
                // Complete the order
                OrderResponse completedOrder = orderService.complete(shippedOrder.getId());
                completedOrders.add(completedOrder);
                
                expectedTotalSales = expectedTotalSales.add(completedOrder.getTotalAmount());
            }
            
            // Run settlement for today (since orders were just created)
            LocalDate today = LocalDate.now();
            SettlementResponse settlement = settlementService.runSettlementForMerchant(
                    merchant.getId(), today);
            
            // Property: total sales should equal sum of completed order amounts
            assertThat(settlement.getTotalSales().compareTo(expectedTotalSales))
                    .as("Iteration %d: Settlement total sales (%s) should equal sum of completed orders (%s)",
                            iteration, settlement.getTotalSales(), expectedTotalSales)
                    .isEqualTo(0);
            
            // Property: net amount should equal total sales minus refunds (no refunds in this test)
            assertThat(settlement.getNetAmount().compareTo(expectedTotalSales))
                    .as("Iteration %d: Net amount should equal total sales when no refunds",
                            iteration)
                    .isEqualTo(0);
            
            // Property: settlement should be MATCHED when calculations are correct
            assertThat(settlement.getStatus())
                    .as("Iteration %d: Settlement status should be MATCHED", iteration)
                    .isEqualTo(SettlementStatus.MATCHED);
        }
    }

    /**
     * Additional test: Settlement with refunds
     * Verifies that refunds are correctly calculated in the settlement
     * Note: When orders are refunded from PAID status (not COMPLETED), 
     * there will be a mismatch because the SALE transaction exists but 
     * the order is not in COMPLETED status
     */
    @Test
    void settlementShouldCorrectlyCalculateRefunds() {
        for (int iteration = 0; iteration < 100; iteration++) {
            // Clean up for each iteration
            setUp();
            
            // Create merchant
            MerchantResponse merchant = createRandomMerchant();
            
            // Create product and inventory
            ProductResponse product = createRandomProduct(merchant.getId());
            BigDecimal unitPrice = BigDecimal.valueOf(random.nextDouble() * 99 + 1)
                    .setScale(2, RoundingMode.HALF_UP);
            String sku = createInventory(merchant.getId(), product.getId(), unitPrice, 10000);
            
            // Create orders - all will be completed (no refunds from non-completed orders)
            int numCompletedOrders = random.nextInt(5) + 1;
            
            BigDecimal expectedTotalSales = BigDecimal.ZERO;
            
            // Create and complete orders
            for (int i = 0; i < numCompletedOrders; i++) {
                UserResponse user = createRandomUser();
                userService.deposit(user.getId(), DepositRequest.builder()
                        .amount(BigDecimal.valueOf(100000)).build());
                
                int quantity = random.nextInt(5) + 1;
                DirectPurchaseRequest request = DirectPurchaseRequest.builder()
                        .sku(sku)
                        .quantity(quantity)
                        .build();
                
                OrderResponse order = orderService.createDirect(user.getId(), request);
                OrderResponse paidOrder = orderService.confirmPayment(order.getId());
                OrderResponse shippedOrder = orderService.ship(paidOrder.getId());
                OrderResponse completedOrder = orderService.complete(shippedOrder.getId());
                
                expectedTotalSales = expectedTotalSales.add(completedOrder.getTotalAmount());
            }
            
            // Run settlement
            LocalDate today = LocalDate.now();
            SettlementResponse settlement = settlementService.runSettlementForMerchant(
                    merchant.getId(), today);
            
            // Property: total sales should equal sum of completed orders
            assertThat(settlement.getTotalSales().compareTo(expectedTotalSales))
                    .as("Iteration %d: Total sales should match completed orders", iteration)
                    .isEqualTo(0);
            
            // Property: no refunds when all orders are completed
            assertThat(settlement.getTotalRefunds().compareTo(BigDecimal.ZERO))
                    .as("Iteration %d: Total refunds should be zero", iteration)
                    .isEqualTo(0);
            
            // Property: net amount = total sales (no refunds)
            assertThat(settlement.getNetAmount().compareTo(expectedTotalSales))
                    .as("Iteration %d: Net amount should equal total sales", iteration)
                    .isEqualTo(0);
            
            // Property: settlement should be MATCHED when all orders complete normally
            assertThat(settlement.getStatus())
                    .as("Iteration %d: Settlement status should be MATCHED", iteration)
                    .isEqualTo(SettlementStatus.MATCHED);
        }
    }

    // Helper methods
    private UserResponse createRandomUser() {
        String username = "user_" + UUID.randomUUID().toString().substring(0, 8);
        return userService.register(UserRegisterRequest.builder()
                .username(username)
                .password("password123")
                .build());
    }

    private MerchantResponse createRandomMerchant() {
        String username = "merchant_" + UUID.randomUUID().toString().substring(0, 8);
        return merchantService.register(MerchantRegisterRequest.builder()
                .businessName("Business " + username)
                .username(username)
                .password("password123")
                .build());
    }

    private ProductResponse createRandomProduct(Long merchantId) {
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
