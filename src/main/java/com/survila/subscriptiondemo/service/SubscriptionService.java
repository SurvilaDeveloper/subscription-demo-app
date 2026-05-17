package com.survila.subscriptiondemo.service;

import com.survila.subscriptiondemo.client.MockPaymentClient;
import com.survila.subscriptiondemo.dto.ChangeSubscriptionPlanRequest;
import com.survila.subscriptiondemo.dto.SimulateRecurringChargeRequest;
import com.survila.subscriptiondemo.dto.StartSubscriptionRequest;
import com.survila.subscriptiondemo.dto.StartSubscriptionResponse;
import com.survila.subscriptiondemo.dto.SubscriptionActionResponse;
import com.survila.subscriptiondemo.dto.CreateSubscriptionRequest;
import com.survila.subscriptiondemo.dto.CreateSubscriptionResponse;
import com.survila.subscriptiondemo.dto.PaySubscriptionRequest;
import com.survila.subscriptiondemo.dto.mock.MockChangePlanRequest;
import com.survila.subscriptiondemo.dto.mock.MockCreatePreapprovalRequest;
import com.survila.subscriptiondemo.dto.mock.MockPayWithCardRequest;
import com.survila.subscriptiondemo.dto.mock.MockPayWithCardResponse;
import com.survila.subscriptiondemo.dto.mock.MockPaymentResponse;
import com.survila.subscriptiondemo.dto.mock.MockPreapprovalResponse;
import com.survila.subscriptiondemo.dto.mock.MockSimulateRecurringChargeRequest;
import com.survila.subscriptiondemo.dto.mock.MockSubscriptionActionRequest;
import com.survila.subscriptiondemo.dto.webhook.MockPaymentWebhookPayload;
import com.survila.subscriptiondemo.exception.BadRequestException;
import com.survila.subscriptiondemo.model.DemoPayment;
import com.survila.subscriptiondemo.model.DemoSubscription;
import com.survila.subscriptiondemo.model.PaymentStatus;
import com.survila.subscriptiondemo.model.Plan;
import com.survila.subscriptiondemo.model.SubscriptionStatus;
import com.survila.subscriptiondemo.store.DemoStore;
import com.survila.subscriptiondemo.util.FriendlyErrorMessages;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;

@Service
public class SubscriptionService {

    private final DemoStore store;
    private final PlanCatalogService planCatalogService;
    private final MockPaymentClient mockPaymentClient;
    private final String publicBaseUrl;
    private final String webhookBaseUrl;

    public SubscriptionService(
            DemoStore store,
            PlanCatalogService planCatalogService,
            MockPaymentClient mockPaymentClient,
            @Value("${demo-app.public-base-url:http://localhost:8080}") String publicBaseUrl,
            @Value("${demo-app.webhook-base-url:http://localhost:8080}") String webhookBaseUrl
    ) {
        this.store = store;
        this.planCatalogService = planCatalogService;
        this.mockPaymentClient = mockPaymentClient;
        this.publicBaseUrl = publicBaseUrl;
        this.webhookBaseUrl = webhookBaseUrl;
    }

    public StartSubscriptionResponse startSubscription(StartSubscriptionRequest request) {
        Plan plan = planCatalogService.findById(request.planId())
                .orElseThrow(() -> new BadRequestException("No se encontró el plan " + request.planId() + "."));

        Instant now = Instant.now();

        DemoSubscription subscription = new DemoSubscription(
                store.nextSubscriptionId(),
                plan.id(),
                plan.name(),
                plan.amount(),
                plan.currency(),
                request.payerEmail(),
                null,
                SubscriptionStatus.CREATING,
                now,
                now
        );

        subscription.setProviderExternalReference(buildProviderExternalReference(subscription));
        store.saveSubscription(subscription);
        store.addEvent(
                "SUBSCRIPTION_CREATED",
                "Se creó la suscripción interna %s para el plan %s con estado %s."
                        .formatted(subscription.getId(), plan.name(), subscription.getStatus())
        );

        MockPreapprovalResponse preapproval = createProviderPreapprovalOrMarkFailed(subscription);

        subscription.setProviderSubscriptionId(preapproval.id());
        subscription.setStatus(mapPreapprovalStatus(preapproval.status()));
        store.saveSubscription(subscription);

        store.addEvent(
                "PROVIDER_PREAPPROVAL_CREATED",
                "Mock Payment Service creó la preapproval %s para la suscripción interna %s."
                        .formatted(preapproval.id(), subscription.getId())
        );

        MockPayWithCardResponse payResponse = mockPaymentClient.payWithCard(
                preapproval.id(),
                buildPayWithCardRequest(request.cardNumber())
        );

        subscription.setStatus(mapPreapprovalStatus(payResponse.preapprovalStatus()));
        store.saveSubscription(subscription);

        DemoPayment payment = savePaymentIfMissing(
                subscription.getId(),
                payResponse.payment()
        );

        store.addEvent(
                "PAYMENT_RESPONSE_RECEIVED",
                "StreamBox recibió la respuesta directa del pago. El pago %s del proveedor quedó con estado %s."
                        .formatted(payResponse.payment().id(), payResponse.payment().status())
        );

        store.addEvent(
                "SUBSCRIPTION_STATUS_UPDATED",
                "La suscripción interna %s cambió a %s según el estado %s informado por el proveedor."
                        .formatted(subscription.getId(), subscription.getStatus(), payResponse.preapprovalStatus())
        );

        return new StartSubscriptionResponse(
                subscription,
                payment,
                preapproval.id(),
                payResponse.payment().id(),
                payResponse.preapprovalStatus(),
                payResponse.payment().status()
        );
    }

    public SubscriptionActionResponse simulateRecurringCharge(
            String subscriptionId,
            SimulateRecurringChargeRequest request
    ) {
        DemoSubscription subscription = getSubscriptionOrThrow(subscriptionId);

        ensureProviderSubscription(subscription);

        MockPayWithCardResponse response = mockPaymentClient.simulateRecurringCharge(
                subscription.getProviderSubscriptionId(),
                new MockSimulateRecurringChargeRequest(
                        request.cardNumber(),
                        true
                )
        );

        subscription.setStatus(mapPreapprovalStatus(response.preapprovalStatus()));
        store.saveSubscription(subscription);

        DemoPayment payment = savePaymentIfMissing(
                subscription.getId(),
                response.payment()
        );

        store.addEvent(
                "RECURRING_CHARGE_SIMULATED",
                "Se simuló un cobro recurrente para la suscripción interna %s. El pago %s del proveedor quedó con estado %s."
                        .formatted(subscription.getId(), response.payment().id(), response.payment().status())
        );

        store.addEvent(
                "SUBSCRIPTION_STATUS_UPDATED",
                "La suscripción interna %s cambió a %s después del cobro recurrente."
                        .formatted(subscription.getId(), subscription.getStatus())
        );

        return new SubscriptionActionResponse(
                subscription,
                payment,
                subscription.getProviderSubscriptionId(),
                response.payment().id(),
                response.preapprovalStatus(),
                response.payment().status()
        );
    }

    public SubscriptionActionResponse changePlan(
            String subscriptionId,
            ChangeSubscriptionPlanRequest request
    ) {
        DemoSubscription subscription = getSubscriptionOrThrow(subscriptionId);

        ensureProviderSubscription(subscription);

        Plan newPlan = planCatalogService.findById(request.planId())
                .orElseThrow(() -> new BadRequestException("No se encontró el plan " + request.planId() + "."));

        mockPaymentClient.changePlan(
                subscription.getProviderSubscriptionId(),
                new MockChangePlanRequest(
                        newPlan.name(),
                        new MockCreatePreapprovalRequest.AutoRecurringRequest(
                                1,
                                "months",
                                newPlan.amount(),
                                newPlan.currency()
                        ),
                        true
                )
        );

        MockPreapprovalResponse providerPreapproval = mockPaymentClient.getPreapproval(
                subscription.getProviderSubscriptionId()
        );

        subscription.replacePlan(newPlan);
        subscription.setStatus(mapPreapprovalStatus(providerPreapproval.status()));
        store.saveSubscription(subscription);

        store.addEvent(
                "PLAN_CHANGED",
                "La suscripción interna %s cambió al plan %s."
                        .formatted(subscription.getId(), newPlan.name())
        );

        store.addEvent(
                "SUBSCRIPTION_STATUS_UPDATED",
                "La suscripción interna %s cambió a %s después del cambio de plan."
                        .formatted(subscription.getId(), subscription.getStatus())
        );

        return new SubscriptionActionResponse(
                subscription,
                null,
                subscription.getProviderSubscriptionId(),
                null,
                providerPreapproval.status(),
                null
        );
    }

    public SubscriptionActionResponse cancelSubscription(String subscriptionId) {
        DemoSubscription subscription = getSubscriptionOrThrow(subscriptionId);

        ensureProviderSubscription(subscription);

        mockPaymentClient.cancelPreapproval(
                subscription.getProviderSubscriptionId(),
                new MockSubscriptionActionRequest(true)
        );

        MockPreapprovalResponse providerPreapproval = mockPaymentClient.getPreapproval(
                subscription.getProviderSubscriptionId()
        );

        subscription.setStatus(mapPreapprovalStatus(providerPreapproval.status()));
        store.saveSubscription(subscription);

        store.addEvent(
                "SUBSCRIPTION_CANCELLED",
                "La suscripción interna %s fue cancelada desde StreamBox Demo."
                        .formatted(subscription.getId())
        );

        return new SubscriptionActionResponse(
                subscription,
                null,
                subscription.getProviderSubscriptionId(),
                null,
                providerPreapproval.status(),
                null
        );
    }

    public CreateSubscriptionResponse createSubscription(CreateSubscriptionRequest request) {
        Plan plan = planCatalogService.findById(request.planId())
                .orElseThrow(() -> new BadRequestException("No se encontró el plan " + request.planId() + "."));

        Instant now = Instant.now();

        DemoSubscription subscription = new DemoSubscription(
                store.nextSubscriptionId(),
                plan.id(),
                plan.name(),
                plan.amount(),
                plan.currency(),
                request.payerEmail(),
                null,
                SubscriptionStatus.CREATING,
                now,
                now
        );

        subscription.setProviderExternalReference(buildProviderExternalReference(subscription));
        store.saveSubscription(subscription);
        store.addEvent(
                "SUBSCRIPTION_CREATED",
                "Se creó la suscripción interna %s para el plan %s con estado %s."
                        .formatted(subscription.getId(), plan.name(), subscription.getStatus())
        );

        MockPreapprovalResponse preapproval = createProviderPreapprovalOrMarkFailed(subscription);

        subscription.setProviderSubscriptionId(preapproval.id());
        subscription.setStatus(mapPreapprovalStatus(preapproval.status()));
        store.saveSubscription(subscription);

        store.addEvent(
                "PROVIDER_PREAPPROVAL_CREATED",
                "Mock Payment Service creó la preapproval %s para la suscripción interna %s."
                        .formatted(preapproval.id(), subscription.getId())
        );

        store.addEvent(
                "SUBSCRIPTION_WAITING_FOR_PAYMENT",
                "La suscripción interna %s está esperando el pago inicial."
                        .formatted(subscription.getId())
        );

        return new CreateSubscriptionResponse(
                subscription,
                preapproval.id(),
                preapproval.status()
        );
    }

    public SubscriptionActionResponse paySubscription(
            String subscriptionId,
            PaySubscriptionRequest request
    ) {
        DemoSubscription subscription = getSubscriptionOrThrow(subscriptionId);

        ensureProviderSubscription(subscription);

        MockPayWithCardResponse payResponse = mockPaymentClient.payWithCard(
                subscription.getProviderSubscriptionId(),
                buildPayWithCardRequest(request.cardNumber())
        );

        subscription.setStatus(mapPreapprovalStatus(payResponse.preapprovalStatus()));
        store.saveSubscription(subscription);

        DemoPayment payment = savePaymentIfMissing(
                subscription.getId(),
                payResponse.payment()
        );

        store.addEvent(
                "PAYMENT_RESPONSE_RECEIVED",
                "StreamBox recibió la respuesta directa del pago. El pago %s del proveedor quedó con estado %s."
                        .formatted(payResponse.payment().id(), payResponse.payment().status())
        );

        store.addEvent(
                "SUBSCRIPTION_STATUS_UPDATED",
                "La suscripción interna %s cambió a %s según el estado %s informado por el proveedor."
                        .formatted(subscription.getId(), subscription.getStatus(), payResponse.preapprovalStatus())
        );

        return new SubscriptionActionResponse(
                subscription,
                payment,
                subscription.getProviderSubscriptionId(),
                payResponse.payment().id(),
                payResponse.preapprovalStatus(),
                payResponse.payment().status()
        );
    }

    public CreateSubscriptionResponse reconcileProviderSubscription(String subscriptionId) {
        DemoSubscription subscription = getSubscriptionOrThrow(subscriptionId);

        if (subscription.getProviderExternalReference() == null || subscription.getProviderExternalReference().isBlank()) {
            throw new BadRequestException(
                    "La suscripción interna %s no tiene referencia externa para reconciliar con Mock Payment Service."
                            .formatted(subscription.getId())
            );
        }

        try {
            MockPreapprovalResponse preapproval = subscription.getProviderSubscriptionId() == null
                    ? mockPaymentClient.getPreapprovalByExternalReference(subscription.getProviderExternalReference())
                    : mockPaymentClient.getPreapproval(subscription.getProviderSubscriptionId());

            subscription.setProviderSubscriptionId(preapproval.id());
            subscription.setStatus(mapPreapprovalStatus(preapproval.status()));
            store.saveSubscription(subscription);

            store.addEvent(
                    "PROVIDER_PREAPPROVAL_RECONCILED",
                    "La suscripción interna %s fue reconciliada con la preapproval %s del proveedor."
                            .formatted(subscription.getId(), preapproval.id())
            );

            return new CreateSubscriptionResponse(
                    subscription,
                    preapproval.id(),
                    preapproval.status()
            );
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                subscription.setStatus(SubscriptionStatus.CREATION_FAILED);
                store.saveSubscription(subscription);

                store.addEvent(
                        "PROVIDER_PREAPPROVAL_NOT_FOUND",
                        "Mock Payment Service no encontró una preapproval para la suscripción interna %s con referencia externa %s."
                                .formatted(subscription.getId(), subscription.getProviderExternalReference())
                );

                return new CreateSubscriptionResponse(
                        subscription,
                        null,
                        null
                );
            }

            markReconciliationNeeded(subscription, ex);
            throw ex;
        } catch (RuntimeException ex) {
            markReconciliationNeeded(subscription, ex);
            throw ex;
        }
    }

    public void processWebhook(MockPaymentWebhookPayload payload) {
        if (payload == null || payload.data() == null || payload.data().id() == null) {
            throw new BadRequestException("El payload del webhook no es válido.");
        }

        store.addEvent(
                "WEBHOOK_RECEIVED",
                "StreamBox recibió un webhook: type=%s, action=%s, data.id=%s."
                        .formatted(payload.type(), payload.action(), payload.data().id())
        );

        if ("payment".equals(payload.type()) && "payment.created".equals(payload.action())) {
            processPaymentCreatedWebhook(payload.data().id());
            return;
        }

        if ("preapproval".equals(payload.type())) {
            processPreapprovalWebhook(payload.action(), payload.data().id());
            return;
        }

        store.addEvent(
                "WEBHOOK_IGNORED",
                "StreamBox ignoró el webhook porque no soporta la combinación type/action %s/%s."
                        .formatted(payload.type(), payload.action())
        );
    }

    private void processPaymentCreatedWebhook(String providerPaymentId) {
        MockPaymentResponse providerPayment = mockPaymentClient.getPayment(providerPaymentId);

        DemoSubscription subscription = store.findSubscriptionByProviderSubscriptionId(providerPayment.preapprovalId())
                .orElseThrow(() -> new BadRequestException(
                        "StreamBox recibió un webhook para " + providerPayment.preapprovalId()
                                + ", pero no encontró una suscripción interna vinculada."
                ));

        DemoPayment payment = savePaymentIfMissing(subscription.getId(), providerPayment);

        MockPreapprovalResponse providerPreapproval = mockPaymentClient.getPreapproval(providerPayment.preapprovalId());

        subscription.setStatus(mapPreapprovalStatus(providerPreapproval.status()));
        store.saveSubscription(subscription);

        store.addEvent(
                "WEBHOOK_PAYMENT_PROCESSED",
                "Se procesó el webhook payment.created. El pago interno %s corresponde al pago %s del proveedor."
                        .formatted(payment.getId(), providerPayment.id())
        );

        store.addEvent(
                "SUBSCRIPTION_UPDATED_FROM_WEBHOOK",
                "La suscripción interna %s se actualizó a %s después de consultar la preapproval %s del proveedor."
                        .formatted(subscription.getId(), subscription.getStatus(), providerPreapproval.id())
        );
    }

    private void processPreapprovalWebhook(String action, String providerPreapprovalId) {
        MockPreapprovalResponse providerPreapproval = mockPaymentClient.getPreapproval(providerPreapprovalId);

        DemoSubscription subscription = store.findSubscriptionByProviderSubscriptionId(providerPreapproval.id())
                .orElseThrow(() -> new BadRequestException(
                        "StreamBox recibió un webhook para " + providerPreapproval.id()
                                + ", pero no encontró una suscripción interna vinculada."
                ));

        subscription.setStatus(mapPreapprovalStatus(providerPreapproval.status()));
        store.saveSubscription(subscription);

        store.addEvent(
                "WEBHOOK_PREAPPROVAL_PROCESSED",
                "Se procesó el webhook %s. La suscripción interna %s se actualizó a %s."
                        .formatted(action, subscription.getId(), subscription.getStatus())
        );
    }

    private DemoPayment savePaymentIfMissing(String subscriptionId, MockPaymentResponse providerPayment) {
        return store.findPaymentByProviderPaymentId(providerPayment.id())
                .orElseGet(() -> {
                    DemoPayment payment = new DemoPayment(
                            store.nextPaymentId(),
                            subscriptionId,
                            providerPayment.id(),
                            mapPaymentStatus(providerPayment.status()),
                            providerPayment.statusDetail(),
                            providerPayment.amount(),
                            providerPayment.currencyId(),
                            Instant.now()
                    );

                    store.savePayment(payment);

                    store.addEvent(
                            "PAYMENT_CREATED",
                            "Se creó el pago interno %s a partir del pago %s del proveedor con estado %s."
                                    .formatted(payment.getId(), payment.getProviderPaymentId(), payment.getStatus())
                    );

                    return payment;
                });
    }

    private MockCreatePreapprovalRequest buildCreatePreapprovalRequest(DemoSubscription subscription) {
        return new MockCreatePreapprovalRequest(
                subscription.getPlanName(),
                subscription.getProviderExternalReference(),
                subscription.getPayerEmail(),
                new MockCreatePreapprovalRequest.AutoRecurringRequest(
                        1,
                        "months",
                        subscription.getAmount(),
                        subscription.getCurrency()
                ),
                publicBaseUrl,
                webhookBaseUrl + "/api/webhooks/mock-payment"
        );
    }

    private String buildProviderExternalReference(DemoSubscription subscription) {
        return "demo_subscription_id=" + subscription.getId();
    }

    private MockPreapprovalResponse createProviderPreapprovalOrMarkFailed(DemoSubscription subscription) {
        try {
            return mockPaymentClient.createPreapproval(
                    buildCreatePreapprovalRequest(subscription)
            );
        } catch (RuntimeException ex) {
            markReconciliationNeeded(subscription, ex);
            throw ex;
        }
    }

    private void markReconciliationNeeded(DemoSubscription subscription, RuntimeException ex) {
        subscription.setStatus(SubscriptionStatus.RECONCILIATION_NEEDED);
        store.saveSubscription(subscription);

        store.addEvent(
                "PROVIDER_PREAPPROVAL_RECONCILIATION_NEEDED",
                "No se pudo confirmar en Mock Payment Service la creación de la suscripción interna %s. %s"
                        .formatted(subscription.getId(), FriendlyErrorMessages.providerCreationFailure(ex))
        );
    }

    private MockPayWithCardRequest buildPayWithCardRequest(String cardNumber) {
        return new MockPayWithCardRequest(
                cardNumber,
                "StreamBox User",
                12,
                2030,
                "123",
                true
        );
    }

    private DemoSubscription getSubscriptionOrThrow(String subscriptionId) {
        return store.findSubscriptionById(subscriptionId)
                .orElseThrow(() -> new BadRequestException("No se encontró la suscripción interna " + subscriptionId + "."));
    }

    private void ensureProviderSubscription(DemoSubscription subscription) {
        if (subscription.getProviderSubscriptionId() == null || subscription.getProviderSubscriptionId().isBlank()) {
            throw new BadRequestException(
                    "La suscripción interna %s todavía no está vinculada con Mock Payment Service. Estado actual: %s."
                            .formatted(subscription.getId(), subscription.getStatus())
            );
        }
    }

    private SubscriptionStatus mapPreapprovalStatus(String providerStatus) {
        if (providerStatus == null) {
            return SubscriptionStatus.PENDING;
        }

        return switch (providerStatus.toLowerCase()) {
            case "authorized" -> SubscriptionStatus.ACTIVE;
            case "payment_failed" -> SubscriptionStatus.PAYMENT_FAILED;
            case "paused" -> SubscriptionStatus.PAUSED;
            case "cancelled" -> SubscriptionStatus.CANCELLED;
            case "pending" -> SubscriptionStatus.PENDING;
            default -> SubscriptionStatus.PENDING;
        };
    }

    private PaymentStatus mapPaymentStatus(String providerStatus) {
        if (providerStatus == null) {
            return PaymentStatus.UNKNOWN;
        }

        return switch (providerStatus.toLowerCase()) {
            case "approved" -> PaymentStatus.APPROVED;
            case "rejected" -> PaymentStatus.REJECTED;
            case "in_process" -> PaymentStatus.IN_PROCESS;
            default -> PaymentStatus.UNKNOWN;
        };
    }
}
