package com.trading.entity;

import com.trading.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易记录实体类
 * 记录用户和商家的所有资金变动
 */
@Entity
@Table(name = "transaction_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRecord {
    
    /**
     * 交易记录唯一标识ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 交易流水号，系统内唯一
     */
    @Column(unique = true, nullable = false)
    private String transactionId;
    
    /**
     * 账户类型：USER（用户）或 MERCHANT（商家）
     */
    @Column(nullable = false)
    private String accountType;
    
    /**
     * 账户ID，根据accountType关联users表或merchants表
     */
    @Column(nullable = false)
    private Long accountId;
    
    /**
     * 交易类型：DEPOSIT（充值）、PURCHASE（购买）、SALE（销售）、
     * REFUND_OUT（退款支出）、REFUND_IN（退款收入）
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;
    
    /**
     * 交易金额，精度19位，小数点后2位
     */
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;
    
    /**
     * 交易前余额，精度19位，小数点后2位
     */
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal balanceBefore;
    
    /**
     * 交易后余额，精度19位，小数点后2位
     */
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal balanceAfter;
    
    /**
     * 关联的订单ID（如果交易与订单相关）
     */
    private Long relatedOrderId;
    
    /**
     * 交易创建时间，不可更新
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
