package com.survila.subscriptiondemo.controller;

import com.survila.subscriptiondemo.dto.webhook.MockPaymentWebhookPayload;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@ControllerAdvice
public class WebhookPayloadCaptureAdvice extends RequestBodyAdviceAdapter {

    private static final String PAYLOAD_ATTRIBUTE =
            WebhookPayloadCaptureAdvice.class.getName() + ".payload";

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return MockPaymentWebhookPayload.class.equals(targetType);
    }

    @Override
    public HttpInputMessage beforeBodyRead(
            HttpInputMessage inputMessage,
            MethodParameter parameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) throws IOException {
        byte[] body = inputMessage.getBody().readAllBytes();
        String payload = new String(body, charset(inputMessage.getHeaders()));

        RequestContextHolder.currentRequestAttributes()
                .setAttribute(PAYLOAD_ATTRIBUTE, payload, RequestAttributes.SCOPE_REQUEST);

        return new CachedBodyHttpInputMessage(inputMessage.getHeaders(), body);
    }

    public static String currentPayload() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return "";
        }

        Object payload = attributes.getAttribute(PAYLOAD_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

        return payload instanceof String value ? value : "";
    }

    private Charset charset(HttpHeaders headers) {
        MediaType contentType = headers.getContentType();

        if (contentType == null || contentType.getCharset() == null) {
            return StandardCharsets.UTF_8;
        }

        return contentType.getCharset();
    }

    private record CachedBodyHttpInputMessage(
            HttpHeaders headers,
            byte[] body
    ) implements HttpInputMessage {

        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }
    }
}
