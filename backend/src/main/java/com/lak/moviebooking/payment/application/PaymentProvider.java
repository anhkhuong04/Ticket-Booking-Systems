package com.lak.moviebooking.payment.application;

import java.util.Optional;

public interface PaymentProvider {

    String name();

    String paymentUrl(PaymentProviderRequest request);

    PaymentProviderEvent verifyWebhook(String rawPayload, String signature);

    Optional<PaymentProviderEvent> reconcile(PaymentProviderRequest request);
}
