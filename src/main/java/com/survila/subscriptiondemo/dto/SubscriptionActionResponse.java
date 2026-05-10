package com.survila.subscriptiondemo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.survila.subscriptiondemo.model.DemoPayment;
import com.survila.subscriptiondemo.model.DemoSubscription;

public record SubscriptionActionResponse(
        DemoSubscription subscription,

        DemoPayment payment,

        @JsonProperty("provider_subscription_id")
        String providerSubscriptionId,

        @JsonProperty("provider_payment_id")
        String providerPaymentId,

        @JsonProperty("provider_subscription_status")
        String providerSubscriptionStatus,

        @JsonProperty("provider_payment_status")
        String providerPaymentStatus
) {
}
