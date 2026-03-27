package com.shopflow.service;

import com.shopflow.dto.request.OrderRequest;
import com.shopflow.dto.response.Responses;
import com.shopflow.entity.*;
import com.shopflow.exception.ShopExceptions;
import com.shopflow.repository.OrderRepository;
import com.shopflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock UserRepository userRepository;
    @Mock CartService cartService;

    @InjectMocks
    OrderService orderService;

    private User buyer;
    private Product product;
    private Cart cartWithItems;
    private OrderRequest orderReq;

    @BeforeEach
    void prepareTestData() {
        buyer = User.builder()
                .id(1L)
                .email("buyer@shopflow.com")
                .fullName("Ravi Sharma")
                .role(User.Role.CUSTOMER)
                .build();

        product = Product.builder()
                .id(5L)
                .name("Mechanical Keyboard")
                .price(new BigDecimal("3499.00"))
                .stockQty(10)
                .active(true)
                .build();

        CartItem cartItem = CartItem.builder()
                .id(1L)
                .product(product)
                .quantity(2)
                .build();

        cartWithItems = Cart.builder()
                .id(1L)
                .owner(buyer)
                .items(new ArrayList<>(List.of(cartItem)))
                .build();

        cartItem.setCart(cartWithItems);

        orderReq = new OrderRequest();
        orderReq.setShippingAddress("B-612 Hinjewadi Phase 1, Pune");
        orderReq.setPincode("411057");
    }

    @Test
    @DisplayName("Placing an order from a non-empty cart should create an order and reduce stock")
    void placeOrder_validCart_createsOrderAndReducesStock() {
        when(cartService.resolveCart(buyer.getEmail())).thenReturn(cartWithItems);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(101L);
            return o;
        });

        Responses.OrderView result = orderService.placeOrderFromCart(buyer.getEmail(), orderReq);

        assertThat(result.getOrderId()).isEqualTo(101L);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("6998.00"); // 3499 × 2
        assertThat(result.getShippingAddress()).isEqualTo("B-612 Hinjewadi Phase 1, Pune");
        assertThat(result.getPaymentStatus()).isEqualTo(Order.PaymentStatus.PENDING);

        // stock should decrease from 10 → 8
        assertThat(product.getStockQty()).isEqualTo(8);
    }

    @Test
    @DisplayName("Placing an order with an empty cart throws EmptyCartException")
    void placeOrder_emptyCart_throwsEmptyCartException() {
        Cart emptyCart = Cart.builder()
                .id(2L).owner(buyer).items(new ArrayList<>()).build();

        when(cartService.resolveCart(buyer.getEmail())).thenReturn(emptyCart);

        assertThatThrownBy(() -> orderService.placeOrderFromCart(buyer.getEmail(), orderReq))
                .isInstanceOf(ShopExceptions.EmptyCartException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Getting an order that belongs to a different user should throw AccessDeniedException")
    void getOrderDetails_wrongUser_throwsForbidden() {
        Order order = Order.builder()
                .id(55L)
                .placedBy(buyer)
                .totalAmount(new BigDecimal("3499.00"))
                .paymentStatus(Order.PaymentStatus.PENDING)
                .orderStatus(Order.OrderStatus.CREATED)
                .lineItems(new ArrayList<>())
                .build();

        when(orderRepository.findById(55L)).thenReturn(Optional.of(order));

        // a different user is requesting order 55
        assertThatThrownBy(() -> orderService.getOrderDetails("intruder@evil.com", 55L))
                .isInstanceOf(ShopExceptions.AccessDeniedException.class);
    }

    @Test
    @DisplayName("markOrderPaid should flip status to PAID and CONFIRMED")
    void markOrderPaid_validIntentId_updatesStatuses() {
        Order pendingOrder = Order.builder()
                .id(10L)
                .placedBy(buyer)
                .stripePaymentIntentId("pi_test_123")
                .paymentStatus(Order.PaymentStatus.PENDING)
                .orderStatus(Order.OrderStatus.CREATED)
                .lineItems(new ArrayList<>())
                .totalAmount(new BigDecimal("3499.00"))
                .build();

        when(orderRepository.findByStripePaymentIntentId("pi_test_123"))
                .thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.markOrderPaid("pi_test_123");

        assertThat(pendingOrder.getPaymentStatus()).isEqualTo(Order.PaymentStatus.PAID);
        assertThat(pendingOrder.getOrderStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("markOrderPaymentFailed should flip payment status to FAILED")
    void markOrderPaymentFailed_knownIntent_setsFailedStatus() {
        Order order = Order.builder()
                .id(11L)
                .placedBy(buyer)
                .stripePaymentIntentId("pi_fail_456")
                .paymentStatus(Order.PaymentStatus.PENDING)
                .orderStatus(Order.OrderStatus.CREATED)
                .lineItems(new ArrayList<>())
                .totalAmount(new BigDecimal("1000.00"))
                .build();

        when(orderRepository.findByStripePaymentIntentId("pi_fail_456"))
                .thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.markOrderPaymentFailed("pi_fail_456");

        assertThat(order.getPaymentStatus()).isEqualTo(Order.PaymentStatus.FAILED);
    }
}
