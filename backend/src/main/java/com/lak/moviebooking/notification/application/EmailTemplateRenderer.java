package com.lak.moviebooking.notification.application;

import org.springframework.stereotype.Service;

@Service
public class EmailTemplateRenderer {

	public EmailContent render(EmailOutboxPayload payload) {
		return switch (payload.template()) {
			case BOOKING_CONFIRMATION -> bookingConfirmation(payload);
		};
	}

	private EmailContent bookingConfirmation(EmailOutboxPayload payload) {
		String bookingCode = requiredVariable(payload, "bookingCode");
		String movieTitle = requiredVariable(payload, "movieTitle");
		String showtime = requiredVariable(payload, "showtime");
		String seats = requiredVariable(payload, "seats");
		String safeBookingCode = escapeHtml(bookingCode);

		return new EmailContent(
				"Xác nhận đặt vé " + bookingCode,
				"""
					<html><body>
					<p>Mã đặt vé: <strong>%s</strong></p>
					<p>Phim: %s</p>
					<p>Suất chiếu: %s</p>
					<p>Ghế: %s</p>
					</body></html>
					""".formatted(
						safeBookingCode,
						escapeHtml(movieTitle),
						escapeHtml(showtime),
						escapeHtml(seats)));
	}

	private String requiredVariable(EmailOutboxPayload payload, String name) {
		String value = payload.variables().get(name);
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Email template data is incomplete");
		}
		if (value.contains("\r") || value.contains("\n")) {
			throw new IllegalArgumentException("Email template data is invalid");
		}
		return value;
	}

	private String escapeHtml(String value) {
		return value.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&#39;");
	}
}
