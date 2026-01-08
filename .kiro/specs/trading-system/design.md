# Design Document: Trading System

## Overview

本设计文档描述了一个基于Spring Boot的商品交易系统的技术架构和实现方案。系统采用分层架构，支持用户和商家两个模块，提供完整的电商购物流程，包括用户管理、商品管理、购物车、订单处理和每日结算功能。

### Technology Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.x, Spring MVC, Spring Data JPA, Spring Security
- **Database**: H2 (开发环境), MySQL (生产环境)
- **Build Tool**: Maven
- **Testing**: JUnit 5, Mockito, jqwik (Property-Based Testing)
- **API Documentation**: SpringDoc OpenAPI

## Architecture

### System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        REST API Layer                           │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │  User    │ │ Merchant │ │  Order   │ │ Product  │           │
│  │Controller│ │Controller│ │Controller│ │Controller│           │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘           │
└───────┼────────────┼────────────┼────────────┼──────────────────┘
        │            │            │            │
┌───────┼────────────┼────────────┼────────────┼──────────────────┐
│       ▼            ▼            ▼            ▼                  │
│                      Service Layer                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │  User    │ │ Merchant │ │  Order   │ │ Product  │           │
│  │ Service  │ │ Service  │ │ Service  │ │ Service  │           │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘           │
│       │            │            │            │                  │
│  ┌────┴────────────┴────────────┴────────────┴─────┐           │
│  │              Transaction Service                 │           │
│  └──────────────────────┬──────────────────────────┘           │
└─────────────────────────┼───────────────────────────────────────┘
                          │
┌─────────────────────────┼───────────────────────────────────────┐
│                         ▼                                       │
│                   Repository Layer                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │  User    │ │ Merchant │ │  Order   │ │ Product  │           │
│  │   Repo   │ │   Repo   │ │   Repo   │ │   Repo   │           │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘           │
└───────┼────────────┼────────────┼────────────┼──────────────────┘
        │            │            │            │
        ▼            ▼            ▼            ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Database (H2/MySQL)                         │
└─────────────────────────────────────────────────────────────────┘
```

### Package Structure

```
com.trading
├── TradingApplication.java
├── config/
│   ├── SecurityConfig.java
│   ├── JpaConfig.java
│   └── SchedulerConfig.java
├── controller/
│   ├── UserController.java
│   ├── MerchantController.java
│   ├── ProductController.java
│   ├── CartController.java
│   ├── OrderController.java
│   └── SettlementController.java
├── service/
│   ├── UserService.java
│   ├── UserServiceImpl.java
│   ├── MerchantService.java
│   ├── MerchantServiceImpl.java
│   ├── ProductService.java
│   ├── ProductServiceImpl.java
│   ├── CartService.java
│   ├── CartServiceImpl.java
│   ├── OrderService.java
│   ├── OrderServiceImpl.java
│   ├── SettlementService.java
│   ├── SettlementServiceImpl.java
│   └── TransactionService.java
├── repository/
│   ├── UserRepository.java
│   ├── MerchantRepository.java
│   ├── ProductRepository.java
│   ├── InventoryRepository.java
│   ├── CartRepository.java
│   ├── OrderRepository.java
│   ├── TransactionRecordRepository.java
│   └── SettlementRepository.java
├── entity/
│   ├── User.java
│   ├── Merchant.java
│   ├── Product.java
│   ├── Inventory.java
│   ├── CartItem.java
│   ├── Order.java
│   ├── OrderItem.java
│   ├── TransactionRecord.java
│   └── Settlement.java
├── dto/
│   ├── request/
│   │   ├── UserRegisterRequest.java
│   │   ├── DepositRequest.java
│   │   ├── ProductCreateRequest.java
│   │   ├── InventoryAddRequest.java
│   │   ├── CartAddRequest.java
│   │   ├── OrderCreateRequest.java
│   │   └── DirectPurchaseRequest.java
│   └── response/
│       ├── ApiResponse.java
│       ├── UserResponse.java
│       ├── ProductResponse.java
│       ├── CartResponse.java
│       ├── OrderResponse.java
│       └── SettlementResponse.java
├── enums/
│   ├── OrderStatus.java
│   ├── TransactionType.java
│   └── SettlementStatus.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── InsufficientBalanceException.java
│   ├── InsufficientStockException.java
│   ├── ResourceNotFoundException.java
│   └── BusinessException.java
└── scheduler/
    └── SettlementScheduler.java
```

## Components and Interfaces

### Core Service Interfaces

```java
// UserService Interface
public interface UserService {
    UserResponse register(UserRegisterRequest request);
    UserResponse login(String username, String password);
    UserResponse getById(Long userId);
    BigDecimal getBalance(Long userId);
    void deposit(Long userId, BigDecimal amount);
    Page<TransactionRecord> getTransactionHistory(Long userId, Pageable pageable);
}

// MerchantService Interface
public interface MerchantService {
    MerchantResponse register(MerchantRegisterRequest request);
    MerchantResponse login(String username, String password);
    MerchantResponse getById(Long merchantId);
    BigDecimal getBalance(Long merchantId);
    void addInventory(Long merchantId, InventoryAddRequest request);
    void updatePrice(Long merchantId, String sku, BigDecimal newPrice);
    Page<InventoryResponse> getInventory(Long merchantId, Pageable pageable);
}

// ProductService Interface
public interface ProductService {
    ProductResponse create(Long merchantId, ProductCreateRequest request);
    ProductResponse getById(Long productId);
    Page<ProductResponse> search(String keyword, String category, Pageable pageable);
    Page<ProductResponse> getByMerchant(Long merchantId, Pageable pageable);
    ProductDetailResponse getDetail(Long productId);
}

// CartService Interface
public interface CartService {
    void addItem(Long userId, CartAddRequest request);
    void updateQuantity(Long userId, String sku, Integer quantity);
    void removeItem(Long userId, String sku);
    CartResponse getCart(Long userId);
    void clearCart(Long userId);
}

// OrderService Interface
public interface OrderService {
    OrderResponse createFromCart(Long userId);
    OrderResponse createDirect(Long userId, DirectPurchaseRequest request);
    OrderResponse confirmPayment(Long orderId);
    OrderResponse ship(Long orderId);
    OrderResponse complete(Long orderId);
    OrderResponse cancel(Long orderId);
    OrderResponse refund(Long orderId);
    OrderResponse getById(Long orderId);
    Page<OrderResponse> getByUser(Long userId, Pageable pageable);
}

// SettlementService Interface
public interface SettlementService {
    void runDailySettlement();
    SettlementResponse getByMerchantAndDate(Long merchantId, LocalDate date);
    Page<SettlementResponse> getByMerchant(Long merchantId, Pageable pageable);
}
```

### REST API Endpoints

#### User APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/v1/users/register | 用户注册 |
| POST | /api/v1/users/login | 用户登录 |
| GET | /api/v1/users/{id} | 获取用户信息 |
| GET | /api/v1/users/{id}/balance | 获取用户余额 |
| POST | /api/v1/users/{id}/deposit | 用户充值 |
| GET | /api/v1/users/{id}/transactions | 获取交易记录 |

#### Merchant APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/v1/merchants/register | 商家注册 |
| POST | /api/v1/merchants/login | 商家登录 |
| GET | /api/v1/merchants/{id} | 获取商家信息 |
| GET | /api/v1/merchants/{id}/balance | 获取商家余额 |
| POST | /api/v1/merchants/{id}/inventory | 添加库存 |
| PUT | /api/v1/merchants/{id}/inventory/{sku}/price | 更新价格 |
| GET | /api/v1/merchants/{id}/inventory | 获取库存列表 |
| GET | /api/v1/merchants/{id}/settlements | 获取结算记录 |

#### Product APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/v1/products | 创建商品 |
| GET | /api/v1/products | 搜索商品列表 |
| GET | /api/v1/products/{id} | 获取商品详情 |

#### Cart APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/v1/cart/items | 添加购物车 |
| PUT | /api/v1/cart/items/{sku} | 更新数量 |
| DELETE | /api/v1/cart/items/{sku} | 删除商品 |
| GET | /api/v1/cart | 获取购物车 |
| DELETE | /api/v1/cart | 清空购物车 |

#### Order APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/v1/orders/from-cart | 从购物车创建订单 |
| POST | /api/v1/orders/direct | 直接购买 |
| POST | /api/v1/orders/{id}/pay | 确认支付 |
| POST | /api/v1/orders/{id}/ship | 发货 |
| POST | /api/v1/orders/{id}/complete | 确认收货 |
| POST | /api/v1/orders/{id}/cancel | 取消订单 |
| POST | /api/v1/orders/{id}/refund | 申请退款 |
| GET | /api/v1/orders/{id} | 获取订单详情 |
| GET | /api/v1/orders | 获取订单列表 |

## Data Models

### Entity Relationship Diagram

```
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│    User     │       │   Merchant  │       │   Product   │
├─────────────┤       ├─────────────┤       ├─────────────┤
│ id          │       │ id          │       │ id          │
│ username    │       │ businessName│       │ name        │
│ password    │       │ username    │       │ description │
│ balance     │       │ password    │       │ category    │
│ createdAt   │       │ balance     │       │ merchantId  │◄──┐
│ version     │       │ createdAt   │       │ createdAt   │   │
└──────┬──────┘       │ version     │       └─────────────┘   │
       │              └──────┬──────┘              │          │
       │                     │                     │          │
       │              ┌──────┴──────┐              │          │
       │              │  Inventory  │◄─────────────┘          │
       │              ├─────────────┤                         │
       │              │ id          │                         │
       │              │ sku         │                         │
       │              │ productId   │                         │
       │              │ merchantId  │─────────────────────────┘
       │              │ quantity    │
       │              │ price       │
       │              │ version     │
       │              └─────────────┘
       │
┌──────┴──────┐       ┌─────────────┐       ┌─────────────┐
│  CartItem   │       │    Order    │       │  OrderItem  │
├─────────────┤       ├─────────────┤       ├─────────────┤
│ id          │       │ id          │       │ id          │
│ userId      │       │ orderNumber │       │ orderId     │◄──┐
│ sku         │       │ userId      │       │ sku         │   │
│ quantity    │       │ merchantId  │       │ productName │   │
│ createdAt   │       │ totalAmount │       │ quantity    │   │
└─────────────┘       │ status      │───────│ unitPrice   │   │
                      │ createdAt   │       │ subtotal    │   │
                      │ updatedAt   │       └─────────────┘   │
                      └──────┬──────┘                         │
                             │                                │
                             └────────────────────────────────┘

┌─────────────────┐       ┌─────────────┐
│TransactionRecord│       │  Settlement │
├─────────────────┤       ├─────────────┤
│ id              │       │ id          │
│ transactionId   │       │ merchantId  │
│ accountType     │       │ settlementDate│
│ accountId       │       │ totalSales  │
│ type            │       │ totalRefunds│
│ amount          │       │ netAmount   │
│ balanceBefore   │       │ balanceChange│
│ balanceAfter    │       │ status      │
│ relatedOrderId  │       │ createdAt   │
│ createdAt       │       └─────────────┘
└─────────────────┘
```

### Entity Definitions

```java
@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;
    
    private LocalDateTime createdAt;
    
    @Version
    private Long version;
}

@Entity
@Table(name = "merchants")
public class Merchant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String businessName;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;
    
    private LocalDateTime createdAt;
    
    @Version
    private Long version;
}

@Entity
@Table(name = "inventory")
public class Inventory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String sku;
    
    @Column(nullable = false)
    private Long productId;
    
    @Column(nullable = false)
    private Long merchantId;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal price;
    
    @Version
    private Long version;
}

@Entity
@Table(name = "orders")
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String orderNumber;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private Long merchantId;
    
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal totalAmount;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

public enum OrderStatus {
    PENDING, PAID, SHIPPED, COMPLETED, CANCELLED, REFUNDED
}

public enum TransactionType {
    DEPOSIT, PURCHASE, SALE, REFUND_OUT, REFUND_IN
}

public enum SettlementStatus {
    MATCHED, MISMATCHED
}
```

### API Response Format

```java
@Data
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("Success");
        response.setData(data);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
    
    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
}
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Account Initialization Correctness

*For any* valid user or merchant registration request, the created account SHALL have a balance of exactly zero.

**Validates: Requirements 1.1, 2.1**

### Property 2: Deposit Balance Correctness

*For any* user with initial balance B and *for any* valid deposit amount A (where A > 0), after the deposit operation, the user's balance SHALL equal B + A.

**Validates: Requirements 1.4**

### Property 3: Invalid Deposit Rejection

*For any* deposit amount A where A <= 0, the deposit operation SHALL be rejected and the user's balance SHALL remain unchanged.

**Validates: Requirements 1.6**

### Property 4: Inventory Addition Correctness

*For any* merchant inventory with initial quantity Q for a SKU and *for any* valid addition amount A (where A > 0), after the add operation, the inventory quantity SHALL equal Q + A.

**Validates: Requirements 2.4**

### Property 5: Invalid Inventory Rejection

*For any* inventory addition with quantity Q where Q <= 0, the operation SHALL be rejected and the inventory quantity SHALL remain unchanged.

**Validates: Requirements 2.8**

### Property 6: Cart Total Calculation Correctness

*For any* shopping cart containing items, the total amount SHALL equal the sum of (quantity × unit price) for all items in the cart.

**Validates: Requirements 4.7**

### Property 7: Order Total Calculation Correctness

*For any* purchase order with quantity Q and unit price P, the total price SHALL equal Q × P.

**Validates: Requirements 5.2**

### Property 8: Purchase Transaction Atomicity

*For any* successful purchase order with total amount T:
- The user's balance SHALL decrease by exactly T
- The merchant's balance SHALL increase by exactly T
- The inventory quantity SHALL decrease by exactly the ordered quantity
- All three changes SHALL occur atomically (all succeed or all fail)

**Validates: Requirements 5.4, 5.5, 5.6, 5.11**

### Property 9: Insufficient Balance Rejection

*For any* purchase order where the total amount T exceeds the user's balance B (T > B), the order SHALL be rejected and no balance or inventory changes SHALL occur.

**Validates: Requirements 5.8**

### Property 10: Insufficient Stock Rejection

*For any* purchase order where the ordered quantity Q exceeds the available inventory I (Q > I), the order SHALL be rejected and no balance or inventory changes SHALL occur.

**Validates: Requirements 5.9**

### Property 11: Refund Transaction Correctness

*For any* refund operation on an order with amount T:
- The user's balance SHALL increase by exactly T
- The merchant's balance SHALL decrease by exactly T

**Validates: Requirements 6.7**

### Property 12: Settlement Calculation Correctness

*For any* merchant, the calculated total sales SHALL equal the sum of all COMPLETED order amounts for that merchant within the settlement period.

**Validates: Requirements 7.1**

### Property 13: Concurrent Purchase Safety

*For any* inventory with quantity Q, when N concurrent purchase requests each requesting quantity 1 are processed, the total successful purchases SHALL NOT exceed Q.

**Validates: Requirements 9.4**

### Property 14: Search Result Relevance

*For any* product search with keyword K, all returned products SHALL contain K in either the product name or description.

**Validates: Requirements 3.2**

### Property 15: Transaction Record Completeness

*For any* successful financial operation (deposit, purchase, refund), a corresponding transaction record SHALL be created with correct type, amount, and timestamps.

**Validates: Requirements 1.7**

## Error Handling

### Exception Hierarchy

```java
// Base exception
public class BusinessException extends RuntimeException {
    private final int code;
    private final String message;
}

// Specific exceptions
public class ResourceNotFoundException extends BusinessException {
    // 404 - Resource not found
}

public class InsufficientBalanceException extends BusinessException {
    // 400 - User balance insufficient
}

public class InsufficientStockException extends BusinessException {
    // 400 - Inventory stock insufficient
}

public class InvalidOperationException extends BusinessException {
    // 400 - Invalid operation (e.g., cancel paid order)
}

public class ConcurrencyException extends BusinessException {
    // 409 - Optimistic lock conflict
}
```

### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNotFound(ResourceNotFoundException e) {
        return ResponseEntity.status(404)
            .body(ApiResponse.error(404, e.getMessage()));
    }
    
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiResponse<?>> handleInsufficientBalance(InsufficientBalanceException e) {
        return ResponseEntity.status(400)
            .body(ApiResponse.error(400, e.getMessage()));
    }
    
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiResponse<?>> handleInsufficientStock(InsufficientStockException e) {
        return ResponseEntity.status(400)
            .body(ApiResponse.error(400, e.getMessage()));
    }
    
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<?>> handleConcurrency(OptimisticLockingFailureException e) {
        return ResponseEntity.status(409)
            .body(ApiResponse.error(409, "Concurrent modification detected, please retry"));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.status(400)
            .body(ApiResponse.error(400, message));
    }
}
```

## Testing Strategy

### Testing Framework

- **Unit Testing**: JUnit 5 + Mockito
- **Property-Based Testing**: jqwik
- **Integration Testing**: Spring Boot Test + MockMvc
- **Database**: H2 in-memory for tests

### Test Structure

```
src/test/java/com/trading/
├── service/
│   ├── UserServiceTest.java
│   ├── MerchantServiceTest.java
│   ├── OrderServiceTest.java
│   ├── CartServiceTest.java
│   └── SettlementServiceTest.java
├── controller/
│   ├── UserControllerTest.java
│   ├── MerchantControllerTest.java
│   ├── OrderControllerTest.java
│   └── ProductControllerTest.java
├── property/
│   ├── BalancePropertyTest.java
│   ├── OrderPropertyTest.java
│   ├── InventoryPropertyTest.java
│   └── ConcurrencyPropertyTest.java
└── integration/
    └── OrderFlowIntegrationTest.java
```

### Unit Test Coverage Requirements

- Service layer: 80%+ coverage
- Focus on business logic validation
- Mock repository layer dependencies

### Property-Based Test Configuration

Each property test will:
- Run minimum 100 iterations
- Use jqwik for random input generation
- Reference design document property number

Example property test annotation:
```java
/**
 * Feature: trading-system, Property 8: Purchase Transaction Atomicity
 * Validates: Requirements 5.4, 5.5, 5.6, 5.11
 */
@Property(tries = 100)
void purchaseTransactionIsAtomic(@ForAll @Positive BigDecimal amount) {
    // Test implementation
}
```

### Integration Test Scenarios

1. Complete purchase flow: register → deposit → add to cart → create order → pay
2. Refund flow: complete order → request refund → verify balances
3. Concurrent purchase: multiple users purchasing same limited inventory
4. Settlement verification: create orders → run settlement → verify match

### Test Data Generation

```java
public class TestDataGenerator {
    
    @Provide
    Arbitrary<BigDecimal> validAmount() {
        return Arbitraries.bigDecimals()
            .between(BigDecimal.valueOf(0.01), BigDecimal.valueOf(10000))
            .ofScale(2);
    }
    
    @Provide
    Arbitrary<Integer> validQuantity() {
        return Arbitraries.integers().between(1, 100);
    }
    
    @Provide
    Arbitrary<String> validSku() {
        return Arbitraries.strings()
            .withCharRange('A', 'Z')
            .ofMinLength(8)
            .ofMaxLength(12);
    }
}
```
