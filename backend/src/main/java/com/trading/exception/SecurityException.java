package com.trading.exception;

/**
 * 安全相关异常基类
 */
public class SecurityException extends BusinessException {
    
    public SecurityException(int code, String message) {
        super(code, message);
    }
    
    public SecurityException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}