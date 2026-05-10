package com.survila.subscriptiondemo.client;

import com.survila.subscriptiondemo.dto.mock.MockCreatePreapprovalRequest;
import com.survila.subscriptiondemo.dto.mock.MockPayWithCardRequest;
import com.survila.subscriptiondemo.dto.mock.MockPayWithCardResponse;
import com.survila.subscriptiondemo.dto.mock.MockPaymentResponse;
import com.survila.subscriptiondemo.dto.mock.MockPreapprovalResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MockPaymentClient {

    private final RestClient restClient;

    public MockPaymentClient(
            @Value("${demo-app.mock-payment.base-url:http://localhost:9090}") String baseUrl
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
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
}
