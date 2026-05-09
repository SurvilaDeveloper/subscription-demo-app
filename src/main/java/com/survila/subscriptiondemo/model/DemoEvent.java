package com.survila.subscriptiondemo.model;

import java.io.Serializable;
import java.time.Instant;

public class DemoEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String type;
    private final String message;
    private final Instant createdAt;

    public DemoEvent(
            String id,
            String type,
            String message,
            Instant createdAt
    ) {
        this.id = id;
        this.type = type;
        this.message = message;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
