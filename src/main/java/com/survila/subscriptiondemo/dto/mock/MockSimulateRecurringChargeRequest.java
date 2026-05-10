package com.survila.subscriptiondemo.dto.mock;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MockSimulateRecurringChargeRequest(
        @JsonProperty("card_number")
        String cardNumber,

        @JsonProperty("send_webhook")
        Boolean sendWebhook
) {
}
