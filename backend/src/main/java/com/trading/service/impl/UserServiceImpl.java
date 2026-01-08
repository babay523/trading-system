package com.trading.service.impl;

import com.trading.dto.request.DepositRequest;
import com.trading.dto.request.LoginRequest;
import com.trading.dto.request.UserRegisterRequest;
import com.trading.dto.response.BalanceResponse;
import com.trading.dto.response.TransactionResponse;
import com.trading.dto.response.UserResponse;
import com.trading.entity.TransactionRecord;
import com.trading.entity.User;
import com.trading.exception.BusinessException;
import com.trading.exception.InvalidOperationException;
import com.trading.exception.ResourceNotFoundException;
import com.trading.repository.TransactionRecordRepository;
import com.trading.repository.UserRepository;
import com.trading.security.JwtUtil;
import com.trading.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    @Override
    @Transactional
    public UserResponse register(UserRegisterRequest request) {
        log.debug("注册新用户: {}", request.getUsername());
        
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(400, "Username already exists: " + request.getUsername());
        }
        
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword())) // 使用BCrypt加密密码
                .balance(BigDecimal.ZERO)
                .build();
        
        User savedUser = userRepository.save(user);
        log.info("用户注册成功: id={}, username={}", savedUser.getId(), savedUser.getUsername());
        
        return UserResponse.fromEntity(savedUser);
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest request) {
        log.debug("用户登录尝试: {}", request.getUsername());
        
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("尝试使用不存在的用户名登录: {}", request.getUsername());
                    return new BusinessException(401, "Invalid credentials");
                });
        
        // 使用BCrypt验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("用户名密码错误的登录尝试: {}", request.getUsername());
            throw new BusinessException(401, "Invalid credentials");
        }
        
        // 为用户生成JWT令牌（使用负数ID来区分用户和商家）
        String token = jwtUtil.generateToken(-user.getId(), user.getUsername(), "USER");
        
        log.info("用户登录成功: id={}, username={}", user.getId(), user.getUsername());
        
        UserResponse response = UserResponse.fromEntity(user);
        response.setToken(token); // 添加令牌到响应中
        return response;
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(Long userId) {
        User user = findUserById(userId);
        return UserResponse.fromEntity(user);
    }
    
    @Override
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(Long userId) {
        User user = findUserById(userId);
        return BalanceResponse.builder()
                .userId(user.getId())
                .balance(user.getBalance())
                .build();
    }
    
    @Override
    @Transactional
    public BalanceResponse deposit(Long userId, DepositRequest request) {
        log.debug("处理用户 {} 的充值: amount={}", userId, request.getAmount());
        
        BigDecimal amount = request.getAmount();
        
        // 验证充值金额 - 拒绝零或负数金额
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Deposit amount must be positive");
        }
        
        User user = findUserById(userId);
        BigDecimal newBalance = user.getBalance().add(amount);
        user.setBalance(newBalance);
        
        User savedUser = userRepository.save(user);
        log.info("用户 {} 充值成功: amount={}, newBalance={}", 
                userId, amount, savedUser.getBalance());
        
        return BalanceResponse.builder()
                .userId(savedUser.getId())
                .balance(savedUser.getBalance())
                .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(Long userId, Pageable pageable) {
        log.debug("获取用户 {} 的交易记录", userId);
        
        // 验证用户存在
        findUserById(userId);
        
        // 查询用户的交易记录
        Page<TransactionRecord> transactions = transactionRecordRepository
                .findByAccountTypeAndAccountId("USER", userId, pageable);
        
        // 转换为响应DTO
        return transactions.map(this::convertToResponse);
    }
    
    /**
     * 将交易记录实体转换为响应DTO
     */
    private TransactionResponse convertToResponse(TransactionRecord record) {
        return TransactionResponse.builder()
                .id(record.getId())
                .transactionId(record.getTransactionId())
                .type(record.getType())
                .amount(record.getAmount())
                .balanceBefore(record.getBalanceBefore())
                .balanceAfter(record.getBalanceAfter())
                .relatedOrderId(record.getRelatedOrderId())
                .createdAt(record.getCreatedAt())
                .build();
    }
    
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
