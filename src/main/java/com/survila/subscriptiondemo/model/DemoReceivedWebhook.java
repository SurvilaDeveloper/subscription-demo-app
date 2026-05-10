package com.survila.subscriptiondemo.model;

import java.io.Serializable;
import java.time.Instant;

public class DemoReceivedWebhook implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String requestId;
    private final String signature;
    private final String type;
    private final String action;
    private final String dataId;
    private final boolean validSignature;
    private boolean processed;
    private String error;
    private final Instant receivedAt;

    public DemoReceivedWebhook(
            String id,
            String requestId,
            String signature,
            String type,
            String action,
            String dataId,
            boolean validSignature,
            boolean processed,
            String error,
            Instant receivedAt
    ) {
        this.id = id;
        this.requestId = requestId;
        this.signature = signature;
        this.type = type;
        this.action = action;
        this.dataId = dataId;
        this.validSignature = validSignature;
        this.processed = processed;
        this.error = error;
        this.receivedAt = receivedAt;
    }

    public String getId() {
        return id;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getSignature() {
        return signature;
    }

    public String getType() {
        return type;
    }

    public String getAction() {
        return action;
    }

    public String getDataId() {
        return dataId;
    }

    public boolean isValidSignature() {
        return validSignature;
    }

    public boolean isProcessed() {
        return processed;
    }

    public String getError() {
        return error;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void markProcessed() {
        this.processed = true;
        this.error = null;
    }

    public void markFailed(String error) {
        this.processed = false;
        this.error = error;
    }
}
