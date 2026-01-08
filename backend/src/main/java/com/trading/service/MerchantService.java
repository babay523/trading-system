package com.trading.service;

import com.trading.dto.request.LoginRequest;
import com.trading.dto.request.MerchantRegisterRequest;
import com.trading.dto.response.AuthResponse;
import com.trading.dto.response.MerchantBalanceResponse;
import com.trading.dto.response.MerchantResponse;
import com.trading.dto.response.MerchantStatsResponse;

public interface MerchantService {
    
    /**
     * Register a new merchant with zero balance
     * @param request registration details
     * @return created merchant response
     */
    MerchantResponse register(MerchantRegisterRequest request);
    
    /**
     * Authenticate merchant with credentials and generate JWT token
     * @param request login credentials
     * @return authentication response with JWT token and merchant info
     */
    AuthResponse login(LoginRequest request);
    
    /**
     * Get merchant by ID
     * @param merchantId merchant ID
     * @return merchant response
     */
    MerchantResponse getById(Long merchantId);
    
    /**
     * Get merchant's current balance
     * @param merchantId merchant ID
     * @return balance response
     */
    MerchantBalanceResponse getBalance(Long merchantId);
    
    /**
     * 获取商家统计数据
     * @param merchantId 商家ID
     * @return 统计数据响应
     */
    MerchantStatsResponse getStats(Long merchantId);
}
