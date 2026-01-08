package com.trading.repository;

import com.trading.entity.TransactionRecord;
import com.trading.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, Long> {
    
    Page<TransactionRecord> findByAccountTypeAndAccountId(String accountType, Long accountId, Pageable pageable);
    
    List<TransactionRecord> findByAccountTypeAndAccountIdAndCreatedAtBetween(
            String accountType, Long accountId, LocalDateTime start, LocalDateTime end);
    
    List<TransactionRecord> findByRelatedOrderId(Long orderId);
    
    List<TransactionRecord> findByAccountTypeAndAccountIdAndType(
            String accountType, Long accountId, TransactionType type);
    
    List<TransactionRecord> findByAccountTypeAndAccountIdAndTypeAndCreatedAtBetween(
            String accountType, Long accountId, TransactionType type, 
            LocalDateTime start, LocalDateTime end);
}
