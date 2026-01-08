package com.trading.service.impl;

import com.trading.dto.response.SettlementResponse;
import com.trading.entity.Merchant;
import com.trading.entity.Order;
import com.trading.entity.Settlement;
import com.trading.entity.TransactionRecord;
import com.trading.enums.OrderStatus;
import com.trading.enums.SettlementStatus;
import com.trading.enums.TransactionType;
import com.trading.exception.ResourceNotFoundException;
import com.trading.repository.MerchantRepository;
import com.trading.repository.OrderRepository;
import com.trading.repository.SettlementRepository;
import com.trading.repository.TransactionRecordRepository;
import com.trading.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementServiceImpl implements SettlementService {

    private final SettlementRepository settlementRepository;
    private final MerchantRepository merchantRepository;
    private final OrderRepository orderRepository;
    private final TransactionRecordRepository transactionRecordRepository;

    @Override
    @Transactional
    public void runDailySettlement() {
        log.info("Starting daily settlement job");
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        List<Merchant> merchants = merchantRepository.findAll();
        for (Merchant merchant : merchants) {
            try {
                runSettlementForMerchant(merchant.getId(), yesterday);
            } catch (Exception e) {
                log.error("Failed to run settlement for merchant {}: {}", 
                        merchant.getId(), e.getMessage());
            }
        }
        
        log.info("Daily settlement job completed");
    }

    @Override
    @Transactional
    public SettlementResponse runSettlementForMerchant(Long merchantId, LocalDate date) {
        log.debug("Running settlement for merchant {} on date {}", merchantId, date);
        
        // Check if merchant exists
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId));
        
        // Check if settlement already exists for this date
        if (settlementRepository.existsByMerchantIdAndSettlementDate(merchantId, date)) {
            log.info("Settlement already exists for merchant {} on date {}", merchantId, date);
            return toSettlementResponse(
                    settlementRepository.findByMerchantIdAndSettlementDate(merchantId, date)
                            .orElseThrow());
        }
        
        // Define date range for the settlement day
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        
        // Calculate total sales from COMPLETED orders
        BigDecimal totalSales = calculateTotalSales(merchantId, startOfDay, endOfDay);
        
        // Calculate total refunds
        BigDecimal totalRefunds = calculateTotalRefunds(merchantId, startOfDay, endOfDay);
        
        // Calculate net amount (sales - refunds)
        BigDecimal netAmount = totalSales.subtract(totalRefunds);
        
        // Calculate actual balance change from transaction records
        BigDecimal balanceChange = calculateBalanceChange(merchantId, startOfDay, endOfDay);
        
        // Determine settlement status
        BigDecimal discrepancy = netAmount.subtract(balanceChange);
        SettlementStatus status = discrepancy.compareTo(BigDecimal.ZERO) == 0 
                ? SettlementStatus.MATCHED 
                : SettlementStatus.MISMATCHED;
        
        if (status == SettlementStatus.MISMATCHED) {
            log.warn("Settlement mismatch for merchant {} on {}: net={}, balanceChange={}, discrepancy={}",
                    merchantId, date, netAmount, balanceChange, discrepancy);
        }
        
        // Create settlement record
        Settlement settlement = Settlement.builder()
                .merchantId(merchantId)
                .settlementDate(date)
                .totalSales(totalSales)
                .totalRefunds(totalRefunds)
                .netAmount(netAmount)
                .balanceChange(balanceChange)
                .discrepancy(discrepancy)
                .status(status)
                .build();
        
        Settlement savedSettlement = settlementRepository.save(settlement);
        log.info("Created settlement for merchant {} on {}: status={}", 
                merchantId, date, status);
        
        return toSettlementResponse(savedSettlement);
    }

    @Override
    @Transactional(readOnly = true)
    public SettlementResponse getByMerchantAndDate(Long merchantId, LocalDate date) {
        Settlement settlement = settlementRepository
                .findByMerchantIdAndSettlementDate(merchantId, date)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Settlement for merchant " + merchantId + " on " + date));
        return toSettlementResponse(settlement);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SettlementResponse> getByMerchant(Long merchantId, Pageable pageable) {
        return settlementRepository
                .findByMerchantIdOrderBySettlementDateDesc(merchantId, pageable)
                .map(this::toSettlementResponse);
    }

    /**
     * Calculate total sales from COMPLETED orders within the date range
     */
    private BigDecimal calculateTotalSales(Long merchantId, LocalDateTime start, LocalDateTime end) {
        List<Order> completedOrders = orderRepository
                .findByMerchantIdAndStatusAndDateRange(merchantId, OrderStatus.COMPLETED, start, end);
        
        return completedOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate total refunds within the date range
     */
    private BigDecimal calculateTotalRefunds(Long merchantId, LocalDateTime start, LocalDateTime end) {
        List<Order> refundedOrders = orderRepository
                .findByMerchantIdAndStatusAndDateRange(merchantId, OrderStatus.REFUNDED, start, end);
        
        return refundedOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate actual balance change from transaction records
     * Balance change = SALE transactions - REFUND_OUT transactions
     */
    private BigDecimal calculateBalanceChange(Long merchantId, LocalDateTime start, LocalDateTime end) {
        // Get all SALE transactions (money in)
        List<TransactionRecord> saleTransactions = transactionRecordRepository
                .findByAccountTypeAndAccountIdAndTypeAndCreatedAtBetween(
                        "MERCHANT", merchantId, TransactionType.SALE, start, end);
        
        BigDecimal totalSaleAmount = saleTransactions.stream()
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Get all REFUND_OUT transactions (money out)
        List<TransactionRecord> refundTransactions = transactionRecordRepository
                .findByAccountTypeAndAccountIdAndTypeAndCreatedAtBetween(
                        "MERCHANT", merchantId, TransactionType.REFUND_OUT, start, end);
        
        BigDecimal totalRefundAmount = refundTransactions.stream()
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return totalSaleAmount.subtract(totalRefundAmount);
    }

    private SettlementResponse toSettlementResponse(Settlement settlement) {
        return SettlementResponse.builder()
                .id(settlement.getId())
                .merchantId(settlement.getMerchantId())
                .settlementDate(settlement.getSettlementDate())
                .totalSales(settlement.getTotalSales())
                .totalRefunds(settlement.getTotalRefunds())
                .netAmount(settlement.getNetAmount())
                .balanceChange(settlement.getBalanceChange())
                .discrepancy(settlement.getDiscrepancy())
                .status(settlement.getStatus())
                .createdAt(settlement.getCreatedAt())
                .build();
    }
}
