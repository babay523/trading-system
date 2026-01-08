package com.trading.property;

import com.trading.dto.request.InventoryAddRequest;
import com.trading.dto.request.MerchantRegisterRequest;
import com.trading.dto.request.ProductCreateRequest;
import com.trading.dto.response.InventoryResponse;
import com.trading.dto.response.MerchantResponse;
import com.trading.dto.response.ProductResponse;
import com.trading.exception.InvalidOperationException;
import com.trading.repository.InventoryRepository;
import com.trading.repository.MerchantRepository;
import com.trading.repository.ProductRepository;
import com.trading.service.InventoryService;
import com.trading.service.MerchantService;
import com.trading.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for Inventory module
 * Feature: trading-system
 */
@SpringBootTest
@ActiveProfiles("test")
public class InventoryPropertyTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private ProductService productService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
        productRepository.deleteAll();
        merchantRepository.deleteAll();
    }

    /**
     * Property 4: Inventory Addition Correctness
     * For any merchant inventory with initial quantity Q for a SKU and for any valid addition amount A (where A > 0),
     * after the add operation, the inventory quantity SHALL equal Q + A.
     * Validates: Requirements 2.4
     */
    @Test
    void inventoryAdditionShouldIncreaseQuantityCorrectly() {
        Random random = new Random();

        for (int i = 0; i < 100; i++) {
            // Create a merchant
            String uniqueUsername = "merchant_" + UUID.randomUUID().toString().substring(0, 8);
            MerchantRegisterRequest merchantRequest = MerchantRegisterRequest.builder()
                    .businessName("Test Business " + i)
                    .username(uniqueUsername)
                    .password("password123")
                    .build();
            MerchantResponse merchant = merchantService.register(merchantRequest);

            // Create a product
            ProductCreateRequest productRequest = ProductCreateRequest.builder()
                    .merchantId(merchant.getId())
                    .name("Test Product " + i)
                    .description("Test Description")
                    .category("Electronics")
                    .build();
            ProductResponse product = productService.create(productRequest);

            // Generate unique SKU
            String sku = "SKU-" + UUID.randomUUID().toString().substring(0, 8);

            // Generate random initial quantity (1 to 100)
            int initialQuantity = random.nextInt(100) + 1;
            BigDecimal price = BigDecimal.valueOf(random.nextDouble() * 999 + 1)
                    .setScale(2, RoundingMode.HALF_UP);

            // Add initial inventory
            InventoryAddRequest initialRequest = InventoryAddRequest.builder()
                    .sku(sku)
                    .productId(product.getId())
                    .quantity(initialQuantity)
                    .price(price)
                    .build();
            InventoryResponse initialInventory = inventoryService.addInventory(merchant.getId(), initialRequest);

            // Verify initial quantity
            assertThat(initialInventory.getQuantity())
                    .as("Initial inventory quantity should match")
                    .isEqualTo(initialQuantity);

            // Generate random addition amount (1 to 50)
            int additionAmount = random.nextInt(50) + 1;

            // Add more inventory
            InventoryAddRequest addRequest = InventoryAddRequest.builder()
                    .sku(sku)
                    .productId(product.getId())
                    .quantity(additionAmount)
                    .price(price)
                    .build();
            InventoryResponse updatedInventory = inventoryService.addInventory(merchant.getId(), addRequest);

            // Property: new quantity = initial quantity + addition amount
            int expectedQuantity = initialQuantity + additionAmount;
            assertThat(updatedInventory.getQuantity())
                    .as("Inventory quantity after addition should be initial + added amount")
                    .isEqualTo(expectedQuantity);
        }
    }

    /**
     * Property 5: Invalid Inventory Rejection
     * For any inventory addition with quantity Q where Q <= 0, the operation SHALL be rejected
     * and the inventory quantity SHALL remain unchanged.
     * Validates: Requirements 2.8
     */
    @Test
    void invalidInventoryAdditionShouldBeRejected() {
        Random random = new Random();

        for (int i = 0; i < 100; i++) {
            // Create a merchant
            String uniqueUsername = "merchant_" + UUID.randomUUID().toString().substring(0, 8);
            MerchantRegisterRequest merchantRequest = MerchantRegisterRequest.builder()
                    .businessName("Test Business " + i)
                    .username(uniqueUsername)
                    .password("password123")
                    .build();
            MerchantResponse merchant = merchantService.register(merchantRequest);

            // Create a product
            ProductCreateRequest productRequest = ProductCreateRequest.builder()
                    .merchantId(merchant.getId())
                    .name("Test Product " + i)
                    .description("Test Description")
                    .category("Electronics")
                    .build();
            ProductResponse product = productService.create(productRequest);

            // Generate unique SKU
            String sku = "SKU-" + UUID.randomUUID().toString().substring(0, 8);
            BigDecimal price = BigDecimal.valueOf(random.nextDouble() * 999 + 1)
                    .setScale(2, RoundingMode.HALF_UP);

            // First, add some valid inventory
            int initialQuantity = random.nextInt(100) + 1;
            InventoryAddRequest validRequest = InventoryAddRequest.builder()
                    .sku(sku)
                    .productId(product.getId())
                    .quantity(initialQuantity)
                    .price(price)
                    .build();
            InventoryResponse initialInventory = inventoryService.addInventory(merchant.getId(), validRequest);

            // Generate invalid quantity (zero or negative)
            int invalidQuantity;
            if (i % 2 == 0) {
                invalidQuantity = 0;
            } else {
                invalidQuantity = -(random.nextInt(100) + 1);
            }

            final int finalInvalidQuantity = invalidQuantity;
            InventoryAddRequest invalidRequest = InventoryAddRequest.builder()
                    .sku(sku)
                    .productId(product.getId())
                    .quantity(finalInvalidQuantity)
                    .price(price)
                    .build();

            // Property: invalid inventory addition should be rejected
            assertThatThrownBy(() -> inventoryService.addInventory(merchant.getId(), invalidRequest))
                    .as("Inventory addition with quantity %d should be rejected", finalInvalidQuantity)
                    .isInstanceOf(InvalidOperationException.class);

            // Property: inventory quantity should remain unchanged
            InventoryResponse afterInventory = inventoryService.getBySku(sku);
            assertThat(afterInventory.getQuantity())
                    .as("Inventory quantity should remain unchanged after rejected addition")
                    .isEqualTo(initialQuantity);
        }
    }
}
