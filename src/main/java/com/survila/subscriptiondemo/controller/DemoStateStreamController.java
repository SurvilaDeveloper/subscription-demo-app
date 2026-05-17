package com.survila.subscriptiondemo.controller;

import com.survila.subscriptiondemo.service.DemoStateChangeBroadcaster;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/demo/state/stream")
public class DemoStateStreamController {

    private final DemoStateChangeBroadcaster stateChangeBroadcaster;

    public DemoStateStreamController(DemoStateChangeBroadcaster stateChangeBroadcaster) {
        this.stateChangeBroadcaster = stateChangeBroadcaster;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamStateChanges() {
        return stateChangeBroadcaster.subscribe();
    }
}
