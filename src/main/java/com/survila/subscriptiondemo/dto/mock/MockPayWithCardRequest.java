package com.survila.subscriptiondemo.dto.mock;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MockPayWithCardRequest(
        @JsonProperty("card_number")
        String cardNumber,

        @JsonProperty("cardholder_name")
        String cardholderName,

        @JsonProperty("expiration_month")
        Integer expirationMonth,

        @JsonProperty("expiration_year")
        Integer expirationYear,

        @JsonProperty("security_code")
        String securityCode,

        @JsonProperty("send_webhook")
        Boolean sendWebhook
) {
}