package com.lak.moviebooking.refund.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.lak.moviebooking.booking.application.BookingCheckout;
import com.lak.moviebooking.booking.application.BookingCheckoutCommand;
import com.lak.moviebooking.booking.application.BookingView;
import com.lak.moviebooking.common.outbox.application.OutboxEvent;
import com.lak.moviebooking.payment.application.PaymentCreateCommand;
import com.lak.moviebooking.payment.application.PaymentManagement;
import com.lak.moviebooking.payment.application.PaymentView;
import com.lak.moviebooking.refund.application.RefundManagement;
import com.lak.moviebooking.refund.application.CustomerRefundCommand;
import com.lak.moviebooking.refund.application.RefundProvider;
import com.lak.moviebooking.refund.application.RefundProviderException;
import com.lak.moviebooking.refund.application.RefundProviderRequest;
import com.lak.moviebooking.refund.application.RefundProviderResult;
import com.lak.moviebooking.reservation.application.SeatHoldCommand;
import com.lak.moviebooking.reservation.application.SeatHoldManagement;
import com.lak.moviebooking.reservation.application.SeatHoldView;
import com.lak.moviebooking.reservation.infrastructure.SeatHoldManagementIT;
import com.lak.moviebooking.showtime.application.ShowtimeManagement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(RefundWorkflowIT.ProviderConfiguration.class)
class RefundWorkflowIT extends SeatHoldManagementIT {

    private static final String SANDBOX_SECRET = "local-development-secret-must-be-replaced";

    @Autowired private SeatHoldManagement seatHolds;
    @Autowired private BookingCheckout bookings;
    @Autowired private PaymentManagement payments;
    @Autowired private RefundManagement refunds;
    @Autowired private LatePaymentRefundOutboxHandler latePaymentHandler;
    @Autowired private ShowtimeCancellationOutboxHandler showtimeCancellationHandler;
    @Autowired private ShowtimeManagement showtimeManagement;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetProviders() {
        TimeoutProvider.invocations.set(0);
    }

    @Test
    void createsOneRefundFromDuplicateLatePaymentEventsAndNeverIssuesATicket() {
        LatePayment late = latePayment("sandbox", "duplicate-refund");
        String event = """
                {"bookingId":"%s","paymentId":"%s","amount":90000}
                """.formatted(late.booking().id(), late.payment().id());
        OutboxEvent first = new OutboxEvent(UUID.randomUUID(), "refund.late_payment_requested", "payment", late.payment().id(), event, 0, Instant.now());
        OutboxEvent duplicate = new OutboxEvent(UUID.randomUUID(), "refund.late_payment_requested", "payment", late.payment().id(), event, 0, Instant.now());

        latePaymentHandler.handle(first);
        latePaymentHandler.handle(duplicate);

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM refunds WHERE payment_id=?", Integer.class, late.payment().id())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM refunds WHERE payment_id=?", String.class, late.payment().id())).isEqualTo("REQUESTED");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM bookings WHERE id=?", String.class, late.booking().id())).isEqualTo("REFUND_PENDING");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM showtime_seats WHERE id=?", String.class, late.fixture().standardSeatId())).isEqualTo("AVAILABLE");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM tickets WHERE booking_id=?", Integer.class, late.booking().id())).isZero();

        assertThat(refunds.processRequestedRefunds(Instant.now())).isEqualTo(1);
        assertThat(refunds.processRequestedRefunds(Instant.now())).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM refunds WHERE payment_id=?", String.class, late.payment().id())).isEqualTo("REFUNDED");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM tickets WHERE booking_id=?", Integer.class, late.booking().id())).isZero();
    }

    @Test
    void retriesProviderTimeoutThenCompletesTheSameRefundIdempotently() {
        LatePayment late = latePayment("timeout", "timeout-refund");
        refunds.requestLatePaymentRefund(late.booking().id(), late.payment().id(), late.payment().amount());

        assertThat(refunds.processRequestedRefunds(Instant.now())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM refunds WHERE payment_id=?", String.class, late.payment().id())).isEqualTo("REQUESTED");
        assertThat(jdbcTemplate.queryForObject("SELECT attempt_count FROM refunds WHERE payment_id=?", Integer.class, late.payment().id())).isEqualTo(1);

        jdbcTemplate.update("UPDATE refunds SET next_attempt_at=? WHERE payment_id=?", OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1), late.payment().id());
        assertThat(refunds.processRequestedRefunds(Instant.now())).isEqualTo(1);
        assertThat(TimeoutProvider.invocations).hasValue(2);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM refunds WHERE payment_id=?", String.class, late.payment().id())).isEqualTo("REFUNDED");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM tickets WHERE booking_id=?", Integer.class, late.booking().id())).isZero();
    }

    @Test
    void sendsNonRetryableProviderFailuresToManualRefundQueue() {
        LatePayment late = latePayment("failed", "failed-refund");
        refunds.requestLatePaymentRefund(late.booking().id(), late.payment().id(), late.payment().amount());

        assertThat(refunds.processRequestedRefunds(Instant.now())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM refunds WHERE payment_id=?", String.class, late.payment().id())).isEqualTo("REFUND_FAILED");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM tickets WHERE booking_id=?", Integer.class, late.booking().id())).isZero();
    }

    @Test
    void cancelsShowtimeIdempotentlyRefundsPaidBookingsAndRestoresAnActiveVoucher() {
        Fixture fixture = fixtureWithPrice();
        String voucherCode = "CANCEL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        UUID voucherId = activeVoucher(voucherCode);
        BookingView booking = booking(fixture, "showtime-cancel", voucherCode);
        PaymentView payment = payments.create(fixture.firstUserId(), new PaymentCreateCommand(booking.bookingCode(), "sandbox"));
        String raw = payload(payment.id(), "evt-cancel-" + UUID.randomUUID(), transaction(payment.id()), payment.amount(), Instant.now());
        payments.receiveWebhook("sandbox", raw, sign(raw));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM tickets WHERE booking_id=?", String.class, booking.id())).isEqualTo("VALID");
        assertThat(showtimeManagement.cancelShowtime(UUID.randomUUID(), fixture.showtimeId()).status()).isEqualTo("CANCELLED");
        OutboxEvent cancellation = new OutboxEvent(UUID.randomUUID(), "refund.showtime_cancellation_requested", "showtime",
                fixture.showtimeId(), jdbcTemplate.queryForObject("SELECT payload FROM outbox_events WHERE event_type='refund.showtime_cancellation_requested' AND aggregate_id=?", String.class, fixture.showtimeId()), 0, Instant.now());

        showtimeCancellationHandler.handle(cancellation);
        showtimeCancellationHandler.handle(cancellation);

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM bookings WHERE id=?", String.class, booking.id())).isEqualTo("REFUND_PENDING");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM tickets WHERE booking_id=?", String.class, booking.id())).isEqualTo("CANCELLED");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM refunds WHERE booking_id=?", Integer.class, booking.id())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT reason FROM refunds WHERE booking_id=?", String.class, booking.id())).isEqualTo("SHOWTIME_CANCELLED");
        assertThat(jdbcTemplate.queryForObject("SELECT usage_count FROM vouchers WHERE id=?", Integer.class, voucherId)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT restored_at IS NOT NULL FROM voucher_redemptions WHERE booking_id=?", Boolean.class, booking.id())).isTrue();

        assertThat(refunds.processRequestedRefunds(Instant.now())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM bookings WHERE id=?", String.class, booking.id())).isEqualTo("REFUNDED");
    }

    @Test
    void customerRefundReplaysByIdempotencyKeyCancelsTicketAndReleasesSeats() {
        Fixture fixture = fixtureWithPrice();
        BookingView booking = booking(fixture, "customer-refund");
        PaymentView payment = payments.create(fixture.firstUserId(), new PaymentCreateCommand(booking.bookingCode(), "sandbox"));
        String raw = payload(payment.id(), "evt-customer-" + UUID.randomUUID(), transaction(payment.id()), payment.amount(), Instant.now());
        payments.receiveWebhook("sandbox", raw, sign(raw));

        CustomerRefundCommand request = new CustomerRefundCommand(fixture.firstUserId(), booking.id(), "customer-refund-key");
        var created = refunds.requestCustomerRefund(request);
        var replay = refunds.requestCustomerRefund(request);

        assertThat(replay.id()).isEqualTo(created.id());
        assertThat(created.amount()).isEqualTo(payment.amount());
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM tickets WHERE booking_id=?", String.class, booking.id())).isEqualTo("CANCELLED");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM showtime_seats WHERE id=?", String.class, fixture.standardSeatId())).isEqualTo("AVAILABLE");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM refunds WHERE booking_id=?", Integer.class, booking.id())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT reason FROM refunds WHERE booking_id=?", String.class, booking.id())).isEqualTo("CUSTOMER_REQUEST");
    }

    private LatePayment latePayment(String provider, String key) {
        Fixture fixture = fixtureWithPrice();
        BookingView booking = booking(fixture, key);
        PaymentView payment = payments.create(fixture.firstUserId(), new PaymentCreateCommand(booking.bookingCode(), "sandbox"));
        OffsetDateTime past = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1);
        jdbcTemplate.update("UPDATE bookings SET payment_deadline=?,hard_deadline=? WHERE id=?", past.minusMinutes(2), past, booking.id());
        jdbcTemplate.update("UPDATE payments SET expires_at=? WHERE id=?", past, payment.id());
        payments.expireDuePayments(Instant.now());
        String raw = payload(payment.id(), "evt-late-" + UUID.randomUUID(), transaction(payment.id()), payment.amount(), Instant.now());
        payments.receiveWebhook("sandbox", raw, sign(raw));
        if (!"sandbox".equals(provider)) jdbcTemplate.update("UPDATE payments SET provider=? WHERE id=?", provider, payment.id());
        return new LatePayment(fixture, booking, payment);
    }

    private BookingView booking(Fixture fixture, String key) {
        return booking(fixture, key, null);
    }

    private BookingView booking(Fixture fixture, String key, String voucherCode) {
        SeatHoldView hold = seatHolds.create(fixture.firstUserId(), new SeatHoldCommand(
                fixture.showtimeId(), List.of(fixture.standardSeatId()), "hold-" + key));
        return bookings.checkout(fixture.firstUserId(), new BookingCheckoutCommand(hold.id(), "checkout-" + key, voucherCode));
    }

    private UUID activeVoucher(String code) {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update("""
                INSERT INTO vouchers (id,code,discount_type,discount_value,max_discount_amount,min_order_amount,usage_limit,usage_count,
                                      per_user_limit,starts_at,ends_at,status,created_at,updated_at)
                VALUES (?,?,'FIXED',10000,NULL,0,1,0,NULL,?,?, 'ACTIVE',?,?)
                """, id, code, now.minusHours(1), now.plusDays(1), now, now);
        return id;
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

    private record LatePayment(Fixture fixture, BookingView booking, PaymentView payment) { }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProviderConfiguration {
        @Bean RefundProvider timeoutRefundProvider() { return new TimeoutProvider(); }
        @Bean RefundProvider failedRefundProvider() { return new FailedProvider(); }
    }

    static final class TimeoutProvider implements RefundProvider {
        static final AtomicInteger invocations = new AtomicInteger();
        @Override public String name() { return "timeout"; }
        @Override public RefundProviderResult refund(RefundProviderRequest request) throws RefundProviderException {
            if (invocations.incrementAndGet() == 1) throw RefundProviderException.temporary("REFUND_PROVIDER_TIMEOUT");
            return new RefundProviderResult("TMO-" + request.refundId());
        }
    }

    static final class FailedProvider implements RefundProvider {
        @Override public String name() { return "failed"; }
        @Override public RefundProviderResult refund(RefundProviderRequest request) throws RefundProviderException {
            throw RefundProviderException.permanent("REFUND_PROVIDER_REJECTED");
        }
    }
}
