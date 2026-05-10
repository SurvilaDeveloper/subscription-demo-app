package com.survila.subscriptiondemo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record SimulateRecurringChargeRequest(
        @JsonProperty("card_number")
        @NotBlank
        String cardNumber
) {
}
