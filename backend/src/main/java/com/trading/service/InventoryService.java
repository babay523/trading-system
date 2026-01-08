package com.trading.service;

import com.trading.dto.request.InventoryAddRequest;
import com.trading.dto.request.PriceUpdateRequest;
import com.trading.dto.response.InventoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryService {
    
    /**
     * Add inventory for a merchant
     * If SKU exists, increase quantity; otherwise create new inventory item
     * @param merchantId merchant ID
     * @param request inventory add request
     * @return inventory response
     */
    InventoryResponse addInventory(Long merchantId, InventoryAddRequest request);
    
    /**
     * Update price for a SKU
     * @param merchantId merchant ID
     * @param sku SKU code
     * @param request price update request
     * @return updated inventory response
     */
    InventoryResponse updatePrice(Long merchantId, String sku, PriceUpdateRequest request);
    
    /**
     * Get inventory by SKU
     * @param sku SKU code
     * @return inventory response
     */
    InventoryResponse getBySku(String sku);
    
    /**
     * Get all inventory for a merchant
     * @param merchantId merchant ID
     * @param pageable pagination info
     * @return page of inventory items
     */
    Page<InventoryResponse> getByMerchant(Long merchantId, Pageable pageable);
    
    /**
     * Get all inventory (for admin)
     * @param pageable pagination info
     * @return page of all inventory items
     */
    Page<InventoryResponse> getAll(Pageable pageable);
    
    /**
     * Get all inventory for a product
     * @param productId product ID
     * @return list of inventory items (SKUs) for the product
     */
    java.util.List<InventoryResponse> getByProductId(Long productId);
}
