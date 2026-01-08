package com.trading.service;

import com.trading.dto.response.SettlementResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface SettlementService {
    
    /**
     * Run daily settlement for all merchants
     * Calculates total sales from COMPLETED orders, total refunds,
     * compares with balance changes, and creates settlement records
     */
    void runDailySettlement();
    
    /**
     * Run settlement for a specific merchant and date
     * @param merchantId merchant ID
     * @param date settlement date
     * @return settlement response
     */
    SettlementResponse runSettlementForMerchant(Long merchantId, LocalDate date);
    
    /**
     * Get settlement by merchant ID and date
     * @param merchantId merchant ID
     * @param date settlement date
     * @return settlement response
     */
    SettlementResponse getByMerchantAndDate(Long merchantId, LocalDate date);
    
    /**
     * Get all settlements for a merchant with pagination
     * @param merchantId merchant ID
     * @param pageable pagination info
     * @return page of settlement responses
     */
    Page<SettlementResponse> getByMerchant(Long merchantId, Pageable pageable);
}
