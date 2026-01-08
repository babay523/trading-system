package com.trading.repository;

import com.trading.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    
    Optional<Inventory> findBySku(String sku);
    
    boolean existsBySku(String sku);
    
    Page<Inventory> findByMerchantId(Long merchantId, Pageable pageable);
    
    List<Inventory> findByMerchantId(Long merchantId);
    
    Optional<Inventory> findByMerchantIdAndSku(Long merchantId, String sku);
    
    List<Inventory> findByProductId(Long productId);
    
    @Query("SELECT MIN(i.price) FROM Inventory i WHERE i.productId = :productId AND i.quantity > 0")
    Optional<BigDecimal> findMinPriceByProductId(@Param("productId") Long productId);
    
    @Query("SELECT MAX(i.price) FROM Inventory i WHERE i.productId = :productId AND i.quantity > 0")
    Optional<BigDecimal> findMaxPriceByProductId(@Param("productId") Long productId);
    
    @Query("SELECT MIN(i.price), MAX(i.price) FROM Inventory i WHERE i.productId IN :productIds AND i.quantity > 0 GROUP BY i.productId")
    List<Object[]> findPriceRangesByProductIds(@Param("productIds") List<Long> productIds);
}
