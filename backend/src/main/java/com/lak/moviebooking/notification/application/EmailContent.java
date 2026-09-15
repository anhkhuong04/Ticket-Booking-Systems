package com.lak.moviebooking.notification.application;

import java.util.Objects;

public record EmailContent(String subject, String htmlBody) {

	public EmailContent {
		if (subject == null || subject.isBlank() || htmlBody == null || htmlBody.isBlank()) {
			throw new IllegalArgumentException("Email content is incomplete");
		}
	}
}
