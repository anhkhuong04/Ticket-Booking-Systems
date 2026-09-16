package com.lak.moviebooking.booking.infrastructure;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import com.lak.moviebooking.audit.application.AuditLogWriter;
import com.lak.moviebooking.booking.application.BookingBilling;
import com.lak.moviebooking.booking.application.BookingBillingRequest;
import com.lak.moviebooking.common.application.error.ApplicationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class JdbcBookingBilling implements BookingBilling {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final AuditLogWriter audit;

    JdbcBookingBilling(JdbcTemplate jdbc, Clock clock, AuditLogWriter audit) {
        this.jdbc = jdbc; this.clock = clock; this.audit = audit;
    }

    @Override
    public BookingBillingRequest find(UUID ownerId, String bookingCode) {
        UUID bookingId = ownerBooking(ownerId, bookingCode, false);
        return jdbc.query("SELECT recipient_type,recipient_name,tax_code,address,email FROM booking_billing_requests WHERE booking_id=?",
                (rs, row) -> new BookingBillingRequest(rs.getString("recipient_type"), rs.getString("recipient_name"),
                        rs.getString("tax_code"), rs.getString("address"), rs.getString("email")), bookingId)
                .stream().findFirst().orElse(null);
    }

    @Override
    @Transactional
    public BookingBillingRequest request(UUID ownerId, String bookingCode, BookingBillingRequest value) {
        UUID bookingId = ownerBooking(ownerId, bookingCode, true);
        if (!"PERSONAL".equals(value.recipientType()) && !"BUSINESS".equals(value.recipientType())
                || "BUSINESS".equals(value.recipientType()) && (blank(value.taxCode()) || blank(value.address()))) {
            throw ApplicationException.businessRule("INVALID_BILLING", "Billing information is invalid");
        }
        jdbc.update("""
                INSERT INTO booking_billing_requests (booking_id,recipient_type,recipient_name,tax_code,address,email,requested_at)
                VALUES (?,?,?,?,?,?,?) ON CONFLICT (booking_id) DO UPDATE SET
                recipient_type=EXCLUDED.recipient_type,recipient_name=EXCLUDED.recipient_name,
                tax_code=EXCLUDED.tax_code,address=EXCLUDED.address,email=EXCLUDED.email,requested_at=EXCLUDED.requested_at
                """, bookingId, value.recipientType(), value.recipientName().trim(),
                "BUSINESS".equals(value.recipientType()) ? value.taxCode().trim() : null,
                blank(value.address()) ? null : value.address().trim(), value.email().trim().toLowerCase(java.util.Locale.ROOT),
                clock.instant().atOffset(ZoneOffset.UTC));
        audit.record(ownerId, "BOOKING_BILLING_REQUESTED", "booking", bookingId, Map.of());
        return find(ownerId, bookingCode);
    }

    private UUID ownerBooking(UUID ownerId, String bookingCode, boolean requirePending) {
        return jdbc.query("SELECT id,status,payment_deadline FROM bookings WHERE booking_code=? AND user_id=?" + (requirePending ? " FOR UPDATE" : ""),
                (rs, row) -> {
                    if (requirePending && (!"PENDING_PAYMENT".equals(rs.getString("status"))
                            || !rs.getObject("payment_deadline", java.time.OffsetDateTime.class).toInstant().isAfter(clock.instant())))
                        throw ApplicationException.businessRule("BILLING_REQUEST_CLOSED", "Billing information can no longer be changed");
                    return rs.getObject("id", UUID.class);
                }, bookingCode, ownerId).stream().findFirst().orElseThrow(() ->
                ApplicationException.notFound("BOOKING_NOT_FOUND", "Booking was not found"));
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
