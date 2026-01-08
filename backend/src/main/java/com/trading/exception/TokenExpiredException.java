package com.trading.exception;

/**
 * 令牌过期异常
 * 当JWT令牌已过期时抛出
 */
public class TokenExpiredException extends SecurityException {
    
    public TokenExpiredException() {
        super(401, "Token has expired");
    }
    
    public TokenExpiredException(String message) {
        super(401, "Token has expired: " + message);
    }
}