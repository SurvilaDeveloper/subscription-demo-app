package com.survila.subscriptiondemo.service;

import com.survila.subscriptiondemo.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class WebhookSignatureVerifier {

    private final String webhookSecret;

    public WebhookSignatureVerifier(
            @Value("${demo-app.mock-payment.webhook-secret:dev-secret}") String webhookSecret
    ) {
        this.webhookSecret = webhookSecret;
    }

    public void verifyOrThrow(String dataId, String requestId, String signatureHeader) {
        if (dataId == null || dataId.isBlank()) {
            throw new BadRequestException("Missing data.id in webhook payload.");
        }

        if (requestId == null || requestId.isBlank()) {
            throw new BadRequestException("Missing x-request-id header.");
        }

        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new BadRequestException("Missing x-signature header.");
        }

        String timestampValue = extractSignaturePart(signatureHeader, "ts");
        String receivedHash = extractSignaturePart(signatureHeader, "v1");

        if (timestampValue == null || timestampValue.isBlank()) {
            throw new BadRequestException("Missing ts in x-signature header.");
        }

        if (receivedHash == null || receivedHash.isBlank()) {
            throw new BadRequestException("Missing v1 in x-signature header.");
        }

        long timestamp;

        try {
            timestamp = Long.parseLong(timestampValue);
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Invalid ts in x-signature header.");
        }

        String expectedHash = createHash(dataId, requestId, timestamp);

        boolean valid = MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.UTF_8),
                receivedHash.getBytes(StandardCharsets.UTF_8)
        );

        if (!valid) {
            throw new BadRequestException("Invalid webhook signature.");
        }
    }

    private String createHash(String dataId, String requestId, long timestamp) {
        String manifest = "id:%s;request-id:%s;ts:%d;".formatted(dataId, requestId, timestamp);

        return hmacSha256Hex(webhookSecret, manifest);
    }

    private String extractSignaturePart(String signatureHeader, String key) {
        String[] parts = signatureHeader.split(",");

        for (String part : parts) {
            String[] keyValue = part.trim().split("=", 2);

            if (keyValue.length == 2 && keyValue[0].trim().equals(key)) {
                return keyValue[1].trim();
            }
        }

        return null;
    }

    private String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            SecretKeySpec secretKey = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );

            mac.init(secretKey);

            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder result = new StringBuilder();

            for (byte b : bytes) {
                result.append(String.format("%02x", b));
            }

            return result.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not verify webhook signature.", ex);
        }
    }
}
