package com.survila.subscriptiondemo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record ChangeSubscriptionPlanRequest(
        @JsonProperty("plan_id")
        @NotBlank
        String planId
) {
}
