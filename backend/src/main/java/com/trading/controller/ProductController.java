package com.trading.controller;

import com.trading.dto.request.ProductCreateRequest;
import com.trading.dto.response.ApiResponse;
import com.trading.dto.response.InventoryResponse;
import com.trading.dto.response.ProductResponse;
import com.trading.service.InventoryService;
import com.trading.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final InventoryService inventoryService;

    /**
     * 创建新商品
     * POST /api/v1/products
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @Valid @RequestBody ProductCreateRequest request) {
        ProductResponse product = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(product));
    }

    /**
     * 根据关键词和/或分类搜索商品
     * GET /api/v1/products
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long merchantId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ProductResponse> products;
        if (merchantId != null) {
            // 如果指定了商家ID，只返回该商家的商品
            products = productService.getByMerchant(merchantId, pageable);
        } else {
            products = productService.search(keyword, category, pageable);
        }
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * 根据ID获取商品
     * GET /api/v1/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(
            @PathVariable Long id) {
        ProductResponse product = productService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    /**
     * 获取商品的库存信息（SKU列表）
     * GET /api/v1/products/{id}/inventory
     */
    @GetMapping("/{id}/inventory")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getProductInventory(
            @PathVariable Long id) {
        List<InventoryResponse> inventory = inventoryService.getByProductId(id);
        return ResponseEntity.ok(ApiResponse.success(inventory));
    }
}
