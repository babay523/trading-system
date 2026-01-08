# Requirements Document

## Introduction

本文档定义了一个完整的商品交易系统的需求规格。该系统参照京东商城等主流电商平台设计，支持用户和商家两个模块，用户可以从商家库存中购买商品，通过预存现金账户付款。系统基于Java Spring Boot技术栈，提供REST API接口，支持本地运行和扩展。

## Glossary

- **User**: 系统中的购买者，拥有预存现金账户，可以充值和购买商品
- **Merchant**: 系统中的商家，拥有商品库存和收款账户，可以添加库存和接收付款
- **SKU**: Stock Keeping Unit，商品库存单位，唯一标识一个商品规格
- **Product**: 商品信息，包含名称、描述、分类等基本信息
- **Balance**: 账户余额，用户或商家账户中的现金数额
- **Inventory**: 商家的商品库存，包含商品SKU、数量和价格
- **Order**: 用户购买商品的订单记录，包含订单状态流转
- **Order_Item**: 订单明细项，一个订单可包含多个商品
- **Settlement_Job**: 商家每日结算任务，匹配销售额与账户余额
- **Transaction_Record**: 资金流水记录，记录所有账户资金变动

## Requirements

### Requirement 1: User Account Management

**User Story:** As a user, I want to manage my prepaid account, so that I can deposit money, check my balance, and view transaction history for purchasing goods.

#### Acceptance Criteria

1. WHEN a user registers with username and password THEN the Trading_System SHALL create a user account with zero balance and return user ID
2. WHEN a user logs in with valid credentials THEN the Trading_System SHALL return an authentication token
3. IF a user logs in with invalid credentials THEN the Trading_System SHALL reject the request with authentication error
4. WHEN a user deposits money via REST API THEN the Trading_System SHALL increase the user's balance by the deposit amount
5. WHEN a user queries their balance via REST API THEN the Trading_System SHALL return the current balance
6. IF a user attempts to deposit a negative or zero amount THEN the Trading_System SHALL reject the request with an error message
7. WHEN a deposit is successful THEN the Trading_System SHALL create a transaction record with type DEPOSIT
8. WHEN a user queries transaction history THEN the Trading_System SHALL return paginated list of all transactions with type, amount, timestamp, and related order ID

### Requirement 2: Merchant Account and Inventory Management

**User Story:** As a merchant, I want to manage my inventory, products, and account, so that I can sell products and receive payments.

#### Acceptance Criteria

1. WHEN a merchant registers with business name and credentials THEN the Trading_System SHALL create a merchant account with zero balance and empty inventory
2. WHEN a merchant logs in with valid credentials THEN the Trading_System SHALL return an authentication token
3. WHEN a merchant creates a product THEN the Trading_System SHALL store product with name, description, category, and generate unique product ID
4. WHEN a merchant adds inventory for a SKU via REST API THEN the Trading_System SHALL increase the stock quantity for the specified SKU
5. WHEN a merchant adds a new SKU THEN the Trading_System SHALL create the inventory item with product ID, quantity, price, and SKU code
6. WHEN a merchant updates SKU price THEN the Trading_System SHALL update the price and record the price change history
7. WHEN a merchant queries inventory via REST API THEN the Trading_System SHALL return paginated inventory items with SKU, product info, quantity, and price
8. IF a merchant attempts to add negative quantity THEN the Trading_System SHALL reject the request with an error message
9. WHEN a merchant queries their balance THEN the Trading_System SHALL return current balance and transaction history
10. WHEN inventory quantity reaches zero THEN the Trading_System SHALL mark the SKU as out of stock

### Requirement 3: Product Catalog

**User Story:** As a user, I want to browse and search products, so that I can find items I want to purchase.

#### Acceptance Criteria

1. WHEN a user queries product list THEN the Trading_System SHALL return paginated products with basic info, price range, and merchant info
2. WHEN a user searches products by keyword THEN the Trading_System SHALL return products matching name or description
3. WHEN a user filters products by category THEN the Trading_System SHALL return products in the specified category
4. WHEN a user views product detail THEN the Trading_System SHALL return product info with all available SKUs, prices, and stock status
5. WHEN a user queries products by merchant THEN the Trading_System SHALL return all products from that merchant

### Requirement 4: Shopping Cart

**User Story:** As a user, I want to manage a shopping cart, so that I can collect items before placing an order.

#### Acceptance Criteria

1. WHEN a user adds item to cart with SKU and quantity THEN the Trading_System SHALL add the item to user's cart
2. WHEN a user updates cart item quantity THEN the Trading_System SHALL update the quantity for that SKU
3. WHEN a user removes item from cart THEN the Trading_System SHALL remove the specified SKU from cart
4. WHEN a user views cart THEN the Trading_System SHALL return all cart items with current price, quantity, subtotal, and stock availability
5. WHEN a user clears cart THEN the Trading_System SHALL remove all items from cart
6. IF cart item SKU becomes out of stock THEN the Trading_System SHALL mark the item as unavailable in cart view
7. WHEN cart is viewed THEN the Trading_System SHALL calculate and return total amount

### Requirement 5: Purchase Order Processing

**User Story:** As a user, I want to purchase products from merchants, so that I can acquire goods using my prepaid balance.

#### Acceptance Criteria

1. WHEN a user submits a purchase order from cart THEN the Trading_System SHALL create order with status PENDING
2. WHEN a user submits a direct purchase with SKU and quantity THEN the Trading_System SHALL calculate the total price as quantity multiplied by unit price
3. WHEN order is created THEN the Trading_System SHALL generate unique order number with timestamp prefix
4. WHEN a purchase order is confirmed THEN the Trading_System SHALL deduct the total price from the user's balance
5. WHEN a purchase order is confirmed THEN the Trading_System SHALL add the total price to the merchant's balance
6. WHEN a purchase order is confirmed THEN the Trading_System SHALL reduce the merchant's inventory by the ordered quantity
7. WHEN a purchase is successful THEN the Trading_System SHALL update order status to PAID and create transaction records for both user and merchant
8. IF the user's balance is insufficient THEN the Trading_System SHALL reject the order with insufficient balance error and set status to FAILED
9. IF the merchant's inventory is insufficient THEN the Trading_System SHALL reject the order with insufficient stock error and set status to FAILED
10. IF the SKU does not exist THEN the Trading_System SHALL reject the order with a product not found error
11. WHEN processing a purchase THEN the Trading_System SHALL ensure atomicity of balance deduction, balance addition, and inventory reduction
12. WHEN a user queries order list THEN the Trading_System SHALL return paginated orders with status, items, and total amount
13. WHEN a user views order detail THEN the Trading_System SHALL return complete order info with all items, prices, and status history

### Requirement 6: Order Status Management

**User Story:** As a user, I want to track my order status, so that I can know the progress of my purchase.

#### Acceptance Criteria

1. THE Order SHALL have status values: PENDING, PAID, SHIPPED, COMPLETED, CANCELLED, REFUNDED
2. WHEN order is paid THEN the Trading_System SHALL update status from PENDING to PAID
3. WHEN merchant ships order THEN the Trading_System SHALL update status from PAID to SHIPPED
4. WHEN user confirms receipt THEN the Trading_System SHALL update status from SHIPPED to COMPLETED
5. WHEN user cancels unpaid order THEN the Trading_System SHALL update status to CANCELLED
6. WHEN user requests refund for paid order THEN the Trading_System SHALL process refund and update status to REFUNDED
7. WHEN order is refunded THEN the Trading_System SHALL return money to user balance and deduct from merchant balance
8. WHEN order status changes THEN the Trading_System SHALL record status change with timestamp

### Requirement 7: Daily Settlement Job

**User Story:** As a merchant, I want the system to perform daily settlement, so that I can verify my sales revenue matches my account balance.

#### Acceptance Criteria

1. WHEN the settlement job runs THEN the Trading_System SHALL calculate total sales value from COMPLETED orders for each merchant since last settlement
2. WHEN the settlement job runs THEN the Trading_System SHALL calculate total refunds for each merchant since last settlement
3. WHEN the settlement job runs THEN the Trading_System SHALL compare net sales (sales minus refunds) with merchant balance change
4. WHEN net sales matches balance change THEN the Trading_System SHALL mark the settlement as MATCHED
5. IF net sales does not match balance change THEN the Trading_System SHALL mark settlement as MISMATCHED and log discrepancy with difference amount
6. WHEN settlement completes THEN the Trading_System SHALL create a settlement record with date, merchant ID, total sales, total refunds, net amount, actual balance change, and match status
7. THE Settlement_Job SHALL run automatically at a configurable time each day (default 00:00)
8. WHEN a merchant queries settlement history THEN the Trading_System SHALL return paginated settlement records

### Requirement 8: REST API Design

**User Story:** As a developer, I want well-designed REST APIs, so that I can integrate with the trading system following standard conventions.

#### Acceptance Criteria

1. THE Trading_System SHALL expose REST endpoints following RESTful naming conventions with resource-based URLs
2. THE Trading_System SHALL use appropriate HTTP methods (GET for queries, POST for creation, PUT for updates, DELETE for removal)
3. THE Trading_System SHALL return appropriate HTTP status codes (200 OK, 201 Created, 400 Bad Request, 401 Unauthorized, 404 Not Found, 409 Conflict, 500 Internal Server Error)
4. THE Trading_System SHALL return JSON formatted responses with consistent structure including code, message, and data fields
5. THE Trading_System SHALL include detailed error messages in response body for failed requests
6. THE Trading_System SHALL support pagination with page, size, total parameters for list endpoints
7. THE Trading_System SHALL require authentication token in header for protected endpoints
8. THE Trading_System SHALL version APIs with /api/v1 prefix

### Requirement 9: Data Persistence and Integrity

**User Story:** As a system administrator, I want reliable data persistence, so that all transactions are safely stored and recoverable.

#### Acceptance Criteria

1. THE Trading_System SHALL persist all data to H2 database for local development and support MySQL for production
2. WHEN processing financial transactions THEN the Trading_System SHALL use database transactions to ensure ACID properties
3. THE Trading_System SHALL validate all input data before persistence using Bean Validation
4. WHEN concurrent purchases occur for the same inventory THEN the Trading_System SHALL handle race conditions using optimistic locking with version field
5. THE Trading_System SHALL log all financial transactions for audit purposes with transaction ID, type, amount, and participants
6. WHEN database operation fails THEN the Trading_System SHALL rollback transaction and return appropriate error

### Requirement 10: System Configuration and Extensibility

**User Story:** As a developer, I want a configurable and extensible system, so that I can easily add new features and modify behavior.

#### Acceptance Criteria

1. THE Trading_System SHALL externalize configuration using Spring Boot application.yml
2. THE Trading_System SHALL use dependency injection for loose coupling between components
3. THE Trading_System SHALL organize code into clear layers: controller, service, repository, entity, dto
4. THE Trading_System SHALL define interfaces for core services (UserService, MerchantService, OrderService, SettlementService)
5. THE Trading_System SHALL use strategy pattern for payment processing to support adding new payment methods
6. THE Trading_System SHALL use factory pattern for order creation to support different order types
7. THE Trading_System SHALL configure settlement job schedule via application properties
8. THE Trading_System SHALL support profile-based configuration for different environments

### Requirement 11: Testing Requirements

**User Story:** As a developer, I want comprehensive test coverage, so that I can ensure system reliability and catch regressions.

#### Acceptance Criteria

1. THE Trading_System SHALL have unit tests covering at least 80% of core business logic in service layer
2. THE Trading_System SHALL include integration tests for all REST API endpoints
3. THE Trading_System SHALL include tests for concurrent purchase scenarios using multiple threads
4. THE Trading_System SHALL use property-based testing for financial calculations to verify correctness across many inputs
5. THE Trading_System SHALL use MockMvc for controller layer testing
6. THE Trading_System SHALL use embedded H2 database for integration tests
