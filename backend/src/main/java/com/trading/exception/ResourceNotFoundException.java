package com.trading.exception;

public class ResourceNotFoundException extends BusinessException {
    
    public ResourceNotFoundException(String message) {
        super(404, message);
    }
    
    public ResourceNotFoundException(String resourceName, Long id) {
        super(404, resourceName + " not found with id: " + id);
    }
    
    public ResourceNotFoundException(String resourceName, String identifier) {
        super(404, resourceName + " not found: " + identifier);
    }
}
