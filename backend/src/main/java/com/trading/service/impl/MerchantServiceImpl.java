package com.trading.service.impl;

import com.trading.dto.request.LoginRequest;
import com.trading.dto.request.MerchantRegisterRequest;
import com.trading.dto.response.AuthResponse;
import com.trading.dto.response.MerchantBalanceResponse;
import com.trading.dto.response.MerchantResponse;
import com.trading.dto.response.MerchantStatsResponse;
import com.trading.entity.Merchant;
import com.trading.enums.OrderStatus;
import com.trading.exception.BusinessException;
import com.trading.exception.ResourceNotFoundException;
import com.trading.repository.MerchantRepository;
import com.trading.repository.OrderRepository;
import com.trading.repository.ProductRepository;
import com.trading.service.AuthenticationService;
import com.trading.service.MerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantServiceImpl implements MerchantService {
    
    private final MerchantRepository merchantRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final AuthenticationService authenticationService;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    @Transactional
    public MerchantResponse register(MerchantRegisterRequest request) {
        log.debug("Registering new merchant: {}", request.getUsername());
        
        if (merchantRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(400, "Username already exists: " + request.getUsername());
        }
        
        Merchant merchant = Merchant.builder()
                .businessName(request.getBusinessName())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword())) // 使用BCrypt加密密码
                .balance(BigDecimal.ZERO)
                .build();
        
        Merchant savedMerchant = merchantRepository.save(merchant);
        log.info("Merchant registered successfully: id={}, username={}", 
                savedMerchant.getId(), savedMerchant.getUsername());
        
        return MerchantResponse.fromEntity(savedMerchant);
    }
    
    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.debug("Delegating login request to AuthenticationService for username: {}", request.getUsername());
        
        // 委托给AuthenticationService处理登录逻辑
        // AuthenticationService已经包含了完整的凭据验证和JWT令牌生成逻辑
        return authenticationService.login(request);
    }
    
    @Override
    @Transactional(readOnly = true)
    public MerchantResponse getById(Long merchantId) {
        Merchant merchant = findMerchantById(merchantId);
        return MerchantResponse.fromEntity(merchant);
    }
    
    @Override
    @Transactional(readOnly = true)
    public MerchantBalanceResponse getBalance(Long merchantId) {
        Merchant merchant = findMerchantById(merchantId);
        return MerchantBalanceResponse.builder()
                .merchantId(merchant.getId())
                .balance(merchant.getBalance())
                .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public MerchantStatsResponse getStats(Long merchantId) {
        log.debug("获取商家 {} 的统计数据", merchantId);
        
        // 验证商家存在
        findMerchantById(merchantId);
        
        // 统计商品数量
        Long productCount = productRepository.countByMerchantId(merchantId);
        
        // 统计待处理订单数量（状态为PAID的订单）
        Long pendingOrders = orderRepository.countByMerchantIdAndStatus(merchantId, OrderStatus.PAID);
        
        return MerchantStatsResponse.builder()
                .productCount(productCount)
                .pendingOrders(pendingOrders)
                .build();
    }
    
    private Merchant findMerchantById(Long merchantId) {
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId));
    }
}
