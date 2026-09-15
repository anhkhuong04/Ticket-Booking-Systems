package com.lak.moviebooking.ticketing.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.lak.moviebooking.ticketing.application.TicketQrPayloadFactory;
import org.springframework.stereotype.Component;

@Component
class HmacTicketQrPayloadFactory implements TicketQrPayloadFactory {

    private final byte[] signingSecret;

    HmacTicketQrPayloadFactory(TicketingProperties properties) {
        this.signingSecret = properties.qrSigningSecret().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String create(String ticketCode) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingSecret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(ticketCode.getBytes(StandardCharsets.UTF_8)));
        }
        catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", exception);
        }
    }
}
