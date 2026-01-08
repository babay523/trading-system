package com.trading.security;

import com.trading.exception.MerchantMismatchException;
import com.trading.exception.UnauthorizedAccessException;
import com.trading.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 商家授权切面
 * 实现AOP切面，拦截带有@RequireMerchantOwnership注解的方法
 * 验证当前认证商家是否有权访问请求的资源
 * 
 * Requirements: 2.4, 2.6, 3.5
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class MerchantAuthorizationAspect {
    
    private final AuthenticationService authenticationService;
    
    /**
     * 在执行带有@RequireMerchantOwnership注解的方法之前进行商家所有权检查
     * 
     * @param joinPoint 连接点
     * @throws MerchantMismatchException 当商家ID不匹配时
     * @throws UnauthorizedAccessException 当用户未认证时
     */
    @Before("@annotation(RequireMerchantOwnership)")
    public void checkMerchantOwnership(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireMerchantOwnership annotation = method.getAnnotation(RequireMerchantOwnership.class);
        
        // 获取当前认证的商家ID
        Long currentMerchantId;
        try {
            currentMerchantId = authenticationService.getCurrentMerchantId();
        } catch (Exception e) {
            log.warn("Failed to get current merchant ID: {}", e.getMessage());
            throw new UnauthorizedAccessException("Authentication required");
        }
        
        // 从方法参数中提取商家ID
        Long requestedMerchantId = extractMerchantIdFromArgs(joinPoint);
        
        if (requestedMerchantId == null) {
            log.warn("Could not extract merchant ID from method arguments for method: {}", 
                    method.getName());
            return; // 如果无法提取商家ID，跳过检查
        }
        
        // 验证商家ID是否匹配
        if (!currentMerchantId.equals(requestedMerchantId)) {
            String resourceDescription = annotation.value().isEmpty() ? 
                method.getName() : annotation.value();
            
            log.warn("Merchant access denied: current={}, requested={}, resource={}", 
                    currentMerchantId, requestedMerchantId, resourceDescription);
            
            // 记录安全事件
            if (annotation.logAccess()) {
                auditSecurityEvent("MERCHANT_MISMATCH", currentMerchantId, 
                    String.format("Attempted to access merchant %d resources", requestedMerchantId));
            }
            
            throw new MerchantMismatchException(currentMerchantId, requestedMerchantId);
        }
        
        // 记录成功的访问（如果启用了日志记录）
        if (annotation.logAccess()) {
            String resourceDescription = annotation.value().isEmpty() ? 
                method.getName() : annotation.value();
            log.debug("Merchant access granted: merchantId={}, resource={}", 
                    currentMerchantId, resourceDescription);
        }
    }
    
    /**
     * 在控制器方法抛出异常后记录安全违规事件
     * 
     * @param joinPoint 连接点
     * @param ex 抛出的异常
     */
    @AfterThrowing(pointcut = "execution(* com.trading.controller..*(..))", throwing = "ex")
    public void logSecurityViolation(JoinPoint joinPoint, Throwable ex) {
        if (ex instanceof UnauthorizedAccessException || ex instanceof MerchantMismatchException) {
            try {
                Long currentMerchantId = authenticationService.getCurrentMerchantId();
                String methodName = joinPoint.getSignature().getName();
                String className = joinPoint.getTarget().getClass().getSimpleName();
                
                auditSecurityEvent("SECURITY_VIOLATION", currentMerchantId, 
                    String.format("Exception in %s.%s: %s", className, methodName, ex.getMessage()));
            } catch (Exception e) {
                // 如果无法获取当前商家ID，记录匿名访问违规
                auditSecurityEvent("SECURITY_VIOLATION", null, 
                    String.format("Anonymous access violation: %s", ex.getMessage()));
            }
        }
    }
    
    /**
     * 从方法参数中提取商家ID
     * 查找带有@PathVariable注解且名称为"id"或"merchantId"的参数
     * 
     * @param joinPoint 连接点
     * @return 提取到的商家ID，如果未找到则返回null
     */
    private Long extractMerchantIdFromArgs(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            
            // 检查是否有@PathVariable注解
            PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
            if (pathVariable != null) {
                String paramName = pathVariable.value().isEmpty() ? 
                    pathVariable.name() : pathVariable.value();
                
                // 如果参数名为空，使用参数的实际名称
                if (paramName.isEmpty()) {
                    paramName = parameter.getName();
                }
                
                // 检查参数名是否为商家ID相关的名称
                if ("id".equals(paramName) || "merchantId".equals(paramName)) {
                    Object arg = args[i];
                    if (arg instanceof Long) {
                        return (Long) arg;
                    } else if (arg instanceof String) {
                        try {
                            return Long.parseLong((String) arg);
                        } catch (NumberFormatException e) {
                            log.warn("Failed to parse merchant ID from string: {}", arg);
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * 记录安全审计事件
     * 包含IP地址、时间戳、用户代理等详细安全信息
     * 
     * @param eventType 事件类型
     * @param merchantId 商家ID
     * @param details 事件详情
     */
    private void auditSecurityEvent(String eventType, Long merchantId, String details) {
        // 使用专门的安全日志记录器
        org.slf4j.Logger securityLogger = org.slf4j.LoggerFactory.getLogger("SECURITY");
        
        String merchantInfo = merchantId != null ? merchantId.toString() : "ANONYMOUS";
        
        // 尝试获取当前HTTP请求信息
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String clientIp = getClientIpAddress(request);
                String userAgent = request.getHeader("User-Agent");
                String requestUri = request.getRequestURI();
                String method = request.getMethod();
                
                securityLogger.warn("Security Event: {} | Method: {} | URI: {} | Merchant: {} | IP: {} | Details: {} | UserAgent: {} | Timestamp: {}", 
                    eventType, method, requestUri, merchantInfo, clientIp, details, userAgent, java.time.LocalDateTime.now());
            } else {
                // 如果无法获取请求信息，记录基本信息
                securityLogger.warn("Security Event: {} | Merchant: {} | Details: {} | Timestamp: {}", 
                    eventType, merchantInfo, details, java.time.LocalDateTime.now());
            }
        } catch (Exception e) {
            // 如果获取请求信息失败，记录基本信息
            securityLogger.warn("Security Event: {} | Merchant: {} | Details: {} | Timestamp: {} | Note: Failed to get request details", 
                eventType, merchantInfo, details, java.time.LocalDateTime.now());
        }
    }
    
    /**
     * 获取客户端真实IP地址
     * 考虑代理和负载均衡器的情况
     * 
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}