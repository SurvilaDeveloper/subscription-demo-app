package com.survila.subscriptiondemo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DemoStateResponse(
        @JsonProperty("subscriptions_count")
        int subscriptionsCount,

        @JsonProperty("payments_count")
        int paymentsCount,

        @JsonProperty("events_count")
        int eventsCount,

        @JsonProperty("received_webhooks_count")
        int receivedWebhooksCount,

        @JsonProperty("storage_type")
        String storageType,

        @JsonProperty("state_file_path")
        String stateFilePath
) {
}