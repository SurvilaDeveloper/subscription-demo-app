package com.survila.subscriptiondemo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.survila.subscriptiondemo.model.DemoSubscription;

public record CreateSubscriptionResponse(
        DemoSubscription subscription,

        @JsonProperty("provider_subscription_id")
        String providerSubscriptionId,

        @JsonProperty("provider_subscription_status")
        String providerSubscriptionStatus
) {
}
