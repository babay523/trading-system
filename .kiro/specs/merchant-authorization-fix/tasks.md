# Implementation Plan: Merchant Authorization Fix

## Overview

本实现计划将商家权限控制漏洞修复的设计分解为可执行的编码任务。采用增量开发方式，从JWT基础设施开始，逐步实现身份验证、授权控制和安全日志，确保每个步骤都能构建和测试。

## Tasks

- [x] 1. 添加JWT依赖和基础配置
  - [x] 1.1 添加JWT相关Maven依赖
    - 在pom.xml中添加jjwt-api, jjwt-impl, jjwt-jackson依赖
    - 添加Spring Security依赖（如果尚未存在）
    - _Requirements: 5.1, 5.3_
  - [x] 1.2 创建JWT配置属性类
    - 实现JwtProperties配置类，包含secret-key, expiration, issuer等属性
    - 在application.yml中添加JWT相关配置
    - _Requirements: 5.1, 5.5, 8.1_
  - [x] 1.3 创建安全相关异常类
    - 实现InvalidTokenException, TokenExpiredException, MissingTokenException
    - 实现UnauthorizedAccessException, MerchantMismatchException
    - _Requirements: 6.4_

- [-] 2. 实现JWT工具类
  - [x] 2.1 创建JwtUtil核心功能
    - 实现JWT令牌生成方法（generateToken）
    - 实现JWT令牌验证方法（validateToken）
    - 实现从令牌提取信息的方法（extractMerchantId, extractUsername, extractExpiration）
    - _Requirements: 1.1, 1.2, 5.2, 5.3_
  - [ ]* 2.2 编写Property Test: JWT令牌生成正确性
    - **Property 1: JWT Token Generation Correctness**
    - **Validates: Requirements 1.1, 1.2**
  - [ ]* 2.3 编写Property Test: 令牌签名完整性
    - **Property 4: Token Signature Integrity**
    - **Validates: Requirements 1.6**
  - [ ]* 2.4 编写Property Test: JWT令牌结构合规性
    - **Property 11: JWT Token Structure Compliance**
    - **Validates: Requirements 5.1, 5.2, 5.3**
  - [ ]* 2.5 编写JwtUtil单元测试
    - 测试令牌生成、验证、解析功能
    - 测试过期令牌处理
    - _Requirements: 5.4_

- [x] 3. 实现JWT认证过滤器 ✅ **COMPLETED**
  - [x] 3.1 创建JwtAuthenticationFilter ✅
    - 继承OncePerRequestFilter实现doFilterInternal方法
    - 实现从Authorization header提取JWT令牌
    - 实现令牌验证和安全上下文设置
    - _Requirements: 4.1, 4.2, 4.3_
  - [x] 3.2 实现认证失败处理 ✅
    - 处理缺失令牌、无效令牌、过期令牌等情况
    - 返回适当的HTTP状态码和错误消息
    - _Requirements: 4.4, 2.2, 2.3_
  - [ ]* 3.3 编写Property Test: 受保护端点认证要求
    - **Property 5: Protected Endpoint Authentication Requirement**
    - **Validates: Requirements 2.1, 2.2**
  - [ ]* 3.4 编写Property Test: 令牌过期强制执行
    - **Property 2: Token Expiration Enforcement**
    - **Validates: Requirements 1.3**
  - [ ]* 3.5 编写Property Test: 安全上下文填充
    - **Property 9: Security Context Population**
    - **Validates: Requirements 4.3, 4.5**

- [x] 4. Checkpoint - JWT基础设施验证 ✅ **COMPLETED**
  - 确保JWT生成、验证和过滤器功能正常，如有问题请询问用户

- [x] 5. 实现Spring Security配置
  - [x] 5.1 创建SecurityConfig配置类
    - 配置SecurityFilterChain，定义公开和受保护的端点
    - 添加JWT认证过滤器到过滤器链
    - 配置CORS设置
    - _Requirements: 4.6, 8.2_
  - [x] 5.2 创建JWT认证入口点
    - 实现JwtAuthenticationEntryPoint处理未认证请求
    - 返回统一的401错误响应
    - _Requirements: 6.4_
  - [x] 5.3 配置密码编码器和认证管理器
    - 配置BCryptPasswordEncoder
    - 配置AuthenticationManager
    - _Requirements: 1.4_
  - [ ]* 5.4 编写Property Test: 认证过滤器覆盖
    - **Property 10: Authentication Filter Coverage**
    - **Validates: Requirements 4.2, 4.6**

- [x] 6. 增强认证服务
  - [x] 6.1 创建AuthenticationService
    - 实现login方法，验证凭据并生成JWT令牌
    - 实现getCurrentMerchantId和isCurrentMerchant方法
    - 实现validateMerchantAccess方法用于授权检查
    - _Requirements: 1.1, 1.2, 2.5, 2.6_
  - [x] 6.2 更新MerchantService的login方法
    - 修改login方法返回AuthResponse（包含JWT令牌）
    - 集成密码验证和令牌生成
    - _Requirements: 1.1, 1.2_
  - [ ]* 6.3 编写Property Test: 无效凭据拒绝
    - **Property 3: Invalid Credentials Rejection**
    - **Validates: Requirements 1.4**
  - [ ]* 6.4 编写AuthenticationService单元测试
    - 测试登录成功和失败场景
    - 测试当前商家验证逻辑

- [x] 7. 实现授权控制
  - [x] 7.1 创建@RequireMerchantOwnership注解
    - 定义自定义注解用于标记需要商家所有权验证的方法
    - _Requirements: 3.6_
  - [x] 7.2 创建MerchantAuthorizationAspect
    - 实现AOP切面，拦截带有@RequireMerchantOwnership注解的方法
    - 验证当前认证商家是否有权访问请求的资源
    - _Requirements: 2.4, 2.6, 3.5_
  - [x] 7.3 更新MerchantController添加授权注解
    - 在所有受保护的端点方法上添加@RequireMerchantOwnership注解
    - 确保路径参数中的merchantId与当前认证商家匹配
    - _Requirements: 3.1, 3.2, 3.3, 3.4_
  - [ ] 7.4 编写Property Test: 跨商家访问防护
    - **Property 6: Cross-Merchant Access Prevention**
    - **Validates: Requirements 2.4, 2.6**
  - [ ]* 7.5 编写Property Test: 商家数据隔离
    - **Property 7: Merchant Data Isolation**
    - **Validates: Requirements 3.1, 3.2, 3.4**
  - [ ]* 7.6 编写Property Test: 商家资源修改控制
    - **Property 8: Merchant Resource Modification Control**
    - **Validates: Requirements 3.3**

- [x] 8. Checkpoint - 授权控制验证 ✅ **COMPLETED**
  - 确保商家只能访问自己的资源，如有问题请询问用户

- [x] 9. 实现安全日志和错误处理
  - [x] 9.1 创建SecurityExceptionHandler
    - 扩展GlobalExceptionHandler处理安全相关异常
    - 实现统一的安全错误响应格式
    - 添加安全事件日志记录
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_
  - [x] 9.2 实现安全审计日志
    - 在AuthorizationAspect中添加安全违规日志
    - 记录认证失败、授权失败等安全事件
    - 包含IP地址、时间戳、用户代理等信息
    - _Requirements: 6.1, 6.2, 6.6_
  - [ ]* 9.3 编写Property Test: 安全事件日志
    - **Property 13: Security Event Logging**
    - **Validates: Requirements 6.1, 6.2, 6.3**
  - [ ]* 9.4 编写Property Test: 错误响应一致性
    - **Property 14: Error Response Consistency**
    - **Validates: Requirements 6.4, 6.5**

- [-] 10. 创建认证控制器
  - [x] 10.1 实现AuthController
    - POST /api/v1/auth/login - 商家登录
    - POST /api/v1/auth/refresh - 刷新令牌
    - POST /api/v1/auth/logout - 登出
    - GET /api/v1/auth/validate - 验证令牌
    - GET /api/v1/auth/current - 获取当前商家信息
    - _Requirements: 1.1, 1.2, 1.3_
  - [ ]* 10.2 编写AuthController集成测试
    - 测试登录、刷新、验证等API端点
    - _Requirements: 7.2_

- [x] 11. 更新现有控制器
  - [x] 11.1 修改MerchantController
    - 更新login方法返回AuthResponse
    - 在所有受保护方法上添加@RequireMerchantOwnership注解
    - 移除不必要的商家存在性验证（由授权切面处理）
    - _Requirements: 7.1, 7.2_
  - [x] 11.2 更新其他相关控制器
    - 在CartController, OrderController等需要商家身份的端点添加认证
    - 确保用户相关的端点不受商家认证影响
    - _Requirements: 7.5, 7.6_

- [x] 12. 配置和环境支持
  - [x] 12.1 完善application.yml配置
    - 添加完整的JWT和安全配置
    - 配置不同环境的安全设置（dev, test, prod）
    - 添加CORS配置
    - _Requirements: 8.1, 8.2, 8.5, 8.6_
  - [x] 12.2 实现配置验证
    - 在应用启动时验证JWT密钥等关键配置
    - 提供清晰的配置错误消息
    - _Requirements: 5.6, 8.4_
  - [ ]* 12.3 编写配置相关测试
    - 测试不同环境配置的加载
    - 测试配置验证逻辑

- [x] 13. Checkpoint - 配置和集成验证
  - 确保所有配置正确，系统可以正常启动，如有问题请询问用户

- [x] 14. 向后兼容性测试 ✅ **COMPLETED**
  - [x] 14.1 验证现有API兼容性 ✅
    - 确保所有现有的API端点URL和请求/响应格式保持不变
    - 验证非安全相关的业务逻辑未受影响
    - _Requirements: 7.1, 7.2, 7.5, 7.6_
  - [ ]* 14.2 编写Property Test: 向后兼容性保持
    - **Property 15: Backward Compatibility Preservation**
    - **Validates: Requirements 7.1, 7.2, 7.5, 7.6**
  - [ ]* 14.3 运行现有测试套件
    - 确保所有现有的单元测试和集成测试仍然通过
    - 修复因安全更改导致的测试失败
    - _Requirements: 7.3_

- [x] 15. 完整安全测试
  - [x] 15.1 编写跨商家访问集成测试
    - 测试商家A无法访问商家B的库存、余额、结算记录
    - 测试所有受保护端点的访问控制
    - _Requirements: 2.4, 3.1, 3.2, 3.3, 3.4_
  - [x] 15.2 编写JWT生命周期集成测试
    - 测试令牌生成、使用、过期、刷新的完整流程
    - 测试令牌篡改检测
    - _Requirements: 1.1, 1.2, 1.3, 1.6_
  - [x] 15.3 编写安全边界测试
    - 测试各种攻击场景（无效令牌、过期令牌、篡改令牌等）
    - 验证错误处理和日志记录
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_
  - [ ]* 15.4 编写Property Test: 令牌验证完整性
    - **Property 12: Token Validation Completeness**
    - **Validates: Requirements 5.4**

- [x] 16. Final Checkpoint - 全面安全验证
  - 运行所有安全测试确保通过
  - 验证系统安全性和功能完整性
  - 如有问题请询问用户

## Notes

- 每个任务都引用了具体的需求条款以确保可追溯性
- 标记为*的任务为可选测试任务，可根据需要跳过以加快开发进度
- Checkpoint任务用于阶段性验证，确保增量开发的正确性
- Property测试验证核心安全属性的正确性
- 集成测试验证完整的安全流程和边界情况
- 重点关注向后兼容性，确保现有功能不受影响
