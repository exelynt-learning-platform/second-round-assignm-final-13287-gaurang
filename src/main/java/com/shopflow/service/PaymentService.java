package com.shopflow.service;

import com.shopflow.config.StripeConfig;
import com.shopflow.dto.response.Responses;
import com.shopflow.entity.Order;
import com.shopflow.exception.ShopExceptions;
import com.shopflow.repository.OrderRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final StripeConfig stripeConfig;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public Responses.PaymentIntentView createPaymentIntent(String userEmail, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ShopExceptions.ResourceNotFoundException(
                        "Order not found: " + orderId));

        if (!order.getPlacedBy().getEmail().equals(userEmail)) {
            throw new ShopExceptions.AccessDeniedException("Access denied to this order");
        }

        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            throw new ShopExceptions.PaymentException("This order has already been paid");
        }

        try {
            long amountInPaise = order.getTotalAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInPaise)
                    .setCurrency(stripeConfig.getCurrency())
                    .putMetadata("orderId", String.valueOf(orderId))
                    .putMetadata("userEmail", userEmail)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            orderService.attachPaymentIntent(orderId, intent.getId());

            log.info("Created PaymentIntent {} for order #{}", intent.getId(), orderId);

            return Responses.PaymentIntentView.builder()
                    .clientSecret(intent.getClientSecret())
                    .paymentIntentId(intent.getId())
                    .amount(order.getTotalAmount())
                    .currency(stripeConfig.getCurrency())
                    .build();

        } catch (StripeException ex) {
            log.error("Stripe error while creating PaymentIntent: {}", ex.getMessage());
            throw new ShopExceptions.PaymentException("Could not initiate payment: " + ex.getMessage());
        }
    }

    public void handleWebhookEvent(String rawPayload, String stripeSignatureHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(
                    rawPayload,
                    stripeSignatureHeader,
                    stripeConfig.getWebhookSecret()
            );
        } catch (SignatureVerificationException ex) {
            log.warn("Webhook signature verification failed: {}", ex.getMessage());
            throw new ShopExceptions.AccessDeniedException("Invalid webhook signature");
        }

        String eventType = event.getType();
        switch (eventType) {
            case "payment_intent.succeeded":
                String successIntentId = extractIntentId(event);
                log.info("Webhook: payment succeeded for intent {}", successIntentId);
                orderService.markOrderPaid(successIntentId);
                break;

            case "payment_intent.payment_failed":
                String failedIntentId = extractIntentId(event);
                log.warn("Webhook: payment failed for intent {}", failedIntentId);
                orderService.markOrderPaymentFailed(failedIntentId);
                break;

            default:
                log.debug("Unhandled Stripe event type: {}", eventType);
                break;
        }
    }

    private String extractIntentId(Event event) {
        return event.getDataObjectDeserializer()
                .getObject()
                .map(obj -> ((PaymentIntent) obj).getId())
                .orElseThrow(() -> new ShopExceptions.PaymentException(
                        "Could not deserialize PaymentIntent from webhook event"));
    }
}
