package com.lak.moviebooking.refund.infrastructure;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.lak.moviebooking.booking.application.BookingPaymentAccess;
import com.lak.moviebooking.booking.application.PaymentBooking;
import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.common.outbox.application.NewOutboxEvent;
import com.lak.moviebooking.common.outbox.application.OutboxEventWriter;
import com.lak.moviebooking.payment.application.PaymentRefundAccess;
import com.lak.moviebooking.payment.application.RefundablePayment;
import com.lak.moviebooking.refund.application.RefundManagement;
import com.lak.moviebooking.refund.application.RefundProvider;
import com.lak.moviebooking.refund.application.RefundProviderException;
import com.lak.moviebooking.refund.application.RefundProviderRequest;
import com.lak.moviebooking.refund.application.RefundProviderResult;
import com.lak.moviebooking.refund.application.RefundView;
import com.lak.moviebooking.showtime.application.ShowtimeSeatInventory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class JdbcRefundManagement implements RefundManagement {

    private static final String REQUESTED = "REQUESTED";
    private static final String REFUNDED = "REFUNDED";
    private static final String REFUND_FAILED = "REFUND_FAILED";
    private static final Duration RETRY_DELAY = Duration.ofSeconds(30);
    private static final int BATCH_SIZE = 100;

    private final JdbcTemplate jdbcTemplate;
    private final PaymentRefundAccess paymentAccess;
    private final BookingPaymentAccess bookingAccess;
    private final ShowtimeSeatInventory seatInventory;
    private final OutboxEventWriter outboxEventWriter;
    private final Map<String, RefundProvider> providers;
    private final Clock clock;

    JdbcRefundManagement(
            JdbcTemplate jdbcTemplate,
            PaymentRefundAccess paymentAccess,
            BookingPaymentAccess bookingAccess,
            ShowtimeSeatInventory seatInventory,
            OutboxEventWriter outboxEventWriter,
            List<RefundProvider> providers,
            Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.paymentAccess = paymentAccess;
        this.bookingAccess = bookingAccess;
        this.seatInventory = seatInventory;
        this.outboxEventWriter = outboxEventWriter;
        this.providers = providers.stream().collect(Collectors.toUnmodifiableMap(
                provider -> provider.name().toLowerCase(java.util.Locale.ROOT), provider -> provider));
        this.clock = clock;
    }

    @Override
    @Transactional
    public RefundView requestLatePaymentRefund(UUID bookingId, UUID paymentId, long amount) {
        if (bookingId == null || paymentId == null || amount <= 0) {
            throw ApplicationException.businessRule("REFUND_REQUEST_INVALID", "Refund request is invalid");
        }
        Instant now = clock.instant();
        RefundablePayment payment = paymentAccess.lockForRefund(paymentId);
        if (!payment.bookingId().equals(bookingId) || payment.amount() != amount || !"SUCCESS".equals(payment.status())
                || !"VND".equals(payment.currency())) {
            throw ApplicationException.businessRule("REFUND_PAYMENT_INVALID", "Payment is not eligible for refund");
        }
        PaymentBooking booking = bookingAccess.lockForPayment(bookingId, now);
        if (!"PAYMENT_REVIEW".equals(booking.status()) && !"REFUND_PENDING".equals(booking.status())) {
            throw ApplicationException.businessRule("REFUND_BOOKING_INVALID", "Booking is not eligible for refund");
        }

        RefundRow existing = refundByPayment(paymentId, true);
        if (existing != null) return view(existing);

        List<UUID> releasedSeats = seatInventory.releaseBooking(bookingId, now);
        RefundRow created = new RefundRow(UUID.randomUUID(), bookingId, paymentId, payment.provider(), null, amount, REQUESTED,
                0, now, now, null);
        jdbcTemplate.update("""
                INSERT INTO refunds (id,booking_id,payment_id,provider,provider_refund_id,amount,reason,status,attempt_count,
                                     next_attempt_at,requested_at,refunded_at,last_error_code,created_at,updated_at)
                VALUES (?,?,?,?,?,?, 'LATE_PAYMENT', ?,?,?,?,NULL,NULL,?,?)
                """, created.id(), created.bookingId(), created.paymentId(), created.provider(), created.providerRefundId(),
                created.amount(), created.status(), created.attemptCount(), atUtc(created.nextAttemptAt()), atUtc(created.requestedAt()),
                atUtc(now), atUtc(now));
        bookingAccess.markRefundPending(bookingId, now);
        if (!releasedSeats.isEmpty()) {
            outboxEventWriter.append(new NewOutboxEvent("reservation.seats_updated", "booking", bookingId,
                    new ReleasedSeatsEvent("SEATS_UPDATED", bookingId, releasedSeats)));
        }
        return view(created);
    }

    @Override
    @Transactional
    public int processRequestedRefunds(Instant now) {
        List<UUID> ids = jdbcTemplate.query("""
                SELECT id FROM refunds
                WHERE status='REQUESTED' AND next_attempt_at <= ?
                ORDER BY next_attempt_at,created_at,id FOR UPDATE SKIP LOCKED LIMIT ?
                """, (resultSet, rowNumber) -> resultSet.getObject("id", UUID.class), atUtc(now), BATCH_SIZE);
        for (UUID id : ids) process(refund(id, true), now);
        return ids.size();
    }

    private void process(RefundRow refund, Instant now) {
        RefundablePayment payment = paymentAccess.lockForRefund(refund.paymentId());
        if (!"SUCCESS".equals(payment.status()) || payment.amount() != refund.amount() || !"VND".equals(payment.currency())) {
            fail(refund.id(), "REFUND_PAYMENT_INVALID", now);
            return;
        }
        try {
            RefundProviderResult result = provider(refund.provider()).refund(new RefundProviderRequest(refund.id(), payment.id(),
                    payment.providerTransactionId(), refund.amount(), payment.currency()));
            jdbcTemplate.update("""
                    UPDATE refunds SET status='REFUNDED',provider_refund_id=?,refunded_at=?,last_error_code=NULL,updated_at=?
                    WHERE id=? AND status='REQUESTED'
                    """, result.providerRefundId(), atUtc(now), atUtc(now), refund.id());
        }
        catch (RefundProviderException exception) {
            if (exception.retryable()) retry(refund.id(), exception.code(), now);
            else fail(refund.id(), exception.code(), now);
        }
    }

    private void retry(UUID refundId, String errorCode, Instant now) {
        jdbcTemplate.update("""
                UPDATE refunds SET attempt_count=attempt_count+1,next_attempt_at=?,last_error_code=?,updated_at=?
                WHERE id=? AND status='REQUESTED'
                """, atUtc(now.plus(RETRY_DELAY)), errorCode, atUtc(now), refundId);
    }

    private void fail(UUID refundId, String errorCode, Instant now) {
        jdbcTemplate.update("""
                UPDATE refunds SET status='REFUND_FAILED',attempt_count=attempt_count+1,last_error_code=?,updated_at=?
                WHERE id=? AND status='REQUESTED'
                """, errorCode, atUtc(now), refundId);
    }

    private RefundRow refundByPayment(UUID paymentId, boolean lock) {
        String suffix = lock ? " FOR UPDATE" : "";
        return jdbcTemplate.query("""
                SELECT id,booking_id,payment_id,provider,provider_refund_id,amount,status,attempt_count,next_attempt_at,requested_at,refunded_at
                FROM refunds WHERE payment_id=?
                """ + suffix, this::mapRefund, paymentId).stream().findFirst().orElse(null);
    }

    private RefundRow refund(UUID id, boolean lock) {
        String suffix = lock ? " FOR UPDATE" : "";
        return jdbcTemplate.query("""
                SELECT id,booking_id,payment_id,provider,provider_refund_id,amount,status,attempt_count,next_attempt_at,requested_at,refunded_at
                FROM refunds WHERE id=?
                """ + suffix, this::mapRefund, id).stream().findFirst()
                .orElseThrow(() -> ApplicationException.notFound("REFUND_NOT_FOUND", "Refund was not found"));
    }

    private RefundProvider provider(String name) {
        RefundProvider provider = providers.get(name.toLowerCase(java.util.Locale.ROOT));
        if (provider == null) throw ApplicationException.businessRule("REFUND_PROVIDER_UNSUPPORTED", "Refund provider is unsupported");
        return provider;
    }

    private RefundRow mapRefund(java.sql.ResultSet resultSet, int rowNumber) throws java.sql.SQLException {
        OffsetDateTime refundedAt = resultSet.getObject("refunded_at", OffsetDateTime.class);
        return new RefundRow(resultSet.getObject("id", UUID.class), resultSet.getObject("booking_id", UUID.class),
                resultSet.getObject("payment_id", UUID.class), resultSet.getString("provider"), resultSet.getString("provider_refund_id"),
                resultSet.getLong("amount"), resultSet.getString("status"), resultSet.getInt("attempt_count"),
                resultSet.getObject("next_attempt_at", OffsetDateTime.class).toInstant(),
                resultSet.getObject("requested_at", OffsetDateTime.class).toInstant(), refundedAt == null ? null : refundedAt.toInstant());
    }

    private RefundView view(RefundRow refund) {
        return new RefundView(refund.id(), refund.bookingId(), refund.paymentId(), refund.amount(), refund.status(),
                refund.requestedAt(), refund.refundedAt());
    }

    private OffsetDateTime atUtc(Instant instant) { return instant.atOffset(ZoneOffset.UTC); }

    private record RefundRow(UUID id, UUID bookingId, UUID paymentId, String provider, String providerRefundId, long amount,
                             String status, int attemptCount, Instant nextAttemptAt, Instant requestedAt, Instant refundedAt) { }
    private record ReleasedSeatsEvent(String type, UUID bookingId, List<UUID> showtimeSeatIds) { }
}
