package com.trading.security;

import com.trading.config.JwtProperties;
import com.trading.exception.InvalidTokenException;
import com.trading.exception.TokenExpiredException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT工具类
 * 提供JWT令牌的生成、验证和解析功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtUtil {
    
    private final JwtProperties jwtProperties;
    
    /**
     * 生成JWT令牌
     * 
     * @param merchantId 商家ID
     * @param username 用户名
     * @return JWT令牌字符串
     */
    public String generateToken(Long merchantId, String username) {
        return generateToken(merchantId, username, "MERCHANT");
    }
    
    /**
     * 生成JWT令牌（带角色）
     * 
     * @param merchantId 商家ID
     * @param username 用户名
     * @param role 角色
     * @return JWT令牌字符串
     */
    public String generateToken(Long merchantId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("merchantId", merchantId);
        claims.put("username", username);
        claims.put("role", role);
        
        return createToken(claims, username);
    }
    
    /**
     * 从令牌中提取角色
     * 
     * @param token JWT令牌
     * @return 角色
     */
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        Object roleObj = claims.get("role");
        return roleObj != null ? roleObj.toString() : "MERCHANT";
    }
    
    /**
     * 验证JWT令牌
     * 
     * @param token JWT令牌
     * @return 令牌是否有效
     * @throws InvalidTokenException 令牌无效时抛出
     * @throws TokenExpiredException 令牌过期时抛出
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            throw new TokenExpiredException();
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token unsupported: {}", e.getMessage());
            throw new InvalidTokenException("Unsupported JWT token");
        } catch (MalformedJwtException e) {
            log.warn("JWT token malformed: {}", e.getMessage());
            throw new InvalidTokenException("Malformed JWT token");
        } catch (SecurityException e) {
            log.warn("JWT token signature invalid: {}", e.getMessage());
            throw new InvalidTokenException("Invalid JWT signature");
        } catch (IllegalArgumentException e) {
            log.warn("JWT token illegal argument: {}", e.getMessage());
            throw new InvalidTokenException("JWT token compact of handler are invalid");
        }
    }
    
    /**
     * 从令牌中提取商家ID
     * 
     * @param token JWT令牌
     * @return 商家ID
     */
    public Long extractMerchantId(String token) {
        Claims claims = extractAllClaims(token);
        Object merchantIdObj = claims.get("merchantId");
        
        if (merchantIdObj == null) {
            throw new InvalidTokenException("Merchant ID not found in token");
        }
        
        // Handle both Integer and Long types
        if (merchantIdObj instanceof Integer) {
            return ((Integer) merchantIdObj).longValue();
        } else if (merchantIdObj instanceof Long) {
            return (Long) merchantIdObj;
        } else {
            throw new InvalidTokenException("Invalid merchant ID format in token");
        }
    }
    
    /**
     * 从令牌中提取用户名
     * 
     * @param token JWT令牌
     * @return 用户名
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * 从令牌中提取过期时间
     * 
     * @param token JWT令牌
     * @return 过期时间
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    /**
     * 检查令牌是否过期
     * 
     * @param token JWT令牌
     * @return 是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            log.warn("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }
    
    /**
     * 从令牌中提取指定声明
     * 
     * @param token JWT令牌
     * @param claimsResolver 声明解析器
     * @param <T> 返回类型
     * @return 声明值
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * 从令牌中提取所有声明
     * 
     * @param token JWT令牌
     * @return 所有声明
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            throw new TokenExpiredException();
        } catch (Exception e) {
            log.warn("Error extracting claims from token: {}", e.getMessage());
            throw new InvalidTokenException("Invalid JWT token");
        }
    }
    
    /**
     * 创建JWT令牌
     * 
     * @param claims 声明
     * @param subject 主题（用户名）
     * @return JWT令牌字符串
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getExpiration());
        
        return Jwts.builder()
            .claims(claims)
            .subject(subject)
            .issuer(jwtProperties.getIssuer())
            .issuedAt(now)
            .expiration(expiration)
            .signWith(getSigningKey())
            .compact();
    }
    
    /**
     * 获取签名密钥
     * 
     * @return 签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecretKey().getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}