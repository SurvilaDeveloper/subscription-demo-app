package com.survila.subscriptiondemo.controller;

import com.survila.subscriptiondemo.model.Plan;
import com.survila.subscriptiondemo.service.PlanCatalogService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanCatalogService planCatalogService;

    public PlanController(PlanCatalogService planCatalogService) {
        this.planCatalogService = planCatalogService;
    }

    @GetMapping
    public List<Plan> getPlans() {
        return planCatalogService.findAll();
    }
}
