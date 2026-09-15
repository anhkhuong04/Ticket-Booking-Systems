package com.lak.moviebooking.notification.application;

import org.springframework.stereotype.Service;

@Service
public class EmailTemplateRenderer {

	public EmailContent render(EmailOutboxPayload payload) {
		return switch (payload.template()) {
			case BOOKING_CONFIRMATION -> bookingConfirmation(payload);
			case TICKET_ISSUED -> ticketIssued(payload);
			case PASSWORD_RESET -> passwordReset(payload);
		};
	}

	private EmailContent ticketIssued(EmailOutboxPayload payload) {
		String ticketCode = requiredVariable(payload, "ticketCode");
		String bookingCode = requiredVariable(payload, "bookingCode");
		String movieTitle = requiredVariable(payload, "movieTitle");
		String cinema = requiredVariable(payload, "cinema");
		String auditorium = requiredVariable(payload, "auditorium");
		String showtime = requiredVariable(payload, "showtime");
		String seats = requiredVariable(payload, "seats");
		return new EmailContent(
				"Vé điện tử LAK " + ticketCode,
				"""
					<html><body>
					<p>Vé điện tử của bạn đã sẵn sàng.</p>
					<p>Mã vé: <strong>%s</strong></p>
					<p>Mã đặt vé: %s</p>
					<p>Phim: %s</p>
					<p>Rạp: %s · %s</p>
					<p>Suất chiếu: %s</p>
					<p>Ghế: %s</p>
					<p>Vui lòng mở Vé của tôi trong ứng dụng để hiển thị mã QR tại rạp.</p>
					</body></html>
					""".formatted(
						escapeHtml(ticketCode),
						escapeHtml(bookingCode),
						escapeHtml(movieTitle),
						escapeHtml(cinema),
						escapeHtml(auditorium),
						escapeHtml(showtime),
						escapeHtml(seats)));
	}

	private EmailContent passwordReset(EmailOutboxPayload payload) {
		String resetUrl = requiredVariable(payload, "resetUrl");
		if (!resetUrl.startsWith("https://") && !resetUrl.startsWith("http://localhost:")) {
			throw new IllegalArgumentException("Password reset URL is invalid");
		}
		return new EmailContent(
				"Đặt lại mật khẩu LAK",
				"""
					<html><body>
					<p>Bạn đã yêu cầu đặt lại mật khẩu.</p>
					<p><a href="%s">Đặt lại mật khẩu</a></p>
					<p>Nếu bạn không yêu cầu, bạn có thể bỏ qua email này.</p>
					</body></html>
					""".formatted(escapeHtml(resetUrl)));
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
