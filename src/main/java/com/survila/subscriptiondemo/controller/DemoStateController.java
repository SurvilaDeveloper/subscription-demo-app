package com.survila.subscriptiondemo.controller;

import com.survila.subscriptiondemo.dto.DemoStateResponse;
import com.survila.subscriptiondemo.store.DemoStore;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/demo/state")
public class DemoStateController {

    private final DemoStore store;

    public DemoStateController(DemoStore store) {
        this.store = store;
    }

    @GetMapping
    public DemoStateResponse getState() {
        return currentState();
    }

    @DeleteMapping
    public DemoStateResponse resetState() {
        store.clearAll();
        store.addEvent("STATE_RESET", "Se reseteó el estado de StreamBox Demo.");
        return currentState();
    }

    private DemoStateResponse currentState() {
        return new DemoStateResponse(
                store.countSubscriptions(),
                store.countPayments(),
                store.countEvents(),
                store.countReceivedWebhooks(),
                store.getStorageType(),
                store.getStateFilePath()
        );
    }
}
