package com.survila.subscriptiondemo.service;

import com.survila.subscriptiondemo.client.MockPaymentClient;
import com.survila.subscriptiondemo.dto.StartSubscriptionRequest;
import com.survila.subscriptiondemo.dto.StartSubscriptionResponse;
import com.survila.subscriptiondemo.dto.mock.MockCreatePreapprovalRequest;
import com.survila.subscriptiondemo.dto.mock.MockPayWithCardRequest;
import com.survila.subscriptiondemo.dto.mock.MockPayWithCardResponse;
import com.survila.subscriptiondemo.dto.mock.MockPreapprovalResponse;
import com.survila.subscriptiondemo.exception.BadRequestException;
import com.survila.subscriptiondemo.model.*;
import com.survila.subscriptiondemo.store.DemoStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SubscriptionService {

    private final DemoStore store;
    private final PlanCatalogService planCatalogService;
    private final MockPaymentClient mockPaymentClient;
    private final String publicBaseUrl;

    public SubscriptionService(
            DemoStore store,
            PlanCatalogService planCatalogService,
            MockPaymentClient mockPaymentClient,
            @Value("${demo-app.public-base-url:http://localhost:8080}") String publicBaseUrl
    ) {
        this.store = store;
        this.planCatalogService = planCatalogService;
        this.mockPaymentClient = mockPaymentClient;
        this.publicBaseUrl = publicBaseUrl;
    }

    public StartSubscriptionResponse startSubscription(StartSubscriptionRequest request) {
        Plan plan = planCatalogService.findById(request.planId())
                .orElseThrow(() -> new BadRequestException("Plan not found: " + request.planId()));

        Instant now = Instant.now();

        DemoSubscription subscription = new DemoSubscription(
                store.nextSubscriptionId(),
                plan.id(),
                plan.name(),
                plan.amount(),
                plan.currency(),
                request.payerEmail(),
                null,
                SubscriptionStatus.PENDING,
                now,
                now
        );

        store.saveSubscription(subscription);
        store.addEvent(
                "SUBSCRIPTION_CREATED",
                "Internal subscription %s was created for plan %s."
                        .formatted(subscription.getId(), plan.name())
        );

        MockPreapprovalResponse preapproval = mockPaymentClient.createPreapproval(
                buildCreatePreapprovalRequest(subscription)
        );

        subscription.setProviderSubscriptionId(preapproval.id());
        store.saveSubscription(subscription);

        store.addEvent(
                "PROVIDER_PREAPPROVAL_CREATED",
                "Provider preapproval %s was created for internal subscription %s."
                        .formatted(preapproval.id(), subscription.getId())
        );

        MockPayWithCardResponse payResponse = mockPaymentClient.payWithCard(
                preapproval.id(),
                buildPayWithCardRequest(request.cardNumber())
        );

        subscription.setStatus(mapPreapprovalStatus(payResponse.preapprovalStatus()));
        store.saveSubscription(subscription);

        DemoPayment payment = new DemoPayment(
                store.nextPaymentId(),
                subscription.getId(),
                payResponse.payment().id(),
                mapPaymentStatus(payResponse.payment().status()),
                payResponse.payment().statusDetail(),
                payResponse.payment().amount(),
                payResponse.payment().currencyId(),
                Instant.now()
        );

        store.savePayment(payment);

        store.addEvent(
                "PAYMENT_CREATED",
                "Payment %s was created from provider payment %s with status %s."
                        .formatted(payment.getId(), payment.getProviderPaymentId(), payment.getStatus())
        );

        store.addEvent(
                "SUBSCRIPTION_STATUS_UPDATED",
                "Internal subscription %s changed to %s from provider status %s."
                        .formatted(subscription.getId(), subscription.getStatus(), payResponse.preapprovalStatus())
        );

        return new StartSubscriptionResponse(
                subscription,
                payment,
                preapproval.id(),
                payResponse.payment().id(),
                payResponse.preapprovalStatus(),
                payResponse.payment().status()
        );
    }

    private MockCreatePreapprovalRequest buildCreatePreapprovalRequest(DemoSubscription subscription) {
        return new MockCreatePreapprovalRequest(
                subscription.getPlanName(),
                "demo_subscription_id=" + subscription.getId(),
                subscription.getPayerEmail(),
                new MockCreatePreapprovalRequest.AutoRecurringRequest(
                        1,
                        "months",
                        subscription.getAmount(),
                        subscription.getCurrency()
                ),
                publicBaseUrl,
                publicBaseUrl + "/api/webhooks/mock-payment"
        );
    }

    private MockPayWithCardRequest buildPayWithCardRequest(String cardNumber) {
        return new MockPayWithCardRequest(
                cardNumber,
                "StreamBox User",
                12,
                2030,
                "123",
                false
        );
    }

    private SubscriptionStatus mapPreapprovalStatus(String providerStatus) {
        if (providerStatus == null) {
            return SubscriptionStatus.PENDING;
        }

        return switch (providerStatus.toLowerCase()) {
            case "authorized" -> SubscriptionStatus.ACTIVE;
            case "payment_failed" -> SubscriptionStatus.PAYMENT_FAILED;
            case "paused" -> SubscriptionStatus.PAUSED;
            case "cancelled" -> SubscriptionStatus.CANCELLED;
            case "pending" -> SubscriptionStatus.PENDING;
            default -> SubscriptionStatus.PENDING;
        };
    }

    private PaymentStatus mapPaymentStatus(String providerStatus) {
        if (providerStatus == null) {
            return PaymentStatus.UNKNOWN;
        }

        return switch (providerStatus.toLowerCase()) {
            case "approved" -> PaymentStatus.APPROVED;
            case "rejected" -> PaymentStatus.REJECTED;
            case "in_process" -> PaymentStatus.IN_PROCESS;
            default -> PaymentStatus.UNKNOWN;
        };
    }
}
