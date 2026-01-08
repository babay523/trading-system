package com.trading.entity;

import com.trading.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 结算记录实体类
 * 记录商家的日结算信息
 */
@Entity
@Table(name = "settlements", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"merchant_id", "settlement_date"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Settlement {
    
    /**
     * 结算记录唯一标识ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 商家ID，关联merchants表
     */
    @Column(nullable = false)
    private Long merchantId;
    
    /**
     * 结算日期
     */
    @Column(nullable = false)
    private LocalDate settlementDate;
    
    /**
     * 当日总销售额，精度19位，小数点后2位
     */
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal totalSales;
    
    /**
     * 当日总退款额，精度19位，小数点后2位
     */
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal totalRefunds;
    
    /**
     * 净收入（销售额 - 退款额），精度19位，小数点后2位
     */
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal netAmount;
    
    /**
     * 实际余额变动，精度19位，小数点后2位
     */
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal balanceChange;
    
    /**
     * 差异金额（净收入与实际余额变动的差额），精度19位，小数点后2位
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal discrepancy;
    
    /**
     * 结算状态：MATCHED（已匹配）、DISCREPANCY（有差异）
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus status;
    
    /**
     * 结算记录创建时间，不可更新
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
