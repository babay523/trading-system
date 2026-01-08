package com.trading.service.impl;

import com.trading.dto.request.DirectPurchaseRequest;
import com.trading.dto.response.OrderItemResponse;
import com.trading.dto.response.OrderResponse;
import com.trading.entity.*;
import com.trading.enums.OrderStatus;
import com.trading.enums.TransactionType;
import com.trading.exception.InsufficientBalanceException;
import com.trading.exception.InsufficientStockException;
import com.trading.exception.InvalidOperationException;
import com.trading.exception.ResourceNotFoundException;
import com.trading.repository.*;
import com.trading.service.OrderService;
import com.trading.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;
    private final TransactionService transactionService;

    @Override
    @Transactional
    public OrderResponse createFromCart(Long userId) {
        log.debug("从购物车为用户 {} 创建订单", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        if (cartItems.isEmpty()) {
            throw new InvalidOperationException("购物车为空");
        }

        // 获取第一个商品的库存以确定商家
        Inventory firstInventory = inventoryRepository.findBySku(cartItems.get(0).getSku())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", cartItems.get(0).getSku()));
        Long merchantId = firstInventory.getMerchantId();

        // 创建订单
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .userId(userId)
                .merchantId(merchantId)
                .totalAmount(BigDecimal.ZERO)
                .status(OrderStatus.PENDING)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            Inventory inventory = inventoryRepository.findBySku(cartItem.getSku())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory", cartItem.getSku()));

            Product product = productRepository.findById(inventory.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", inventory.getProductId()));

            BigDecimal subtotal = inventory.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .sku(cartItem.getSku())
                    .productName(product.getName())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(inventory.getPrice())
                    .subtotal(subtotal)
                    .build();

            order.addItem(orderItem);
            totalAmount = totalAmount.add(subtotal);
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        log.info("为用户 {} 从购物车创建订单 {}", savedOrder.getOrderNumber(), userId);
        return toOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse createDirect(Long userId, DirectPurchaseRequest request) {
        log.debug("为用户 {} 创建直接购买订单: sku={}, quantity={}",
                userId, request.getSku(), request.getQuantity());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Inventory inventory = inventoryRepository.findBySku(request.getSku())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", request.getSku()));

        Product product = productRepository.findById(inventory.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", inventory.getProductId()));

        BigDecimal subtotal = inventory.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .userId(userId)
                .merchantId(inventory.getMerchantId())
                .totalAmount(subtotal)
                .status(OrderStatus.PENDING)
                .build();

        OrderItem orderItem = OrderItem.builder()
                .sku(request.getSku())
                .productName(product.getName())
                .quantity(request.getQuantity())
                .unitPrice(inventory.getPrice())
                .subtotal(subtotal)
                .build();

        order.addItem(orderItem);
        Order savedOrder = orderRepository.save(order);

        log.info("为用户 {} 创建直接购买订单 {}", savedOrder.getOrderNumber(), userId);
        return toOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse confirmPayment(Long orderId) {
        log.debug("确认订单 {} 的支付", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOperationException("订单不在待支付状态");
        }

        User user = userRepository.findById(order.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", order.getUserId()));

        Merchant merchant = merchantRepository.findById(order.getMerchantId())
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", order.getMerchantId()));

        // 检查用户余额
        if (user.getBalance().compareTo(order.getTotalAmount()) < 0) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            throw new InsufficientBalanceException("余额不足");
        }

        // 检查并减少每件商品的库存
        for (OrderItem item : order.getItems()) {
            Inventory inventory = inventoryRepository.findBySku(item.getSku())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory", item.getSku()));

            if (inventory.getQuantity() < item.getQuantity()) {
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                throw new InsufficientStockException("SKU: " + item.getSku() + " 库存不足");
            }

            inventory.setQuantity(inventory.getQuantity() - item.getQuantity());
            inventoryRepository.save(inventory);
        }

        // 扣除用户余额
        BigDecimal userBalanceBefore = user.getBalance();
        user.setBalance(user.getBalance().subtract(order.getTotalAmount()));
        userRepository.save(user);

        // 增加商家余额
        BigDecimal merchantBalanceBefore = merchant.getBalance();
        merchant.setBalance(merchant.getBalance().add(order.getTotalAmount()));
        merchantRepository.save(merchant);

        // 更新订单状态
        order.setStatus(OrderStatus.PAID);
        Order savedOrder = orderRepository.save(order);

        // 创建交易记录
        transactionService.createUserTransaction(
                user.getId(),
                TransactionType.PURCHASE,
                order.getTotalAmount(),
                userBalanceBefore,
                user.getBalance(),
                order.getId()
        );

        transactionService.createMerchantTransaction(
                merchant.getId(),
                TransactionType.SALE,
                order.getTotalAmount(),
                merchantBalanceBefore,
                merchant.getBalance(),
                order.getId()
        );

        // 支付成功后清空购物车
        cartItemRepository.deleteByUserId(order.getUserId());

        log.info("订单 {} 支付确认", savedOrder.getOrderNumber());
        return toOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse ship(Long orderId) {
        log.debug("发货订单 {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getStatus() != OrderStatus.PAID) {
            throw new InvalidOperationException("订单必须是已支付状态才能发货");
        }

        order.setStatus(OrderStatus.SHIPPED);
        Order savedOrder = orderRepository.save(order);

        log.info("订单 {} 已发货", savedOrder.getOrderNumber());
        return toOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse complete(Long orderId) {
        log.debug("完成订单 {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new InvalidOperationException("订单必须是已发货状态才能完成");
        }

        order.setStatus(OrderStatus.COMPLETED);
        Order savedOrder = orderRepository.save(order);

        log.info("订单 {} 已完成", savedOrder.getOrderNumber());
        return toOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse cancel(Long orderId) {
        log.debug("取消订单 {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOperationException("只能取消待支付的订单");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);

        log.info("订单 {} 已取消", savedOrder.getOrderNumber());
        return toOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse refund(Long orderId) {
        log.debug("退款订单 {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.SHIPPED) {
            throw new InvalidOperationException("只能退款已支付或已发货的订单");
        }

        User user = userRepository.findById(order.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", order.getUserId()));

        Merchant merchant = merchantRepository.findById(order.getMerchantId())
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", order.getMerchantId()));

        // 退款给用户
        BigDecimal userBalanceBefore = user.getBalance();
        user.setBalance(user.getBalance().add(order.getTotalAmount()));
        userRepository.save(user);

        // 从商家扣款
        BigDecimal merchantBalanceBefore = merchant.getBalance();
        merchant.setBalance(merchant.getBalance().subtract(order.getTotalAmount()));
        merchantRepository.save(merchant);

        // 更新订单状态
        order.setStatus(OrderStatus.REFUNDED);
        Order savedOrder = orderRepository.save(order);

        // 创建交易记录
        transactionService.createUserTransaction(
                user.getId(),
                TransactionType.REFUND_IN,
                order.getTotalAmount(),
                userBalanceBefore,
                user.getBalance(),
                order.getId()
        );

        transactionService.createMerchantTransaction(
                merchant.getId(),
                TransactionType.REFUND_OUT,
                order.getTotalAmount(),
                merchantBalanceBefore,
                merchant.getBalance(),
                order.getId()
        );

        log.info("订单 {} 已退款", savedOrder.getOrderNumber());
        return toOrderResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        return toOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getByUser(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable)
                .map(this::toOrderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getByMerchant(Long merchantId, Pageable pageable) {
        return orderRepository.findByMerchantId(merchantId, pageable)
                .map(this::toOrderResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getByMerchantAndStatus(Long merchantId, OrderStatus status, Pageable pageable) {
        log.debug("获取商家 {} 状态为 {} 的订单", merchantId, status);
        return orderRepository.findByMerchantIdAndStatus(merchantId, status, pageable)
                .map(this::toOrderResponse);
    }

    /**
     * 生成订单号
     * 基于当前时间戳和UUID生成唯一的订单号
     * 
     * @return 生成的订单号
     */
    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD" + timestamp + random;
    }

    /**
     * 将订单实体转换为响应对象
     * 将Order实体对象转换为OrderResponse响应对象
     * 
     * @param order 订单实体
     * @return 订单响应对象
     */
    private OrderResponse toOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(this::toOrderItemResponse)
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .merchantId(order.getMerchantId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    /**
     * 将订单项实体转换为响应对象
     * 将OrderItem实体对象转换为OrderItemResponse响应对象
     * 
     * @param item 订单项实体
     * @return 订单项响应对象
     */
    private OrderItemResponse toOrderItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .sku(item.getSku())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .build();
    }
}