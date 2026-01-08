package com.trading.repository;

import com.trading.entity.Order;
import com.trading.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Page<Order> findByUserId(Long userId, Pageable pageable);
    
    Page<Order> findByMerchantId(Long merchantId, Pageable pageable);
    
    Page<Order> findByMerchantIdAndStatus(Long merchantId, OrderStatus status, Pageable pageable);
    
    Long countByMerchantIdAndStatus(Long merchantId, OrderStatus status);
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    List<Order> findByMerchantIdAndStatus(Long merchantId, OrderStatus status);
    
    @Query("SELECT o FROM Order o WHERE o.merchantId = :merchantId AND o.status = :status " +
           "AND o.updatedAt >= :startDate AND o.updatedAt < :endDate")
    List<Order> findByMerchantIdAndStatusAndDateRange(
            @Param("merchantId") Long merchantId,
            @Param("status") OrderStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
