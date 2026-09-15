package com.lak.moviebooking.notification.application;

import java.util.Map;
import java.util.regex.Pattern;

public record EmailOutboxPayload(String recipient, EmailTemplate template, Map<String, String> variables) {

	private static final Pattern EMAIL_ADDRESS = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

	public EmailOutboxPayload {
		recipient = recipient == null ? null : recipient.trim();
		if (recipient == null || recipient.length() > 320 || !EMAIL_ADDRESS.matcher(recipient).matches()
				|| template == null || variables == null) {
			throw new IllegalArgumentException("Email outbox payload is invalid");
		}
		variables = Map.copyOf(variables);
	}
}
