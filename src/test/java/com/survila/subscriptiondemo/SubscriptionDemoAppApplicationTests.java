package com.survila.subscriptiondemo;

import com.survila.subscriptiondemo.model.DemoReceivedWebhook;
import com.survila.subscriptiondemo.model.SubscriptionStatus;
import com.survila.subscriptiondemo.store.DemoStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "demo-app.mock-payment.base-url=http://127.0.0.1:1")
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class SubscriptionDemoAppApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DemoStore store;

    @BeforeEach
    void setUp() {
        store.clearAll();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void receivedWebhookKeepsRawPayload() throws Exception {
        String payload = "{\"id\":\"evt-test\",\"live_mode\":false,\"type\":\"payment\","
                + "\"date_created\":\"2026-05-16T22:12:00Z\",\"user_id\":\"demo-user\","
                + "\"api_version\":\"v1\",\"action\":\"payment.created\","
                + "\"data\":{\"id\":\"payment-test\"}}";

        mockMvc.perform(post("/api/webhooks/mock-payment")
                        .contentType(APPLICATION_JSON)
                .header("x-request-id", "request-test")
                .header("x-signature", "ts=1,v1=invalid")
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", containsString("La firma del webhook no es válida")));

        List<DemoReceivedWebhook> webhooks = store.findAllReceivedWebhooks();

        assertThat(webhooks).hasSize(1);
        assertThat(webhooks.get(0).getPayload()).isEqualTo(payload);
        assertThat(webhooks.get(0).getError()).contains("La firma del webhook no es válida");
    }

    @Test
    void failedProviderPreapprovalMarksSubscriptionForReconciliation() throws Exception {
        String request = "{\"plan_id\":\"basic\",\"payer_email\":\"cliente@test.com\"}";

        mockMvc.perform(post("/api/subscriptions")
                        .contentType(APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isInternalServerError());

        var subscriptions = store.findAllSubscriptions();

        assertThat(subscriptions).hasSize(1);
        assertThat(subscriptions.get(0).getProviderExternalReference()).isEqualTo("demo_subscription_id=demo-subscription-1");
        assertThat(subscriptions.get(0).getProviderSubscriptionId()).isNull();
        assertThat(subscriptions.get(0).getStatus()).isEqualTo(SubscriptionStatus.RECONCILIATION_NEEDED);
        assertThat(store.findAllEvents())
                .anySatisfy(event -> assertThat(event.getType())
                        .isEqualTo("PROVIDER_PREAPPROVAL_RECONCILIATION_NEEDED"));
        assertThat(store.findAllEvents())
                .filteredOn(event -> event.getType().equals("PROVIDER_PREAPPROVAL_RECONCILIATION_NEEDED"))
                .allSatisfy(event -> {
                    assertThat(event.getMessage()).contains("No se pudo confirmar en Mock Payment Service");
                    assertThat(event.getMessage()).doesNotContain("Connection refused");
                    assertThat(event.getMessage()).doesNotContain("Exception");
                });
    }

}
