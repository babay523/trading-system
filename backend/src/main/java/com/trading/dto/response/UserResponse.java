package com.trading.dto.response;

import com.trading.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    
    private Long id;
    private String username;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    private String token; // JWT令牌
    
    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .balance(user.getBalance())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
