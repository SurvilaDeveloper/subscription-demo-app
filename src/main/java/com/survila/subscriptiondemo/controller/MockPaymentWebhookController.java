package com.survila.subscriptiondemo.controller;

import com.survila.subscriptiondemo.dto.webhook.MockPaymentWebhookPayload;
import com.survila.subscriptiondemo.model.DemoReceivedWebhook;
import com.survila.subscriptiondemo.service.SubscriptionService;
import com.survila.subscriptiondemo.service.WebhookSignatureVerifier;
import com.survila.subscriptiondemo.store.DemoStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/webhooks/mock-payment")
public class MockPaymentWebhookController {

    private final WebhookSignatureVerifier signatureVerifier;
    private final SubscriptionService subscriptionService;
    private final DemoStore store;

    public MockPaymentWebhookController(
            WebhookSignatureVerifier signatureVerifier,
            SubscriptionService subscriptionService,
            DemoStore store
    ) {
        this.signatureVerifier = signatureVerifier;
        this.subscriptionService = subscriptionService;
        this.store = store;
    }

    @PostMapping
    public ResponseEntity<Void> receiveWebhook(
            @RequestHeader(value = "x-request-id", required = false) String requestId,
            @RequestHeader(value = "x-signature", required = false) String signature,
            @RequestBody(required = false) MockPaymentWebhookPayload payload
    ) {
        String type = extractType(payload);
        String action = extractAction(payload);
        String dataId = extractDataId(payload);

        DemoReceivedWebhook receivedWebhook = null;

        try {
            signatureVerifier.verifyOrThrow(
                    dataId,
                    requestId,
                    signature
            );

            receivedWebhook = new DemoReceivedWebhook(
                    store.nextReceivedWebhookId(),
                    requestId,
                    signature,
                    type,
                    action,
                    dataId,
                    true,
                    false,
                    null,
                    Instant.now()
            );

            store.saveReceivedWebhook(receivedWebhook);

            subscriptionService.processWebhook(payload);

            receivedWebhook.markProcessed();
            store.saveReceivedWebhook(receivedWebhook);

            return ResponseEntity.ok().build();
        } catch (RuntimeException ex) {
            if (receivedWebhook == null) {
                receivedWebhook = new DemoReceivedWebhook(
                        store.nextReceivedWebhookId(),
                        requestId,
                        signature,
                        type,
                        action,
                        dataId,
                        false,
                        false,
                        ex.getMessage(),
                        Instant.now()
                );
            } else {
                receivedWebhook.markFailed(ex.getMessage());
            }

            store.saveReceivedWebhook(receivedWebhook);

            throw ex;
        }
    }

    private String extractType(MockPaymentWebhookPayload payload) {
        return payload == null ? null : payload.type();
    }

    private String extractAction(MockPaymentWebhookPayload payload) {
        return payload == null ? null : payload.action();
    }

    private String extractDataId(MockPaymentWebhookPayload payload) {
        if (payload == null || payload.data() == null) {
            return null;
        }

        return payload.data().id();
    }
}