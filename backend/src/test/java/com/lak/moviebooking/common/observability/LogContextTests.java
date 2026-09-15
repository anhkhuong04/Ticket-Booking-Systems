package com.lak.moviebooking.common.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class LogContextTests {

	@AfterEach
	void clearMdc() {
		MDC.clear();
	}

	@Test
	void scopesBookingAndPaymentIdentifiersWithoutLeakingThemBeyondTheOperation() {
		UUID paymentId = UUID.randomUUID();

		try (MDC.MDCCloseable bookingScope = LogContext.withBookingCode("LAK-20260915-001");
				MDC.MDCCloseable paymentScope = LogContext.withPaymentId(paymentId)) {
			assertThat(MDC.get(LogContext.BOOKING_CODE)).isEqualTo("LAK-20260915-001");
			assertThat(MDC.get(LogContext.PAYMENT_ID)).isEqualTo(paymentId.toString());
		}

		assertThat(MDC.get(LogContext.BOOKING_CODE)).isNull();
		assertThat(MDC.get(LogContext.PAYMENT_ID)).isNull();
	}

	@Test
	void rejectsUnsafeBookingCodeBeforeItCanReachTheLog() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> LogContext.withBookingCode("LAK-001\npassword=secret"));
	}
}
