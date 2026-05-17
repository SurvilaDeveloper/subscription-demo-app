package com.survila.subscriptiondemo.service;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class DemoStateChangeBroadcaster {

    private static final long SSE_TIMEOUT_MS = 60L * 60L * 1000L;

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final AtomicLong eventSequence = new AtomicLong(1);

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onError(ignored -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));

        emitters.add(emitter);
        send(emitter, "connected", "connected");

        return emitter;
    }

    public void publish(String reason) {
        for (SseEmitter emitter : emitters) {
            send(emitter, "state-changed", reason);
        }
    }

    private void send(SseEmitter emitter, String eventName, String reason) {
        try {
            emitter.send(
                    SseEmitter.event()
                            .id(String.valueOf(eventSequence.getAndIncrement()))
                            .name(eventName)
                            .data("%s:%s".formatted(reason, Instant.now()))
            );
        } catch (IOException | IllegalStateException ex) {
            emitters.remove(emitter);
            emitter.completeWithError(ex);
        }
    }
}
