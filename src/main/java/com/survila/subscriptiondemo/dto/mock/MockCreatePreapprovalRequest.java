package com.survila.subscriptiondemo.dto.mock;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record MockCreatePreapprovalRequest(
        String reason,

        @JsonProperty("external_reference")
        String externalReference,

        @JsonProperty("payer_email")
        String payerEmail,

        @JsonProperty("auto_recurring")
        AutoRecurringRequest autoRecurring,

        @JsonProperty("back_url")
        String backUrl,

        @JsonProperty("notification_url")
        String notificationUrl
) {
    public record AutoRecurringRequest(
            Integer frequency,

            @JsonProperty("frequency_type")
            String frequencyType,

            @JsonProperty("transaction_amount")
            BigDecimal transactionAmount,

            @JsonProperty("currency_id")
            String currencyId
    ) {
    }
}
