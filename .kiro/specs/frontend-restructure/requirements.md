# Requirements Document

## Introduction

本文档定义了将现有商品交易系统重构为前后端分离架构的需求规格。后端保持现有Spring Boot实现，前端使用Vue 3框架实现完整的用户界面，支持用户和商家两个模块的所有功能。

## Glossary

- **Backend**: 后端服务，基于Spring Boot的REST API服务
- **Frontend**: 前端应用，基于Vue 3的单页面应用(SPA)
- **User_Portal**: 用户门户，面向购买者的前端界面
- **Merchant_Portal**: 商家门户，面向商家的前端界面
- **API_Client**: 前端与后端通信的HTTP客户端模块

## Requirements

### Requirement 1: Project Structure Reorganization

**User Story:** As a developer, I want the project organized into separate backend and frontend directories, so that I can develop and deploy them independently.

#### Acceptance Criteria

1. THE Project SHALL have a `backend/` directory containing all Spring Boot code
2. THE Project SHALL have a `frontend/` directory containing the Vue 3 application
3. WHEN the backend is moved THEN all existing tests SHALL continue to pass
4. THE Project SHALL have a root-level README documenting the project structure

### Requirement 2: User Authentication UI

**User Story:** As a user, I want to register and login through a web interface, so that I can access the trading system.

#### Acceptance Criteria

1. WHEN a user visits the application THEN the Frontend SHALL display a login page
2. WHEN a user clicks register THEN the Frontend SHALL display a registration form with username and password fields
3. WHEN a user submits valid credentials THEN the Frontend SHALL authenticate and redirect to the home page
4. IF login fails THEN the Frontend SHALL display an error message
5. WHEN a user is logged in THEN the Frontend SHALL store the session and display user info in the header

### Requirement 3: User Balance Management UI

**User Story:** As a user, I want to view my balance and deposit money through the web interface, so that I can manage my prepaid account.

#### Acceptance Criteria

1. WHEN a user views their profile THEN the Frontend SHALL display current balance
2. WHEN a user clicks deposit THEN the Frontend SHALL display a deposit form
3. WHEN a user submits a valid deposit amount THEN the Frontend SHALL update the displayed balance
4. IF deposit fails THEN the Frontend SHALL display an error message
5. WHEN a user views transaction history THEN the Frontend SHALL display a paginated list of transactions

### Requirement 4: Product Browsing UI

**User Story:** As a user, I want to browse and search products through the web interface, so that I can find items to purchase.

#### Acceptance Criteria

1. WHEN a user visits the products page THEN the Frontend SHALL display a paginated product list
2. WHEN a user enters a search keyword THEN the Frontend SHALL filter products by name or description
3. WHEN a user selects a category THEN the Frontend SHALL filter products by category
4. WHEN a user clicks a product THEN the Frontend SHALL display product details with available SKUs and prices
5. THE Product_List SHALL display product name, price range, and merchant info

### Requirement 5: Shopping Cart UI

**User Story:** As a user, I want to manage my shopping cart through the web interface, so that I can collect items before purchasing.

#### Acceptance Criteria

1. WHEN a user adds a product to cart THEN the Frontend SHALL update the cart icon with item count
2. WHEN a user views the cart THEN the Frontend SHALL display all items with quantity, price, and subtotal
3. WHEN a user updates item quantity THEN the Frontend SHALL recalculate totals
4. WHEN a user removes an item THEN the Frontend SHALL remove it from the cart display
5. THE Cart_View SHALL display the total amount for all items

### Requirement 6: Order Management UI

**User Story:** As a user, I want to place orders and track their status through the web interface, so that I can complete purchases.

#### Acceptance Criteria

1. WHEN a user clicks checkout THEN the Frontend SHALL create an order and display order confirmation
2. WHEN a user confirms payment THEN the Frontend SHALL process payment and update order status
3. WHEN a user views orders THEN the Frontend SHALL display a paginated order list with status
4. WHEN a user clicks an order THEN the Frontend SHALL display order details with items and status history
5. WHEN a user cancels an order THEN the Frontend SHALL update the order status
6. WHEN a user requests refund THEN the Frontend SHALL process refund and update status

### Requirement 7: Merchant Portal UI

**User Story:** As a merchant, I want to manage my products and inventory through the web interface, so that I can sell products.

#### Acceptance Criteria

1. WHEN a merchant logs in THEN the Frontend SHALL display the merchant dashboard
2. WHEN a merchant views inventory THEN the Frontend SHALL display a paginated inventory list
3. WHEN a merchant adds inventory THEN the Frontend SHALL display a form for SKU, quantity, and price
4. WHEN a merchant updates price THEN the Frontend SHALL update the displayed price
5. WHEN a merchant views balance THEN the Frontend SHALL display current balance and transaction history
6. WHEN a merchant views settlements THEN the Frontend SHALL display settlement records

### Requirement 8: Merchant Order Management UI

**User Story:** As a merchant, I want to manage orders through the web interface, so that I can fulfill customer purchases.

#### Acceptance Criteria

1. WHEN a merchant views orders THEN the Frontend SHALL display orders for their products
2. WHEN a merchant ships an order THEN the Frontend SHALL update order status to SHIPPED
3. WHEN a merchant processes refund THEN the Frontend SHALL update order status and balance

### Requirement 9: Frontend Technical Requirements

**User Story:** As a developer, I want the frontend built with modern best practices, so that it is maintainable and performant.

#### Acceptance Criteria

1. THE Frontend SHALL use Vue 3 with Composition API
2. THE Frontend SHALL use Vue Router for navigation
3. THE Frontend SHALL use Pinia for state management
4. THE Frontend SHALL use Axios for API communication
5. THE Frontend SHALL use Element Plus or similar UI component library
6. THE Frontend SHALL handle API errors gracefully with user-friendly messages
7. THE Frontend SHALL support responsive design for desktop and mobile

### Requirement 10: Backend CORS Configuration

**User Story:** As a developer, I want the backend to support CORS, so that the frontend can communicate with it.

#### Acceptance Criteria

1. WHEN the frontend makes API requests THEN the Backend SHALL accept cross-origin requests
2. THE Backend SHALL configure allowed origins for development and production
3. THE Backend SHALL allow necessary HTTP methods (GET, POST, PUT, DELETE)

