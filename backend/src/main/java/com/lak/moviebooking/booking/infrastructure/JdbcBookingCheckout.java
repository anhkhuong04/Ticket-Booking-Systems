package com.lak.moviebooking.booking.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.lak.moviebooking.booking.application.BookingCheckout;
import com.lak.moviebooking.booking.application.BookingCheckoutCommand;
import com.lak.moviebooking.booking.application.BookingItemView;
import com.lak.moviebooking.booking.application.BookingView;
import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.common.outbox.application.NewOutboxEvent;
import com.lak.moviebooking.common.outbox.application.OutboxEventWriter;
import com.lak.moviebooking.reservation.application.SeatHoldManagement;
import com.lak.moviebooking.reservation.application.SeatHoldView;
import com.lak.moviebooking.showtime.application.ShowtimeSeatForBooking;
import com.lak.moviebooking.showtime.application.ShowtimeSeatInventory;
import com.lak.moviebooking.voucher.application.AppliedVoucher;
import com.lak.moviebooking.voucher.application.VoucherRedemption;
import com.lak.moviebooking.voucher.application.VoucherRedemptionCommand;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class JdbcBookingCheckout implements BookingCheckout {

    private static final String ACTIVE_HOLD = "ACTIVE";
    private static final String PENDING_PAYMENT = "PENDING_PAYMENT";
    private final JdbcTemplate jdbcTemplate;
    private final SeatHoldManagement seatHoldManagement;
    private final ShowtimeSeatInventory seatInventory;
    private final VoucherRedemption voucherRedemption;
    private final OutboxEventWriter outboxEventWriter;
    private final BookingProperties properties;
    private final Clock clock;

    JdbcBookingCheckout(
            JdbcTemplate jdbcTemplate,
            SeatHoldManagement seatHoldManagement,
            ShowtimeSeatInventory seatInventory,
            VoucherRedemption voucherRedemption,
            OutboxEventWriter outboxEventWriter,
            BookingProperties properties,
            Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.seatHoldManagement = seatHoldManagement;
        this.seatInventory = seatInventory;
        this.voucherRedemption = voucherRedemption;
        this.outboxEventWriter = outboxEventWriter;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public BookingView checkout(UUID userId, BookingCheckoutCommand command) {
        validate(command);
        Instant now = clock.instant();
        UUID bookingId = claimIdempotency(userId, command, now);
        BookingRow existing = findBooking(bookingId);
        if (existing != null) {
            return view(existing, now);
        }

        SeatHoldView hold = seatHoldManagement.find(userId, command.holdId());
        if (!ACTIVE_HOLD.equals(hold.status())) {
            throw ApplicationException.businessRule("SEAT_HOLD_NOT_ACTIVE", "Seat hold is no longer active");
        }
        List<ShowtimeSeatForBooking> seats = seatInventory.lockForCheckout(
                hold.showtimeId(), hold.showtimeSeatIds(), hold.id(), now);
        long subtotal = total(seats);
        AppliedVoucher appliedVoucher = command.voucherCode() == null ? null : voucherRedemption.redeem(
                new VoucherRedemptionCommand(userId, bookingId, command.voucherCode(), subtotal));
        long discountAmount = appliedVoucher == null ? 0 : appliedVoucher.discountAmount();
        Instant paymentDeadline = hold.expiresAt();
        Instant hardDeadline = paymentDeadline.plus(properties.paymentGracePeriod());
        BookingRow booking = new BookingRow(bookingId, bookingCode(), userId, hold.showtimeId(), hold.id(), PENDING_PAYMENT,
                appliedVoucher == null ? null : appliedVoucher.voucherId(),
                appliedVoucher == null ? null : appliedVoucher.code(), subtotal, discountAmount, 0,
                Math.subtractExact(subtotal, discountAmount), paymentDeadline, hardDeadline);

        insertBooking(booking, now);
        insertItems(booking.id(), seats, now);
        seatInventory.markPaymentPending(seats.stream().map(ShowtimeSeatForBooking::id).toList(), hold.id(), booking.id(), now);
        seatHoldManagement.consumeForCheckout(userId, hold.id());
        outboxEventWriter.append(new NewOutboxEvent(
                "reservation.seats_updated", "booking", booking.id(),
                new SeatAvailabilityChanged("SEATS_UPDATED", booking.showtimeId(), booking.id(),
                        seats.stream().map(ShowtimeSeatForBooking::id).toList())));
        return view(booking, now, items(seats));
    }

    @Override
    public BookingView findByCode(UUID userId, String bookingCode) {
        if (bookingCode == null || bookingCode.isBlank()) {
            throw ApplicationException.notFound("BOOKING_NOT_FOUND", "Booking was not found");
        }
        BookingRow booking = jdbcTemplate.query("""
                SELECT id,booking_code,user_id,showtime_id,hold_id,voucher_id,voucher_code_snapshot,status,subtotal,discount_amount,service_fee,total_amount,
                       payment_deadline,hard_deadline
                FROM bookings WHERE booking_code=?
                """, this::mapBooking, bookingCode.trim()).stream().findFirst()
                .orElseThrow(() -> ApplicationException.notFound("BOOKING_NOT_FOUND", "Booking was not found"));
        if (!booking.userId().equals(userId)) {
            throw ApplicationException.forbidden("BOOKING_FORBIDDEN", "You do not have access to this booking");
        }
        return view(booking, clock.instant());
    }

    private UUID claimIdempotency(UUID userId, BookingCheckoutCommand command, Instant now) {
        jdbcTemplate.update("DELETE FROM booking_checkout_requests WHERE expires_at <= ?", atUtc(now));
        UUID proposedBookingId = UUID.randomUUID();
        String requestHash = sha256(command.holdId() + ":" + (command.voucherCode() == null ? "" : command.voucherCode()));
        int inserted = jdbcTemplate.update("""
                INSERT INTO booking_checkout_requests (id,actor_id,idempotency_key,request_hash,booking_id,created_at,expires_at)
                VALUES (?,?,?,?,?,?,?) ON CONFLICT (actor_id,idempotency_key) DO NOTHING
                """, UUID.randomUUID(), userId, command.idempotencyKey(), requestHash, proposedBookingId,
                atUtc(now), atUtc(now.plus(properties.checkoutIdempotencyTtl())));
        if (inserted == 1) {
            return proposedBookingId;
        }
        CheckoutRequestRow stored = jdbcTemplate.query("""
                SELECT request_hash,booking_id FROM booking_checkout_requests
                WHERE actor_id=? AND idempotency_key=? FOR UPDATE
                """, (resultSet, rowNumber) -> new CheckoutRequestRow(resultSet.getString("request_hash"),
                resultSet.getObject("booking_id", UUID.class)), userId, command.idempotencyKey()).stream().findFirst()
                .orElseThrow(() -> ApplicationException.conflict(
                        "IDEMPOTENCY_REQUEST_UNAVAILABLE", "The checkout request is still being processed"));
        if (!stored.requestHash().equals(requestHash)) {
            throw ApplicationException.businessRule("IDEMPOTENCY_KEY_REUSED", "Idempotency key was already used for a different request");
        }
        return stored.bookingId();
    }

    private void insertBooking(BookingRow booking, Instant now) {
        jdbcTemplate.update("""
                INSERT INTO bookings (id,booking_code,user_id,showtime_id,hold_id,voucher_id,voucher_code_snapshot,subtotal,discount_amount,service_fee,
                                      total_amount,status,payment_deadline,hard_deadline,created_at,updated_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, booking.id(), booking.bookingCode(), booking.userId(), booking.showtimeId(), booking.holdId(),
                booking.voucherId(), booking.voucherCodeSnapshot(), booking.subtotal(), booking.discountAmount(),
                booking.serviceFee(), booking.totalAmount(), booking.status(),
                atUtc(booking.paymentDeadline()), atUtc(booking.hardDeadline()), atUtc(now), atUtc(now));
    }

    private void insertItems(UUID bookingId, List<ShowtimeSeatForBooking> seats, Instant now) {
        for (ShowtimeSeatForBooking seat : seats) {
            jdbcTemplate.update("""
                    INSERT INTO booking_items (id,booking_id,showtime_seat_id,seat_label_snapshot,seat_type_snapshot,unit_price,created_at)
                    VALUES (?,?,?,?,?,?,?)
                    """, UUID.randomUUID(), bookingId, seat.id(), seat.seatLabel(), seat.seatType(), seat.unitPrice(), atUtc(now));
        }
    }

    private BookingRow findBooking(UUID bookingId) {
        return jdbcTemplate.query("""
                SELECT id,booking_code,user_id,showtime_id,hold_id,voucher_id,voucher_code_snapshot,status,subtotal,discount_amount,service_fee,total_amount,
                       payment_deadline,hard_deadline
                FROM bookings WHERE id=?
                """, this::mapBooking, bookingId).stream().findFirst().orElse(null);
    }

    private BookingView view(BookingRow booking, Instant now) {
        return view(booking, now, jdbcTemplate.query("""
                SELECT showtime_seat_id,seat_label_snapshot,seat_type_snapshot,unit_price
                FROM booking_items WHERE booking_id=? ORDER BY seat_label_snapshot,showtime_seat_id
                """, (resultSet, rowNumber) -> new BookingItemView(
                resultSet.getObject("showtime_seat_id", UUID.class), resultSet.getString("seat_label_snapshot"),
                resultSet.getString("seat_type_snapshot"), resultSet.getLong("unit_price")), booking.id()));
    }

    private BookingView view(BookingRow booking, Instant now, List<BookingItemView> items) {
        BookingContext context = context(booking.showtimeId());
        return new BookingView(booking.id(), booking.bookingCode(), booking.holdId(), booking.showtimeId(),
                context.movieTitle(), context.cinemaName(), context.auditoriumName(), context.startAt(), booking.status(),
                booking.subtotal(), booking.voucherCodeSnapshot(), booking.discountAmount(), booking.serviceFee(), booking.totalAmount(),
                booking.paymentDeadline(), booking.hardDeadline(), now, items);
    }

    private BookingContext context(UUID showtimeId) {
        return jdbcTemplate.query("""
                SELECT movie.title AS movie_title, cinema.name AS cinema_name, auditorium.name AS auditorium_name, showtime.start_at
                FROM showtimes showtime
                JOIN movies movie ON movie.id = showtime.movie_id
                JOIN auditoriums auditorium ON auditorium.id = showtime.auditorium_id
                JOIN cinemas cinema ON cinema.id = auditorium.cinema_id
                WHERE showtime.id=?
                """, (resultSet, rowNumber) -> new BookingContext(
                resultSet.getString("movie_title"), resultSet.getString("cinema_name"),
                resultSet.getString("auditorium_name"), instant(resultSet, "start_at")), showtimeId)
                .stream().findFirst().orElseThrow(() -> ApplicationException.notFound(
                        "SHOWTIME_NOT_FOUND", "Showtime was not found"));
    }

    private List<BookingItemView> items(List<ShowtimeSeatForBooking> seats) {
        return seats.stream().sorted(Comparator.comparing(ShowtimeSeatForBooking::seatLabel))
                .map(seat -> new BookingItemView(seat.id(), seat.seatLabel(), seat.seatType(), seat.unitPrice())).toList();
    }

    private long total(List<ShowtimeSeatForBooking> seats) {
        try {
            long total = 0;
            for (ShowtimeSeatForBooking seat : seats) {
                total = Math.addExact(total, seat.unitPrice());
            }
            return total;
        }
        catch (ArithmeticException exception) {
            throw ApplicationException.businessRule("BOOKING_TOTAL_INVALID", "Booking total is invalid");
        }
    }

    private void validate(BookingCheckoutCommand command) {
        if (command == null || command.holdId() == null || command.idempotencyKey() == null
                || command.idempotencyKey().isBlank() || command.idempotencyKey().length() > 128
                || (command.voucherCode() != null && command.voucherCode().length() > 64)) {
            throw ApplicationException.businessRule("INVALID_CHECKOUT", "Checkout request is invalid");
        }
    }

    private String bookingCode() {
        return "LAK-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(java.util.Locale.ROOT);
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

    private BookingRow mapBooking(java.sql.ResultSet resultSet, int rowNumber) throws java.sql.SQLException {
        return new BookingRow(resultSet.getObject("id", UUID.class), resultSet.getString("booking_code"),
                resultSet.getObject("user_id", UUID.class), resultSet.getObject("showtime_id", UUID.class),
                resultSet.getObject("hold_id", UUID.class), resultSet.getString("status"),
                resultSet.getObject("voucher_id", UUID.class), resultSet.getString("voucher_code_snapshot"), resultSet.getLong("subtotal"),
                resultSet.getLong("discount_amount"), resultSet.getLong("service_fee"), resultSet.getLong("total_amount"),
                instant(resultSet, "payment_deadline"), instant(resultSet, "hard_deadline"));
    }

    private Instant instant(java.sql.ResultSet resultSet, String column) throws java.sql.SQLException {
        return resultSet.getObject(column, OffsetDateTime.class).toInstant();
    }

    private OffsetDateTime atUtc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private record CheckoutRequestRow(String requestHash, UUID bookingId) {
    }

    private record BookingRow(
            UUID id, String bookingCode, UUID userId, UUID showtimeId, UUID holdId, String status,
            UUID voucherId, String voucherCodeSnapshot,
            long subtotal, long discountAmount, long serviceFee, long totalAmount,
            Instant paymentDeadline, Instant hardDeadline) {
    }

    private record BookingContext(String movieTitle, String cinemaName, String auditoriumName, Instant startAt) {
    }

    private record SeatAvailabilityChanged(String type, UUID showtimeId, UUID resourceId, List<UUID> showtimeSeatIds) {

        private SeatAvailabilityChanged {
            showtimeSeatIds = List.copyOf(showtimeSeatIds);
        }
    }
}
