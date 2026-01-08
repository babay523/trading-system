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
    private String productName;
    private Long merchantId;
    private Integer quantity;
    private BigDecimal price;
    private boolean inStock;
    
    public static InventoryResponse fromEntity(Inventory inventory) {
        return fromEntity(inventory, null);
    }
    
    public static InventoryResponse fromEntity(Inventory inventory, String productName) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .sku(inventory.getSku())
                .productId(inventory.getProductId())
                .productName(productName)
                .merchantId(inventory.getMerchantId())
                .quantity(inventory.getQuantity())
                .price(inventory.getPrice())
                .inStock(inventory.getQuantity() > 0)
                .build();
    }
}
