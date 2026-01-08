package com.trading.integration;

import com.trading.dto.request.DirectPurchaseRequest;
import com.trading.dto.request.InventoryAddRequest;
import com.trading.dto.request.MerchantRegisterRequest;
import com.trading.dto.request.ProductCreateRequest;
import com.trading.dto.request.UserRegisterRequest;
import com.trading.dto.request.DepositRequest;
import com.trading.dto.response.MerchantResponse;
import com.trading.dto.response.ProductResponse;
import com.trading.dto.response.UserResponse;
import com.trading.entity.Inventory;
import com.trading.exception.ConcurrencyException;
import com.trading.exception.InsufficientStockException;
import com.trading.repository.*;
import com.trading.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrent Purchase Integration Test
 * 
 * Tests that optimistic locking prevents overselling under concurrent load.
 * Uses CountDownLatch to synchronize multiple threads attempting to purchase
 * from limited inventory.
 * 
 * Validates: Requirements 1.1, 1.2, 2.2
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Concurrent Purchase Integration Tests")
class ConcurrentPurchaseIntegrationTest {

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
    private InventoryRepository inventoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TransactionRecordRepository transactionRecordRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // Clean up all data before each test
        transactionRecordRepository.deleteAll();
        cartItemRepository.deleteAll();
        orderRepository.deleteAll();
        inventoryRepository.deleteAll();
        productRepository.deleteAll();
        merchantRepository.deleteAll();
        userRepository.deleteAll();
    }

    /**
     * Task 6.1: Concurrent Purchase Integration Test
     * 
     * Tests that when multiple threads attempt to purchase from limited inventory,
     * optimistic locking prevents overselling. Some purchases should succeed,
     * some should fail with ConcurrencyException or InsufficientStockException,
     * and the final inventory quantity should be correct (0 or positive).
     * 
     * Validates: Requirements 1.1, 1.2, 2.2
     */
    @Test
    @DisplayName("Concurrent purchases with limited stock should prevent overselling")
    void concurrentPurchases_WithLimitedStock_ShouldPreventOverselling() throws InterruptedException {
        // Setup: Create inventory with limited stock
        final int INITIAL_STOCK = 10;
        final int NUM_THREADS = 5;
        final int QUANTITY_PER_PURCHASE = 3;
        final int TOTAL_REQUESTED = NUM_THREADS * QUANTITY_PER_PURCHASE; // 15 items requested, only 10 available

        // Create merchant with product and inventory
        MerchantResponse merchant = createMerchant();
        ProductResponse product = createProduct(merchant.getId());
        String sku = createInventory(merchant.getId(), product.getId(), new BigDecimal("100.00"), INITIAL_STOCK);

        // Create multiple users with sufficient balance
        List<UserResponse> users = new ArrayList<>();
        for (int i = 0; i < NUM_THREADS; i++) {
            users.add(createUserWithBalance(new BigDecimal("1000.00")));
        }

        // Synchronization primitives
        CountDownLatch startLatch = new CountDownLatch(1); // To start all threads simultaneously
        CountDownLatch doneLatch = new CountDownLatch(NUM_THREADS); // To wait for all threads to complete

        // Track results
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger insufficientStockCount = new AtomicInteger(0);
        List<Exception> unexpectedExceptions = new ArrayList<>();

        // Launch concurrent purchase threads
        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadIndex = i;
            final UserResponse user = users.get(i);

            Thread thread = new Thread(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();

                    // Attempt to purchase
                    DirectPurchaseRequest request = DirectPurchaseRequest.builder()
                            .sku(sku)
                            .quantity(QUANTITY_PER_PURCHASE)
                            .build();

                    Long orderId = orderService.createDirect(user.getId(), request).getId();
                    orderService.confirmPayment(orderId);

                    // If we reach here, purchase succeeded
                    successCount.incrementAndGet();

                } catch (ConcurrencyException e) {
                    // Expected: concurrent modification detected
                    conflictCount.incrementAndGet();
                } catch (InsufficientStockException e) {
                    // Expected: not enough stock
                    insufficientStockCount.incrementAndGet();
                } catch (Exception e) {
                    // Unexpected exception
                    synchronized (unexpectedExceptions) {
                        unexpectedExceptions.add(e);
                    }
                } finally {
                    doneLatch.countDown();
                }
            });

            thread.start();
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for all threads to complete
        doneLatch.await();

        // Verify no unexpected exceptions occurred
        assertThat(unexpectedExceptions)
                .as("No unexpected exceptions should occur during concurrent purchases")
                .isEmpty();

        // Verify some purchases succeeded and some failed
        int totalProcessed = successCount.get() + conflictCount.get() + insufficientStockCount.get();
        assertThat(totalProcessed)
                .as("All purchase attempts should be processed")
                .isEqualTo(NUM_THREADS);

        assertThat(successCount.get())
                .as("Some purchases should succeed")
                .isGreaterThan(0);

        assertThat(conflictCount.get() + insufficientStockCount.get())
                .as("Some purchases should fail due to conflicts or insufficient stock")
                .isGreaterThan(0);

        // Verify final inventory quantity is correct
        Inventory finalInventory = inventoryRepository.findBySku(sku).orElseThrow();
        int expectedFinalQuantity = INITIAL_STOCK - (successCount.get() * QUANTITY_PER_PURCHASE);

        assertThat(finalInventory.getQuantity())
                .as("Final inventory quantity should match expected value")
                .isEqualTo(expectedFinalQuantity);

        assertThat(finalInventory.getQuantity())
                .as("Final inventory quantity should never be negative (no overselling)")
                .isGreaterThanOrEqualTo(0);

        // Verify total deducted quantity never exceeds initial stock
        int totalDeducted = successCount.get() * QUANTITY_PER_PURCHASE;
        assertThat(totalDeducted)
                .as("Total deducted quantity should never exceed initial stock")
                .isLessThanOrEqualTo(INITIAL_STOCK);

        // Log results for visibility
        System.out.println("=== Concurrent Purchase Test Results ===");
        System.out.println("Initial Stock: " + INITIAL_STOCK);
        System.out.println("Threads: " + NUM_THREADS);
        System.out.println("Quantity per purchase: " + QUANTITY_PER_PURCHASE);
        System.out.println("Total requested: " + TOTAL_REQUESTED);
        System.out.println("Successful purchases: " + successCount.get());
        System.out.println("Conflict failures: " + conflictCount.get());
        System.out.println("Insufficient stock failures: " + insufficientStockCount.get());
        System.out.println("Total deducted: " + totalDeducted);
        System.out.println("Final inventory: " + finalInventory.getQuantity());
        System.out.println("========================================");
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
