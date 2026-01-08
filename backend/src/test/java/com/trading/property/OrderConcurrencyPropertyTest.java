package com.trading.property;

import com.trading.dto.request.DirectPurchaseRequest;
import com.trading.dto.response.OrderResponse;
import com.trading.entity.*;
import com.trading.enums.OrderStatus;
import com.trading.exception.ConcurrencyException;
import com.trading.exception.InsufficientStockException;
import com.trading.repository.*;
import com.trading.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for Order Concurrency
 * Feature: inventory-concurrency-fix
 * 
 * Note: Since jqwik doesn't integrate well with Spring's dependency injection,
 * we use JUnit 5 tests with randomized inputs to achieve property-based testing.
 */
@SpringBootTest
@ActiveProfiles("test")
public class OrderConcurrencyPropertyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TransactionRecordRepository transactionRecordRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @BeforeEach
    void setUp() {
        cleanupDatabase();
    }

    /**
     * Property 1: No Overselling Under Concurrent Load
     * For any inventory item with quantity Q, if N concurrent purchase requests each attempt to buy quantity q
     * where N × q > Q, then the total quantity deducted should never exceed Q.
     * Validates: Requirements 1.1, 1.2
     */
    @Test
    void concurrentPurchasesShouldNotOversell() throws InterruptedException {
        Random random = new Random();
        
        // Run 100 iterations with random parameters
        for (int iteration = 0; iteration < 100; iteration++) {
            // Generate random parameters
            int initialStock = random.nextInt(91) + 10; // 10 to 100
            int numThreads = random.nextInt(9) + 2; // 2 to 10
            int quantityPerPurchase = random.nextInt(16) + 5; // 5 to 20
            
            // Clean up before each iteration
            cleanupDatabase();

            // Setup: Create test data
            User testUser = createTestUser("user_" + System.nanoTime(), new BigDecimal("100000.00"));
            Merchant testMerchant = createTestMerchant("merchant_" + System.nanoTime());
            Product testProduct = createTestProduct(testMerchant.getId(), "Product_" + System.nanoTime());
            String testSku = "SKU-" + System.nanoTime();
            Inventory testInventory = createTestInventory(
                    testSku,
                    testProduct.getId(),
                    testMerchant.getId(),
                    initialStock,
                    new BigDecimal("10.00")
            );

            // Track successful and failed purchases
            AtomicInteger successfulPurchases = new AtomicInteger(0);
            AtomicInteger failedPurchases = new AtomicInteger(0);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(numThreads);

            // Launch concurrent purchase threads
            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < numThreads; i++) {
                Thread thread = new Thread(() -> {
                    try {
                        // Wait for all threads to be ready
                        startLatch.await();

                        // Create and confirm order
                        DirectPurchaseRequest request = DirectPurchaseRequest.builder()
                                .sku(testSku)
                                .quantity(quantityPerPurchase)
                                .build();

                        OrderResponse order = orderService.createDirect(testUser.getId(), request);
                        orderService.confirmPayment(order.getId());
                        successfulPurchases.incrementAndGet();

                    } catch (ConcurrencyException | InsufficientStockException e) {
                        // Expected exceptions for concurrent conflicts or insufficient stock
                        failedPurchases.incrementAndGet();
                    } catch (Exception e) {
                        // Unexpected exception
                        e.printStackTrace();
                    } finally {
                        doneLatch.countDown();
                    }
                });
                threads.add(thread);
                thread.start();
            }

            // Start all threads simultaneously
            startLatch.countDown();

            // Wait for all threads to complete
            doneLatch.await();

            // Verify: Total deducted quantity should never exceed initial stock
            Inventory finalInventory = inventoryRepository.findBySku(testSku).orElseThrow();
            int totalDeducted = initialStock - finalInventory.getQuantity();

            assertThat(totalDeducted)
                    .as("Iteration %d: Total deducted quantity should never exceed initial stock (stock=%d, threads=%d, qty=%d)",
                            iteration, initialStock, numThreads, quantityPerPurchase)
                    .isLessThanOrEqualTo(initialStock);

            // Verify: Final inventory quantity should be non-negative
            assertThat(finalInventory.getQuantity())
                    .as("Iteration %d: Final inventory quantity should be non-negative", iteration)
                    .isGreaterThanOrEqualTo(0);

            // Verify: Successful purchases should match the deducted quantity
            int expectedDeducted = successfulPurchases.get() * quantityPerPurchase;
            assertThat(totalDeducted)
                    .as("Iteration %d: Total deducted should equal successful purchases × quantity per purchase", iteration)
                    .isEqualTo(expectedDeducted);

            // Verify: At least some purchases should fail if total demand exceeds stock
            int totalDemand = numThreads * quantityPerPurchase;
            if (totalDemand > initialStock) {
                assertThat(failedPurchases.get())
                        .as("Iteration %d: Some purchases should fail when total demand exceeds stock", iteration)
                        .isGreaterThan(0);
            }
        }
    }

    /**
     * Property 2: Version Increment on Successful Update
     * For any inventory update that succeeds, the version number should be exactly one greater than
     * the version number before the update.
     * Validates: Requirements 1.4
     */
    @Test
    void successfulUpdateShouldIncrementVersion() {
        Random random = new Random();
        
        // Run 100 iterations with random parameters
        for (int iteration = 0; iteration < 100; iteration++) {
            // Generate random parameters
            int initialQuantity = random.nextInt(91) + 10; // 10 to 100
            int purchaseQuantity = random.nextInt(5) + 1; // 1 to 5
            
            // Clean up before each iteration
            cleanupDatabase();

            // Setup: Create test data
            User testUser = createTestUser("user_" + System.nanoTime(), new BigDecimal("100000.00"));
            Merchant testMerchant = createTestMerchant("merchant_" + System.nanoTime());
            Product testProduct = createTestProduct(testMerchant.getId(), "Product_" + System.nanoTime());
            String testSku = "SKU-" + System.nanoTime();
            Inventory testInventory = createTestInventory(
                    testSku,
                    testProduct.getId(),
                    testMerchant.getId(),
                    initialQuantity,
                    new BigDecimal("10.00")
            );

            // Capture version before purchase
            Long versionBefore = testInventory.getVersion();

            // Perform purchase
            DirectPurchaseRequest request = DirectPurchaseRequest.builder()
                    .sku(testSku)
                    .quantity(purchaseQuantity)
                    .build();

            OrderResponse order = orderService.createDirect(testUser.getId(), request);
            orderService.confirmPayment(order.getId());

            // Verify: Version should increment by exactly 1
            Inventory inventoryAfter = inventoryRepository.findBySku(testSku).orElseThrow();
            assertThat(inventoryAfter.getVersion())
                    .as("Iteration %d: Version should increment by exactly 1 (before=%d, after=%d)",
                            iteration, versionBefore, inventoryAfter.getVersion())
                    .isEqualTo(versionBefore + 1);
        }
    }

    /**
     * Property 3: Transaction Atomicity on Conflict
     * For any order confirmation that fails due to OptimisticLockException, no changes should be persisted
     * to user balance, merchant balance, inventory quantity, or transaction records.
     * Validates: Requirements 3.1, 3.2, 3.3, 3.4
     */
    @Test
    void failedPurchaseShouldNotModifyAnyState() throws InterruptedException {
        Random random = new Random();
        
        // Run 100 iterations with random parameters
        for (int iteration = 0; iteration < 100; iteration++) {
            // Generate random parameters
            int initialStock = random.nextInt(16) + 5; // 5 to 20
            
            // Clean up before each iteration
            cleanupDatabase();

            // Setup: Create test data
            User testUser = createTestUser("user_" + System.nanoTime(), new BigDecimal("100000.00"));
            Merchant testMerchant = createTestMerchant("merchant_" + System.nanoTime());
            Product testProduct = createTestProduct(testMerchant.getId(), "Product_" + System.nanoTime());
            String testSku = "SKU-" + System.nanoTime();
            Inventory testInventory = createTestInventory(
                    testSku,
                    testProduct.getId(),
                    testMerchant.getId(),
                    initialStock,
                    new BigDecimal("10.00")
            );

            // Capture initial state
            BigDecimal initialUserBalance = testUser.getBalance();
            BigDecimal initialMerchantBalance = testMerchant.getBalance();
            int initialInventoryQuantity = testInventory.getQuantity();
            Long initialInventoryVersion = testInventory.getVersion();

            // Create two concurrent purchase requests that will conflict
            int purchaseQuantity = initialStock; // Buy all stock
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(2);
            
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            // Launch two threads that will conflict
            for (int i = 0; i < 2; i++) {
                new Thread(() -> {
                    try {
                        startLatch.await();
                        
                        DirectPurchaseRequest request = DirectPurchaseRequest.builder()
                                .sku(testSku)
                                .quantity(purchaseQuantity)
                                .build();

                        OrderResponse order = orderService.createDirect(testUser.getId(), request);
                        orderService.confirmPayment(order.getId());
                        successCount.incrementAndGet();

                    } catch (ConcurrencyException | InsufficientStockException e) {
                        failureCount.incrementAndGet();
                    } catch (Exception e) {
                        // Unexpected exception - could be optimistic lock on user/merchant
                        failureCount.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            // Start both threads
            startLatch.countDown();
            doneLatch.await();

            // Verify: Exactly one should succeed
            assertThat(successCount.get())
                    .as("Iteration %d: Exactly one purchase should succeed", iteration)
                    .isEqualTo(1);

            // Verify: Final state should reflect exactly one successful purchase
            User finalUser = userRepository.findById(testUser.getId()).orElseThrow();
            Merchant finalMerchant = merchantRepository.findById(testMerchant.getId()).orElseThrow();
            Inventory finalInventory = inventoryRepository.findBySku(testSku).orElseThrow();

            BigDecimal expectedUserBalance = initialUserBalance.subtract(new BigDecimal("10.00").multiply(BigDecimal.valueOf(purchaseQuantity)));
            BigDecimal expectedMerchantBalance = initialMerchantBalance.add(new BigDecimal("10.00").multiply(BigDecimal.valueOf(purchaseQuantity)));
            int expectedInventoryQuantity = initialInventoryQuantity - purchaseQuantity;

            assertThat(finalUser.getBalance())
                    .as("Iteration %d: User balance should reflect exactly one purchase", iteration)
                    .isEqualByComparingTo(expectedUserBalance);

            assertThat(finalMerchant.getBalance())
                    .as("Iteration %d: Merchant balance should reflect exactly one purchase", iteration)
                    .isEqualByComparingTo(expectedMerchantBalance);

            assertThat(finalInventory.getQuantity())
                    .as("Iteration %d: Inventory quantity should reflect exactly one purchase", iteration)
                    .isEqualTo(expectedInventoryQuantity);

            // Verify: Version should increment by exactly 1 (one successful update)
            assertThat(finalInventory.getVersion())
                    .as("Iteration %d: Version should increment by exactly 1", iteration)
                    .isEqualTo(initialInventoryVersion + 1);
        }
    }

    // Helper methods

    private void cleanupDatabase() {
        transactionRecordRepository.deleteAll();
        cartItemRepository.deleteAll();
        orderRepository.deleteAll();
        inventoryRepository.deleteAll();
        productRepository.deleteAll();
        merchantRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createTestUser(String username, BigDecimal balance) {
        User user = User.builder()
                .username(username)
                .password("password123")
                .balance(balance)
                .build();
        return userRepository.save(user);
    }

    private Merchant createTestMerchant(String username) {
        Merchant merchant = Merchant.builder()
                .businessName("Test Store " + username)
                .username(username)
                .password("password123")
                .balance(BigDecimal.ZERO)
                .build();
        return merchantRepository.save(merchant);
    }

    private Product createTestProduct(Long merchantId, String name) {
        Product product = Product.builder()
                .name(name)
                .description("Test Description")
                .category("Electronics")
                .merchantId(merchantId)
                .build();
        return productRepository.save(product);
    }

    private Inventory createTestInventory(String sku, Long productId, Long merchantId, int quantity, BigDecimal price) {
        Inventory inventory = Inventory.builder()
                .sku(sku)
                .productId(productId)
                .merchantId(merchantId)
                .quantity(quantity)
                .price(price)
                .build();
        return inventoryRepository.save(inventory);
    }
}
