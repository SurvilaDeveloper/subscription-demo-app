package com.survila.subscriptiondemo.util;

import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FriendlyErrorMessages {

    private static final Pattern DETAILS_PATTERN = Pattern.compile(
            "\"details\"\\s*:\\s*\\[\\s*\"((?:\\\\.|[^\"])*)\"",
            Pattern.DOTALL
    );
    private static final Pattern MESSAGE_PATTERN = Pattern.compile(
            "\"message\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"",
            Pattern.DOTALL
    );

    private FriendlyErrorMessages() {
    }

    public static String providerCreationFailure(Throwable ex) {
        String detail = fromThrowable(ex);

        if (isBlank(detail)) {
            return "La suscripción quedó pendiente de reconciliación.";
        }

        return detail;
    }

    public static String webhookProcessingFailure(Throwable ex) {
        String detail = fromThrowable(ex);

        if (isBlank(detail)) {
            return "StreamBox recibió el webhook, pero no pudo procesarlo.";
        }

        return detail;
    }

    public static String apiDetail(Throwable ex) {
        String detail = fromThrowable(ex);

        if (isBlank(detail)) {
            return "No se pudo completar la operación. Revisá el estado de StreamBox y Mock Payment Service.";
        }

        return detail;
    }

    private static String fromThrowable(Throwable ex) {
        if (ex == null) {
            return "";
        }

        if (ex instanceof RestClientResponseException responseException) {
            Optional<String> responseDetail = firstDetailFromJson(responseException.getResponseBodyAsString());

            if (responseDetail.isPresent()) {
                return humanize(responseDetail.get());
            }
        }

        return humanize(ex.getMessage());
    }

    private static String humanize(String rawMessage) {
        if (isBlank(rawMessage)) {
            return "";
        }

        String message = stripJavaPrefix(rawMessage.trim());
        Optional<String> embeddedDetail = firstDetailFromEmbeddedJson(message);

        if (embeddedDetail.isPresent()) {
            return humanize(embeddedDetail.get());
        }

        String normalized = message.toLowerCase();

        if (normalized.contains("internal subscription not found for provider subscription")
                || normalized.contains("streambox recibió un webhook")) {
            String providerId = valueAfterColon(message).orElse("esa suscripción del proveedor");
            if (normalized.contains("streambox recibió un webhook")) {
                return message;
            }
            return "StreamBox recibió un webhook para " + providerId
                    + ", pero no encontró una suscripción interna vinculada. Puede pasar si esa suscripción se creó directamente en el mock o si StreamBox fue reseteado.";
        }

        if (normalized.contains("internal subscription") && normalized.contains("does not have provider subscription id")) {
            return "La suscripción todavía no está vinculada con Mock Payment Service. Primero reconciliá la creación.";
        }

        if (normalized.contains("does not have provider external reference")) {
            return "La suscripción no tiene referencia externa para reconciliar con Mock Payment Service.";
        }

        if (normalized.contains("invalid webhook signature")
                || normalized.contains("la firma del webhook no es válida")) {
            return "La firma del webhook no es válida. Verificá que ambas aplicaciones usen el mismo secreto.";
        }

        if (normalized.contains("missing data.id in webhook payload")
                || normalized.contains("no incluye data.id")) {
            return "El webhook no incluye el identificador del recurso afectado.";
        }

        if (normalized.contains("missing x-request-id header")
                || normalized.contains("no incluye el header x-request-id")) {
            return "El webhook no incluye el header x-request-id.";
        }

        if (normalized.contains("missing x-signature header")
                || normalized.contains("no incluye el header x-signature")) {
            return "El webhook no incluye el header de firma.";
        }

        if (normalized.contains("missing ts in x-signature header")
                || normalized.contains("missing v1 in x-signature header")
                || normalized.contains("invalid ts in x-signature header")
                || normalized.contains("x-signature no incluye ts")
                || normalized.contains("x-signature no incluye v1")
                || normalized.contains("ts del header x-signature no es válido")) {
            return "El header de firma del webhook tiene un formato inválido.";
        }

        if (normalized.contains("simulated response failure after creating preapproval")
                || normalized.contains("error while extracting response for type")
                || normalized.contains("content type [application/octet-stream]")) {
            return "Mock Payment Service no devolvió una respuesta válida después de crear la suscripción. Si usaste la simulación de pérdida de respuesta, reconciliá la suscripción.";
        }

        if (normalized.contains("connection refused")
                || normalized.contains("i/o error")
                || normalized.contains("connect timed out")
                || normalized.contains("read timed out")) {
            return "No se pudo conectar con Mock Payment Service. Verificá que esté levantado y que el puerto configurado sea correcto.";
        }

        if (normalized.contains("preapproval not found")) {
            return "Mock Payment Service no encontró la suscripción solicitada.";
        }

        if (normalized.contains("plan not found")) {
            return "No se encontró el plan seleccionado.";
        }

        if (message.startsWith("{") || message.startsWith("<") || normalized.contains("java.")) {
            return "No se pudo completar la operación. Revisá el estado de ambas aplicaciones.";
        }

        return message;
    }

    private static Optional<String> firstDetailFromEmbeddedJson(String message) {
        int start = message.indexOf('{');
        int end = message.lastIndexOf('}');

        if (start < 0 || end <= start) {
            return Optional.empty();
        }

        String candidate = message.substring(start, end + 1);
        Optional<String> detail = firstDetailFromJson(candidate);

        if (detail.isPresent()) {
            return detail;
        }

        return firstDetailFromJson(candidate.replace("\\\"", "\""));
    }

    private static Optional<String> firstDetailFromJson(String json) {
        if (isBlank(json)) {
            return Optional.empty();
        }

        Matcher detailsMatcher = DETAILS_PATTERN.matcher(json);

        if (detailsMatcher.find()) {
            return Optional.of(unescapeJson(detailsMatcher.group(1)));
        }

        Matcher messageMatcher = MESSAGE_PATTERN.matcher(json);

        if (messageMatcher.find()) {
            return Optional.of(unescapeJson(messageMatcher.group(1)));
        }

        return Optional.empty();
    }

    private static String unescapeJson(String value) {
        return value
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\");
    }

    private static Optional<String> valueAfterColon(String message) {
        int index = message.lastIndexOf(':');

        if (index < 0 || index + 1 >= message.length()) {
            return Optional.empty();
        }

        return Optional.of(message.substring(index + 1).trim());
    }

    private static String stripJavaPrefix(String message) {
        return message.replaceFirst("^(?:[a-zA-Z_$][\\w$]*\\.)+[A-Za-z_$][\\w$]*(?:Exception|Error)?:\\s*", "");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
