package com.survila.subscriptiondemo.dto.mock;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

public record MockPreapprovalResponse(
        String id,
        String reason,

        @JsonProperty("external_reference")
        String externalReference,

        @JsonProperty("payer_email")
        String payerEmail,

        String status,

        Integer frequency,

        @JsonProperty("frequency_type")
        String frequencyType,

        @JsonProperty("transaction_amount")
        BigDecimal transactionAmount,

        @JsonProperty("currency_id")
        String currencyId,

        @JsonProperty("back_url")
        String backUrl,

        @JsonProperty("notification_url")
        String notificationUrl,

        @JsonProperty("init_point")
        String initPoint,

        @JsonProperty("consecutive_failed_charges")
        int consecutiveFailedCharges,

        @JsonProperty("created_at")
        Instant createdAt,

        @JsonProperty("updated_at")
        Instant updatedAt
) {
}
