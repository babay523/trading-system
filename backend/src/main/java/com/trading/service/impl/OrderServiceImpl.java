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
        log.debug("Creating order from cart for user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        if (cartItems.isEmpty()) {
            throw new InvalidOperationException("Cart is empty");
        }

        // Get first item's inventory to determine merchant
        Inventory firstInventory = inventoryRepository.findBySku(cartItems.get(0).getSku())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", cartItems.get(0).getSku()));
        Long merchantId = firstInventory.getMerchantId();

        // Create order
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

        log.info("Created order {} from cart for user {}", savedOrder.getOrderNumber(), userId);
        return toOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse createDirect(Long userId, DirectPurchaseRequest request) {
        log.debug("Creating direct order for user {}: sku={}, quantity={}",
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

        log.info("Created direct order {} for user {}", savedOrder.getOrderNumber(), userId);
        return toOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse confirmPayment(Long orderId) {
        log.debug("Confirming payment for order {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOperationException("Order is not in PENDING status");
        }

        User user = userRepository.findById(order.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", order.getUserId()));

        Merchant merchant = merchantRepository.findById(order.getMerchantId())
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", order.getMerchantId()));

        // Check user balance
        if (user.getBalance().compareTo(order.getTotalAmount()) < 0) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            throw new InsufficientBalanceException("Insufficient balance");
        }

        // Check and reduce inventory for each item
        for (OrderItem item : order.getItems()) {
            Inventory inventory = inventoryRepository.findBySku(item.getSku())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory", item.getSku()));

            if (inventory.getQuantity() < item.getQuantity()) {
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                throw new InsufficientStockException("Insufficient stock for SKU: " + item.getSku());
            }

            inventory.setQuantity(inventory.getQuantity() - item.getQuantity());
            inventoryRepository.save(inventory);
        }

        // Deduct user balance
        BigDecimal userBalanceBefore = user.getBalance();
        user.setBalance(user.getBalance().subtract(order.getTotalAmount()));
        userRepository.save(user);

        // Add merchant balance
        BigDecimal merchantBalanceBefore = merchant.getBalance();
        merchant.setBalance(merchant.getBalance().add(order.getTotalAmount()));
        merchantRepository.save(merchant);

        // Update order status
        order.setStatus(OrderStatus.PAID);
        Order savedOrder = orderRepository.save(order);

        // Create transaction records
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

        // Clear cart after successful payment
        cartItemRepository.deleteByUserId(order.getUserId());

        log.info("Payment confirmed for order {}", savedOrder.getOrderNumber());
        return toOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse ship(Long orderId) {
        log.debug("Shipping order {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getStatus() != OrderStatus.PAID) {
            throw new InvalidOperationException("Order must be PAID to ship");
        }

        order.setStatus(OrderStatus.SHIPPED);
        Order savedOrder = orderRepository.save(order);

        log.info("Order {} shipped", savedOrder.getOrderNumber());
        return toOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse complete(Long orderId) {
        log.debug("Completing order {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new InvalidOperationException("Order must be SHIPPED to complete");
        }

        order.setStatus(OrderStatus.COMPLETED);
        Order savedOrder = orderRepository.save(order);

        log.info("Order {} completed", savedOrder.getOrderNumber());
        return toOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse cancel(Long orderId) {
        log.debug("Cancelling order {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOperationException("Only PENDING orders can be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);

        log.info("Order {} cancelled", savedOrder.getOrderNumber());
        return toOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse refund(Long orderId) {
        log.debug("Refunding order {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.SHIPPED) {
            throw new InvalidOperationException("Only PAID or SHIPPED orders can be refunded");
        }

        User user = userRepository.findById(order.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", order.getUserId()));

        Merchant merchant = merchantRepository.findById(order.getMerchantId())
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", order.getMerchantId()));

        // Return money to user
        BigDecimal userBalanceBefore = user.getBalance();
        user.setBalance(user.getBalance().add(order.getTotalAmount()));
        userRepository.save(user);

        // Deduct from merchant
        BigDecimal merchantBalanceBefore = merchant.getBalance();
        merchant.setBalance(merchant.getBalance().subtract(order.getTotalAmount()));
        merchantRepository.save(merchant);

        // Update order status
        order.setStatus(OrderStatus.REFUNDED);
        Order savedOrder = orderRepository.save(order);

        // Create transaction records
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

        log.info("Order {} refunded", savedOrder.getOrderNumber());
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

    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD" + timestamp + random;
    }

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
