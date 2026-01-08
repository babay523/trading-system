package com.trading.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商家实体类
 * 表示系统中的商家用户，可以发布商品和管理订单
 */
@Entity
@Table(name = "merchants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Merchant {
    
    /**
     * 商家唯一标识ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 商家店铺名称
     */
    @Column(nullable = false)
    private String businessName;
    
    /**
     * 商家用户名，系统内唯一
     */
    @Column(unique = true, nullable = false)
    private String username;
    
    /**
     * 商家密码（BCrypt加密存储）
     */
    @Column(nullable = false)
    private String password;
    
    /**
     * 商家角色：MERCHANT（普通商家）或 ADMIN（系统管理员）
     */
    @Column(nullable = false)
    @Builder.Default
    private String role = "MERCHANT";
    
    /**
     * 商家账户余额，精度19位，小数点后2位
     */
    @Column(precision = 19, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;
    
    /**
     * 商家创建时间，不可更新
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 乐观锁版本号，用于并发控制
     */
    @Version
    private Long version;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }
        if (role == null) {
            role = "MERCHANT";
        }
    }
    
    /**
     * 判断是否为系统管理员
     * @return true if role is ADMIN
     */
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
