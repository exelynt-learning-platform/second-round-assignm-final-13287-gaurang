package com.shopflow.controller;

import com.shopflow.dto.response.Responses;
import com.shopflow.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/intent/{orderId}")
    public ResponseEntity<Responses.PaymentIntentView> createIntent(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long orderId) {
        String email = principal.getUsername();
        Responses.PaymentIntentView result = paymentService.createPaymentIntent(email, orderId);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> stripeWebhook(
            @RequestBody String rawPayload,
            @RequestHeader("Stripe-Signature") String signature) {
        paymentService.handleWebhookEvent(rawPayload, signature);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
