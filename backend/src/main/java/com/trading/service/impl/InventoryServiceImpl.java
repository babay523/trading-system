package com.trading.service.impl;

import com.trading.dto.request.InventoryAddRequest;
import com.trading.dto.request.PriceUpdateRequest;
import com.trading.dto.response.InventoryResponse;
import com.trading.entity.Inventory;
import com.trading.entity.Product;
import com.trading.exception.InvalidOperationException;
import com.trading.exception.ResourceNotFoundException;
import com.trading.repository.InventoryRepository;
import com.trading.repository.MerchantRepository;
import com.trading.repository.ProductRepository;
import com.trading.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {
    
    private final InventoryRepository inventoryRepository;
    private final MerchantRepository merchantRepository;
    private final ProductRepository productRepository;
    
    @Override
    @Transactional
    public InventoryResponse addInventory(Long merchantId, InventoryAddRequest request) {
        log.debug("为商家 {} 添加库存: sku={}, quantity={}", 
                merchantId, request.getSku(), request.getQuantity());
        
        // 验证数量为正数
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new InvalidOperationException("Quantity must be positive");
        }
        
        // 验证商品是否存在
        if (!productRepository.existsById(request.getProductId())) {
            throw new ResourceNotFoundException("Product", request.getProductId());
        }
        
        // 检查该商家是否已有此SKU
        Optional<Inventory> existingInventory = inventoryRepository.findByMerchantIdAndSku(merchantId, request.getSku());
        
        Inventory inventory;
        if (existingInventory.isPresent()) {
            // 为现有SKU增加数量
            inventory = existingInventory.get();
            inventory.setQuantity(inventory.getQuantity() + request.getQuantity());
            // 如果提供了价格则更新价格
            if (request.getPrice() != null) {
                inventory.setPrice(request.getPrice());
            }
            log.info("增加库存 sku={}: newQuantity={}", request.getSku(), inventory.getQuantity());
        } else {
            // 创建新的库存项
            inventory = Inventory.builder()
                    .sku(request.getSku())
                    .productId(request.getProductId())
                    .merchantId(merchantId)
                    .quantity(request.getQuantity())
                    .price(request.getPrice())
                    .build();
            log.info("创建新库存: sku={}, quantity={}", request.getSku(), request.getQuantity());
        }
        
        Inventory savedInventory = inventoryRepository.save(inventory);
        return InventoryResponse.fromEntity(savedInventory);
    }
    
    @Override
    @Transactional
    public InventoryResponse updatePrice(Long merchantId, String sku, PriceUpdateRequest request) {
        log.debug("更新商家 {} SKU {} 的价格: newPrice={}", merchantId, sku, request.getPrice());
        
        Inventory inventory = inventoryRepository.findByMerchantIdAndSku(merchantId, sku)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory with SKU: " + sku));
        
        inventory.setPrice(request.getPrice());
        Inventory savedInventory = inventoryRepository.save(inventory);
        
        log.info("价格更新成功 sku={}: newPrice={}", sku, request.getPrice());
        return InventoryResponse.fromEntity(savedInventory);
    }
    
    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getBySku(String sku) {
        Inventory inventory = inventoryRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory with SKU: " + sku));
        
        // Fetch product name
        String productName = productRepository.findById(inventory.getProductId())
                .map(Product::getName)
                .orElse(null);
        
        return InventoryResponse.fromEntity(inventory, productName);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponse> getByMerchant(Long merchantId, Pageable pageable) {
        Page<Inventory> inventories = inventoryRepository.findByMerchantId(merchantId, pageable);
        List<InventoryResponse> enrichedResponses = enrichInventoryWithProductNames(inventories.getContent());
        return new org.springframework.data.domain.PageImpl<>(
                enrichedResponses,
                pageable,
                inventories.getTotalElements()
        );
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponse> getAll(Pageable pageable) {
        Page<Inventory> inventories = inventoryRepository.findAll(pageable);
        List<InventoryResponse> enrichedResponses = enrichInventoryWithProductNames(inventories.getContent());
        return new org.springframework.data.domain.PageImpl<>(
                enrichedResponses,
                pageable,
                inventories.getTotalElements()
        );
    }
    
    @Override
    @Transactional(readOnly = true)
    public java.util.List<InventoryResponse> getByProductId(Long productId) {
        // 验证商品是否存在
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", productId);
        }
        
        List<Inventory> inventories = inventoryRepository.findByProductId(productId);
        return enrichInventoryWithProductNames(inventories);
    }
    
    /**
     * Enrich inventory list with product names
     * Fetches product names in a single batch query to avoid N+1 query problem
     * 
     * @param inventories List of inventory entities to enrich
     * @return List of InventoryResponse objects with product names populated
     */
    private List<InventoryResponse> enrichInventoryWithProductNames(List<Inventory> inventories) {
        if (inventories == null || inventories.isEmpty()) {
            return List.of();
        }
        
        // Extract unique product IDs
        Set<Long> productIds = inventories.stream()
                .map(Inventory::getProductId)
                .collect(Collectors.toSet());
        
        // Fetch all products in one query
        List<Product> products = productRepository.findAllById(productIds);
        
        // Create productId -> productName map
        Map<Long, String> productNameMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Product::getName));
        
        // Build responses with product names
        return inventories.stream()
                .map(inventory -> {
                    String productName = productNameMap.get(inventory.getProductId());
                    return InventoryResponse.fromEntity(inventory, productName);
                })
                .toList();
    }
}
