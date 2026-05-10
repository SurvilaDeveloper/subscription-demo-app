package com.survila.subscriptiondemo.controller;

import com.survila.subscriptiondemo.dto.webhook.MockPaymentWebhookPayload;
import com.survila.subscriptiondemo.service.SubscriptionService;
import com.survila.subscriptiondemo.service.WebhookSignatureVerifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/mock-payment")
public class MockPaymentWebhookController {

    private final WebhookSignatureVerifier signatureVerifier;
    private final SubscriptionService subscriptionService;

    public MockPaymentWebhookController(
            WebhookSignatureVerifier signatureVerifier,
            SubscriptionService subscriptionService
    ) {
        this.signatureVerifier = signatureVerifier;
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public ResponseEntity<Void> receiveWebhook(
            @RequestHeader(value = "x-request-id", required = false) String requestId,
            @RequestHeader(value = "x-signature", required = false) String signature,
            @RequestBody MockPaymentWebhookPayload payload
    ) {
        signatureVerifier.verifyOrThrow(
                payload.data().id(),
                requestId,
                signature
        );

        subscriptionService.processWebhook(payload);

        return ResponseEntity.ok().build();
    }
}
