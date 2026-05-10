package com.survila.subscriptiondemo.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MockPaymentWebhookPayload(
        String id,

        @JsonProperty("live_mode")
        boolean liveMode,

        String type,

        @JsonProperty("date_created")
        String dateCreated,

        @JsonProperty("user_id")
        String userId,

        @JsonProperty("api_version")
        String apiVersion,

        String action,

        WebhookData data
) {
    public record WebhookData(
            String id
    ) {
    }
}
