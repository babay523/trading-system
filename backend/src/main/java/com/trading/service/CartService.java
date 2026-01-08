package com.trading.service;

import com.trading.dto.request.CartAddRequest;
import com.trading.dto.request.CartUpdateRequest;
import com.trading.dto.response.CartResponse;

public interface CartService {
    
    /**
     * Add item to user's cart
     * @param userId user ID
     * @param request cart add request with SKU and quantity
     */
    void addItem(Long userId, CartAddRequest request);
    
    /**
     * Update quantity of an item in cart
     * @param userId user ID
     * @param sku SKU of the item
     * @param request update request with new quantity
     */
    void updateQuantity(Long userId, String sku, CartUpdateRequest request);
    
    /**
     * Remove item from cart
     * @param userId user ID
     * @param sku SKU of the item to remove
     */
    void removeItem(Long userId, String sku);
    
    /**
     * Get user's cart with all items, prices, and total
     * @param userId user ID
     * @return cart response with items and total amount
     */
    CartResponse getCart(Long userId);
    
    /**
     * Clear all items from user's cart
     * @param userId user ID
     */
    void clearCart(Long userId);
}
