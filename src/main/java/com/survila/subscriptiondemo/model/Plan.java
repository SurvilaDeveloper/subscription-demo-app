package com.survila.subscriptiondemo.model;

import java.io.Serializable;
import java.math.BigDecimal;

public record Plan(
        String id,
        String name,
        String description,
        BigDecimal amount,
        String currency
) implements Serializable {

    private static final long serialVersionUID = 1L;
}
