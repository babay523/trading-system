package com.trading.service;

import com.trading.dto.request.DirectPurchaseRequest;
import com.trading.dto.response.OrderResponse;
import com.trading.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    
    /**
     * Create order from user's shopping cart
     * @param userId user ID
     * @return created order response
     */
    OrderResponse createFromCart(Long userId);
    
    /**
     * Create order for direct purchase (single item)
     * @param userId user ID
     * @param request direct purchase request with SKU and quantity
     * @return created order response
     */
    OrderResponse createDirect(Long userId, DirectPurchaseRequest request);
    
    /**
     * Confirm payment for an order
     * Atomically: deduct user balance, add merchant balance, reduce inventory
     * @param orderId order ID
     * @return updated order response
     */
    OrderResponse confirmPayment(Long orderId);
    
    /**
     * Ship an order (PAID -> SHIPPED)
     * @param orderId order ID
     * @return updated order response
     */
    OrderResponse ship(Long orderId);
    
    /**
     * Complete an order (SHIPPED -> COMPLETED)
     * @param orderId order ID
     * @return updated order response
     */
    OrderResponse complete(Long orderId);
    
    /**
     * Cancel an order (PENDING -> CANCELLED)
     * @param orderId order ID
     * @return updated order response
     */
    OrderResponse cancel(Long orderId);
    
    /**
     * Refund an order (PAID/SHIPPED -> REFUNDED)
     * Returns money to user and deducts from merchant
     * @param orderId order ID
     * @return updated order response
     */
    OrderResponse refund(Long orderId);
    
    /**
     * Get order by ID
     * @param orderId order ID
     * @return order response
     */
    OrderResponse getById(Long orderId);
    
    /**
     * Get orders by user ID with pagination
     * @param userId user ID
     * @param pageable pagination info
     * @return page of order responses
     */
    Page<OrderResponse> getByUser(Long userId, Pageable pageable);
    
    /**
     * Get orders by merchant ID with pagination
     * @param merchantId merchant ID
     * @param pageable pagination info
     * @return page of order responses
     */
    Page<OrderResponse> getByMerchant(Long merchantId, Pageable pageable);
    
    /**
     * 根据商家ID和订单状态获取订单列表（分页）
     * @param merchantId 商家ID
     * @param status 订单状态
     * @param pageable 分页参数
     * @return 订单响应分页列表
     */
    Page<OrderResponse> getByMerchantAndStatus(Long merchantId, OrderStatus status, Pageable pageable);
}
