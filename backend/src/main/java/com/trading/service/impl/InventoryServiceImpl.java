package com.trading.service.impl;

import com.trading.dto.request.InventoryAddRequest;
import com.trading.dto.request.PriceUpdateRequest;
import com.trading.dto.response.InventoryResponse;
import com.trading.entity.Inventory;
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

import java.util.Optional;

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
        return InventoryResponse.fromEntity(inventory);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponse> getByMerchant(Long merchantId, Pageable pageable) {
        Page<Inventory> inventories = inventoryRepository.findByMerchantId(merchantId, pageable);
        return inventories.map(InventoryResponse::fromEntity);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponse> getAll(Pageable pageable) {
        Page<Inventory> inventories = inventoryRepository.findAll(pageable);
        return inventories.map(InventoryResponse::fromEntity);
    }
    
    @Override
    @Transactional(readOnly = true)
    public java.util.List<InventoryResponse> getByProductId(Long productId) {
        // 验证商品是否存在
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", productId);
        }
        
        return inventoryRepository.findByProductId(productId).stream()
                .map(InventoryResponse::fromEntity)
                .toList();
    }
}
