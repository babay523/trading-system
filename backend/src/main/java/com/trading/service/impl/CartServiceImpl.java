package com.trading.service.impl;

import com.trading.dto.request.CartAddRequest;
import com.trading.dto.request.CartUpdateRequest;
import com.trading.dto.response.CartItemResponse;
import com.trading.dto.response.CartResponse;
import com.trading.entity.CartItem;
import com.trading.entity.Inventory;
import com.trading.exception.ResourceNotFoundException;
import com.trading.repository.CartItemRepository;
import com.trading.repository.InventoryRepository;
import com.trading.repository.UserRepository;
import com.trading.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {
    
    private final CartItemRepository cartItemRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    
    @Override
    @Transactional
    public void addItem(Long userId, CartAddRequest request) {
        log.debug("Adding item to cart for user {}: sku={}, quantity={}", 
                userId, request.getSku(), request.getQuantity());
        
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", userId);
        }
        
        // Verify SKU exists in inventory
        if (!inventoryRepository.existsBySku(request.getSku())) {
            throw new ResourceNotFoundException("Inventory", request.getSku());
        }
        
        // Check if item already in cart
        Optional<CartItem> existingItem = cartItemRepository.findByUserIdAndSku(userId, request.getSku());
        
        if (existingItem.isPresent()) {
            // Update quantity if item exists
            CartItem cartItem = existingItem.get();
            cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
            cartItemRepository.save(cartItem);
            log.info("Updated cart item quantity for user {}: sku={}, newQuantity={}", 
                    userId, request.getSku(), cartItem.getQuantity());
        } else {
            // Create new cart item
            CartItem cartItem = CartItem.builder()
                    .userId(userId)
                    .sku(request.getSku())
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(cartItem);
            log.info("Added new item to cart for user {}: sku={}, quantity={}", 
                    userId, request.getSku(), request.getQuantity());
        }
    }
    
    @Override
    @Transactional
    public void updateQuantity(Long userId, String sku, CartUpdateRequest request) {
        log.debug("Updating cart item quantity for user {}: sku={}, quantity={}", 
                userId, sku, request.getQuantity());
        
        CartItem cartItem = cartItemRepository.findByUserIdAndSku(userId, sku)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", sku));
        
        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);
        
        log.info("Updated cart item for user {}: sku={}, newQuantity={}", 
                userId, sku, request.getQuantity());
    }
    
    @Override
    @Transactional
    public void removeItem(Long userId, String sku) {
        log.debug("Removing item from cart for user {}: sku={}", userId, sku);
        
        if (!cartItemRepository.existsByUserIdAndSku(userId, sku)) {
            throw new ResourceNotFoundException("Cart item", sku);
        }
        
        cartItemRepository.deleteByUserIdAndSku(userId, sku);
        log.info("Removed item from cart for user {}: sku={}", userId, sku);
    }
    
    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        log.debug("Getting cart for user {}", userId);
        
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", userId);
        }
        
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        List<CartItemResponse> itemResponses = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (CartItem cartItem : cartItems) {
            Optional<Inventory> inventoryOpt = inventoryRepository.findBySku(cartItem.getSku());
            
            CartItemResponse itemResponse;
            if (inventoryOpt.isPresent()) {
                Inventory inventory = inventoryOpt.get();
                BigDecimal subtotal = inventory.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
                boolean available = inventory.getQuantity() >= cartItem.getQuantity();
                
                itemResponse = CartItemResponse.builder()
                        .sku(cartItem.getSku())
                        .quantity(cartItem.getQuantity())
                        .unitPrice(inventory.getPrice())
                        .subtotal(subtotal)
                        .available(available)
                        .stockQuantity(inventory.getQuantity())
                        .build();
                
                // Only add to total if available
                if (available) {
                    totalAmount = totalAmount.add(subtotal);
                }
            } else {
                // SKU no longer exists in inventory
                itemResponse = CartItemResponse.builder()
                        .sku(cartItem.getSku())
                        .quantity(cartItem.getQuantity())
                        .unitPrice(BigDecimal.ZERO)
                        .subtotal(BigDecimal.ZERO)
                        .available(false)
                        .stockQuantity(0)
                        .build();
            }
            
            itemResponses.add(itemResponse);
        }
        
        return CartResponse.builder()
                .userId(userId)
                .items(itemResponses)
                .totalAmount(totalAmount)
                .itemCount(itemResponses.size())
                .build();
    }
    
    @Override
    @Transactional
    public void clearCart(Long userId) {
        log.debug("Clearing cart for user {}", userId);
        
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", userId);
        }
        
        cartItemRepository.deleteByUserId(userId);
        log.info("Cleared cart for user {}", userId);
    }
}
