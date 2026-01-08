package com.trading.exception;

import java.math.BigDecimal;

public class InsufficientBalanceException extends BusinessException {
    
    public InsufficientBalanceException(String message) {
        super(400, message);
    }
    
    public InsufficientBalanceException(BigDecimal required, BigDecimal available) {
        super(400, String.format("Insufficient balance: required %.2f, available %.2f", 
                required, available));
    }
}
