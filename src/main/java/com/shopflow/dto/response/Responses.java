package com.shopflow.dto.response;

import com.shopflow.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Responses {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Auth {
        private String token;
        private String email;
        private String fullName;
        private String role;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProductSummary {
        private Long id;
        private String name;
        private String description;
        private BigDecimal price;
        private int stockQty;
        private String imageUrl;
        private LocalDateTime listedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CartItemView {
        private Long cartItemId;
        private Long productId;
        private String productName;
        private BigDecimal unitPrice;
        private int quantity;
        private BigDecimal lineTotal;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CartView {
        private Long cartId;
        private List<CartItemView> items;
        private BigDecimal grandTotal;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrderItemView {
        private Long productId;
        private String productName;
        private BigDecimal priceAtPurchase;
        private int quantity;
        private BigDecimal lineTotal;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrderView {
        private Long orderId;
        private List<OrderItemView> lineItems;
        private BigDecimal totalAmount;
        private String shippingAddress;
        private String pincode;
        private Order.PaymentStatus paymentStatus;
        private Order.OrderStatus orderStatus;
        private LocalDateTime orderedAt;
        private String stripePaymentIntentId;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PaymentIntentView {
        private String clientSecret;
        private String paymentIntentId;
        private BigDecimal amount;
        private String currency;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ApiMessage {
        private String message;
    }
}
