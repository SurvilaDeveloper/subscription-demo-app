package com.survila.subscriptiondemo.dto.mock;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MockPayWithCardResponse(
        @JsonProperty("preapproval_id")
        String preapprovalId,

        @JsonProperty("preapproval_status")
        String preapprovalStatus,

        MockPaymentResponse payment,

        @JsonProperty("webhook_sent")
        boolean webhookSent
) {
}
