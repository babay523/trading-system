package com.trading.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 商品实体类
 * 表示商家发布的商品信息
 */
@Entity
@Table(name = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    
    /**
     * 商品唯一标识ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 商品名称
     */
    @Column(nullable = false)
    private String name;
    
    /**
     * 商品描述，最大长度1000字符
     */
    @Column(length = 1000)
    private String description;
    
    /**
     * 商品分类
     */
    @Column(nullable = false)
    private String category;
    
    /**
     * 所属商家ID，关联merchants表
     */
    @Column(nullable = false)
    private Long merchantId;
    
    /**
     * 商品创建时间，不可更新
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
