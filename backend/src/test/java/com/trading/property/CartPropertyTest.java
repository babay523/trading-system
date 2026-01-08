package com.trading.property;

import com.trading.dto.request.CartAddRequest;
import com.trading.dto.request.UserRegisterRequest;
import com.trading.dto.response.CartItemResponse;
import com.trading.dto.response.CartResponse;
import com.trading.dto.response.UserResponse;
import com.trading.entity.Inventory;
import com.trading.repository.CartItemRepository;
import com.trading.repository.InventoryRepository;
import com.trading.repository.UserRepository;
import com.trading.service.CartService;
import com.trading.service.UserService;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for Cart module
 * Feature: trading-system
 */
@SpringBootTest
@ActiveProfiles("test")
public class CartPropertyTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    private final Random random = new Random();

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll();
        inventoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    /**
     * Property 6: Cart Total Calculation Correctness
     * For any shopping cart containing items, the total amount SHALL equal
     * the sum of (quantity × unit price) for all items in the cart.
     * Validates: Requirements 4.7
     */
    @Test
    void cartTotalShouldEqualSumOfItemSubtotals() {
        // Run 100 iterations with random inputs
        for (int i = 0; i < 100; i++) {
            // Clean up for each iteration
            cartItemRepository.deleteAll();
            inventoryRepository.deleteAll();
            userRepository.deleteAll();

            // Create a user
            String uniqueUsername = "user_" + UUID.randomUUID().toString().substring(0, 8);
            UserRegisterRequest registerRequest = UserRegisterRequest.builder()
                    .username(uniqueUsername)
                    .password("password123")
                    .build();
            UserResponse user = userService.register(registerRequest);

            // Generate random number of items (1 to 5)
            int numItems = random.nextInt(5) + 1;
            List<TestItem> testItems = new ArrayList<>();

            // Create inventory items and add to cart
            for (int j = 0; j < numItems; j++) {
                String sku = "SKU_" + UUID.randomUUID().toString().substring(0, 8);
                
                // Random price between 0.01 and 1000.00
                BigDecimal price = BigDecimal.valueOf(random.nextDouble() * 999.99 + 0.01)
                        .setScale(2, RoundingMode.HALF_UP);
                
                // Random stock quantity (enough for cart)
                int stockQuantity = random.nextInt(100) + 10;
                
                // Random cart quantity (1 to stockQuantity)
                int cartQuantity = random.nextInt(Math.min(stockQuantity, 10)) + 1;

                // Create inventory
                Inventory inventory = Inventory.builder()
                        .sku(sku)
                        .productId(1L)
                        .merchantId(1L)
                        .quantity(stockQuantity)
                        .price(price)
                        .build();
                inventoryRepository.save(inventory);

                // Add to cart
                CartAddRequest cartRequest = CartAddRequest.builder()
                        .sku(sku)
                        .quantity(cartQuantity)
                        .build();
                cartService.addItem(user.getId(), cartRequest);

                // Track expected values
                testItems.add(new TestItem(sku, cartQuantity, price));
            }

            // Get cart and verify total
            CartResponse cart = cartService.getCart(user.getId());

            // Calculate expected total
            BigDecimal expectedTotal = BigDecimal.ZERO;
            for (TestItem item : testItems) {
                BigDecimal subtotal = item.price.multiply(BigDecimal.valueOf(item.quantity));
                expectedTotal = expectedTotal.add(subtotal);
            }

            // Property: cart total should equal sum of (quantity × unit price) for all items
            assertThat(cart.getTotalAmount().compareTo(expectedTotal))
                    .as("Cart total should equal sum of item subtotals. Expected: %s, Actual: %s", 
                            expectedTotal, cart.getTotalAmount())
                    .isEqualTo(0);

            // Also verify each item's subtotal
            for (CartItemResponse cartItem : cart.getItems()) {
                BigDecimal expectedSubtotal = cartItem.getUnitPrice()
                        .multiply(BigDecimal.valueOf(cartItem.getQuantity()));
                assertThat(cartItem.getSubtotal().compareTo(expectedSubtotal))
                        .as("Item subtotal should equal quantity × unit price")
                        .isEqualTo(0);
            }
        }
    }

    private static class TestItem {
        String sku;
        int quantity;
        BigDecimal price;

        TestItem(String sku, int quantity, BigDecimal price) {
            this.sku = sku;
            this.quantity = quantity;
            this.price = price;
        }
    }
}
