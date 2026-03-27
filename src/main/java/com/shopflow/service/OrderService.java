package com.shopflow.service;

import com.shopflow.dto.request.OrderRequest;
import com.shopflow.dto.response.Responses;
import com.shopflow.entity.*;
import com.shopflow.exception.ShopExceptions;
import com.shopflow.repository.OrderRepository;
import com.shopflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartService cartService;

    @Transactional
    public Responses.OrderView placeOrderFromCart(String userEmail, OrderRequest req) {
        Cart cart = cartService.resolveCart(userEmail);

        if (cart.getItems().isEmpty()) {
            throw new ShopExceptions.EmptyCartException();
        }

        User buyer = cart.getOwner();

        Order order = Order.builder()
                .placedBy(buyer)
                .shippingAddress(req.getShippingAddress())
                .pincode(req.getPincode())
                .totalAmount(cart.calculateTotal())
                .paymentStatus(Order.PaymentStatus.PENDING)
                .orderStatus(Order.OrderStatus.CREATED)
                .build();

        for (CartItem ci : cart.getItems()) {
            Product product = ci.getProduct();
            product.decreaseStock(ci.getQuantity());

            OrderItem line = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productNameSnapshot(product.getName())
                    .priceAtPurchase(product.getPrice())
                    .quantity(ci.getQuantity())
                    .build();

            order.getLineItems().add(line);
        }

        Order saved = orderRepository.save(order);

        cart.clearItems();
        cartService.resolveCart(userEmail);

        log.info("Order #{} placed for user: {}", saved.getId(), userEmail);
        return toOrderView(saved);
    }

    @Transactional(readOnly = true)
    public Responses.OrderView getOrderDetails(String userEmail, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ShopExceptions.ResourceNotFoundException(
                        "Order not found: " + orderId));

        if (!order.getPlacedBy().getEmail().equals(userEmail)) {
            throw new ShopExceptions.AccessDeniedException("You can only view your own orders");
        }

        return toOrderView(order);
    }

    @Transactional(readOnly = true)
    public List<Responses.OrderView> listUserOrders(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ShopExceptions.ResourceNotFoundException("User not found"));

        List<Order> userOrders = orderRepository.findByPlacedByOrderByOrderedAtDesc(user);
        List<Responses.OrderView> result = new ArrayList<>();
        for (Order order : userOrders) {
            result.add(toOrderView(order));
        }
        return result;
    }

    @Transactional
    public void markOrderPaid(String stripeIntentId) {
        Order order = orderRepository.findByStripePaymentIntentId(stripeIntentId)
                .orElseThrow(() -> new ShopExceptions.ResourceNotFoundException(
                        "No order found for payment intent: " + stripeIntentId));

        order.setPaymentStatus(Order.PaymentStatus.PAID);
        order.setOrderStatus(Order.OrderStatus.CONFIRMED);
        orderRepository.save(order);
        log.info("Order #{} marked as PAID via Stripe intent: {}", order.getId(), stripeIntentId);
    }

    @Transactional
    public void markOrderPaymentFailed(String stripeIntentId) {
        Order order = orderRepository.findByStripePaymentIntentId(stripeIntentId).orElse(null);
        if (order != null) {
            order.setPaymentStatus(Order.PaymentStatus.FAILED);
            orderRepository.save(order);
            log.warn("Payment FAILED for order #{}", order.getId());
        }
    }

    @Transactional
    public void attachPaymentIntent(Long orderId, String intentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ShopExceptions.ResourceNotFoundException("Order not found: " + orderId));
        order.setStripePaymentIntentId(intentId);
        orderRepository.save(order);
    }

    private Responses.OrderView toOrderView(Order order) {
        List<Responses.OrderItemView> lines = new ArrayList<>();
        for (OrderItem li : order.getLineItems()) {
            Long productId = null;
            if (li.getProduct() != null) {
                productId = li.getProduct().getId();
            }

            Responses.OrderItemView line = Responses.OrderItemView.builder()
                    .productId(productId)
                    .productName(li.getProductNameSnapshot())
                    .priceAtPurchase(li.getPriceAtPurchase())
                    .quantity(li.getQuantity())
                    .lineTotal(li.subtotal())
                    .build();
            lines.add(line);
        }

        return Responses.OrderView.builder()
                .orderId(order.getId())
                .lineItems(lines)
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .pincode(order.getPincode())
                .paymentStatus(order.getPaymentStatus())
                .orderStatus(order.getOrderStatus())
                .orderedAt(order.getOrderedAt())
                .stripePaymentIntentId(order.getStripePaymentIntentId())
                .build();
    }
}
