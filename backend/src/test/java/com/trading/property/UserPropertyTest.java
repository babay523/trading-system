package com.trading.property;

import com.trading.dto.request.DepositRequest;
import com.trading.dto.request.UserRegisterRequest;
import com.trading.dto.response.BalanceResponse;
import com.trading.dto.response.UserResponse;
import com.trading.exception.InvalidOperationException;
import com.trading.repository.UserRepository;
import com.trading.service.UserService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.BigRange;
import net.jqwik.api.constraints.Positive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for User module
 * Feature: trading-system
 * 
 * Note: Since jqwik doesn't integrate well with Spring's dependency injection,
 * we use JUnit 5 tests with randomized inputs to achieve property-based testing.
 */
@SpringBootTest
@ActiveProfiles("test")
public class UserPropertyTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    /**
     * Property 1: Account Initialization Correctness
     * For any valid user registration request, the created account SHALL have a balance of exactly zero.
     * Validates: Requirements 1.1
     */
    @Test
    void accountInitializationShouldHaveZeroBalance() {
        // Run 100 iterations with random inputs
        for (int i = 0; i < 100; i++) {
            String uniqueUsername = "user_" + UUID.randomUUID().toString().substring(0, 8);
            String password = "password" + i;
            
            UserRegisterRequest request = UserRegisterRequest.builder()
                    .username(uniqueUsername)
                    .password(password)
                    .build();

            UserResponse response = userService.register(request);

            // Property: newly created account must have zero balance
            assertThat(response.getBalance())
                    .as("User %s should have zero balance on creation", uniqueUsername)
                    .isEqualTo(BigDecimal.ZERO);
        }
    }

    /**
     * Property 2: Deposit Balance Correctness
     * For any user with initial balance B and for any valid deposit amount A (where A > 0),
     * after the deposit operation, the user's balance SHALL equal B + A.
     * Validates: Requirements 1.4
     */
    @Test
    void depositShouldIncreaseBalanceCorrectly() {
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < 100; i++) {
            // Create a new user
            String uniqueUsername = "user_" + UUID.randomUUID().toString().substring(0, 8);
            UserRegisterRequest registerRequest = UserRegisterRequest.builder()
                    .username(uniqueUsername)
                    .password("password123")
                    .build();
            UserResponse user = userService.register(registerRequest);
            
            // Get initial balance
            BigDecimal initialBalance = user.getBalance();
            
            // Generate random positive deposit amount (0.01 to 10000.00)
            BigDecimal depositAmount = BigDecimal.valueOf(random.nextDouble() * 9999.99 + 0.01)
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            
            DepositRequest depositRequest = DepositRequest.builder()
                    .amount(depositAmount)
                    .build();
            
            BalanceResponse result = userService.deposit(user.getId(), depositRequest);
            
            // Property: new balance = initial balance + deposit amount
            BigDecimal expectedBalance = initialBalance.add(depositAmount);
            assertThat(result.getBalance().compareTo(expectedBalance))
                    .as("Balance after deposit should be initial + deposit amount")
                    .isEqualTo(0);
        }
    }

    /**
     * Property 3: Invalid Deposit Rejection
     * For any deposit amount A where A <= 0, the deposit operation SHALL be rejected
     * and the user's balance SHALL remain unchanged.
     * Validates: Requirements 1.6
     */
    @Test
    void invalidDepositShouldBeRejected() {
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < 100; i++) {
            // Create a new user
            String uniqueUsername = "user_" + UUID.randomUUID().toString().substring(0, 8);
            UserRegisterRequest registerRequest = UserRegisterRequest.builder()
                    .username(uniqueUsername)
                    .password("password123")
                    .build();
            UserResponse user = userService.register(registerRequest);
            
            // Get initial balance
            BalanceResponse initialBalanceResponse = userService.getBalance(user.getId());
            BigDecimal initialBalance = initialBalanceResponse.getBalance();
            
            // Generate invalid deposit amount (zero or negative)
            BigDecimal invalidAmount;
            if (i % 2 == 0) {
                // Zero amount
                invalidAmount = BigDecimal.ZERO;
            } else {
                // Negative amount
                invalidAmount = BigDecimal.valueOf(-random.nextDouble() * 1000 - 0.01)
                        .setScale(2, java.math.RoundingMode.HALF_UP);
            }
            
            final BigDecimal finalInvalidAmount = invalidAmount;
            DepositRequest depositRequest = DepositRequest.builder()
                    .amount(finalInvalidAmount)
                    .build();
            
            // Property: invalid deposit should be rejected
            assertThatThrownBy(() -> userService.deposit(user.getId(), depositRequest))
                    .as("Deposit with amount %s should be rejected", finalInvalidAmount)
                    .isInstanceOf(InvalidOperationException.class);
            
            // Property: balance should remain unchanged
            BalanceResponse afterBalance = userService.getBalance(user.getId());
            assertThat(afterBalance.getBalance())
                    .as("Balance should remain unchanged after rejected deposit")
                    .isEqualTo(initialBalance);
        }
    }
}
