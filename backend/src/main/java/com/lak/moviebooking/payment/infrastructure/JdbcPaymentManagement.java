package com.lak.moviebooking.payment.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
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
import com.lak.moviebooking.payment.application.PaymentCreateCommand;
import com.lak.moviebooking.payment.application.PaymentManagement;
import com.lak.moviebooking.payment.application.PaymentProvider;
import com.lak.moviebooking.payment.application.PaymentProviderEvent;
import com.lak.moviebooking.payment.application.PaymentProviderRequest;
import com.lak.moviebooking.payment.application.PaymentRefundAccess;
import com.lak.moviebooking.payment.application.PaymentView;
import com.lak.moviebooking.payment.application.RefundablePayment;
import com.lak.moviebooking.showtime.application.ShowtimeSeatInventory;
import com.lak.moviebooking.ticketing.application.TicketIssuer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class JdbcPaymentManagement implements PaymentManagement, PaymentRefundAccess {

    private static final String INITIATED = "INITIATED";
    private final JdbcTemplate jdbcTemplate;
    private final BookingPaymentAccess bookingAccess;
    private final ShowtimeSeatInventory seatInventory;
    private final TicketIssuer ticketIssuer;
    private final OutboxEventWriter outboxEventWriter;
    private final Map<String, PaymentProvider> providers;
    private final PaymentProperties properties;
    private final Clock clock;

    JdbcPaymentManagement(
            JdbcTemplate jdbcTemplate,
            BookingPaymentAccess bookingAccess,
            ShowtimeSeatInventory seatInventory,
            TicketIssuer ticketIssuer,
            OutboxEventWriter outboxEventWriter,
            List<PaymentProvider> providers,
            PaymentProperties properties,
            Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.bookingAccess = bookingAccess;
        this.seatInventory = seatInventory;
        this.ticketIssuer = ticketIssuer;
        this.outboxEventWriter = outboxEventWriter;
        this.providers = providers.stream().collect(Collectors.toUnmodifiableMap(
                provider -> provider.name().toLowerCase(java.util.Locale.ROOT), provider -> provider));
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PaymentView create(UUID userId, PaymentCreateCommand command) {
        if (command == null || blank(command.bookingCode()) || blank(command.provider())) {
            throw ApplicationException.businessRule("INVALID_PAYMENT_REQUEST", "Payment request is invalid");
        }
        Instant now = clock.instant();
        PaymentProvider provider = provider(command.provider());
        PaymentBooking booking = bookingAccess.lockForPayment(command.bookingCode(), userId, now);
        if (!"PENDING_PAYMENT".equals(booking.status())) {
            throw ApplicationException.businessRule("BOOKING_NOT_PAYABLE", "Booking is no longer payable");
        }
        if (!booking.hardDeadline().isAfter(now)) {
            expireBooking(booking, now);
            throw ApplicationException.expired("BOOKING_PAYMENT_EXPIRED", "Payment deadline has expired");
        }
        PaymentRow existing = activePayment(booking.id());
        if (existing != null) return view(existing, booking, provider, now);

        PaymentRow payment = new PaymentRow(UUID.randomUUID(), booking.id(), provider.name(), transactionId(), booking.totalAmount(),
                "VND", INITIATED, null, booking.hardDeadline());
        jdbcTemplate.update("""
                INSERT INTO payments (id,booking_id,provider,provider_transaction_id,amount,currency,status,paid_at,expires_at,created_at,updated_at)
                VALUES (?,?,?,?,?,?,?,NULL,?,?,?)
                """, payment.id(), payment.bookingId(), payment.provider(), payment.providerTransactionId(), payment.amount(),
                payment.currency(), payment.status(), atUtc(payment.expiresAt()), atUtc(now), atUtc(now));
        return view(payment, booking, provider, now);
    }

    @Override
    public PaymentView find(UUID userId, UUID paymentId) {
        PaymentRow payment = payment(paymentId, false);
        PaymentBooking booking = bookingAccess.lockForPayment(payment.bookingId(), clock.instant());
        if (!booking.userId().equals(userId)) {
            throw ApplicationException.forbidden("PAYMENT_FORBIDDEN", "You do not have access to this payment");
        }
        return view(payment, booking, provider(payment.provider()), clock.instant());
    }

    @Override
    public RefundablePayment lockForRefund(UUID paymentId) {
        PaymentRow payment = payment(paymentId, true);
        return new RefundablePayment(payment.id(), payment.bookingId(), payment.provider(), payment.providerTransactionId(),
                payment.amount(), payment.currency(), payment.status());
    }

    @Override
    @Transactional
    public void receiveWebhook(String providerName, String rawPayload, String signature) {
        PaymentProvider provider = provider(providerName);
        PaymentProviderEvent event = provider.verifyWebhook(rawPayload, signature);
        if (!"SUCCESS".equals(event.status())) {
            throw ApplicationException.businessRule("PAYMENT_STATUS_INVALID", "Payment webhook status is invalid");
        }
        Instant now = clock.instant();
        PaymentRow payment = payment(event.paymentId(), true);
        if (!payment.provider().equals(provider.name()) || !payment.providerTransactionId().equals(event.providerTransactionId())
                || payment.amount() != event.amount() || !payment.currency().equals(event.currency())) {
            throw ApplicationException.businessRule("PAYMENT_WEBHOOK_MISMATCH", "Payment webhook does not match the payment");
        }
        PaymentBooking booking = bookingAccess.lockForPayment(payment.bookingId(), now);
        if (!recordEvent(payment.id(), event, rawPayload, now)) return;
        if ("SUCCESS".equals(payment.status())) return;

        markPaymentSuccess(payment.id(), event, now);
        if ("PENDING_PAYMENT".equals(booking.status()) && !now.isAfter(booking.hardDeadline())) {
            seatInventory.markSoldForBooking(booking.id(), now);
            bookingAccess.markPaid(booking.id(), now);
            ticketIssuer.issueForPaidBooking(booking.id());
            outboxEventWriter.append(new NewOutboxEvent("payment.booking_paid", "payment", payment.id(),
                    new PaymentStateEvent("BOOKING_PAID", booking.id(), payment.id())));
            return;
        }
        if ("PENDING_PAYMENT".equals(booking.status())) expireBooking(booking, now);
        if ("EXPIRED".equals(booking.status()) || "PENDING_PAYMENT".equals(booking.status())) {
            bookingAccess.markPaymentReview(booking.id(), now);
            outboxEventWriter.append(new NewOutboxEvent("refund.late_payment_requested", "payment", payment.id(),
                    new LatePaymentEvent(booking.id(), payment.id(), payment.amount())));
        }
    }

    @Override
    @Transactional
    public int expireDuePayments(Instant now) {
        List<UUID> bookingIds = jdbcTemplate.query("""
                SELECT id FROM bookings WHERE status='PENDING_PAYMENT' AND hard_deadline <= ?
                ORDER BY hard_deadline,id FOR UPDATE SKIP LOCKED LIMIT ?
                """, (resultSet, rowNumber) -> resultSet.getObject("id", UUID.class), atUtc(now), properties.reconciliationBatchSize());
        for (UUID bookingId : bookingIds) {
            PaymentBooking booking = bookingAccess.lockForPayment(bookingId, now);
            if ("PENDING_PAYMENT".equals(booking.status()) && !booking.hardDeadline().isAfter(now)) expireBooking(booking, now);
        }
        return bookingIds.size();
    }

    @Override
    @Transactional
    public int reconcilePendingPayments(Instant now) {
        List<PaymentRow> payments = jdbcTemplate.query("""
                SELECT id,booking_id,provider,provider_transaction_id,amount,currency,status,paid_at,expires_at
                FROM payments WHERE status='INITIATED' AND expires_at > ? ORDER BY created_at,id LIMIT ? FOR UPDATE SKIP LOCKED
                """, this::mapPayment, atUtc(now), properties.reconciliationBatchSize());
        // The sandbox has no remote ledger; real adapters may return a verified event here. Deadline expiry still runs independently.
        payments.forEach(payment -> provider(payment.provider()).reconcile(request(payment, "")));
        return payments.size();
    }

    private void expireBooking(PaymentBooking booking, Instant now) {
        List<UUID> releasedSeats = seatInventory.releaseBooking(booking.id(), now);
        bookingAccess.markExpired(booking.id(), now);
        jdbcTemplate.update("UPDATE payments SET status='EXPIRED',updated_at=? WHERE booking_id=? AND status='INITIATED'", atUtc(now), booking.id());
        outboxEventWriter.append(new NewOutboxEvent("payment.booking_expired", "booking", booking.id(),
                new PaymentStateEvent("BOOKING_EXPIRED", booking.id(), null)));
        if (!releasedSeats.isEmpty()) outboxEventWriter.append(new NewOutboxEvent("reservation.seats_updated", "booking", booking.id(),
                new ReleasedSeatsEvent("SEATS_UPDATED", booking.id(), releasedSeats)));
    }

    private void markPaymentSuccess(UUID paymentId, PaymentProviderEvent event, Instant now) {
        jdbcTemplate.update("""
                UPDATE payments SET status='SUCCESS',provider_event_id=?,paid_at=?,updated_at=?
                WHERE id=? AND status <> 'SUCCESS'
                """, event.providerEventId(), atUtc(event.paidAt()), atUtc(now), paymentId);
    }

    private boolean recordEvent(UUID paymentId, PaymentProviderEvent event, String rawPayload, Instant now) {
        return jdbcTemplate.update("""
                INSERT INTO payment_events (id,payment_id,provider_event_id,event_type,payload_hash,received_at)
                VALUES (?,?,?,?,?,?) ON CONFLICT (provider_event_id) DO NOTHING
                """, UUID.randomUUID(), paymentId, event.providerEventId(), event.status(), sha256(rawPayload), atUtc(now)) == 1;
    }

    private PaymentRow activePayment(UUID bookingId) {
        return jdbcTemplate.query("""
                SELECT id,booking_id,provider,provider_transaction_id,amount,currency,status,paid_at,expires_at
                FROM payments WHERE booking_id=? AND status='INITIATED' ORDER BY created_at DESC FOR UPDATE
                """, this::mapPayment, bookingId).stream().findFirst().orElse(null);
    }

    private PaymentRow payment(UUID paymentId, boolean lock) {
        String suffix = lock ? " FOR UPDATE" : "";
        return jdbcTemplate.query(("""
                SELECT id,booking_id,provider,provider_transaction_id,amount,currency,status,paid_at,expires_at
                FROM payments WHERE id=?""" + suffix), this::mapPayment, paymentId).stream().findFirst()
                .orElseThrow(() -> ApplicationException.notFound("PAYMENT_NOT_FOUND", "Payment was not found"));
    }

    private PaymentView view(PaymentRow payment, PaymentBooking booking, PaymentProvider provider, Instant now) {
        String url = "INITIATED".equals(payment.status()) ? provider.paymentUrl(request(payment, booking.bookingCode())) : null;
        return new PaymentView(payment.id(), booking.bookingCode(), booking.status(), payment.provider(), payment.amount(), payment.currency(), payment.status(),
                url, payment.expiresAt(), payment.paidAt(), now);
    }

    private PaymentProviderRequest request(PaymentRow payment, String bookingCode) {
        return new PaymentProviderRequest(payment.id(), payment.providerTransactionId(), bookingCode, payment.amount(), payment.currency(), payment.expiresAt());
    }

    private PaymentProvider provider(String name) {
        PaymentProvider provider = providers.get(name == null ? "" : name.trim().toLowerCase(java.util.Locale.ROOT));
        if (provider == null) throw ApplicationException.businessRule("PAYMENT_PROVIDER_UNSUPPORTED", "Payment provider is unsupported");
        return provider;
    }

    private PaymentRow mapPayment(java.sql.ResultSet resultSet, int rowNumber) throws java.sql.SQLException {
        OffsetDateTime paidAt = resultSet.getObject("paid_at", OffsetDateTime.class);
        return new PaymentRow(resultSet.getObject("id", UUID.class), resultSet.getObject("booking_id", UUID.class),
                resultSet.getString("provider"), resultSet.getString("provider_transaction_id"), resultSet.getLong("amount"),
                resultSet.getString("currency"), resultSet.getString("status"), paidAt == null ? null : paidAt.toInstant(),
                resultSet.getObject("expires_at", OffsetDateTime.class).toInstant());
    }

    private String transactionId() { return "SBX-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(java.util.Locale.ROOT); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private OffsetDateTime atUtc(Instant value) { return value.atOffset(ZoneOffset.UTC); }
    private String sha256(String value) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 is unavailable", exception); }
    }

    private record PaymentRow(UUID id, UUID bookingId, String provider, String providerTransactionId, long amount, String currency,
                              String status, Instant paidAt, Instant expiresAt) { }
    private record PaymentStateEvent(String type, UUID bookingId, UUID paymentId) { }
    private record LatePaymentEvent(UUID bookingId, UUID paymentId, long amount) { }
    private record ReleasedSeatsEvent(String type, UUID bookingId, List<UUID> showtimeSeatIds) { }
}
