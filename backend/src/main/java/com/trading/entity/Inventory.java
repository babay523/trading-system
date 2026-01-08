package com.trading.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 库存实体类
 * 表示商品的具体库存信息，包括SKU、数量和价格
 */
@Entity
@Table(name = "inventory")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {
    
    /**
     * 库存记录唯一标识ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 商品SKU（Stock Keeping Unit），系统内唯一
     */
    @Column(unique = true, nullable = false)
    private String sku;
    
    /**
     * 关联的商品ID，关联products表
     */
    @Column(nullable = false)
    private Long productId;
    
    /**
     * 所属商家ID，关联merchants表
     */
    @Column(nullable = false)
    private Long merchantId;
    
    /**
     * 库存数量
     */
    @Column(nullable = false)
    private Integer quantity;
    
    /**
     * 商品价格，精度19位，小数点后2位
     */
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal price;
    
    /**
     * 乐观锁版本号，用于并发控制库存更新
     */
    @Version
    private Long version;
}
