package com.trading.service.impl;

import com.trading.entity.TransactionRecord;
import com.trading.enums.TransactionType;
import com.trading.repository.TransactionRecordRepository;
import com.trading.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    
    private static final String ACCOUNT_TYPE_USER = "USER";
    private static final String ACCOUNT_TYPE_MERCHANT = "MERCHANT";
    
    private final TransactionRecordRepository transactionRecordRepository;
    
    @Override
    @Transactional
    public TransactionRecord createUserTransaction(Long userId, TransactionType type,
            BigDecimal amount, BigDecimal balanceBefore, BigDecimal balanceAfter, Long orderId) {
        return createTransaction(ACCOUNT_TYPE_USER, userId, type, amount, balanceBefore, balanceAfter, orderId);
    }
    
    @Override
    @Transactional
    public TransactionRecord createMerchantTransaction(Long merchantId, TransactionType type,
            BigDecimal amount, BigDecimal balanceBefore, BigDecimal balanceAfter, Long orderId) {
        return createTransaction(ACCOUNT_TYPE_MERCHANT, merchantId, type, amount, balanceBefore, balanceAfter, orderId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<TransactionRecord> getUserTransactionHistory(Long userId, Pageable pageable) {
        return transactionRecordRepository.findByAccountTypeAndAccountId(ACCOUNT_TYPE_USER, userId, pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<TransactionRecord> getMerchantTransactionHistory(Long merchantId, Pageable pageable) {
        return transactionRecordRepository.findByAccountTypeAndAccountId(ACCOUNT_TYPE_MERCHANT, merchantId, pageable);
    }
    
    private TransactionRecord createTransaction(String accountType, Long accountId, TransactionType type,
            BigDecimal amount, BigDecimal balanceBefore, BigDecimal balanceAfter, Long orderId) {
        TransactionRecord record = TransactionRecord.builder()
                .transactionId(generateTransactionId())
                .accountType(accountType)
                .accountId(accountId)
                .type(type)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .relatedOrderId(orderId)
                .build();
        return transactionRecordRepository.save(record);
    }
    
    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
