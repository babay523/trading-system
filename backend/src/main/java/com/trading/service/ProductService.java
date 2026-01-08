package com.trading.service;

import com.trading.dto.request.ProductCreateRequest;
import com.trading.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    
    /**
     * Create a new product
     * @param request product creation details
     * @return created product response
     */
    ProductResponse create(ProductCreateRequest request);
    
    /**
     * Get product by ID
     * @param productId product ID
     * @return product response
     */
    ProductResponse getById(Long productId);
    
    /**
     * Search products by keyword and/or category
     * @param keyword search keyword (optional)
     * @param category category filter (optional)
     * @param pageable pagination info
     * @return page of products
     */
    Page<ProductResponse> search(String keyword, String category, Pageable pageable);
    
    /**
     * Get products by merchant
     * @param merchantId merchant ID
     * @param pageable pagination info
     * @return page of products
     */
    Page<ProductResponse> getByMerchant(Long merchantId, Pageable pageable);
}
