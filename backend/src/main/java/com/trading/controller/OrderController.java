package com.trading.controller;

import com.trading.dto.request.DirectPurchaseRequest;
import com.trading.dto.response.ApiResponse;
import com.trading.dto.response.OrderResponse;
import com.trading.enums.OrderStatus;
import com.trading.security.RequireMerchantOwnership;
import com.trading.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 从购物车创建订单
     * POST /api/v1/users/{userId}/orders/from-cart
     */
    @PostMapping("/users/{userId}/orders/from-cart")
    public ResponseEntity<ApiResponse<OrderResponse>> createFromCart(
            @PathVariable Long userId) {
        OrderResponse order = orderService.createFromCart(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(order));
    }

    /**
     * 创建直接购买订单
     * POST /api/v1/users/{userId}/orders/direct
     */
    @PostMapping("/users/{userId}/orders/direct")
    public ResponseEntity<ApiResponse<OrderResponse>> createDirect(
            @PathVariable Long userId,
            @Valid @RequestBody DirectPurchaseRequest request) {
        OrderResponse order = orderService.createDirect(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(order));
    }

    /**
     * 确认订单支付
     * POST /api/v1/orders/{id}/pay
     */
    @PostMapping("/orders/{id}/pay")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmPayment(
            @PathVariable Long id) {
        OrderResponse order = orderService.confirmPayment(id);
        return ResponseEntity.ok(ApiResponse.success("Payment confirmed", order));
    }

    /**
     * 发货
     * POST /api/v1/orders/{id}/ship
     */
    @PostMapping("/orders/{id}/ship")
    public ResponseEntity<ApiResponse<OrderResponse>> ship(
            @PathVariable Long id) {
        OrderResponse order = orderService.ship(id);
        return ResponseEntity.ok(ApiResponse.success("Order shipped", order));
    }

    /**
     * 完成订单
     * POST /api/v1/orders/{id}/complete
     */
    @PostMapping("/orders/{id}/complete")
    public ResponseEntity<ApiResponse<OrderResponse>> complete(
            @PathVariable Long id) {
        OrderResponse order = orderService.complete(id);
        return ResponseEntity.ok(ApiResponse.success("Order completed", order));
    }

    /**
     * 取消订单
     * POST /api/v1/orders/{id}/cancel
     */
    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(
            @PathVariable Long id) {
        OrderResponse order = orderService.cancel(id);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled", order));
    }

    /**
     * 退款
     * POST /api/v1/orders/{id}/refund
     */
    @PostMapping("/orders/{id}/refund")
    public ResponseEntity<ApiResponse<OrderResponse>> refund(
            @PathVariable Long id) {
        OrderResponse order = orderService.refund(id);
        return ResponseEntity.ok(ApiResponse.success("Order refunded", order));
    }

    /**
     * 根据ID获取订单
     * GET /api/v1/orders/{id}
     */
    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getById(
            @PathVariable Long id) {
        OrderResponse order = orderService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    /**
     * 获取用户的订单列表
     * GET /api/v1/users/{userId}/orders
     */
    @GetMapping("/users/{userId}/orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getByUser(
            @PathVariable Long userId,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<OrderResponse> orders = orderService.getByUser(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    /**
     * 获取商家的订单列表
     * GET /api/v1/merchants/{merchantId}/orders
     */
    @GetMapping("/merchants/{merchantId}/orders")
    @RequireMerchantOwnership("merchant orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getByMerchant(
            @PathVariable Long merchantId,
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<OrderResponse> orders;
        if (status != null) {
            orders = orderService.getByMerchantAndStatus(merchantId, status, pageable);
        } else {
            orders = orderService.getByMerchant(merchantId, pageable);
        }
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
}
