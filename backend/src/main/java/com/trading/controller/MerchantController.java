package com.trading.controller;

import com.trading.dto.request.InventoryAddRequest;
import com.trading.dto.request.LoginRequest;
import com.trading.dto.request.MerchantRegisterRequest;
import com.trading.dto.request.PriceUpdateRequest;
import com.trading.dto.response.ApiResponse;
import com.trading.dto.response.AuthResponse;
import com.trading.dto.response.InventoryResponse;
import com.trading.dto.response.MerchantBalanceResponse;
import com.trading.dto.response.MerchantResponse;
import com.trading.dto.response.MerchantStatsResponse;
import com.trading.dto.response.SettlementResponse;
import com.trading.security.RequireMerchantOwnership;
import com.trading.service.InventoryService;
import com.trading.service.MerchantService;
import com.trading.service.SettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;
    private final InventoryService inventoryService;
    private final SettlementService settlementService;

    /**
     * 注册新商家
     * POST /api/v1/merchants/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<MerchantResponse>> register(
            @Valid @RequestBody MerchantRegisterRequest request) {
        MerchantResponse merchant = merchantService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(merchant));
    }

    /**
     * 商家登录
     * POST /api/v1/merchants/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = merchantService.login(request);
        return ResponseEntity.ok(ApiResponse.success(authResponse));
    }

    /**
     * 根据ID获取商家信息
     * GET /api/v1/merchants/{id}
     */
    @GetMapping("/{id}")
    @RequireMerchantOwnership("merchant profile")
    public ResponseEntity<ApiResponse<MerchantResponse>> getMerchant(
            @PathVariable Long id) {
        MerchantResponse merchant = merchantService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(merchant));
    }

    /**
     * 获取商家余额
     * GET /api/v1/merchants/{id}/balance
     */
    @GetMapping("/{id}/balance")
    @RequireMerchantOwnership("merchant balance")
    public ResponseEntity<ApiResponse<MerchantBalanceResponse>> getBalance(
            @PathVariable Long id) {
        MerchantBalanceResponse balance = merchantService.getBalance(id);
        return ResponseEntity.ok(ApiResponse.success(balance));
    }
    
    /**
     * 获取商家统计数据
     * GET /api/v1/merchants/{id}/stats
     */
    @GetMapping("/{id}/stats")
    @RequireMerchantOwnership("merchant stats")
    public ResponseEntity<ApiResponse<MerchantStatsResponse>> getStats(
            @PathVariable Long id) {
        MerchantStatsResponse stats = merchantService.getStats(id);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 为商家添加库存
     * POST /api/v1/merchants/{id}/inventory
     */
    @PostMapping("/{id}/inventory")
    @RequireMerchantOwnership("inventory management")
    public ResponseEntity<ApiResponse<InventoryResponse>> addInventory(
            @PathVariable Long id,
            @Valid @RequestBody InventoryAddRequest request) {
        InventoryResponse inventory = inventoryService.addInventory(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(inventory));
    }

    /**
     * 更新库存价格
     * PUT /api/v1/merchants/{id}/inventory/{sku}/price
     */
    @PutMapping("/{id}/inventory/{sku}/price")
    @RequireMerchantOwnership("inventory price update")
    public ResponseEntity<ApiResponse<InventoryResponse>> updatePrice(
            @PathVariable Long id,
            @PathVariable String sku,
            @Valid @RequestBody PriceUpdateRequest request) {
        InventoryResponse inventory = inventoryService.updatePrice(id, sku, request);
        return ResponseEntity.ok(ApiResponse.success("Price updated successfully", inventory));
    }

    /**
     * 获取商家库存
     * GET /api/v1/merchants/{id}/inventory
     */
    @GetMapping("/{id}/inventory")
    @RequireMerchantOwnership("inventory access")
    public ResponseEntity<ApiResponse<Page<InventoryResponse>>> getInventory(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<InventoryResponse> inventory;
        // 管理员可以查看所有库存（当id为0时）
        if (id == 0 && com.trading.security.SecurityContextUtil.isAdmin()) {
            inventory = inventoryService.getAll(pageable);
        } else {
            inventory = inventoryService.getByMerchant(id, pageable);
        }
        return ResponseEntity.ok(ApiResponse.success(inventory));
    }

    /**
     * 获取商家结算历史
     * GET /api/v1/merchants/{id}/settlements
     */
    @GetMapping("/{id}/settlements")
    @RequireMerchantOwnership("settlement records")
    public ResponseEntity<ApiResponse<Page<SettlementResponse>>> getSettlements(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<SettlementResponse> settlements = settlementService.getByMerchant(id, pageable);
        return ResponseEntity.ok(ApiResponse.success(settlements));
    }

    /**
     * 为商家在指定日期运行结算
     * POST /api/v1/merchants/{id}/settlements/run
     */
    @PostMapping("/{id}/settlements/run")
    @RequireMerchantOwnership("settlement execution")
    public ResponseEntity<ApiResponse<SettlementResponse>> runSettlement(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate settlementDate = date != null ? date : LocalDate.now();
        SettlementResponse settlement = settlementService.runSettlementForMerchant(id, settlementDate);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(settlement));
    }
}
