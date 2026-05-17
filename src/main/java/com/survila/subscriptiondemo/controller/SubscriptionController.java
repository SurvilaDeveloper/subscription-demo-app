package com.survila.subscriptiondemo.controller;

import com.survila.subscriptiondemo.dto.ChangeSubscriptionPlanRequest;
import com.survila.subscriptiondemo.dto.CreateSubscriptionRequest;
import com.survila.subscriptiondemo.dto.CreateSubscriptionResponse;
import com.survila.subscriptiondemo.dto.PaySubscriptionRequest;
import com.survila.subscriptiondemo.dto.SimulateRecurringChargeRequest;
import com.survila.subscriptiondemo.dto.StartSubscriptionRequest;
import com.survila.subscriptiondemo.dto.StartSubscriptionResponse;
import com.survila.subscriptiondemo.dto.SubscriptionActionResponse;
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

    @PostMapping
    public CreateSubscriptionResponse createSubscription(
            @Valid @RequestBody CreateSubscriptionRequest request
    ) {
        return subscriptionService.createSubscription(request);
    }

    @PostMapping("/{id}/pay")
    public SubscriptionActionResponse paySubscription(
            @PathVariable String id,
            @Valid @RequestBody PaySubscriptionRequest request
    ) {
        return subscriptionService.paySubscription(id, request);
    }

    @PostMapping("/{id}/reconcile-provider")
    public CreateSubscriptionResponse reconcileProviderSubscription(@PathVariable String id) {
        return subscriptionService.reconcileProviderSubscription(id);
    }

    @PostMapping("/start")
    public StartSubscriptionResponse startSubscription(
            @Valid @RequestBody StartSubscriptionRequest request
    ) {
        return subscriptionService.startSubscription(request);
    }

    @PostMapping("/{id}/simulate-recurring-charge")
    public SubscriptionActionResponse simulateRecurringCharge(
            @PathVariable String id,
            @Valid @RequestBody SimulateRecurringChargeRequest request
    ) {
        return subscriptionService.simulateRecurringCharge(id, request);
    }

    @PostMapping("/{id}/change-plan")
    public SubscriptionActionResponse changePlan(
            @PathVariable String id,
            @Valid @RequestBody ChangeSubscriptionPlanRequest request
    ) {
        return subscriptionService.changePlan(id, request);
    }

    @PostMapping("/{id}/cancel")
    public SubscriptionActionResponse cancelSubscription(
            @PathVariable String id
    ) {
        return subscriptionService.cancelSubscription(id);
    }
}
