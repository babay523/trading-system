package com.trading.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.dto.request.CartAddRequest;
import com.trading.dto.request.CartUpdateRequest;
import com.trading.dto.request.UserRegisterRequest;
import com.trading.entity.Inventory;
import com.trading.repository.CartItemRepository;
import com.trading.repository.InventoryRepository;
import com.trading.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CartController
 * Validates: Requirements 11.2
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    private Long userId;
    private static final String TEST_SKU = "TEST-SKU-001";

    @BeforeEach
    void setUp() throws Exception {
        cartItemRepository.deleteAll();
        inventoryRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        UserRegisterRequest registerRequest = UserRegisterRequest.builder()
                .username("cartuser")
                .password("password123")
                .build();

        String response = mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        userId = objectMapper.readTree(response).get("data").get("id").asLong();

        // Create test inventory
        Inventory inventory = Inventory.builder()
                .sku(TEST_SKU)
                .productId(1L)
                .merchantId(1L)
                .quantity(100)
                .price(new BigDecimal("29.99"))
                .build();
        inventoryRepository.save(inventory);
    }

    @Test
    void addItem_WithValidRequest_ShouldAddToCart() throws Exception {
        CartAddRequest request = CartAddRequest.builder()
                .sku(TEST_SKU)
                .quantity(2)
                .build();

        mockMvc.perform(post("/api/v1/users/" + userId + "/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201));
    }

    @Test
    void addItem_WithNonExistentSku_ShouldReturnNotFound() throws Exception {
        CartAddRequest request = CartAddRequest.builder()
                .sku("NON-EXISTENT-SKU")
                .quantity(1)
                .build();

        mockMvc.perform(post("/api/v1/users/" + userId + "/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void getCart_WithItems_ShouldReturnCartWithTotal() throws Exception {
        // Add item to cart
        CartAddRequest request = CartAddRequest.builder()
                .sku(TEST_SKU)
                .quantity(3)
                .build();

        mockMvc.perform(post("/api/v1/users/" + userId + "/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Get cart
        mockMvc.perform(get("/api/v1/users/" + userId + "/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].sku").value(TEST_SKU))
                .andExpect(jsonPath("$.data.items[0].quantity").value(3))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(29.99))
                .andExpect(jsonPath("$.data.items[0].subtotal").value(89.97))
                .andExpect(jsonPath("$.data.totalAmount").value(89.97));
    }

    @Test
    void updateQuantity_WithValidRequest_ShouldUpdateCart() throws Exception {
        // Add item first
        CartAddRequest addRequest = CartAddRequest.builder()
                .sku(TEST_SKU)
                .quantity(2)
                .build();

        mockMvc.perform(post("/api/v1/users/" + userId + "/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isCreated());

        // Update quantity
        CartUpdateRequest updateRequest = CartUpdateRequest.builder()
                .quantity(5)
                .build();

        mockMvc.perform(put("/api/v1/users/" + userId + "/cart/items/" + TEST_SKU)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Verify update
        mockMvc.perform(get("/api/v1/users/" + userId + "/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].quantity").value(5));
    }

    @Test
    void removeItem_WithExistingItem_ShouldRemoveFromCart() throws Exception {
        // Add item first
        CartAddRequest addRequest = CartAddRequest.builder()
                .sku(TEST_SKU)
                .quantity(2)
                .build();

        mockMvc.perform(post("/api/v1/users/" + userId + "/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isCreated());

        // Remove item
        mockMvc.perform(delete("/api/v1/users/" + userId + "/cart/items/" + TEST_SKU))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Verify removal
        mockMvc.perform(get("/api/v1/users/" + userId + "/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(0)));
    }

    @Test
    void clearCart_WithItems_ShouldRemoveAllItems() throws Exception {
        // Add items
        CartAddRequest request = CartAddRequest.builder()
                .sku(TEST_SKU)
                .quantity(2)
                .build();

        mockMvc.perform(post("/api/v1/users/" + userId + "/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Clear cart
        mockMvc.perform(delete("/api/v1/users/" + userId + "/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Verify cart is empty
        mockMvc.perform(get("/api/v1/users/" + userId + "/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(0)))
                .andExpect(jsonPath("$.data.totalAmount").value(0));
    }

    @Test
    void getCart_WithNonExistentUser_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/99999/cart"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}
