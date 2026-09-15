package com.lak.moviebooking.notification.application;

/** External email provider adapter. Providers must honor the outbox event ID as an idempotency key. */
public interface EmailDeliveryProvider {

	String providerName();

	void send(EmailDeliveryCommand command);
}
