package com.trading.exception;

/**
 * 未授权访问异常
 * 当用户试图访问没有权限的资源时抛出
 */
public class UnauthorizedAccessException extends SecurityException {
    
    public UnauthorizedAccessException(String message) {
        super(403, message);
    }
    
    public UnauthorizedAccessException(String resource, String reason) {
        super(403, "Access denied to resource: " + resource + ". Reason: " + reason);
    }
}