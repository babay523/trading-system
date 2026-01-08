package com.trading.service;

import com.trading.dto.request.DirectPurchaseRequest;
import com.trading.dto.response.OrderResponse;
import com.trading.entity.*;
import com.trading.enums.OrderStatus;
import com.trading.repository.*;
import com.trading.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OrderServiceImpl
 * Validates: Requirements 4.1, 4.2
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderServiceImplTest {

    @Autowired
    private OrderServiceImpl orderService;

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

    private User testUser;
    private Merchant testMerchant;
    private Product testProduct;
    private Inventory testInventory;
    private static final String TEST_SKU = "TEST-SKU-001";
    private static final BigDecimal UNIT_PRICE = new BigDecimal("50.00");
    private static final int INITIAL_QUANTITY = 100;

    @BeforeEach
    void setUp() {
        // Clean up database
        transactionRecordRepository.deleteAll();
        cartItemRepository.deleteAll();
        orderRepository.deleteAll();
        inventoryRepository.deleteAll();
        productRepository.deleteAll();
        merchantRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user with sufficient balance
        testUser = User.builder()
                .username("testuser")
                .password("password123")
                .balance(new BigDecimal("1000.00"))
                .build();
        testUser = userRepository.save(testUser);

        // Create test merchant
        testMerchant = Merchant.builder()
                .businessName("Test Store")
                .username("testmerchant")
                .password("password123")
                .balance(BigDecimal.ZERO)
                .build();
        testMerchant = merchantRepository.save(testMerchant);

        // Create test product
        testProduct = Product.builder()
                .name("Test Product")
                .description("Test Description")
                .category("Electronics")
                .merchantId(testMerchant.getId())
                .build();
        testProduct = productRepository.save(testProduct);

        // Create test inventory with known quantity and version
        testInventory = Inventory.builder()
                .sku(TEST_SKU)
                .productId(testProduct.getId())
                .merchantId(testMerchant.getId())
                .quantity(INITIAL_QUANTITY)
                .price(UNIT_PRICE)
                .build();
        testInventory = inventoryRepository.save(testInventory);
    }

    /**
     * Test successful purchase with sufficient stock
     * Verifies inventory quantity decreases correctly
     * Verifies version number increments
     * Validates: Requirements 4.1, 4.2
     */
    @Test
    @Transactional
    void confirmPayment_WithSufficientStock_ShouldDecreaseInventoryAndIncrementVersion() {
        // Given: Create a direct purchase order
        int purchaseQuantity = 5;
        DirectPurchaseRequest request = DirectPurchaseRequest.builder()
                .sku(TEST_SKU)
                .quantity(purchaseQuantity)
                .build();

        OrderResponse orderResponse = orderService.createDirect(testUser.getId(), request);
        Long orderId = orderResponse.getId();

        // Capture initial inventory state
        Inventory inventoryBeforePayment = inventoryRepository.findBySku(TEST_SKU).orElseThrow();
        int initialQuantity = inventoryBeforePayment.getQuantity();
        Long initialVersion = inventoryBeforePayment.getVersion();

        // When: Confirm payment
        OrderResponse paidOrder = orderService.confirmPayment(orderId);

        // Then: Verify order status is PAID
        assertThat(paidOrder.getStatus())
                .as("Order status should be PAID after successful payment")
                .isEqualTo(OrderStatus.PAID);

        // Verify inventory quantity decreased correctly
        Inventory inventoryAfterPayment = inventoryRepository.findBySku(TEST_SKU).orElseThrow();
        assertThat(inventoryAfterPayment.getQuantity())
                .as("Inventory quantity should decrease by purchase quantity")
                .isEqualTo(initialQuantity - purchaseQuantity);

        // Verify version number incremented
        assertThat(inventoryAfterPayment.getVersion())
                .as("Version number should increment by exactly 1")
                .isEqualTo(initialVersion + 1);

        // Verify user balance decreased
        User userAfterPayment = userRepository.findById(testUser.getId()).orElseThrow();
        BigDecimal expectedUserBalance = new BigDecimal("1000.00")
                .subtract(UNIT_PRICE.multiply(BigDecimal.valueOf(purchaseQuantity)));
        assertThat(userAfterPayment.getBalance())
                .as("User balance should decrease by total purchase amount")
                .isEqualByComparingTo(expectedUserBalance);

        // Verify merchant balance increased
        Merchant merchantAfterPayment = merchantRepository.findById(testMerchant.getId()).orElseThrow();
        BigDecimal expectedMerchantBalance = UNIT_PRICE.multiply(BigDecimal.valueOf(purchaseQuantity));
        assertThat(merchantAfterPayment.getBalance())
                .as("Merchant balance should increase by total purchase amount")
                .isEqualByComparingTo(expectedMerchantBalance);
    }
}
