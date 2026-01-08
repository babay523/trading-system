package com.trading.property;

import com.trading.dto.request.*;
import com.trading.dto.response.*;
import com.trading.entity.*;
import com.trading.enums.OrderStatus;
import com.trading.exception.InsufficientBalanceException;
import com.trading.exception.InsufficientStockException;
import com.trading.repository.*;
import com.trading.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for Order module
 * Feature: trading-system
 */
@SpringBootTest
@ActiveProfiles("test")
public class OrderPropertyTest {

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
        transactionRecordRepository.deleteAll();
        cartItemRepository.deleteAll();
        orderRepository.deleteAll();
        inventoryRepository.deleteAll();
        productRepository.deleteAll();
        merchantRepository.deleteAll();
        userRepository.deleteAll();
    }

    /**
     * Property 7: Order Total Calculation Correctness
     * For any purchase order with quantity Q and unit price P, the total price SHALL equal Q × P.
     * Validates: Requirements 5.2
     */
    @Test
    void orderTotalShouldEqualQuantityTimesUnitPrice() {
        for (int i = 0; i < 100; i++) {
            // Create user
            UserResponse user = createRandomUser();

            // Create merchant
            MerchantResponse merchant = createRandomMerchant();

            // Create product
            ProductResponse product = createRandomProduct(merchant.getId());

            // Generate random price and quantity
            BigDecimal unitPrice = BigDecimal.valueOf(random.nextDouble() * 999 + 1)
                    .setScale(2, RoundingMode.HALF_UP);
            int quantity = random.nextInt(10) + 1;

            // Create inventory with random price
            String sku = createInventory(merchant.getId(), product.getId(), unitPrice, 1000);

            // Create direct purchase order
            DirectPurchaseRequest request = DirectPurchaseRequest.builder()
                    .sku(sku)
                    .quantity(quantity)
                    .build();

            OrderResponse order = orderService.createDirect(user.getId(), request);

            // Property: total = quantity × unit price
            BigDecimal expectedTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            assertThat(order.getTotalAmount().compareTo(expectedTotal))
                    .as("Order total should equal quantity × unit price")
                    .isEqualTo(0);

            // Verify order item subtotal
            assertThat(order.getItems()).hasSize(1);
            OrderItemResponse item = order.getItems().get(0);
            assertThat(item.getSubtotal().compareTo(expectedTotal))
                    .as("Order item subtotal should equal quantity × unit price")
                    .isEqualTo(0);
        }
    }

    /**
     * Property 8: Purchase Transaction Atomicity
     * For any successful purchase order with total amount T:
     * - The user's balance SHALL decrease by exactly T
     * - The merchant's balance SHALL increase by exactly T
     * - The inventory quantity SHALL decrease by exactly the ordered quantity
     * Validates: Requirements 5.4, 5.5, 5.6, 5.11
     */
    @Test
    void purchaseTransactionShouldBeAtomic() {
        for (int i = 0; i < 100; i++) {
            // Create user with sufficient balance
            UserResponse user = createRandomUser();
            BigDecimal depositAmount = BigDecimal.valueOf(random.nextDouble() * 9000 + 1000)
                    .setScale(2, RoundingMode.HALF_UP);
            userService.deposit(user.getId(), DepositRequest.builder().amount(depositAmount).build());

            // Create merchant
            MerchantResponse merchant = createRandomMerchant();

            // Create product
            ProductResponse product = createRandomProduct(merchant.getId());

            // Generate random price and quantity
            BigDecimal unitPrice = BigDecimal.valueOf(random.nextDouble() * 99 + 1)
                    .setScale(2, RoundingMode.HALF_UP);
            int orderQuantity = random.nextInt(5) + 1;
            int initialStock = orderQuantity + random.nextInt(100) + 10;

            // Create inventory
            String sku = createInventory(merchant.getId(), product.getId(), unitPrice, initialStock);

            // Get initial states
            BigDecimal userBalanceBefore = userService.getBalance(user.getId()).getBalance();
            BigDecimal merchantBalanceBefore = merchantService.getBalance(merchant.getId()).getBalance();
            Inventory inventoryBefore = inventoryRepository.findBySku(sku).orElseThrow();
            int stockBefore = inventoryBefore.getQuantity();

            // Create and pay order
            DirectPurchaseRequest request = DirectPurchaseRequest.builder()
                    .sku(sku)
                    .quantity(orderQuantity)
                    .build();
            OrderResponse order = orderService.createDirect(user.getId(), request);
            OrderResponse paidOrder = orderService.confirmPayment(order.getId());

            // Get final states
            BigDecimal userBalanceAfter = userService.getBalance(user.getId()).getBalance();
            BigDecimal merchantBalanceAfter = merchantService.getBalance(merchant.getId()).getBalance();
            Inventory inventoryAfter = inventoryRepository.findBySku(sku).orElseThrow();
            int stockAfter = inventoryAfter.getQuantity();

            BigDecimal totalAmount = order.getTotalAmount();

            // Property: user balance decreased by exactly T
            assertThat(userBalanceBefore.subtract(userBalanceAfter).compareTo(totalAmount))
                    .as("User balance should decrease by order total")
                    .isEqualTo(0);

            // Property: merchant balance increased by exactly T
            assertThat(merchantBalanceAfter.subtract(merchantBalanceBefore).compareTo(totalAmount))
                    .as("Merchant balance should increase by order total")
                    .isEqualTo(0);

            // Property: inventory decreased by exactly ordered quantity
            assertThat(stockBefore - stockAfter)
                    .as("Inventory should decrease by ordered quantity")
                    .isEqualTo(orderQuantity);

            // Property: order status is PAID
            assertThat(paidOrder.getStatus())
                    .as("Order status should be PAID after payment")
                    .isEqualTo(OrderStatus.PAID);
        }
    }

    /**
     * Property 9: Insufficient Balance Rejection
     * For any purchase order where the total amount T exceeds the user's balance B (T > B),
     * the order SHALL be rejected and no balance or inventory changes SHALL occur.
     * Validates: Requirements 5.8
     */
    @Test
    void insufficientBalanceShouldRejectOrder() {
        for (int i = 0; i < 100; i++) {
            // Create user with limited balance
            UserResponse user = createRandomUser();
            BigDecimal userBalance = BigDecimal.valueOf(random.nextDouble() * 50 + 10)
                    .setScale(2, RoundingMode.HALF_UP);
            userService.deposit(user.getId(), DepositRequest.builder().amount(userBalance).build());

            // Create merchant
            MerchantResponse merchant = createRandomMerchant();

            // Create product
            ProductResponse product = createRandomProduct(merchant.getId());

            // Create inventory with price higher than user balance
            BigDecimal unitPrice = userBalance.add(BigDecimal.valueOf(random.nextDouble() * 100 + 1))
                    .setScale(2, RoundingMode.HALF_UP);
            int initialStock = 100;
            String sku = createInventory(merchant.getId(), product.getId(), unitPrice, initialStock);

            // Get initial states
            BigDecimal userBalanceBefore = userService.getBalance(user.getId()).getBalance();
            BigDecimal merchantBalanceBefore = merchantService.getBalance(merchant.getId()).getBalance();
            int stockBefore = inventoryRepository.findBySku(sku).orElseThrow().getQuantity();

            // Create order
            DirectPurchaseRequest request = DirectPurchaseRequest.builder()
                    .sku(sku)
                    .quantity(1)
                    .build();
            OrderResponse order = orderService.createDirect(user.getId(), request);

            // Property: payment should be rejected
            assertThatThrownBy(() -> orderService.confirmPayment(order.getId()))
                    .as("Payment should be rejected when balance is insufficient")
                    .isInstanceOf(InsufficientBalanceException.class);

            // Property: user balance unchanged
            BigDecimal userBalanceAfter = userService.getBalance(user.getId()).getBalance();
            assertThat(userBalanceAfter.compareTo(userBalanceBefore))
                    .as("User balance should remain unchanged")
                    .isEqualTo(0);

            // Property: merchant balance unchanged
            BigDecimal merchantBalanceAfter = merchantService.getBalance(merchant.getId()).getBalance();
            assertThat(merchantBalanceAfter.compareTo(merchantBalanceBefore))
                    .as("Merchant balance should remain unchanged")
                    .isEqualTo(0);

            // Property: inventory unchanged
            int stockAfter = inventoryRepository.findBySku(sku).orElseThrow().getQuantity();
            assertThat(stockAfter)
                    .as("Inventory should remain unchanged")
                    .isEqualTo(stockBefore);
        }
    }

    /**
     * Property 10: Insufficient Stock Rejection
     * For any purchase order where the ordered quantity Q exceeds the available inventory I (Q > I),
     * the order SHALL be rejected and no balance or inventory changes SHALL occur.
     * Validates: Requirements 5.9
     */
    @Test
    void insufficientStockShouldRejectOrder() {
        for (int i = 0; i < 100; i++) {
            // Create user with sufficient balance
            UserResponse user = createRandomUser();
            userService.deposit(user.getId(), DepositRequest.builder()
                    .amount(BigDecimal.valueOf(10000)).build());

            // Create merchant
            MerchantResponse merchant = createRandomMerchant();

            // Create product
            ProductResponse product = createRandomProduct(merchant.getId());

            // Create inventory with limited stock
            BigDecimal unitPrice = BigDecimal.valueOf(10);
            int initialStock = random.nextInt(5) + 1;
            String sku = createInventory(merchant.getId(), product.getId(), unitPrice, initialStock);

            // Order quantity exceeds stock
            int orderQuantity = initialStock + random.nextInt(10) + 1;

            // Get initial states
            BigDecimal userBalanceBefore = userService.getBalance(user.getId()).getBalance();
            BigDecimal merchantBalanceBefore = merchantService.getBalance(merchant.getId()).getBalance();
            int stockBefore = inventoryRepository.findBySku(sku).orElseThrow().getQuantity();

            // Create order
            DirectPurchaseRequest request = DirectPurchaseRequest.builder()
                    .sku(sku)
                    .quantity(orderQuantity)
                    .build();
            OrderResponse order = orderService.createDirect(user.getId(), request);

            // Property: payment should be rejected
            assertThatThrownBy(() -> orderService.confirmPayment(order.getId()))
                    .as("Payment should be rejected when stock is insufficient")
                    .isInstanceOf(InsufficientStockException.class);

            // Property: user balance unchanged
            BigDecimal userBalanceAfter = userService.getBalance(user.getId()).getBalance();
            assertThat(userBalanceAfter.compareTo(userBalanceBefore))
                    .as("User balance should remain unchanged")
                    .isEqualTo(0);

            // Property: merchant balance unchanged
            BigDecimal merchantBalanceAfter = merchantService.getBalance(merchant.getId()).getBalance();
            assertThat(merchantBalanceAfter.compareTo(merchantBalanceBefore))
                    .as("Merchant balance should remain unchanged")
                    .isEqualTo(0);

            // Property: inventory unchanged
            int stockAfter = inventoryRepository.findBySku(sku).orElseThrow().getQuantity();
            assertThat(stockAfter)
                    .as("Inventory should remain unchanged")
                    .isEqualTo(stockBefore);
        }
    }

    /**
     * Property 11: Refund Transaction Correctness
     * For any refund operation on an order with amount T:
     * - The user's balance SHALL increase by exactly T
     * - The merchant's balance SHALL decrease by exactly T
     * Validates: Requirements 6.7
     */
    @Test
    void refundTransactionShouldBeCorrect() {
        for (int i = 0; i < 100; i++) {
            // Create user with sufficient balance
            UserResponse user = createRandomUser();
            userService.deposit(user.getId(), DepositRequest.builder()
                    .amount(BigDecimal.valueOf(10000)).build());

            // Create merchant
            MerchantResponse merchant = createRandomMerchant();

            // Create product
            ProductResponse product = createRandomProduct(merchant.getId());

            // Create inventory
            BigDecimal unitPrice = BigDecimal.valueOf(random.nextDouble() * 99 + 1)
                    .setScale(2, RoundingMode.HALF_UP);
            int quantity = random.nextInt(5) + 1;
            String sku = createInventory(merchant.getId(), product.getId(), unitPrice, 1000);

            // Create and pay order
            DirectPurchaseRequest request = DirectPurchaseRequest.builder()
                    .sku(sku)
                    .quantity(quantity)
                    .build();
            OrderResponse order = orderService.createDirect(user.getId(), request);
            orderService.confirmPayment(order.getId());

            // Get balances after payment (before refund)
            BigDecimal userBalanceBeforeRefund = userService.getBalance(user.getId()).getBalance();
            BigDecimal merchantBalanceBeforeRefund = merchantService.getBalance(merchant.getId()).getBalance();

            // Refund order
            OrderResponse refundedOrder = orderService.refund(order.getId());
            BigDecimal refundAmount = order.getTotalAmount();

            // Get balances after refund
            BigDecimal userBalanceAfterRefund = userService.getBalance(user.getId()).getBalance();
            BigDecimal merchantBalanceAfterRefund = merchantService.getBalance(merchant.getId()).getBalance();

            // Property: user balance increased by exactly T
            assertThat(userBalanceAfterRefund.subtract(userBalanceBeforeRefund).compareTo(refundAmount))
                    .as("User balance should increase by refund amount")
                    .isEqualTo(0);

            // Property: merchant balance decreased by exactly T
            assertThat(merchantBalanceBeforeRefund.subtract(merchantBalanceAfterRefund).compareTo(refundAmount))
                    .as("Merchant balance should decrease by refund amount")
                    .isEqualTo(0);

            // Property: order status is REFUNDED
            assertThat(refundedOrder.getStatus())
                    .as("Order status should be REFUNDED")
                    .isEqualTo(OrderStatus.REFUNDED);
        }
    }

    /**
     * Property 15: Transaction Record Completeness
     * For any successful financial operation (deposit, purchase, refund),
     * a corresponding transaction record SHALL be created with correct type, amount, and timestamps.
     * Validates: Requirements 1.7
     */
    @Test
    void transactionRecordsShouldBeComplete() {
        for (int i = 0; i < 100; i++) {
            // Create user and deposit
            UserResponse user = createRandomUser();
            BigDecimal depositAmount = BigDecimal.valueOf(random.nextDouble() * 9000 + 1000)
                    .setScale(2, RoundingMode.HALF_UP);
            userService.deposit(user.getId(), DepositRequest.builder().amount(depositAmount).build());

            // Create merchant
            MerchantResponse merchant = createRandomMerchant();

            // Create product and inventory
            ProductResponse product = createRandomProduct(merchant.getId());
            BigDecimal unitPrice = BigDecimal.valueOf(random.nextDouble() * 99 + 1)
                    .setScale(2, RoundingMode.HALF_UP);
            int quantity = random.nextInt(5) + 1;
            String sku = createInventory(merchant.getId(), product.getId(), unitPrice, 1000);

            // Create and pay order
            DirectPurchaseRequest request = DirectPurchaseRequest.builder()
                    .sku(sku)
                    .quantity(quantity)
                    .build();
            OrderResponse order = orderService.createDirect(user.getId(), request);
            orderService.confirmPayment(order.getId());

            // Verify transaction records exist for the order
            var records = transactionRecordRepository.findByRelatedOrderId(order.getId());

            // Property: at least 2 transaction records (user purchase + merchant sale)
            assertThat(records.size())
                    .as("Should have transaction records for purchase")
                    .isGreaterThanOrEqualTo(2);

            // Property: records have correct amounts
            BigDecimal orderTotal = order.getTotalAmount();
            boolean hasUserPurchase = records.stream()
                    .anyMatch(r -> r.getAccountType().equals("USER") && 
                            r.getAmount().compareTo(orderTotal) == 0);
            boolean hasMerchantSale = records.stream()
                    .anyMatch(r -> r.getAccountType().equals("MERCHANT") && 
                            r.getAmount().compareTo(orderTotal) == 0);

            assertThat(hasUserPurchase)
                    .as("Should have user purchase transaction record")
                    .isTrue();
            assertThat(hasMerchantSale)
                    .as("Should have merchant sale transaction record")
                    .isTrue();
        }
    }

    /**
     * Property 13: Concurrent Purchase Safety
     * For any inventory with quantity Q, when N concurrent purchase requests each requesting quantity 1
     * are processed, the total successful purchases SHALL NOT exceed Q.
     * Validates: Requirements 9.4
     */
    @Test
    void concurrentPurchasesShouldNotExceedInventory() throws InterruptedException {
        for (int iteration = 0; iteration < 100; iteration++) {
            // Setup: Create merchant and product
            MerchantResponse merchant = createRandomMerchant();
            ProductResponse product = createRandomProduct(merchant.getId());

            // Random inventory quantity between 5 and 15
            int inventoryQuantity = random.nextInt(11) + 5;
            BigDecimal unitPrice = BigDecimal.valueOf(10);
            String sku = createInventory(merchant.getId(), product.getId(), unitPrice, inventoryQuantity);

            // Number of concurrent users (more than inventory to test contention)
            int numConcurrentUsers = inventoryQuantity + random.nextInt(10) + 5;

            // Create users with sufficient balance
            List<UserResponse> users = new ArrayList<>();
            for (int i = 0; i < numConcurrentUsers; i++) {
                UserResponse user = createRandomUser();
                userService.deposit(user.getId(), DepositRequest.builder()
                        .amount(BigDecimal.valueOf(1000)).build());
                users.add(user);
            }

            // Track successful purchases
            AtomicInteger successfulPurchases = new AtomicInteger(0);
            AtomicInteger failedDueToStock = new AtomicInteger(0);
            AtomicInteger otherFailures = new AtomicInteger(0);

            // Use CountDownLatch to synchronize concurrent execution
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(numConcurrentUsers);

            ExecutorService executor = Executors.newFixedThreadPool(Math.min(numConcurrentUsers, 20));

            // Submit concurrent purchase tasks
            for (UserResponse user : users) {
                executor.submit(() -> {
                    try {
                        startLatch.await(); // Wait for all threads to be ready

                        // Create and pay order
                        DirectPurchaseRequest request = DirectPurchaseRequest.builder()
                                .sku(sku)
                                .quantity(1)
                                .build();
                        OrderResponse order = orderService.createDirect(user.getId(), request);
                        orderService.confirmPayment(order.getId());
                        successfulPurchases.incrementAndGet();
                    } catch (InsufficientStockException e) {
                        failedDueToStock.incrementAndGet();
                    } catch (Exception e) {
                        // Other exceptions (e.g., optimistic locking) count as failures
                        otherFailures.incrementAndGet();
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

            // Property: successful purchases should not exceed inventory quantity
            assertThat(successfulPurchases.get())
                    .as("Iteration %d: Successful purchases (%d) should not exceed inventory quantity (%d)",
                            iteration, successfulPurchases.get(), inventoryQuantity)
                    .isLessThanOrEqualTo(inventoryQuantity);

            // Verify final inventory is non-negative
            Inventory finalInventory = inventoryRepository.findBySku(sku).orElseThrow();
            assertThat(finalInventory.getQuantity())
                    .as("Iteration %d: Final inventory should be non-negative", iteration)
                    .isGreaterThanOrEqualTo(0);

            // Verify inventory consistency: initial - successful = final
            assertThat(inventoryQuantity - successfulPurchases.get())
                    .as("Iteration %d: Inventory should be consistent", iteration)
                    .isEqualTo(finalInventory.getQuantity());
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
