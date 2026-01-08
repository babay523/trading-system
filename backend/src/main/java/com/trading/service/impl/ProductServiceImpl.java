package com.trading.service.impl;

import com.trading.dto.request.ProductCreateRequest;
import com.trading.dto.response.ProductResponse;
import com.trading.entity.Product;
import com.trading.exception.ResourceNotFoundException;
import com.trading.repository.InventoryRepository;
import com.trading.repository.MerchantRepository;
import com.trading.repository.ProductRepository;
import com.trading.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {
    
    private final ProductRepository productRepository;
    private final MerchantRepository merchantRepository;
    private final InventoryRepository inventoryRepository;
    
    @Override
    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        log.debug("为商家 {} 创建新商品: {}", request.getMerchantId(), request.getName());
        
        // 验证商家是否存在
        if (!merchantRepository.existsById(request.getMerchantId())) {
            throw new ResourceNotFoundException("Merchant", request.getMerchantId());
        }
        
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .merchantId(request.getMerchantId())
                .build();
        
        Product savedProduct = productRepository.save(product);
        log.info("商品创建成功: id={}, name={}", savedProduct.getId(), savedProduct.getName());
        
        return ProductResponse.fromEntity(savedProduct);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        
        // 获取该商品的价格区间
        BigDecimal minPrice = inventoryRepository.findMinPriceByProductId(productId).orElse(BigDecimal.ZERO);
        BigDecimal maxPrice = inventoryRepository.findMaxPriceByProductId(productId).orElse(BigDecimal.ZERO);
        
        // 获取商家名称
        String merchantName = merchantRepository.findById(product.getMerchantId())
                .map(m -> m.getBusinessName() != null ? m.getBusinessName() : m.getUsername())
                .orElse("未知商家");
        
        return ProductResponse.fromEntityWithPrices(product, minPrice, maxPrice, merchantName);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> search(String keyword, String category, Pageable pageable) {
        log.debug("搜索商品: keyword={}, category={}", keyword, category);
        
        Page<Product> products;
        
        boolean hasKeyword = StringUtils.hasText(keyword);
        boolean hasCategory = StringUtils.hasText(category);
        
        if (hasKeyword && hasCategory) {
            products = productRepository.searchByKeywordAndCategory(keyword, category, pageable);
        } else if (hasKeyword) {
            products = productRepository.searchByKeyword(keyword, pageable);
        } else if (hasCategory) {
            products = productRepository.findByCategory(category, pageable);
        } else {
            products = productRepository.findAll(pageable);
        }
        
        return products.map(this::enrichWithPrices);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getByMerchant(Long merchantId, Pageable pageable) {
        // 验证商家是否存在
        if (!merchantRepository.existsById(merchantId)) {
            throw new ResourceNotFoundException("Merchant", merchantId);
        }
        
        Page<Product> products = productRepository.findByMerchantId(merchantId, pageable);
        return products.map(this::enrichWithPrices);
    }
    
    private ProductResponse enrichWithPrices(Product product) {
        BigDecimal minPrice = inventoryRepository.findMinPriceByProductId(product.getId()).orElse(BigDecimal.ZERO);
        BigDecimal maxPrice = inventoryRepository.findMaxPriceByProductId(product.getId()).orElse(BigDecimal.ZERO);
        
        // 获取商家名称
        String merchantName = merchantRepository.findById(product.getMerchantId())
                .map(m -> m.getBusinessName() != null ? m.getBusinessName() : m.getUsername())
                .orElse("未知商家");
        
        return ProductResponse.fromEntityWithPrices(product, minPrice, maxPrice, merchantName);
    }
}
