package com.survila.subscriptiondemo.model;

public enum SubscriptionStatus {
    CREATING,
    CREATION_FAILED,
    RECONCILIATION_NEEDED,
    PENDING,
    ACTIVE,
    PAYMENT_FAILED,
    PAUSED,
    CANCELLED
}
