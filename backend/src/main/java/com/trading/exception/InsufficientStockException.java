package com.trading.exception;

public class InsufficientStockException extends BusinessException {
    
    public InsufficientStockException(String message) {
        super(400, message);
    }
    
    public InsufficientStockException(String sku, int required, int available) {
        super(400, String.format("Insufficient stock for SKU %s: required %d, available %d", 
                sku, required, available));
    }
}
