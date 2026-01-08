package com.trading.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解，用于标记需要商家所有权验证的方法
 * 当方法被此注解标记时，系统会验证当前认证的商家是否有权访问请求的资源
 * 
 * Requirements: 3.6
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireMerchantOwnership {
    
    /**
     * 可选的资源描述，用于日志记录和错误消息
     * @return 资源描述
     */
    String value() default "";
    
    /**
     * 是否记录访问日志
     * @return true表示记录访问日志，false表示不记录
     */
    boolean logAccess() default true;
}