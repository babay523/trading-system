# 商品交易系统

一个基于 Spring Boot + Vue 3 的前后端分离商品交易系统。

## 项目结构

```
trading-system/
├── backend/                    # Spring Boot 后端
│   ├── src/
│   │   ├── main/java/com/trading/
│   │   │   ├── controller/     # REST API 控制器
│   │   │   ├── service/        # 业务逻辑层
│   │   │   ├── repository/     # 数据访问层
│   │   │   ├── entity/         # 实体类
│   │   │   ├── dto/            # 数据传输对象
│   │   │   └── exception/      # 异常处理
│   │   └── resources/
│   └── pom.xml
├── frontend/                   # Vue 3 前端
│   ├── src/
│   │   ├── api/               # API 调用模块
│   │   ├── stores/            # Pinia 状态管理
│   │   ├── views/             # 页面组件
│   │   ├── layouts/           # 布局组件
│   │   └── router/            # 路由配置
│   ├── package.json
│   └── vite.config.js
└── README.md
```

## 技术栈

### 后端
- Java 17
- Spring Boot 3.x
- Spring Data JPA
- H2 Database (开发环境)
- jqwik (属性测试)

### 前端
- Vue 3 (Composition API)
- Vue Router 4
- Pinia
- Axios
- Element Plus
- Vite

## 功能模块

### 用户端
- 用户注册/登录
- 余额充值
- 商品浏览和搜索
- 购物车管理
- 订单创建和支付
- 订单状态跟踪

### 商家端
- 商家注册/登录
- 商品管理
- 库存管理
- 订单处理（发货/退款）
- 结算记录查看

## 环境要求

- Java 17+
- Maven 3.8+
- Node.js 18+
- npm 9+

## 快速开始

### 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端服务将在 http://localhost:8080 启动

### 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器将在 http://localhost:5173 启动

### 同时启动前后端

在两个终端窗口分别执行上述命令，或使用以下方式：

**终端 1 (后端):**
```bash
cd backend && mvn spring-boot:run
```

**终端 2 (前端):**
```bash
cd frontend && npm run dev
```

## 默认账户

系统启动后可以注册新用户和商家，或使用以下测试流程：

1. 访问 http://localhost:5173/register 注册用户
2. 访问 http://localhost:5173/merchant/register 注册商家
3. 商家登录后可以创建商品和添加库存
4. 用户登录后可以浏览商品、加入购物车、下单支付
5. 也可以使用普通用户 user_alice:password123、merchant_digital:password123 验证

## API 文档

### 用户相关
- `POST /api/v1/users/register` - 用户注册
- `POST /api/v1/users/login` - 用户登录
- `GET /api/v1/users/{id}/balance` - 查询余额
- `POST /api/v1/users/{id}/deposit` - 充值

### 商品相关
- `GET /api/v1/products` - 商品列表
- `GET /api/v1/products/{id}` - 商品详情
- `POST /api/v1/products` - 创建商品

### 购物车相关
- `GET /api/v1/users/{userId}/cart` - 查看购物车
- `POST /api/v1/users/{userId}/cart/items` - 添加商品
- `PUT /api/v1/users/{userId}/cart/items/{sku}` - 更新数量
- `DELETE /api/v1/users/{userId}/cart/items/{sku}` - 删除商品

### 订单相关
- `POST /api/v1/users/{userId}/orders/from-cart` - 从购物车创建订单
- `POST /api/v1/orders/{id}/pay` - 支付订单
- `POST /api/v1/orders/{id}/ship` - 发货
- `POST /api/v1/orders/{id}/complete` - 确认收货
- `POST /api/v1/orders/{id}/refund` - 退款

### 商家相关
- `POST /api/v1/merchants/register` - 商家注册
- `POST /api/v1/merchants/login` - 商家登录
- `GET /api/v1/merchants/{id}/inventory` - 库存列表
- `POST /api/v1/merchants/{id}/inventory` - 添加库存
- `GET /api/v1/merchants/{id}/orders` - 商家订单
- `GET /api/v1/merchants/{id}/settlements` - 结算记录

## 运行测试

### 后端测试
```bash
cd backend
mvn test
```

### 前端构建
```bash
cd frontend
npm run build
```

## 开发说明

- 后端使用 H2 内存数据库，每次重启数据会重置
- 前端开发服务器已配置代理，自动转发 `/api` 请求到后端
- CORS 已配置允许前端开发服务器跨域访问

## License

MIT
