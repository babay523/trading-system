package com.trading.exception;

/**
 * 商家不匹配异常
 * 当商家试图访问不属于自己的资源时抛出
 */
public class MerchantMismatchException extends SecurityException {
    
    public MerchantMismatchException() {
        super(403, "Access denied: merchant ID mismatch");
    }
    
    public MerchantMismatchException(Long authenticatedMerchantId, Long requestedMerchantId) {
        super(403, String.format("Access denied: merchant ID mismatch. Authenticated: %d, Requested: %d", 
            authenticatedMerchantId, requestedMerchantId));
    }
}