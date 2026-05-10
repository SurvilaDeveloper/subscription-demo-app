package com.survila.subscriptiondemo.dto.mock;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MockSubscriptionActionRequest(
        @JsonProperty("send_webhook")
        Boolean sendWebhook
) {
}
