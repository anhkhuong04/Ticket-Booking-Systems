package com.lak.moviebooking.payment.application;

import java.time.Instant;
import java.util.UUID;

public interface PaymentManagement {

    PaymentView create(UUID userId, PaymentCreateCommand command);

    PaymentView find(UUID userId, UUID paymentId);

    void receiveWebhook(String provider, String rawPayload, String signature);

    int expireDuePayments(Instant now);

    int reconcilePendingPayments(Instant now);
}
