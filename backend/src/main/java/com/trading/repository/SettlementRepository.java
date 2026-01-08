package com.trading.repository;

import com.trading.entity.Settlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    
    /**
     * Find settlement by merchant ID and date
     */
    Optional<Settlement> findByMerchantIdAndSettlementDate(Long merchantId, LocalDate settlementDate);
    
    /**
     * Find all settlements for a merchant with pagination
     */
    Page<Settlement> findByMerchantIdOrderBySettlementDateDesc(Long merchantId, Pageable pageable);
    
    /**
     * Check if settlement exists for merchant and date
     */
    boolean existsByMerchantIdAndSettlementDate(Long merchantId, LocalDate settlementDate);
}
