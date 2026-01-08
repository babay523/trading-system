package com.trading.controller;

import com.trading.dto.request.CartAddRequest;
import com.trading.dto.request.CartUpdateRequest;
import com.trading.dto.response.ApiResponse;
import com.trading.dto.response.CartResponse;
import com.trading.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/{userId}/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * 添加商品到购物车
     * POST /api/v1/users/{userId}/cart/items
     */
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<Void>> addItem(
            @PathVariable Long userId,
            @Valid @RequestBody CartAddRequest request) {
        cartService.addItem(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(null));
    }

    /**
     * 更新购物车商品数量
     * PUT /api/v1/users/{userId}/cart/items/{sku}
     */
    @PutMapping("/items/{sku}")
    public ResponseEntity<ApiResponse<Void>> updateQuantity(
            @PathVariable Long userId,
            @PathVariable String sku,
            @Valid @RequestBody CartUpdateRequest request) {
        cartService.updateQuantity(userId, sku, request);
        return ResponseEntity.ok(ApiResponse.success("Cart item updated", null));
    }

    /**
     * 从购物车移除商品
     * DELETE /api/v1/users/{userId}/cart/items/{sku}
     */
    @DeleteMapping("/items/{sku}")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @PathVariable Long userId,
            @PathVariable String sku) {
        cartService.removeItem(userId, sku);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", null));
    }

    /**
     * 获取用户购物车
     * GET /api/v1/users/{userId}/cart
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @PathVariable Long userId) {
        CartResponse cart = cartService.getCart(userId);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    /**
     * 清空购物车
     * DELETE /api/v1/users/{userId}/cart
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @PathVariable Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", null));
    }
}
