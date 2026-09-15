package com.lak.moviebooking.payment.infrastructure;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.payment.application.PaymentProvider;
import com.lak.moviebooking.payment.application.PaymentProviderEvent;
import com.lak.moviebooking.payment.application.PaymentProviderRequest;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Local adapter with the same signature boundary as a real payment provider. */
@Component
class SandboxPaymentProvider implements PaymentProvider {

    private final PaymentProperties properties;
    private final ObjectMapper objectMapper;

    SandboxPaymentProvider(PaymentProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "sandbox";
    }

    @Override
    public String paymentUrl(PaymentProviderRequest request) {
        String separator = properties.sandbox().checkoutBaseUrl().contains("?") ? "&" : "?";
        return properties.sandbox().checkoutBaseUrl() + separator + "paymentId="
                + URLEncoder.encode(request.paymentId().toString(), StandardCharsets.UTF_8);
    }

    @Override
    public PaymentProviderEvent verifyWebhook(String rawPayload, String signature) {
        if (signature == null || !MessageDigest.isEqual(hex(hmac(rawPayload)), hex(signature))) {
            throw ApplicationException.forbidden("PAYMENT_SIGNATURE_INVALID", "Payment webhook signature is invalid");
        }
        try {
            SandboxWebhook payload = objectMapper.readValue(rawPayload, SandboxWebhook.class);
            if (payload.paymentId() == null || blank(payload.eventId()) || blank(payload.transactionId()) || payload.amount() <= 0
                    || blank(payload.currency()) || blank(payload.status()) || payload.paidAt() == null) {
                throw ApplicationException.businessRule("PAYMENT_WEBHOOK_INVALID", "Payment webhook payload is invalid");
            }
            return new PaymentProviderEvent(payload.paymentId(), payload.eventId(), payload.transactionId(), payload.amount(),
                    payload.currency(), payload.status(), payload.paidAt());
        }
        catch (JacksonException exception) {
            throw ApplicationException.businessRule("PAYMENT_WEBHOOK_INVALID", "Payment webhook payload is invalid");
        }
    }

    @Override
    public Optional<PaymentProviderEvent> reconcile(PaymentProviderRequest request) {
        return Optional.empty();
    }

    String sign(String rawPayload) {
        return hmac(rawPayload);
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.sandbox().webhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        }
        catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", exception);
        }
    }

    private byte[] hex(String value) {
        try { return HexFormat.of().parseHex(value); }
        catch (IllegalArgumentException exception) { return new byte[0]; }
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }

    private record SandboxWebhook(
            java.util.UUID paymentId, String eventId, String transactionId, long amount, String currency,
            String status, Instant paidAt) {
    }
}
