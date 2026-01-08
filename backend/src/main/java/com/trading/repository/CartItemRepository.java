package com.trading.repository;

import com.trading.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    
    List<CartItem> findByUserId(Long userId);
    
    Optional<CartItem> findByUserIdAndSku(Long userId, String sku);
    
    void deleteByUserIdAndSku(Long userId, String sku);
    
    void deleteByUserId(Long userId);
    
    boolean existsByUserIdAndSku(Long userId, String sku);
}
