package com.trading.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantStatsResponse {
    
    /**
     * 商品数量（该商家的产品总数）
     */
    private Long productCount;
    
    /**
     * 待处理订单数量（状态为PAID的订单）
     */
    private Long pendingOrders;
}
