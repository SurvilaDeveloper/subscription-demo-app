package com.survila.subscriptiondemo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DemoInfoResponse(
        String name,

        @JsonProperty("public_base_url")
        String publicBaseUrl,

        @JsonProperty("webhook_base_url")
        String webhookBaseUrl,

        @JsonProperty("mock_payment_base_url")
        String mockPaymentBaseUrl,

        @JsonProperty("webhook_path")
        String webhookPath,

        @JsonProperty("webhook_url")
        String webhookUrl
) {
}
