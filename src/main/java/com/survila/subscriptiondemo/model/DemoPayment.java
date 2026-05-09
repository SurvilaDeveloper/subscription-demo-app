package com.survila.subscriptiondemo.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public class DemoPayment implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String subscriptionId;
    private final String providerPaymentId;
    private final PaymentStatus status;
    private final String statusDetail;
    private final BigDecimal amount;
    private final String currency;
    private final Instant createdAt;

    public DemoPayment(
            String id,
            String subscriptionId,
            String providerPaymentId,
            PaymentStatus status,
            String statusDetail,
            BigDecimal amount,
            String currency,
            Instant createdAt
    ) {
        this.id = id;
        this.subscriptionId = subscriptionId;
        this.providerPaymentId = providerPaymentId;
        this.status = status;
        this.statusDetail = statusDetail;
        this.amount = amount;
        this.currency = currency;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getProviderPaymentId() {
        return providerPaymentId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getStatusDetail() {
        return statusDetail;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
