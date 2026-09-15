package com.lak.moviebooking.common.observability;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.MDC;

/**
 * Scoped MDC fields approved for operational logs. Values are validated before
 * reaching a log pattern to avoid log injection.
 */
public final class LogContext {

	public static final String BOOKING_CODE = "booking_code";
	public static final String PAYMENT_ID = "payment_id";
	private static final Pattern SAFE_BOOKING_CODE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9-]{0,63}");

	private LogContext() {
	}

	public static MDC.MDCCloseable withBookingCode(String bookingCode) {
		String value = Objects.requireNonNull(bookingCode, "bookingCode is required");
		if (!SAFE_BOOKING_CODE.matcher(value).matches()) {
			throw new IllegalArgumentException("bookingCode contains unsafe characters");
		}
		return MDC.putCloseable(BOOKING_CODE, value);
	}

	public static MDC.MDCCloseable withPaymentId(UUID paymentId) {
		return MDC.putCloseable(PAYMENT_ID, Objects.requireNonNull(paymentId, "paymentId is required").toString());
	}
}
