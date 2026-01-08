# Design Document: Merchant Authorization Fix

## Overview

本设计文档描述了修复商家权限控制漏洞的技术实现方案。当前系统存在严重的安全问题：商家可以通过修改URL参数访问其他商家的敏感信息。本方案通过实现JWT（JSON Web Token）身份验证和基于角色的访问控制（RBAC）来解决这个问题。

### Technology Stack

- **Authentication**: JWT (JSON Web Token) with HS256 signing
- **Framework**: Spring Security 6.x with Spring Boot 3.x
- **Token Library**: jjwt-api, jjwt-impl, jjwt-jackson
- **Filter**: OncePerRequestFilter for request interception
- **Configuration**: Spring Boot application.yml for security settings

## Architecture

### Security Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    Client Request                                │
│                         │                                       │
│                         ▼                                       │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │              JWT Authentication Filter                       ││
│  │  1. Extract JWT from Authorization header                   ││
│  │  2. Validate token signature and expiration                 ││
│  │  3. Extract merchant ID from token                          ││
│  │  4. Set SecurityContext with authenticated merchant         ││
│  └─────────────────────┬───────────────────────────────────────┘│
│                        │                                        │
│                        ▼                                        │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                Controller Layer                              ││
│  │  1. Receive authenticated request                           ││
│  │  2. Extract current merchant ID from SecurityContext       ││
│  │  3. Validate merchant ID matches path parameter            ││
│  │  4. Proceed with business logic if authorized              ││
│  └─────────────────────┬───────────────────────────────────────┘│
│                        │                                        │
│                        ▼                                        │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                Service Layer                                 ││
│  │  Business logic with merchant-specific data access         ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

### Component Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Security Components                           │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │  JwtUtil     │  │ JwtAuthFilter│  │SecurityConfig│           │
│  │              │  │              │  │              │           │
│  │ - generate() │  │ - doFilter() │  │ - configure()│           │
│  │ - validate() │  │ - extract()  │  │ - filterChain│           │
│  │ - extract()  │  │ - authenticate│  │ - corsConfig │           │
│  └──────────────┘  └──────────────┘  └──────────────┘           │
├─────────────────────────────────────────────────────────────────┤
│                   Authorization Components                       │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │AuthService   │  │AuthController│  │AuthAspect    │           │
│  │              │  │              │  │              │           │
│  │ - login()    │  │ - login()    │  │ - checkOwner │           │
│  │ - refresh()  │  │ - refresh()  │  │ - logAccess  │           │
│  │ - validate() │  │ - validate() │  │ - auditLog   │           │
│  └──────────────┘  └──────────────┘  └──────────────┘           │
└─────────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### JWT Utility Service

```java
@Service
public class JwtUtil {
    
    // JWT Configuration
    private String secretKey;
    private long jwtExpiration;
    private String issuer;
    
    // Core JWT Operations
    public String generateToken(Long merchantId, String username);
    public boolean validateToken(String token);
    public Long extractMerchantId(String token);
    public String extractUsername(String token);
    public Date extractExpiration(String token);
    public boolean isTokenExpired(String token);
    
    // Token Parsing
    private Claims extractAllClaims(String token);
    private String createToken(Map<String, Object> claims, String subject);
    private Boolean validateToken(String token, String username);
}
```

### JWT Authentication Filter

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;
    private final MerchantService merchantService;
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException;
    
    // Helper Methods
    private String extractTokenFromRequest(HttpServletRequest request);
    private void setAuthenticationContext(String token);
    private boolean shouldSkipAuthentication(String requestPath);
    private void handleAuthenticationFailure(HttpServletResponse response, String error);
}
```

### Security Configuration

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint jwtAuthEntryPoint;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception;
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config);
    
    @Bean
    public PasswordEncoder passwordEncoder();
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource();
}
```

### Enhanced Authentication Service

```java
@Service
public class AuthenticationService {
    
    private final MerchantService merchantService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    
    // Authentication Operations
    public AuthResponse login(LoginRequest request);
    public AuthResponse refreshToken(String refreshToken);
    public void logout(String token);
    public boolean validateToken(String token);
    
    // Security Utilities
    public Long getCurrentMerchantId();
    public boolean isCurrentMerchant(Long merchantId);
    public void validateMerchantAccess(Long merchantId);
}
```

### Authorization Aspect

```java
@Aspect
@Component
public class MerchantAuthorizationAspect {
    
    private final AuthenticationService authService;
    
    // Authorization Checks
    @Before("@annotation(RequireMerchantOwnership)")
    public void checkMerchantOwnership(JoinPoint joinPoint);
    
    @AfterThrowing("execution(* com.trading.controller..*(..))")
    public void logSecurityViolation(JoinPoint joinPoint, Throwable ex);
    
    // Utility Methods
    private Long extractMerchantIdFromArgs(Object[] args);
    private void auditSecurityEvent(String event, Long merchantId, String details);
}
```

## Data Models

### JWT Token Structure

```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "merchant_username",
    "merchantId": 123,
    "username": "merchant_username",
    "iat": 1640995200,
    "exp": 1641081600,
    "iss": "trading-system"
  },
  "signature": "HMACSHA256(base64UrlEncode(header) + '.' + base64UrlEncode(payload), secret)"
}
```

### Authentication DTOs

```java
// Login Request
@Data
public class LoginRequest {
    @NotBlank
    private String username;
    
    @NotBlank
    private String password;
}

// Authentication Response
@Data
public class AuthResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private MerchantResponse merchant;
    private LocalDateTime issuedAt;
}

// JWT Claims
@Data
public class JwtClaims {
    private Long merchantId;
    private String username;
    private Date issuedAt;
    private Date expiration;
    private String issuer;
}
```

### Security Context

```java
// Custom Authentication Token
public class MerchantAuthenticationToken extends AbstractAuthenticationToken {
    private final Long merchantId;
    private final String username;
    private final String token;
    
    public MerchantAuthenticationToken(Long merchantId, String username, String token);
    
    @Override
    public Object getCredentials();
    
    @Override
    public Object getPrincipal();
}

// Security Context Holder Utility
@Component
public class SecurityContextUtil {
    
    public static Long getCurrentMerchantId();
    public static String getCurrentUsername();
    public static boolean isAuthenticated();
    public static MerchantAuthenticationToken getCurrentAuthentication();
}
```

## API Changes

### Updated Merchant Controller

```java
@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
public class MerchantController {
    
    // Public endpoints (no authentication required)
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<MerchantResponse>> register(@Valid @RequestBody MerchantRegisterRequest request);
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request);
    
    // Protected endpoints (JWT authentication required)
    @GetMapping("/{id}")
    @RequireMerchantOwnership
    public ResponseEntity<ApiResponse<MerchantResponse>> getMerchant(@PathVariable Long id);
    
    @GetMapping("/{id}/balance")
    @RequireMerchantOwnership
    public ResponseEntity<ApiResponse<MerchantBalanceResponse>> getBalance(@PathVariable Long id);
    
    @PostMapping("/{id}/inventory")
    @RequireMerchantOwnership
    public ResponseEntity<ApiResponse<InventoryResponse>> addInventory(
        @PathVariable Long id,
        @Valid @RequestBody InventoryAddRequest request);
    
    @PutMapping("/{id}/inventory/{sku}/price")
    @RequireMerchantOwnership
    public ResponseEntity<ApiResponse<InventoryResponse>> updatePrice(
        @PathVariable Long id,
        @PathVariable String sku,
        @Valid @RequestBody PriceUpdateRequest request);
    
    @GetMapping("/{id}/inventory")
    @RequireMerchantOwnership
    public ResponseEntity<ApiResponse<Page<InventoryResponse>>> getInventory(
        @PathVariable Long id,
        @PageableDefault(size = 20) Pageable pageable);
    
    @GetMapping("/{id}/settlements")
    @RequireMerchantOwnership
    public ResponseEntity<ApiResponse<Page<SettlementResponse>>> getSettlements(
        @PathVariable Long id,
        @PageableDefault(size = 20) Pageable pageable);
}
```

### Authentication Controller

```java
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthenticationService authService;
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request);
    
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestBody RefreshTokenRequest request);
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request);
    
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateToken(HttpServletRequest request);
    
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<MerchantResponse>> getCurrentMerchant();
}
```

## Configuration

### Security Properties

```yaml
# application.yml
trading:
  security:
    jwt:
      secret-key: ${JWT_SECRET_KEY:your-256-bit-secret-key-here}
      expiration: ${JWT_EXPIRATION:86400000} # 24 hours in milliseconds
      issuer: ${JWT_ISSUER:trading-system}
      header: ${JWT_HEADER:Authorization}
      prefix: ${JWT_PREFIX:Bearer }
    
    cors:
      allowed-origins: ${CORS_ORIGINS:http://localhost:3000,http://localhost:8080}
      allowed-methods: ${CORS_METHODS:GET,POST,PUT,DELETE,OPTIONS}
      allowed-headers: ${CORS_HEADERS:*}
      allow-credentials: ${CORS_CREDENTIALS:true}
    
    endpoints:
      public:
        - /api/v1/merchants/register
        - /api/v1/merchants/login
        - /api/v1/auth/**
        - /api/v1/products/**
        - /h2-console/**
        - /actuator/health
      
      protected:
        - /api/v1/merchants/{id}/**
        - /api/v1/cart/**
        - /api/v1/orders/**

logging:
  level:
    com.trading.security: DEBUG
    org.springframework.security: DEBUG
```

### Environment-Specific Configuration

```yaml
# application-dev.yml
trading:
  security:
    jwt:
      expiration: 3600000 # 1 hour for development
    authentication:
      enabled: true

# application-test.yml
trading:
  security:
    authentication:
      enabled: false # Disable for integration tests

# application-prod.yml
trading:
  security:
    jwt:
      expiration: 86400000 # 24 hours for production
    authentication:
      enabled: true
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: JWT Token Generation Correctness

*For any* valid merchant login credentials, the system SHALL generate a JWT token containing the correct merchant ID and a valid expiration time.

**Validates: Requirements 1.1, 1.2**

### Property 2: Token Expiration Enforcement

*For any* expired JWT token, all protected endpoint requests SHALL be rejected with 401 Unauthorized status.

**Validates: Requirements 1.3**

### Property 3: Invalid Credentials Rejection

*For any* invalid login credentials, the system SHALL reject the authentication attempt and SHALL NOT generate any JWT token.

**Validates: Requirements 1.4**

### Property 4: Token Signature Integrity

*For any* JWT token with tampered signature or payload, the system SHALL reject the token and deny access to protected resources.

**Validates: Requirements 1.6**

### Property 5: Protected Endpoint Authentication Requirement

*For any* protected merchant endpoint, requests without valid JWT tokens SHALL be rejected with 401 Unauthorized status.

**Validates: Requirements 2.1, 2.2**

### Property 6: Cross-Merchant Access Prevention

*For any* authenticated merchant attempting to access another merchant's resources, the system SHALL reject the request with 403 Forbidden status.

**Validates: Requirements 2.4, 2.6**

### Property 7: Merchant Data Isolation

*For any* authenticated merchant, all data access operations (inventory, balance, settlements) SHALL return only data belonging to that specific merchant.

**Validates: Requirements 3.1, 3.2, 3.4**

### Property 8: Merchant Resource Modification Control

*For any* authenticated merchant, all modification operations (inventory updates, price changes) SHALL only affect resources owned by that merchant.

**Validates: Requirements 3.3**

### Property 9: Security Context Population

*For any* successful JWT authentication, the security context SHALL contain the correct merchant ID and username extracted from the token.

**Validates: Requirements 4.3, 4.5**

### Property 10: Authentication Filter Coverage

*For any* request to protected endpoints, the JWT authentication filter SHALL process the request and validate the token before allowing access to controllers.

**Validates: Requirements 4.2, 4.6**

### Property 11: JWT Token Structure Compliance

*For any* generated JWT token, it SHALL contain merchant ID, username, and expiration timestamp in the payload, and SHALL be signed with the configured secret key.

**Validates: Requirements 5.1, 5.2, 5.3**

### Property 12: Token Validation Completeness

*For any* JWT token validation, the system SHALL verify signature, expiration, and format, rejecting tokens that fail any validation step.

**Validates: Requirements 5.4**

### Property 13: Security Event Logging

*For any* authentication failure, authorization failure, or security violation, the system SHALL create appropriate log entries with relevant details while protecting sensitive information.

**Validates: Requirements 6.1, 6.2, 6.3**

### Property 14: Error Response Consistency

*For any* authentication or authorization failure, the system SHALL return consistent error response format without exposing sensitive internal information.

**Validates: Requirements 6.4, 6.5**

### Property 15: Backward Compatibility Preservation

*For any* existing API endpoint functionality (excluding security), the behavior SHALL remain unchanged after implementing JWT authentication.

**Validates: Requirements 7.1, 7.2, 7.5, 7.6**

## Error Handling

### Security Exception Hierarchy

```java
// Base Security Exception
public class SecurityException extends BusinessException {
    public SecurityException(int code, String message) {
        super(code, message);
    }
}

// Authentication Exceptions
public class InvalidTokenException extends SecurityException {
    public InvalidTokenException(String message) {
        super(401, "Invalid token: " + message);
    }
}

public class TokenExpiredException extends SecurityException {
    public TokenExpiredException() {
        super(401, "Token has expired");
    }
}

public class MissingTokenException extends SecurityException {
    public MissingTokenException() {
        super(401, "Authentication token is required");
    }
}

// Authorization Exceptions
public class UnauthorizedAccessException extends SecurityException {
    public UnauthorizedAccessException(String resource) {
        super(403, "Access denied to resource: " + resource);
    }
}

public class MerchantMismatchException extends SecurityException {
    public MerchantMismatchException() {
        super(403, "Access denied: merchant ID mismatch");
    }
}
```

### Enhanced Global Exception Handler

```java
@RestControllerAdvice
public class SecurityExceptionHandler extends GlobalExceptionHandler {
    
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY");
    
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse<?>> handleInvalidToken(InvalidTokenException e, HttpServletRequest request) {
        logSecurityEvent("INVALID_TOKEN", request, e.getMessage());
        return ResponseEntity.status(401)
            .body(ApiResponse.error(401, "Authentication failed"));
    }
    
    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ApiResponse<?>> handleExpiredToken(TokenExpiredException e, HttpServletRequest request) {
        logSecurityEvent("EXPIRED_TOKEN", request, e.getMessage());
        return ResponseEntity.status(401)
            .body(ApiResponse.error(401, "Token has expired"));
    }
    
    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ApiResponse<?>> handleUnauthorizedAccess(UnauthorizedAccessException e, HttpServletRequest request) {
        logSecurityEvent("UNAUTHORIZED_ACCESS", request, e.getMessage());
        return ResponseEntity.status(403)
            .body(ApiResponse.error(403, "Access denied"));
    }
    
    @ExceptionHandler(MerchantMismatchException.class)
    public ResponseEntity<ApiResponse<?>> handleMerchantMismatch(MerchantMismatchException e, HttpServletRequest request) {
        logSecurityEvent("MERCHANT_MISMATCH", request, e.getMessage());
        return ResponseEntity.status(403)
            .body(ApiResponse.error(403, "Access denied"));
    }
    
    private void logSecurityEvent(String eventType, HttpServletRequest request, String details) {
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String requestUri = request.getRequestURI();
        
        securityLogger.warn("Security Event: {} | IP: {} | URI: {} | Details: {} | UserAgent: {}", 
            eventType, clientIp, requestUri, details, userAgent);
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

## Testing Strategy

### Testing Framework

- **Unit Testing**: JUnit 5 + Mockito + Spring Boot Test
- **Property-Based Testing**: jqwik for security property validation
- **Integration Testing**: MockMvc + TestContainers
- **Security Testing**: Spring Security Test

### Test Structure

```
src/test/java/com/trading/security/
├── jwt/
│   ├── JwtUtilTest.java
│   ├── JwtAuthenticationFilterTest.java
│   └── JwtPropertyTest.java
├── auth/
│   ├── AuthenticationServiceTest.java
│   ├── AuthControllerTest.java
│   └── AuthPropertyTest.java
├── authorization/
│   ├── MerchantAuthorizationTest.java
│   ├── ResourceAccessControlTest.java
│   └── AuthorizationPropertyTest.java
└── integration/
    ├── SecurityIntegrationTest.java
    ├── MerchantEndpointSecurityTest.java
    └── CrossMerchantAccessTest.java
```

### Property-Based Test Configuration

Each security property test will:
- Run minimum 100 iterations with random inputs
- Test edge cases and boundary conditions
- Validate security invariants across all scenarios

Example property test:
```java
/**
 * Feature: merchant-authorization-fix, Property 6: Cross-Merchant Access Prevention
 * Validates: Requirements 2.4, 2.6
 */
@Property(tries = 100)
void crossMerchantAccessIsAlwaysDenied(
    @ForAll @LongRange(min = 1, max = 1000) Long authenticatedMerchantId,
    @ForAll @LongRange(min = 1, max = 1000) Long targetMerchantId
) {
    assumeThat(authenticatedMerchantId).isNotEqualTo(targetMerchantId);
    
    // Test implementation
    String token = jwtUtil.generateToken(authenticatedMerchantId, "merchant" + authenticatedMerchantId);
    
    mockMvc.perform(get("/api/v1/merchants/" + targetMerchantId + "/balance")
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
}
```

### Integration Test Scenarios

1. **Complete Authentication Flow**: Register → Login → Access Protected Resources
2. **Cross-Merchant Access Attempts**: Authenticate as Merchant A → Try to access Merchant B's data
3. **Token Lifecycle**: Generate → Use → Expire → Reject
4. **Security Filter Chain**: Verify filter order and processing
5. **Error Handling**: Test all security exception scenarios

### Security Test Data Generation

```java
public class SecurityTestDataGenerator {
    
    @Provide
    Arbitrary<String> validJwtToken() {
        return Arbitraries.longs().between(1L, 1000L)
            .map(merchantId -> jwtUtil.generateToken(merchantId, "merchant" + merchantId));
    }
    
    @Provide
    Arbitrary<String> expiredJwtToken() {
        return Arbitraries.longs().between(1L, 1000L)
            .map(merchantId -> jwtUtil.generateExpiredToken(merchantId, "merchant" + merchantId));
    }
    
    @Provide
    Arbitrary<String> invalidJwtToken() {
        return Arbitraries.strings()
            .withCharRange('a', 'z')
            .ofMinLength(10)
            .ofMaxLength(50);
    }
    
    @Provide
    Arbitrary<LoginRequest> validLoginRequest() {
        return Arbitraries.strings().withCharRange('a', 'z').ofLength(8)
            .map(username -> new LoginRequest(username, "password123"));
    }
}
```