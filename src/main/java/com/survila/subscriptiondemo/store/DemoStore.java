package com.survila.subscriptiondemo.store;

import com.survila.subscriptiondemo.model.DemoEvent;
import com.survila.subscriptiondemo.model.DemoPayment;
import com.survila.subscriptiondemo.model.DemoSubscription;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class DemoStore {

    private static final String STORAGE_TYPE_MEMORY = "memory";
    private static final String STORAGE_TYPE_FILE = "file";

    private final ConcurrentHashMap<String, DemoSubscription> subscriptions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DemoPayment> payments = new ConcurrentHashMap<>();
    private final List<DemoEvent> events = new ArrayList<>();

    private final AtomicLong subscriptionSequence = new AtomicLong(1);
    private final AtomicLong paymentSequence = new AtomicLong(1);
    private final AtomicLong eventSequence = new AtomicLong(1);

    private final String storageType;
    private final Path stateFilePath;

    public DemoStore(
            @Value("${demo-app.storage.type:memory}") String storageType,
            @Value("${demo-app.storage.file-path:./data/subscription-demo-state.bin}") String stateFilePath
    ) {
        this.storageType = normalizeStorageType(storageType);
        this.stateFilePath = Path.of(stateFilePath);

        loadFromFileIfNeeded();
    }

    public String nextSubscriptionId() {
        return "demo-subscription-" + subscriptionSequence.getAndIncrement();
    }

    public String nextPaymentId() {
        return "demo-payment-" + paymentSequence.getAndIncrement();
    }

    public String nextEventId() {
        return "demo-event-" + eventSequence.getAndIncrement();
    }

    public synchronized DemoSubscription saveSubscription(DemoSubscription subscription) {
        subscriptions.put(subscription.getId(), subscription);
        persistIfNeeded();
        return subscription;
    }

    public Optional<DemoSubscription> findSubscriptionById(String id) {
        return Optional.ofNullable(subscriptions.get(id));
    }

    public Optional<DemoSubscription> findSubscriptionByProviderSubscriptionId(String providerSubscriptionId) {
        return subscriptions.values()
                .stream()
                .filter(subscription -> providerSubscriptionId.equals(subscription.getProviderSubscriptionId()))
                .findFirst();
    }

    public List<DemoSubscription> findAllSubscriptions() {
        return subscriptions.values()
                .stream()
                .sorted(Comparator.comparing(DemoSubscription::getCreatedAt))
                .toList();
    }

    public synchronized DemoPayment savePayment(DemoPayment payment) {
        payments.put(payment.getId(), payment);
        persistIfNeeded();
        return payment;
    }

    public Optional<DemoPayment> findPaymentByProviderPaymentId(String providerPaymentId) {
        return payments.values()
                .stream()
                .filter(payment -> providerPaymentId.equals(payment.getProviderPaymentId()))
                .findFirst();
    }

    public List<DemoPayment> findAllPayments() {
        return payments.values()
                .stream()
                .sorted(Comparator.comparing(DemoPayment::getCreatedAt))
                .toList();
    }

    public synchronized DemoEvent addEvent(String type, String message) {
        DemoEvent event = new DemoEvent(
                nextEventId(),
                type,
                message,
                java.time.Instant.now()
        );

        events.add(event);
        persistIfNeeded();

        return event;
    }

    public synchronized List<DemoEvent> findAllEvents() {
        return List.copyOf(events);
    }

    public int countSubscriptions() {
        return subscriptions.size();
    }

    public int countPayments() {
        return payments.size();
    }

    public synchronized int countEvents() {
        return events.size();
    }

    public synchronized void clearAll() {
        subscriptions.clear();
        payments.clear();
        events.clear();

        subscriptionSequence.set(1);
        paymentSequence.set(1);
        eventSequence.set(1);

        persistIfNeeded();
    }

    public String getStorageType() {
        return storageType;
    }

    public String getStateFilePath() {
        return stateFilePath.toString();
    }

    private String normalizeStorageType(String value) {
        String normalized = value == null ? STORAGE_TYPE_MEMORY : value.trim().toLowerCase();

        if (!normalized.equals(STORAGE_TYPE_MEMORY) && !normalized.equals(STORAGE_TYPE_FILE)) {
            throw new IllegalArgumentException(
                    "Invalid demo-app.storage.type: %s. Supported values: memory, file.".formatted(value)
            );
        }

        return normalized;
    }

    private boolean isFileStorageEnabled() {
        return STORAGE_TYPE_FILE.equals(storageType);
    }

    private void loadFromFileIfNeeded() {
        if (!isFileStorageEnabled()) {
            return;
        }

        if (!Files.exists(stateFilePath)) {
            return;
        }

        try (
                FileInputStream fileInputStream = new FileInputStream(stateFilePath.toFile());
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)
        ) {
            Object object = objectInputStream.readObject();

            if (!(object instanceof DemoStoreSnapshot snapshot)) {
                throw new IllegalStateException("Invalid demo state file content.");
            }

            subscriptions.clear();
            payments.clear();
            events.clear();

            for (DemoSubscription subscription : snapshot.subscriptions()) {
                subscriptions.put(subscription.getId(), subscription);
            }

            for (DemoPayment payment : snapshot.payments()) {
                payments.put(payment.getId(), payment);
            }

            events.addAll(snapshot.events());

            subscriptionSequence.set(snapshot.nextSubscriptionSequence());
            paymentSequence.set(snapshot.nextPaymentSequence());
            eventSequence.set(snapshot.nextEventSequence());
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Could not load demo state file: %s. Delete the file if it is corrupted or incompatible."
                            .formatted(stateFilePath),
                    ex
            );
        }
    }

    private synchronized void persistIfNeeded() {
        if (!isFileStorageEnabled()) {
            return;
        }

        try {
            Path parent = stateFilePath.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            DemoStoreSnapshot snapshot = new DemoStoreSnapshot(
                    findAllSubscriptions(),
                    findAllPayments(),
                    findAllEvents(),
                    subscriptionSequence.get(),
                    paymentSequence.get(),
                    eventSequence.get()
            );

            try (
                    FileOutputStream fileOutputStream = new FileOutputStream(stateFilePath.toFile());
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)
            ) {
                objectOutputStream.writeObject(snapshot);
            }
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Could not persist demo state file: %s".formatted(stateFilePath),
                    ex
            );
        }
    }

    private record DemoStoreSnapshot(
            List<DemoSubscription> subscriptions,
            List<DemoPayment> payments,
            List<DemoEvent> events,
            long nextSubscriptionSequence,
            long nextPaymentSequence,
            long nextEventSequence
    ) implements Serializable {

        private static final long serialVersionUID = 1L;
    }
}