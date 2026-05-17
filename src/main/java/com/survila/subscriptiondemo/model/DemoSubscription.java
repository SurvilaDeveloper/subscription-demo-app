package com.survila.subscriptiondemo.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public class DemoSubscription implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private String planId;
    private String planName;
    private BigDecimal amount;
    private String currency;
    private final String payerEmail;

    private String providerExternalReference;
    private String providerSubscriptionId;
    private SubscriptionStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public DemoSubscription(
            String id,
            String planId,
            String planName,
            BigDecimal amount,
            String currency,
            String payerEmail,
            String providerSubscriptionId,
            SubscriptionStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.planId = planId;
        this.planName = planName;
        this.amount = amount;
        this.currency = currency;
        this.payerEmail = payerEmail;
        this.providerSubscriptionId = providerSubscriptionId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getPlanId() {
        return planId;
    }

    public String getPlanName() {
        return planName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getPayerEmail() {
        return payerEmail;
    }

    public String getProviderExternalReference() {
        return providerExternalReference;
    }

    public void setProviderExternalReference(String providerExternalReference) {
        this.providerExternalReference = providerExternalReference;
        this.updatedAt = Instant.now();
    }

    public String getProviderSubscriptionId() {
        return providerSubscriptionId;
    }

    public void setProviderSubscriptionId(String providerSubscriptionId) {
        this.providerSubscriptionId = providerSubscriptionId;
        this.updatedAt = Instant.now();
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void replacePlan(Plan plan) {
        this.planId = plan.id();
        this.planName = plan.name();
        this.amount = plan.amount();
        this.currency = plan.currency();
        this.updatedAt = Instant.now();
    }
}
