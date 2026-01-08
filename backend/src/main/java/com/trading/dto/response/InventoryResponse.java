package com.trading.dto.response;

import com.trading.entity.Inventory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {
    
    private Long id;
    private String sku;
    private Long productId;
    private Long merchantId;
    private Integer quantity;
    private BigDecimal price;
    private boolean inStock;
    
    public static InventoryResponse fromEntity(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .sku(inventory.getSku())
                .productId(inventory.getProductId())
                .merchantId(inventory.getMerchantId())
                .quantity(inventory.getQuantity())
                .price(inventory.getPrice())
                .inStock(inventory.getQuantity() > 0)
                .build();
    }
}
