package com.survila.subscriptiondemo.controller;

import com.survila.subscriptiondemo.dto.StartSubscriptionRequest;
import com.survila.subscriptiondemo.dto.StartSubscriptionResponse;
import com.survila.subscriptiondemo.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/start")
    public StartSubscriptionResponse startSubscription(
            @Valid @RequestBody StartSubscriptionRequest request
    ) {
        return subscriptionService.startSubscription(request);
    }
}
