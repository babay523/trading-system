package com.trading.dto.response;

import com.trading.entity.Merchant;
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
public class MerchantResponse {
    
    private Long id;
    private String businessName;
    private String username;
    private String role;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    
    public static MerchantResponse fromEntity(Merchant merchant) {
        return MerchantResponse.builder()
                .id(merchant.getId())
                .businessName(merchant.getBusinessName())
                .username(merchant.getUsername())
                .role(merchant.getRole())
                .balance(merchant.getBalance())
                .createdAt(merchant.getCreatedAt())
                .build();
    }
}
