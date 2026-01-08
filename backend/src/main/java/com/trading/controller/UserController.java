package com.trading.controller;

import com.trading.dto.request.DepositRequest;
import com.trading.dto.request.LoginRequest;
import com.trading.dto.request.UserRegisterRequest;
import com.trading.dto.response.ApiResponse;
import com.trading.dto.response.BalanceResponse;
import com.trading.dto.response.TransactionResponse;
import com.trading.dto.response.UserResponse;
import com.trading.security.SecurityContextUtil;
import com.trading.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 注册新用户
     * POST /api/v1/users/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody UserRegisterRequest request) {
        UserResponse user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(user));
    }

    /**
     * 用户登录
     * POST /api/v1/users/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        UserResponse user = userService.login(request);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * 获取用户余额
     * GET /api/v1/users/{id}/balance
     */
    @GetMapping("/{id}/balance")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(
            @PathVariable Long id) {
        // 验证用户只能访问自己的余额信息
        validateUserAccess(id);
        
        BalanceResponse balance = userService.getBalance(id);
        return ResponseEntity.ok(ApiResponse.success(balance));
    }

    /**
     * 用户充值
     * POST /api/v1/users/{id}/deposit
     */
    @PostMapping("/{id}/deposit")
    public ResponseEntity<ApiResponse<BalanceResponse>> deposit(
            @PathVariable Long id,
            @Valid @RequestBody DepositRequest request) {
        // 验证用户只能为自己充值
        validateUserAccess(id);
        
        BalanceResponse balance = userService.deposit(id, request);
        return ResponseEntity.ok(ApiResponse.success("Deposit successful", balance));
    }

    /**
     * 根据ID获取用户信息
     * GET /api/v1/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @PathVariable Long id) {
        // 验证用户只能访问自己的信息
        validateUserAccess(id);
        
        UserResponse user = userService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
    
    /**
     * 获取用户交易记录
     * GET /api/v1/users/{id}/transactions
     */
    @GetMapping("/{id}/transactions")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // 验证用户只能访问自己的交易记录
        validateUserAccess(id);
        
        // 按创建时间倒序排列（最新的在前）
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<TransactionResponse> transactions = userService.getTransactions(id, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }
    
    /**
     * 验证用户访问权限
     * 确保用户只能访问自己的资源
     * 
     * @param requestedUserId 请求访问的用户ID
     */
    private void validateUserAccess(Long requestedUserId) {
        // 管理员可以访问所有用户资源
        if (SecurityContextUtil.isAdmin()) {
            return;
        }
        
        // 普通用户只能访问自己的资源
        if (SecurityContextUtil.isUser()) {
            Long currentUserId = SecurityContextUtil.getCurrentUserId();
            if (!currentUserId.equals(requestedUserId)) {
                throw new com.trading.exception.UnauthorizedAccessException(
                    "Access denied: Users can only access their own resources");
            }
        } else {
            // 商家不能访问用户资源
            throw new com.trading.exception.UnauthorizedAccessException(
                "Access denied: Merchants cannot access user resources");
        }
    }
}
