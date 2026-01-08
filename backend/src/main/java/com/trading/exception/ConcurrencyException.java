package com.trading.exception;

public class ConcurrencyException extends BusinessException {
    
    public ConcurrencyException(String message) {
        super(409, message);
    }
    
    public ConcurrencyException() {
        super(409, "Concurrent modification detected, please retry");
    }
}
