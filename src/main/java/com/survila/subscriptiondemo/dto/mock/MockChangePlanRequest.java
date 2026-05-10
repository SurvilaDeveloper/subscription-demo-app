package com.survila.subscriptiondemo.dto.mock;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MockChangePlanRequest(
        String reason,

        @JsonProperty("auto_recurring")
        MockCreatePreapprovalRequest.AutoRecurringRequest autoRecurring,

        @JsonProperty("send_webhook")
        Boolean sendWebhook
) {
}
