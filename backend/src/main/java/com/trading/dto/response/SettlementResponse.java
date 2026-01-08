package com.trading.dto.response;

import com.trading.enums.SettlementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementResponse {
    
    private Long id;
    private Long merchantId;
    private LocalDate settlementDate;
    private BigDecimal totalSales;
    private BigDecimal totalRefunds;
    private BigDecimal netAmount;
    private BigDecimal balanceChange;
    private BigDecimal discrepancy;
    private SettlementStatus status;
    private LocalDateTime createdAt;
}
