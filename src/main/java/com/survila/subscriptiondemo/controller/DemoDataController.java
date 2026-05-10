package com.survila.subscriptiondemo.controller;

import com.survila.subscriptiondemo.model.DemoEvent;
import com.survila.subscriptiondemo.model.DemoPayment;
import com.survila.subscriptiondemo.model.DemoReceivedWebhook;
import com.survila.subscriptiondemo.model.DemoSubscription;
import com.survila.subscriptiondemo.store.DemoStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/demo")
public class DemoDataController {

    private final DemoStore store;

    public DemoDataController(DemoStore store) {
        this.store = store;
    }

    @GetMapping("/subscriptions")
    public List<DemoSubscription> getSubscriptions() {
        return store.findAllSubscriptions();
    }

    @GetMapping("/payments")
    public List<DemoPayment> getPayments() {
        return store.findAllPayments();
    }

    @GetMapping("/events")
    public List<DemoEvent> getEvents() {
        return store.findAllEvents();
    }

    @GetMapping("/webhooks")
    public List<DemoReceivedWebhook> getReceivedWebhooks() {
        return store.findAllReceivedWebhooks();
    }
}
