package com.survila.subscriptiondemo.client;

import com.survila.subscriptiondemo.dto.mock.MockChangePlanRequest;
import com.survila.subscriptiondemo.dto.mock.MockCreatePreapprovalRequest;
import com.survila.subscriptiondemo.dto.mock.MockPayWithCardRequest;
import com.survila.subscriptiondemo.dto.mock.MockPayWithCardResponse;
import com.survila.subscriptiondemo.dto.mock.MockPaymentResponse;
import com.survila.subscriptiondemo.dto.mock.MockPreapprovalResponse;
import com.survila.subscriptiondemo.dto.mock.MockSimulateRecurringChargeRequest;
import com.survila.subscriptiondemo.dto.mock.MockSubscriptionActionRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MockPaymentClient {

    private final RestClient restClient;

    public MockPaymentClient(
            @Value("${demo-app.mock-payment.base-url:http://localhost:9090}") String baseUrl
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(3000);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public MockPreapprovalResponse createPreapproval(MockCreatePreapprovalRequest request) {
        return restClient.post()
                .uri("/preapproval")
                .body(request)
                .retrieve()
                .body(MockPreapprovalResponse.class);
    }

    public MockPreapprovalResponse getPreapproval(String id) {
        return restClient.get()
                .uri("/preapproval/{id}", id)
                .retrieve()
                .body(MockPreapprovalResponse.class);
    }

    public MockPreapprovalResponse getPreapprovalByExternalReference(String externalReference) {
        return restClient.get()
                .uri(
                        uriBuilder -> uriBuilder
                                .path("/preapproval/by-external-reference/{externalReference}")
                                .build(externalReference)
                )
                .retrieve()
                .body(MockPreapprovalResponse.class);
    }

    public MockPaymentResponse getPayment(String id) {
        return restClient.get()
                .uri("/payment/{id}", id)
                .retrieve()
                .body(MockPaymentResponse.class);
    }

    public MockPayWithCardResponse payWithCard(String preapprovalId, MockPayWithCardRequest request) {
        return restClient.post()
                .uri("/mock/preapproval/{id}/pay-with-card", preapprovalId)
                .body(request)
                .retrieve()
                .body(MockPayWithCardResponse.class);
    }

    public MockPayWithCardResponse simulateRecurringCharge(
            String preapprovalId,
            MockSimulateRecurringChargeRequest request
    ) {
        return restClient.post()
                .uri("/mock/preapproval/{id}/simulate-recurring-charge", preapprovalId)
                .body(request)
                .retrieve()
                .body(MockPayWithCardResponse.class);
    }

    public void changePlan(String preapprovalId, MockChangePlanRequest request) {
        restClient.post()
                .uri("/mock/preapproval/{id}/change-plan", preapprovalId)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public void cancelPreapproval(String preapprovalId, MockSubscriptionActionRequest request) {
        restClient.post()
                .uri("/mock/preapproval/{id}/cancel", preapprovalId)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
