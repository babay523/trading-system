package com.trading.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 购物车商品实体类
 * 表示用户购物车中的商品项
 */
@Entity
@Table(name = "cart_items", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "sku"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    
    /**
     * 购物车项唯一标识ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 用户ID，关联users表
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * 商品SKU，关联inventory表
     */
    @Column(nullable = false)
    private String sku;
    
    /**
     * 商品数量
     */
    @Column(nullable = false)
    private Integer quantity;
    
    /**
     * 添加到购物车的时间，不可更新
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
