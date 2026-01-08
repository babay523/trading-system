package com.trading.dto.response;

import com.trading.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    
    private Long id;
    private String name;
    private String description;
    private String category;
    private Long merchantId;
    private String merchantName;
    private LocalDateTime createdAt;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    
    public static ProductResponse fromEntity(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .merchantId(product.getMerchantId())
                .createdAt(product.getCreatedAt())
                .build();
    }
    
    public static ProductResponse fromEntityWithPrices(Product product, BigDecimal minPrice, BigDecimal maxPrice, String merchantName) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .merchantId(product.getMerchantId())
                .merchantName(merchantName)
                .createdAt(product.getCreatedAt())
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .build();
    }
}
