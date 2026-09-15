package com.lak.moviebooking.reservation.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.lak.moviebooking.booking.application.BookingCheckout;
import com.lak.moviebooking.booking.application.BookingCheckoutCommand;
import com.lak.moviebooking.booking.application.BookingView;
import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.payment.application.PaymentCreateCommand;
import com.lak.moviebooking.payment.application.PaymentManagement;
import com.lak.moviebooking.payment.application.PaymentView;
import com.lak.moviebooking.reservation.application.SeatHoldCommand;
import com.lak.moviebooking.reservation.application.SeatHoldManagement;
import com.lak.moviebooking.reservation.application.SeatHoldView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class PaymentWorkflowIT extends SeatHoldManagementIT {

    private static final String SANDBOX_SECRET = "local-development-secret-must-be-replaced";

    @Autowired private SeatHoldManagement seatHolds;
    @Autowired private BookingCheckout bookings;
    @Autowired private PaymentManagement payments;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void createsAServerPricedSandboxPaymentAndProcessesASignedWebhookExactlyOnce() {
        Fixture fixture = fixtureWithPrice();
        BookingView booking = booking(fixture, "payment-success");

        PaymentView created = payments.create(fixture.firstUserId(), new PaymentCreateCommand(booking.bookingCode(), "sandbox"));
        PaymentView replay = payments.create(fixture.firstUserId(), new PaymentCreateCommand(booking.bookingCode(), "sandbox"));
        assertThat(replay.id()).isEqualTo(created.id());
        assertThat(created.amount()).isEqualTo(90_000L);
        assertThat(created.currency()).isEqualTo("VND");
        assertThat(created.expiresAt()).isEqualTo(booking.hardDeadline());
        assertThat(created.paymentUrl()).contains(created.id().toString());

        String wrongAmount = payload(created.id(), "evt-wrong-" + UUID.randomUUID(), transaction(created.id()), 1, Instant.now());
        assertThatThrownBy(() -> payments.receiveWebhook("sandbox", wrongAmount, sign(wrongAmount)))
                .isInstanceOf(ApplicationException.class)
                .extracting(error -> ((ApplicationException) error).code()).isEqualTo("PAYMENT_WEBHOOK_MISMATCH");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM payment_events WHERE payment_id=?", Integer.class, created.id())).isZero();

        String raw = payload(created.id(), "evt-paid-" + UUID.randomUUID(), transaction(created.id()), 90_000L, Instant.now());
        assertThatThrownBy(() -> payments.receiveWebhook("sandbox", raw, "not-a-valid-signature"))
                .isInstanceOf(ApplicationException.class)
                .extracting(error -> ((ApplicationException) error).code()).isEqualTo("PAYMENT_SIGNATURE_INVALID");
        payments.receiveWebhook("sandbox", raw, sign(raw));
        payments.receiveWebhook("sandbox", raw, sign(raw));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM payments WHERE id=?", String.class, created.id())).isEqualTo("SUCCESS");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM bookings WHERE id=?", String.class, booking.id())).isEqualTo("PAID");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM showtime_seats WHERE id=?", String.class, fixture.standardSeatId())).isEqualTo("SOLD");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM tickets WHERE booking_id=?", Integer.class, booking.id())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM payment_events WHERE payment_id=?", Integer.class, created.id())).isEqualTo(1);
    }

    @Test
    void expiresUnpaidBookingsIdempotentlyAndTreatsVerifiedLatePaymentAsReviewWithoutTicket() {
        Fixture fixture = fixtureWithPrice();
        BookingView booking = booking(fixture, "payment-late");
        PaymentView payment = payments.create(fixture.firstUserId(), new PaymentCreateCommand(booking.bookingCode(), "sandbox"));
        OffsetDateTime past = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1);
        jdbcTemplate.update("UPDATE bookings SET payment_deadline=?,hard_deadline=? WHERE id=?", past.minusMinutes(2), past, booking.id());
        jdbcTemplate.update("UPDATE payments SET expires_at=? WHERE id=?", past, payment.id());

        assertThat(payments.expireDuePayments(Instant.now())).isEqualTo(1);
        assertThat(payments.expireDuePayments(Instant.now())).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM bookings WHERE id=?", String.class, booking.id())).isEqualTo("EXPIRED");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM showtime_seats WHERE id=?", String.class, fixture.standardSeatId())).isEqualTo("AVAILABLE");

        String raw = payload(payment.id(), "evt-late-" + UUID.randomUUID(), transaction(payment.id()), 90_000L, Instant.now());
        payments.receiveWebhook("sandbox", raw, sign(raw));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM payments WHERE id=?", String.class, payment.id())).isEqualTo("SUCCESS");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM bookings WHERE id=?", String.class, booking.id())).isEqualTo("PAYMENT_REVIEW");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM tickets WHERE booking_id=?", Integer.class, booking.id())).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM outbox_events WHERE event_type='refund.late_payment_requested' AND aggregate_id=?", Integer.class,
                payment.id())).isEqualTo(1);
    }

    private BookingView booking(Fixture fixture, String key) {
        SeatHoldView hold = seatHolds.create(fixture.firstUserId(), new SeatHoldCommand(
                fixture.showtimeId(), List.of(fixture.standardSeatId()), "hold-" + key));
        return bookings.checkout(fixture.firstUserId(), new BookingCheckoutCommand(hold.id(), "checkout-" + key));
    }

    private Fixture fixtureWithPrice() {
        Fixture fixture = fixture();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update("""
                INSERT INTO showtime_prices (id,showtime_id,seat_type,price,source,created_at,updated_at)
                VALUES (?,?, 'STANDARD',90000,'SYSTEM_PROFILE',?,?)
                """, UUID.randomUUID(), fixture.showtimeId(), now, now);
        return fixture;
    }

    private String transaction(UUID paymentId) {
        return jdbcTemplate.queryForObject("SELECT provider_transaction_id FROM payments WHERE id=?", String.class, paymentId);
    }

    private String payload(UUID paymentId, String eventId, String transactionId, long amount, Instant paidAt) {
        return """
                {"paymentId":"%s","eventId":"%s","transactionId":"%s","amount":%d,"currency":"VND","status":"SUCCESS","paidAt":"%s"}
                """.formatted(paymentId, eventId, transactionId, amount, paidAt);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SANDBOX_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        }
        catch (java.security.GeneralSecurityException exception) {
            throw new AssertionError(exception);
        }
    }
}
