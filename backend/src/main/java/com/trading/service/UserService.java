package com.trading.service;

import com.trading.dto.request.DepositRequest;
import com.trading.dto.request.LoginRequest;
import com.trading.dto.request.UserRegisterRequest;
import com.trading.dto.response.BalanceResponse;
import com.trading.dto.response.TransactionResponse;
import com.trading.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface UserService {
    
    /**
     * Register a new user with zero balance
     * @param request registration details
     * @return created user response
     */
    UserResponse register(UserRegisterRequest request);
    
    /**
     * Authenticate user with credentials
     * @param request login credentials
     * @return authenticated user response
     */
    UserResponse login(LoginRequest request);
    
    /**
     * Get user by ID
     * @param userId user ID
     * @return user response
     */
    UserResponse getById(Long userId);
    
    /**
     * Get user's current balance
     * @param userId user ID
     * @return balance response
     */
    BalanceResponse getBalance(Long userId);
    
    /**
     * Deposit money to user's account
     * @param userId user ID
     * @param request deposit details
     * @return updated balance response
     */
    BalanceResponse deposit(Long userId, DepositRequest request);
    
    /**
     * 获取用户交易记录
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 交易记录分页列表
     */
    Page<TransactionResponse> getTransactions(Long userId, Pageable pageable);
}
