package com.lak.moviebooking.refund.infrastructure;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.lak.moviebooking.audit.application.AuditLogWriter;
import com.lak.moviebooking.booking.application.BookingPaymentAccess;
import com.lak.moviebooking.booking.application.BookingRefundAccess;
import com.lak.moviebooking.booking.application.CustomerRefundBooking;
import com.lak.moviebooking.booking.application.PaymentBooking;
import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.common.outbox.application.NewOutboxEvent;
import com.lak.moviebooking.common.outbox.application.OutboxEventWriter;
import com.lak.moviebooking.payment.application.PaymentRefundAccess;
import com.lak.moviebooking.payment.application.RefundablePayment;
import com.lak.moviebooking.refund.application.RefundManagement;
import com.lak.moviebooking.refund.application.CustomerRefundCommand;
import com.lak.moviebooking.refund.application.RefundProvider;
import com.lak.moviebooking.refund.application.RefundProviderException;
import com.lak.moviebooking.refund.application.RefundProviderRequest;
import com.lak.moviebooking.refund.application.RefundProviderResult;
import com.lak.moviebooking.refund.application.RefundView;
import com.lak.moviebooking.reservation.application.SeatAvailabilityEvent;
import com.lak.moviebooking.reservation.application.SeatHoldManagement;
import com.lak.moviebooking.showtime.application.ShowtimeSeatInventory;
import com.lak.moviebooking.ticketing.application.TicketIssuer;
import com.lak.moviebooking.voucher.application.VoucherRedemption;
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
    private final BookingRefundAccess bookingRefundAccess;
    private final ShowtimeSeatInventory seatInventory;
    private final SeatHoldManagement seatHoldManagement;
    private final TicketIssuer ticketIssuer;
    private final VoucherRedemption voucherRedemption;
    private final OutboxEventWriter outboxEventWriter;
    private final AuditLogWriter auditLogWriter;
    private final Map<String, RefundProvider> providers;
    private final Clock clock;

    JdbcRefundManagement(
            JdbcTemplate jdbcTemplate,
            PaymentRefundAccess paymentAccess,
            BookingPaymentAccess bookingAccess,
            BookingRefundAccess bookingRefundAccess,
            ShowtimeSeatInventory seatInventory,
            SeatHoldManagement seatHoldManagement,
            TicketIssuer ticketIssuer,
            VoucherRedemption voucherRedemption,
            OutboxEventWriter outboxEventWriter,
            AuditLogWriter auditLogWriter,
            List<RefundProvider> providers,
            Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.paymentAccess = paymentAccess;
        this.bookingAccess = bookingAccess;
        this.bookingRefundAccess = bookingRefundAccess;
        this.seatInventory = seatInventory;
        this.seatHoldManagement = seatHoldManagement;
        this.ticketIssuer = ticketIssuer;
        this.voucherRedemption = voucherRedemption;
        this.outboxEventWriter = outboxEventWriter;
        this.auditLogWriter = auditLogWriter;
        this.providers = providers.stream().collect(Collectors.toUnmodifiableMap(
                provider -> provider.name().toLowerCase(java.util.Locale.ROOT), provider -> provider));
        this.clock = clock;
    }

    @Override
    @Transactional
    public RefundView requestLatePaymentRefund(UUID bookingId, UUID paymentId, long amount) {
        return requestRefund(bookingId, paymentId, amount, "LATE_PAYMENT", "PAYMENT_REVIEW", SeatRelease.PAYMENT_PENDING);
    }

    @Override
    @Transactional
    public RefundView requestShowtimeCancellationRefund(UUID bookingId) {
        if (bookingId == null) {
            throw ApplicationException.businessRule("REFUND_REQUEST_INVALID", "Refund request is invalid");
        }
        RefundablePayment payment = paymentAccess.lockSuccessfulForBooking(bookingId);
        return requestRefund(bookingId, payment.id(), payment.amount(), "SHOWTIME_CANCELLED", "PAID", SeatRelease.NONE);
    }

    @Override
    @Transactional
    public RefundView requestCustomerRefund(CustomerRefundCommand command) {
        validateCustomerCommand(command);
        Instant now = clock.instant();
        CustomerRefundRequest request = claimCustomerRequest(command, now);
        if (request.refundId() != null) return view(refund(request.refundId(), false));

        RefundablePayment payment = paymentAccess.lockSuccessfulForBooking(command.bookingId());
        CustomerRefundBooking booking = bookingRefundAccess.lockForCustomerRefund(command.bookingId());
        requireCustomerEligibility(command.userId(), booking, now);
        ticketIssuer.cancelForRefund(booking.id());
        voucherRedemption.restoreForRefund(booking.id(), now);
        RefundView refund = requestRefund(booking.id(), payment.id(), payment.amount(), "CUSTOMER_REQUEST", "PAID", SeatRelease.SOLD);
        jdbcTemplate.update("UPDATE customer_refund_requests SET refund_id=? WHERE id=? AND refund_id IS NULL", refund.id(), request.id());
        auditLogWriter.record(command.userId(), "CUSTOMER_REFUND_REQUESTED", "refund", refund.id(),
                Map.of("bookingId", booking.id().toString()));
        return refund;
    }

    @Override
    public RefundView findCustomerRefund(UUID userId, UUID refundId) {
        RefundRow refund = jdbcTemplate.query("""
                SELECT refund.id,refund.booking_id,refund.payment_id,refund.provider,refund.provider_refund_id,refund.amount,
                       refund.status,refund.attempt_count,refund.next_attempt_at,refund.requested_at,refund.refunded_at
                FROM refunds refund JOIN bookings booking ON booking.id=refund.booking_id
                WHERE refund.id=? AND booking.user_id=?
                """, this::mapRefund, refundId, userId).stream().findFirst()
                .orElseThrow(() -> ApplicationException.notFound("REFUND_NOT_FOUND", "Refund was not found"));
        return view(refund);
    }

    @Override
    @Transactional
    public void processShowtimeCancellation(UUID showtimeId, Instant cancelledAt) {
        if (showtimeId == null || cancelledAt == null) throw ApplicationException.businessRule("SHOWTIME_CANCELLATION_INVALID", "Showtime is invalid");
        Instant now = clock.instant();
        List<UUID> releasedHoldSeats = seatHoldManagement.cancelActiveForShowtime(showtimeId, now);
        int refundRequests = 0;
        for (UUID bookingId : bookingAccess.findBookingIdsForShowtimeCancellation(showtimeId)) {
            PaymentBooking booking = bookingAccess.findForShowtimeCancellation(bookingId);
            if ("PENDING_PAYMENT".equals(booking.status())) {
                seatInventory.releaseBooking(bookingId, now);
                if (bookingAccess.expirePendingForShowtimeCancellation(bookingId, now)) continue;
                booking = bookingAccess.findForShowtimeCancellation(bookingId);
            }
            if ("PAID".equals(booking.status())) {
                ticketIssuer.cancelForRefund(bookingId);
                voucherRedemption.restoreForRefund(bookingId, cancelledAt);
                requestShowtimeCancellationRefund(bookingId);
                refundRequests++;
            }
        }
        outboxEventWriter.append(new NewOutboxEvent("reservation.showtime_cancelled", "showtime", showtimeId,
                new SeatAvailabilityEvent("SHOWTIME_CANCELLED", showtimeId, showtimeId, releasedHoldSeats)));
    }

    private RefundView requestRefund(UUID bookingId, UUID paymentId, long amount, String reason, String eligibleStatus,
            SeatRelease seatRelease) {
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
        if (!eligibleStatus.equals(booking.status()) && !"REFUND_PENDING".equals(booking.status())) {
            throw ApplicationException.businessRule("REFUND_BOOKING_INVALID", "Booking is not eligible for refund");
        }

        RefundRow existing = refundByPayment(paymentId, true);
        if (existing != null) return view(existing);

        List<UUID> releasedSeats = switch (seatRelease) {
            case PAYMENT_PENDING -> seatInventory.releaseBooking(bookingId, now);
            case SOLD -> seatInventory.releaseSoldBookingForRefund(bookingId, now);
            case NONE -> List.of();
        };
        RefundRow created = new RefundRow(UUID.randomUUID(), bookingId, paymentId, payment.provider(), null, amount, REQUESTED,
                0, now, now, null);
        jdbcTemplate.update("""
                INSERT INTO refunds (id,booking_id,payment_id,provider,provider_refund_id,amount,reason,status,attempt_count,
                                     next_attempt_at,requested_at,refunded_at,last_error_code,created_at,updated_at)
                VALUES (?,?,?,?,?,?, ?, ?,?,?,?,NULL,NULL,?,?)
                """, created.id(), created.bookingId(), created.paymentId(), created.provider(), created.providerRefundId(),
                created.amount(), reason, created.status(), created.attemptCount(), atUtc(created.nextAttemptAt()), atUtc(created.requestedAt()),
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
            bookingAccess.markRefunded(refund.bookingId(), now);
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

    private CustomerRefundRequest claimCustomerRequest(CustomerRefundCommand command, Instant now) {
        jdbcTemplate.update("DELETE FROM customer_refund_requests WHERE expires_at <= ?", atUtc(now));
        String hash = sha256(command.bookingId().toString());
        UUID id = UUID.randomUUID();
        int inserted = jdbcTemplate.update("""
                INSERT INTO customer_refund_requests (id,actor_id,idempotency_key,request_hash,booking_id,refund_id,created_at,expires_at)
                VALUES (?,?,?,?,?,NULL,?,?) ON CONFLICT (actor_id,idempotency_key) DO NOTHING
                """, id, command.userId(), command.idempotencyKey(), hash, command.bookingId(), atUtc(now), atUtc(now.plus(Duration.ofHours(24))));
        if (inserted == 1) return new CustomerRefundRequest(id, hash, null);
        CustomerRefundRequest stored = jdbcTemplate.query("""
                SELECT id,request_hash,refund_id FROM customer_refund_requests
                WHERE actor_id=? AND idempotency_key=? FOR UPDATE
                """, (resultSet, rowNumber) -> new CustomerRefundRequest(resultSet.getObject("id", UUID.class),
                resultSet.getString("request_hash"), resultSet.getObject("refund_id", UUID.class)), command.userId(), command.idempotencyKey())
                .stream().findFirst().orElseThrow(() -> ApplicationException.conflict(
                        "IDEMPOTENCY_REQUEST_UNAVAILABLE", "Refund request is still being processed"));
        if (!stored.requestHash().equals(hash)) {
            throw ApplicationException.businessRule("IDEMPOTENCY_KEY_REUSED", "Idempotency key was already used for a different request");
        }
        if (stored.refundId() == null) {
            throw ApplicationException.conflict("IDEMPOTENCY_REQUEST_UNAVAILABLE", "Refund request is still being processed");
        }
        return stored;
    }

    private void validateCustomerCommand(CustomerRefundCommand command) {
        if (command == null || command.userId() == null || command.bookingId() == null || command.idempotencyKey() == null
                || command.idempotencyKey().isBlank() || command.idempotencyKey().length() > 128) {
            throw ApplicationException.businessRule("INVALID_REFUND_REQUEST", "Refund request is invalid");
        }
    }

    private void requireCustomerEligibility(UUID userId, CustomerRefundBooking booking, Instant now) {
        if (!booking.userId().equals(userId)) {
            throw ApplicationException.forbidden("REFUND_FORBIDDEN", "You do not have access to this refund");
        }
        if (!"PAID".equals(booking.status())) {
            throw ApplicationException.businessRule("REFUND_ALREADY_REQUESTED", "Booking is not eligible for refund");
        }
        if (now.isAfter(booking.showtimeStartAt().minus(Duration.ofMinutes(45)))) {
            throw ApplicationException.businessRule("REFUND_WINDOW_CLOSED", "Booking is no longer eligible for refund");
        }
    }

    private String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
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
    private record CustomerRefundRequest(UUID id, String requestHash, UUID refundId) { }
    private enum SeatRelease { PAYMENT_PENDING, SOLD, NONE }
    private record ReleasedSeatsEvent(String type, UUID bookingId, List<UUID> showtimeSeatIds) { }
}
