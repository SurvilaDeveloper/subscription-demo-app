package com.survila.subscriptiondemo.service;

import com.survila.subscriptiondemo.model.Plan;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class PlanCatalogService {

    private final List<Plan> plans = List.of(
            new Plan(
                    "basic",
                    "Plan Básico",
                    "Acceso a películas y series en calidad estándar.",
                    new BigDecimal("10000"),
                    "ARS"
            ),
            new Plan(
                    "pro",
                    "Plan Pro",
                    "Acceso HD, múltiples dispositivos y contenido destacado.",
                    new BigDecimal("20000"),
                    "ARS"
            ),
            new Plan(
                    "enterprise",
                    "Plan Empresarial",
                    "Acceso para equipos, soporte prioritario y administración avanzada.",
                    new BigDecimal("50000"),
                    "ARS"
            )
    );

    public List<Plan> findAll() {
        return plans;
    }

    public Optional<Plan> findById(String id) {
        return plans.stream()
                .filter(plan -> plan.id().equals(id))
                .findFirst();
    }
}
