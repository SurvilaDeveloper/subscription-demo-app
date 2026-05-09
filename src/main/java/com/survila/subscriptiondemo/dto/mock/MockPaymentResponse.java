package com.survila.subscriptiondemo.dto.mock;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

public record MockPaymentResponse(
        String id,

        @JsonProperty("preapproval_id")
        String preapprovalId,

        String status,

        @JsonProperty("status_detail")
        String statusDetail,

        String reason,

        BigDecimal amount,

        @JsonProperty("currency_id")
        String currencyId,

        @JsonProperty("card_last_four")
        String cardLastFour,

        @JsonProperty("created_at")
        Instant createdAt
) {
}
