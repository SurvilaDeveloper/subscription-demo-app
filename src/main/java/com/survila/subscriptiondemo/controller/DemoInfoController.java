package com.survila.subscriptiondemo.controller;

import com.survila.subscriptiondemo.dto.DemoInfoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/demo/info")
public class DemoInfoController {

    private static final String WEBHOOK_PATH = "/api/webhooks/mock-payment";

    private final String publicBaseUrl;
    private final String webhookBaseUrl;
    private final String mockPaymentBaseUrl;

    public DemoInfoController(
            @Value("${demo-app.public-base-url:http://localhost:8080}") String publicBaseUrl,
            @Value("${demo-app.webhook-base-url:http://localhost:8080}") String webhookBaseUrl,
            @Value("${demo-app.mock-payment.base-url:http://localhost:9090}") String mockPaymentBaseUrl
    ) {
        this.publicBaseUrl = publicBaseUrl;
        this.webhookBaseUrl = webhookBaseUrl;
        this.mockPaymentBaseUrl = mockPaymentBaseUrl;
    }

    @GetMapping
    public DemoInfoResponse getInfo() {
        return new DemoInfoResponse(
                "subscription-demo-app",
                publicBaseUrl,
                webhookBaseUrl,
                mockPaymentBaseUrl,
                WEBHOOK_PATH,
                webhookBaseUrl + WEBHOOK_PATH
        );
    }
}
