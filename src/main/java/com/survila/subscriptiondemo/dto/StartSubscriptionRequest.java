package com.survila.subscriptiondemo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record StartSubscriptionRequest(
        @JsonProperty("plan_id")
        @NotBlank
        String planId,

        @JsonProperty("payer_email")
        @Email
        @NotBlank
        String payerEmail,

        @JsonProperty("card_number")
        @NotBlank
        String cardNumber
) {
}
