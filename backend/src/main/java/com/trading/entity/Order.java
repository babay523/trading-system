package com.trading.entity;

import com.trading.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单实体类
 * 表示用户的购买订单
 */
@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    /**
     * 订单唯一标识ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 订单号，系统内唯一
     */
    @Column(unique = true, nullable = false)
    private String orderNumber;
    
    /**
     * 下单用户ID，关联users表
     */
    @Column(nullable = false)
    private Long userId;
    
    /**
     * 商家ID，关联merchants表
     */
    @Column(nullable = false)
    private Long merchantId;
    
    /**
     * 订单总金额，精度19位，小数点后2位
     */
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal totalAmount;
    
    /**
     * 订单状态：PENDING（待支付）、PAID（已支付）、SHIPPED（已发货）、
     * COMPLETED（已完成）、CANCELLED（已取消）、REFUNDED（已退款）
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;
    
    /**
     * 订单商品项列表
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();
    
    /**
     * 订单创建时间，不可更新
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 订单最后更新时间
     */
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = OrderStatus.PENDING;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * 添加订单项到订单中
     * @param item 订单项
     */
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}
