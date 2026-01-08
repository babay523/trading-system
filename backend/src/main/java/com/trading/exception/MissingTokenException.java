package com.trading.exception;

/**
 * 缺失令牌异常
 * 当请求需要JWT令牌但未提供时抛出
 */
public class MissingTokenException extends SecurityException {
    
    public MissingTokenException() {
        super(401, "Authentication token is required");
    }
    
    public MissingTokenException(String message) {
        super(401, "Authentication token is required: " + message);
    }
}