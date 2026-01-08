package com.trading.exception;

/**
 * 无效令牌异常
 * 当JWT令牌格式错误、签名无效或其他验证失败时抛出
 */
public class InvalidTokenException extends SecurityException {
    
    public InvalidTokenException(String message) {
        super(401, "Invalid token: " + message);
    }
    
    public InvalidTokenException(String message, Throwable cause) {
        super(401, "Invalid token: " + message, cause);
    }
}