package com.trading.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单商品项实体类
 * 表示订单中的具体商品信息
 */
@Entity
@Table(name = "order_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    
    /**
     * 订单项唯一标识ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 所属订单，关联orders表
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    /**
     * 商品SKU
     */
    @Column(nullable = false)
    private String sku;
    
    /**
     * 商品名称（下单时的快照）
     */
    @Column(nullable = false)
    private String productName;
    
    /**
     * 购买数量
     */
    @Column(nullable = false)
    private Integer quantity;
    
    /**
     * 单价（下单时的快照），精度19位，小数点后2位
     */
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal unitPrice;
    
    /**
     * 小计金额（单价 × 数量），精度19位，小数点后2位
     */
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal subtotal;
    
    @PrePersist
    protected void onCreate() {
        if (subtotal == null && unitPrice != null && quantity != null) {
            subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
