package com.shopflow.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class StripeConfig {

    @Value("${shopflow.stripe.secret-key}")
    private String secretKey;

    @Value("${shopflow.stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${shopflow.stripe.currency}")
    private String currency;

    @PostConstruct
    public void initStripe() {
        Stripe.apiKey = secretKey;
    }
}
