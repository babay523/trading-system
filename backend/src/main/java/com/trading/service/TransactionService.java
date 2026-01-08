package com.trading.service;

import com.trading.entity.TransactionRecord;
import com.trading.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface TransactionService {
    
    /**
     * Create a transaction record for user account
     * @param userId user ID
     * @param type transaction type
     * @param amount transaction amount
     * @param balanceBefore balance before transaction
     * @param balanceAfter balance after transaction
     * @param orderId related order ID (nullable)
     * @return created transaction record
     */
    TransactionRecord createUserTransaction(Long userId, TransactionType type, 
            BigDecimal amount, BigDecimal balanceBefore, BigDecimal balanceAfter, Long orderId);
    
    /**
     * Create a transaction record for merchant account
     * @param merchantId merchant ID
     * @param type transaction type
     * @param amount transaction amount
     * @param balanceBefore balance before transaction
     * @param balanceAfter balance after transaction
     * @param orderId related order ID (nullable)
     * @return created transaction record
     */
    TransactionRecord createMerchantTransaction(Long merchantId, TransactionType type,
            BigDecimal amount, BigDecimal balanceBefore, BigDecimal balanceAfter, Long orderId);
    
    /**
     * Get transaction history for user
     * @param userId user ID
     * @param pageable pagination info
     * @return page of transaction records
     */
    Page<TransactionRecord> getUserTransactionHistory(Long userId, Pageable pageable);
    
    /**
     * Get transaction history for merchant
     * @param merchantId merchant ID
     * @param pageable pagination info
     * @return page of transaction records
     */
    Page<TransactionRecord> getMerchantTransactionHistory(Long merchantId, Pageable pageable);
}
