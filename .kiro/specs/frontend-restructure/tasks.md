# Implementation Plan: Frontend Restructure

## Overview

本实现计划将商品交易系统重构为前后端分离架构，将现有后端移至`backend/`目录，并创建完整的Vue 3前端应用。

## Tasks

- [x] 1. 项目结构重组
  - [x] 1.1 创建backend目录并移动后端代码
    - 创建`backend/`目录
    - 移动`src/`, `pom.xml`, `.jqwik-database`到backend目录
    - 更新backend目录下的配置
    - _Requirements: 1.1, 1.3_
  - [x] 1.2 验证后端测试通过
    - 在backend目录运行`mvn test`
    - 确保所有76个测试通过
    - _Requirements: 1.3_
  - [x] 1.3 配置后端CORS支持
    - 创建CorsConfig配置类
    - 允许前端开发服务器跨域访问
    - _Requirements: 10.1, 10.2, 10.3_

- [x] 2. Checkpoint - 后端重组验证
  - 确保后端测试通过，CORS配置正确

- [x] 3. 前端项目初始化
  - [x] 3.1 创建Vue 3项目
    - 使用Vite创建Vue 3项目
    - 安装依赖：vue-router, pinia, axios, element-plus
    - 配置vite.config.js代理
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_
  - [x] 3.2 创建项目基础结构
    - 创建目录结构：api/, stores/, views/, components/, layouts/, router/
    - 配置Element Plus
    - 创建基础布局组件
    - _Requirements: 9.1_
  - [x] 3.3 配置API模块
    - 创建axios实例和拦截器
    - 创建各模块API文件
    - _Requirements: 9.4, 9.6_
  - [x] 3.4 配置路由
    - 创建路由配置
    - 添加路由守卫
    - _Requirements: 9.2_
  - [x] 3.5 创建Pinia stores
    - 创建userStore, cartStore, productStore, orderStore, merchantStore
    - _Requirements: 9.3_

- [x] 4. Checkpoint - 前端基础验证
  - 确保前端项目可以启动，能够访问后端API

- [x] 5. 用户认证模块
  - [x] 5.1 实现登录页面
    - 创建Login.vue组件
    - 实现表单验证和提交
    - 处理登录成功/失败
    - _Requirements: 2.1, 2.3, 2.4_
  - [x] 5.2 实现注册页面
    - 创建Register.vue组件
    - 实现表单验证和提交
    - _Requirements: 2.2_
  - [x] 5.3 实现用户状态管理
    - 完善userStore登录/登出逻辑
    - 实现会话持久化
    - _Requirements: 2.5_

- [x] 6. 用户余额模块
  - [x] 6.1 实现用户个人中心
    - 创建Profile.vue组件
    - 显示用户信息和余额
    - _Requirements: 3.1_
  - [x] 6.2 实现充值功能
    - 创建充值对话框组件
    - 实现充值表单和提交
    - _Requirements: 3.2, 3.3, 3.4_
  - [x] 6.3 实现交易记录查看
    - 创建交易记录列表组件
    - 实现分页加载
    - _Requirements: 3.5_

- [x] 7. 商品浏览模块
  - [x] 7.1 实现商品列表页
    - 创建Products.vue组件
    - 实现商品卡片组件
    - 实现分页
    - _Requirements: 4.1, 4.5_
  - [x] 7.2 实现搜索和筛选
    - 添加搜索框
    - 添加分类筛选
    - _Requirements: 4.2, 4.3_
  - [x] 7.3 实现商品详情页
    - 创建ProductDetail.vue组件
    - 显示SKU列表和价格
    - 添加加入购物车按钮
    - _Requirements: 4.4_

- [x] 8. 购物车模块
  - [x] 8.1 实现购物车页面
    - 创建Cart.vue组件
    - 显示购物车商品列表
    - 显示总价
    - _Requirements: 5.2, 5.5_
  - [x] 8.2 实现购物车操作
    - 实现添加商品到购物车
    - 实现修改数量
    - 实现删除商品
    - _Requirements: 5.1, 5.3, 5.4_
  - [x] 8.3 实现购物车图标
    - 在Header显示购物车图标和数量
    - _Requirements: 5.1_

- [ ] 9. Checkpoint - 用户购物流程验证
  - 确保用户可以浏览商品、加入购物车

- [x] 10. 订单模块
  - [x] 10.1 实现下单功能
    - 创建结算确认页面
    - 实现从购物车创建订单
    - 实现直接购买
    - _Requirements: 6.1_
  - [x] 10.2 实现支付功能
    - 创建支付确认对话框
    - 实现支付API调用
    - _Requirements: 6.2_
  - [x] 10.3 实现订单列表页
    - 创建Orders.vue组件
    - 显示订单状态标签
    - 实现分页
    - _Requirements: 6.3_
  - [x] 10.4 实现订单详情页
    - 创建OrderDetail.vue组件
    - 显示订单商品和状态
    - 添加操作按钮（取消、退款、确认收货）
    - _Requirements: 6.4, 6.5, 6.6_

- [x] 11. Checkpoint - 用户完整流程验证
  - 确保用户可以完成完整购物流程

- [x] 12. 商家认证模块
  - [x] 12.1 实现商家登录页面
    - 创建merchant/Login.vue组件
    - _Requirements: 7.1_
  - [x] 12.2 实现商家注册页面
    - 创建merchant/Register.vue组件
    - _Requirements: 7.1_
  - [x] 12.3 实现商家状态管理
    - 完善merchantStore
    - _Requirements: 7.1_

- [x] 13. 商家仪表盘
  - [x] 13.1 实现商家仪表盘
    - 创建merchant/Dashboard.vue组件
    - 显示统计概览
    - _Requirements: 7.1_
  - [x] 13.2 实现商家余额查看
    - 显示余额和交易记录
    - _Requirements: 7.5_

- [x] 14. 商家库存管理
  - [x] 14.1 实现商品管理页面
    - 创建merchant/Products.vue组件
    - 实现商品创建表单
    - _Requirements: 7.2_
  - [x] 14.2 实现库存管理页面
    - 创建merchant/Inventory.vue组件
    - 显示库存列表
    - _Requirements: 7.2_
  - [x] 14.3 实现库存添加功能
    - 创建添加库存对话框
    - _Requirements: 7.3_
  - [x] 14.4 实现价格更新功能
    - 创建价格编辑功能
    - _Requirements: 7.4_

- [x] 15. 商家订单管理
  - [x] 15.1 实现商家订单列表
    - 创建merchant/Orders.vue组件
    - 显示商家相关订单
    - _Requirements: 8.1_
  - [x] 15.2 实现发货功能
    - 添加发货按钮和确认
    - _Requirements: 8.2_
  - [x] 15.3 实现退款处理
    - 添加退款处理功能
    - _Requirements: 8.3_

- [-] 16. 商家结算模块
  - [ ] 16.1 实现结算记录页面
    - 创建merchant/Settlements.vue组件
    - 显示结算记录列表
    - _Requirements: 7.6_

- [x] 17. Checkpoint - 商家完整流程验证
  - 确保商家可以完成库存管理和订单处理

- [x] 18. 项目文档
  - [x] 18.1 创建项目README
    - 编写项目说明
    - 添加启动指南
    - _Requirements: 1.4_

- [x] 19. Final Checkpoint - 全部功能验证
  - 验证所有用户功能
  - 验证所有商家功能
  - 确保前后端正常通信

## Notes

- 每个任务都引用了具体的需求条款以确保可追溯性
- Checkpoint任务用于阶段性验证
- 前端使用Vue 3 Composition API和Element Plus组件库
- 后端需要配置CORS以支持前端跨域访问

