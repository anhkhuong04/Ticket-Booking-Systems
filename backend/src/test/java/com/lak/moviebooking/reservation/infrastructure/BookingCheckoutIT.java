package com.lak.moviebooking.reservation.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.lak.moviebooking.booking.application.BookingCheckout;
import com.lak.moviebooking.booking.application.BookingCheckoutCommand;
import com.lak.moviebooking.booking.application.BookingView;
import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.reservation.application.SeatHoldCommand;
import com.lak.moviebooking.reservation.application.SeatHoldManagement;
import com.lak.moviebooking.reservation.application.SeatHoldView;
import com.lak.moviebooking.ticketing.application.TicketIssuer;
import com.lak.moviebooking.ticketing.application.TicketIssuance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class BookingCheckoutIT extends SeatHoldManagementIT {

    @Autowired
    private SeatHoldManagement seatHoldManagement;

    @Autowired
    private BookingCheckout bookingCheckout;

    @Autowired
    private TicketIssuer ticketIssuer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void checkoutSnapshotsServerPricesConsumesHoldAndReplaysIdempotently() {
        Fixture fixture = fixtureWithPrices();
        SeatHoldView hold = seatHoldManagement.create(fixture.firstUserId(), new SeatHoldCommand(
                fixture.showtimeId(), List.of(fixture.standardSeatId()), "hold-for-checkout"));

        BookingView created = bookingCheckout.checkout(fixture.firstUserId(), new BookingCheckoutCommand(hold.id(), "checkout-1"));
        BookingView replay = bookingCheckout.checkout(fixture.firstUserId(), new BookingCheckoutCommand(hold.id(), "checkout-1"));

        assertThat(replay.id()).isEqualTo(created.id());
        assertThat(created.status()).isEqualTo("PENDING_PAYMENT");
        assertThat(created.subtotal()).isEqualTo(90_000L);
        assertThat(created.totalAmount()).isEqualTo(90_000L);
        assertThat(created.paymentDeadline()).isEqualTo(hold.expiresAt());
        assertThat(created.hardDeadline()).isEqualTo(hold.expiresAt().plusSeconds(120));
        assertThat(created.items()).singleElement().satisfies(item -> {
            assertThat(item.seatLabel()).isEqualTo("A1");
            assertThat(item.seatType()).isEqualTo("STANDARD");
            assertThat(item.unitPrice()).isEqualTo(90_000L);
        });
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM seat_holds WHERE id=?", String.class, hold.id())).isEqualTo("CONSUMED");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM showtime_seats WHERE id=?", String.class, fixture.standardSeatId())).isEqualTo("PAYMENT_PENDING");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM bookings WHERE hold_id=?", Integer.class, hold.id())).isEqualTo(1);
    }

    @Test
    void checkoutAppliesPercentVoucherWithCapAndSnapshotsItsCode() {
        Fixture fixture = fixtureWithPrices();
        UUID voucherId = voucher("SAVE50", "PERCENT", 50, 20_000L, 50_000L, 10, 2);
        SeatHoldView hold = seatHoldManagement.create(fixture.firstUserId(), new SeatHoldCommand(
                fixture.showtimeId(), List.of(fixture.standardSeatId()), "hold-voucher"));

        BookingView booking = bookingCheckout.checkout(fixture.firstUserId(), new BookingCheckoutCommand(
                hold.id(), "checkout-voucher", "save50"));

        assertThat(booking.voucherCode()).isEqualTo("SAVE50");
        assertThat(booking.subtotal()).isEqualTo(90_000L);
        assertThat(booking.discountAmount()).isEqualTo(20_000L);
        assertThat(booking.totalAmount()).isEqualTo(70_000L);
        assertThat(jdbcTemplate.queryForObject("SELECT voucher_id FROM bookings WHERE id=?", UUID.class, booking.id())).isEqualTo(voucherId);
        assertThat(jdbcTemplate.queryForObject("SELECT discount_amount FROM voucher_redemptions WHERE booking_id=?", Long.class,
                booking.id())).isEqualTo(20_000L);
        assertThat(jdbcTemplate.queryForObject("SELECT usage_count FROM vouchers WHERE id=?", Integer.class, voucherId)).isEqualTo(1);
    }

    @Test
    void checkoutRejectsVoucherBelowMinimumWithoutConsumingHoldOrQuota() {
        Fixture fixture = fixtureWithPrices();
        UUID voucherId = voucher("MIN100", "FIXED", 10_000L, null, 100_000L, 2, null);
        SeatHoldView hold = seatHoldManagement.create(fixture.firstUserId(), new SeatHoldCommand(
                fixture.showtimeId(), List.of(fixture.standardSeatId()), "hold-minimum-voucher"));

        assertThatThrownBy(() -> bookingCheckout.checkout(fixture.firstUserId(), new BookingCheckoutCommand(
                hold.id(), "checkout-minimum-voucher", "MIN100")))
                .isInstanceOf(ApplicationException.class)
                .extracting(error -> ((ApplicationException) error).code())
                .isEqualTo("VOUCHER_NOT_APPLICABLE");

        assertThat(jdbcTemplate.queryForObject("SELECT usage_count FROM vouchers WHERE id=?", Integer.class, voucherId)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM voucher_redemptions WHERE voucher_id=?", Integer.class,
                voucherId)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM seat_holds WHERE id=?", String.class, hold.id())).isEqualTo("ACTIVE");
    }

    @Test
    void checkoutRejectsVoucherOutsideItsValidityWindow() {
        Fixture fixture = fixtureWithPrices();
        UUID voucherId = voucher("EXPIRED10", "FIXED", 10_000L, null, 0L, 2, null);
        jdbcTemplate.update("UPDATE vouchers SET ends_at=? WHERE id=?", OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1), voucherId);
        SeatHoldView hold = seatHoldManagement.create(fixture.firstUserId(), new SeatHoldCommand(
                fixture.showtimeId(), List.of(fixture.standardSeatId()), "hold-expired-voucher"));

        assertThatThrownBy(() -> bookingCheckout.checkout(fixture.firstUserId(), new BookingCheckoutCommand(
                hold.id(), "checkout-expired-voucher", "EXPIRED10")))
                .isInstanceOf(ApplicationException.class)
                .extracting(error -> ((ApplicationException) error).code())
                .isEqualTo("VOUCHER_NOT_APPLICABLE");

        assertThat(jdbcTemplate.queryForObject("SELECT usage_count FROM vouchers WHERE id=?", Integer.class, voucherId)).isZero();
    }

    @Test
    void checkoutEnforcesVoucherUsageQuotaWhenConcurrent() throws Exception {
        UUID voucherId = voucher("ONEUSE", "FIXED", 10_000L, null, 0L, 1, 1);
        Fixture first = fixtureWithPrices();
        Fixture second = fixtureWithPrices();
        SeatHoldView firstHold = seatHoldManagement.create(first.firstUserId(), new SeatHoldCommand(
                first.showtimeId(), List.of(first.standardSeatId()), "first-voucher-hold"));
        SeatHoldView secondHold = seatHoldManagement.create(second.firstUserId(), new SeatHoldCommand(
                second.showtimeId(), List.of(second.standardSeatId()), "second-voucher-hold"));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<String> firstResult = executor.submit(checkoutVoucher(first.firstUserId(), firstHold.id(), "first-voucher-checkout", ready, start));
            Future<String> secondResult = executor.submit(checkoutVoucher(second.firstUserId(), secondHold.id(), "second-voucher-checkout", ready, start));
            ready.await();
            start.countDown();
            assertThat(List.of(firstResult.get(), secondResult.get()))
                    .containsExactlyInAnyOrder("SUCCESS", "VOUCHER_USAGE_LIMIT_REACHED");
        }
        assertThat(jdbcTemplate.queryForObject("SELECT usage_count FROM vouchers WHERE id=?", Integer.class, voucherId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM voucher_redemptions WHERE voucher_id=?", Integer.class,
                voucherId)).isEqualTo(1);
    }

    @Test
    void checkoutEnforcesVoucherPerUserLimitAcrossSeparateBookings() {
        Fixture fixture = fixtureWithPrices();
        UUID voucherId = voucher("ONCEPERUSER", "FIXED", 10_000L, null, 0L, 3, 1);
        SeatHoldView firstHold = seatHoldManagement.create(fixture.firstUserId(), new SeatHoldCommand(
                fixture.showtimeId(), List.of(fixture.standardSeatId()), "first-per-user-hold"));
        bookingCheckout.checkout(fixture.firstUserId(), new BookingCheckoutCommand(
                firstHold.id(), "first-per-user-checkout", "ONCEPERUSER"));
        SeatHoldView secondHold = seatHoldManagement.create(fixture.firstUserId(), new SeatHoldCommand(
                fixture.showtimeId(), List.of(fixture.coupleSeatOneId(), fixture.coupleSeatTwoId()), "second-per-user-hold"));

        assertThatThrownBy(() -> bookingCheckout.checkout(fixture.firstUserId(), new BookingCheckoutCommand(
                secondHold.id(), "second-per-user-checkout", "ONCEPERUSER")))
                .isInstanceOf(ApplicationException.class)
                .extracting(error -> ((ApplicationException) error).code())
                .isEqualTo("VOUCHER_PER_USER_LIMIT_REACHED");

        assertThat(jdbcTemplate.queryForObject("SELECT usage_count FROM vouchers WHERE id=?", Integer.class, voucherId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM seat_holds WHERE id=?", String.class, secondHold.id())).isEqualTo("ACTIVE");
    }

    @Test
    void checkoutRejectsHoldOwnedByAnotherCustomerWithoutCreatingBooking() {
        Fixture fixture = fixtureWithPrices();
        SeatHoldView hold = seatHoldManagement.create(fixture.firstUserId(), new SeatHoldCommand(
                fixture.showtimeId(), List.of(fixture.standardSeatId()), "hold-owned-by-first"));

        assertThatThrownBy(() -> bookingCheckout.checkout(fixture.secondUserId(), new BookingCheckoutCommand(hold.id(), "checkout-other-user")))
                .isInstanceOf(ApplicationException.class)
                .extracting(error -> ((ApplicationException) error).code())
                .isEqualTo("SEAT_HOLD_FORBIDDEN");

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM bookings WHERE hold_id=?", Integer.class, hold.id())).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM showtime_seats WHERE id=?", String.class, fixture.standardSeatId())).isEqualTo("HELD");
    }

    @Test
    void checkoutRejectsAnExpiredHoldAndLeavesNoPartialBooking() {
        Fixture fixture = fixtureWithPrices();
        SeatHoldView hold = seatHoldManagement.create(fixture.firstUserId(), new SeatHoldCommand(
                fixture.showtimeId(), List.of(fixture.standardSeatId()), "expired-hold-for-checkout"));
        expireHold(hold.id());

        assertThatThrownBy(() -> bookingCheckout.checkout(fixture.firstUserId(), new BookingCheckoutCommand(hold.id(), "expired-checkout")))
                .isInstanceOf(ApplicationException.class)
                .extracting(error -> ((ApplicationException) error).code())
                .isEqualTo("SEAT_HOLD_EXPIRED");

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM bookings WHERE hold_id=?", Integer.class, hold.id())).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM showtime_seats WHERE id=?", String.class, fixture.standardSeatId())).isEqualTo("AVAILABLE");
    }

    @Test
    void checkoutRollsBackBookingAndIdempotencyClaimWhenSeatTransitionFails() {
        Fixture fixture = fixtureWithPrices();
        SeatHoldView hold = seatHoldManagement.create(fixture.firstUserId(), new SeatHoldCommand(
                fixture.showtimeId(), List.of(fixture.standardSeatId()), "hold-for-rollback"));
        jdbcTemplate.execute("""
                CREATE FUNCTION reject_payment_pending_transition() RETURNS trigger AS $$
                BEGIN
                    IF NEW.status = 'PAYMENT_PENDING' THEN
                        RAISE EXCEPTION 'test checkout transition failure';
                    END IF;
                    RETURN NEW;
                END;
                $$ LANGUAGE plpgsql
                """);
        jdbcTemplate.execute("""
                CREATE TRIGGER reject_payment_pending_transition
                BEFORE UPDATE ON showtime_seats
                FOR EACH ROW EXECUTE FUNCTION reject_payment_pending_transition()
                """);
        try {
            assertThatThrownBy(() -> bookingCheckout.checkout(
                    fixture.firstUserId(), new BookingCheckoutCommand(hold.id(), "checkout-rollback")))
                    .isInstanceOf(RuntimeException.class);
        }
        finally {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS reject_payment_pending_transition ON showtime_seats");
            jdbcTemplate.execute("DROP FUNCTION IF EXISTS reject_payment_pending_transition()");
        }

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM bookings WHERE hold_id=?", Integer.class, hold.id())).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM booking_checkout_requests WHERE actor_id=?", Integer.class,
                fixture.firstUserId())).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM seat_holds WHERE id=?", String.class, hold.id())).isEqualTo("ACTIVE");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM showtime_seats WHERE id=?", String.class,
                fixture.standardSeatId())).isEqualTo("HELD");
    }

    @Test
    void issuesExactlyOneTicketForAVerifiedPaidBookingEvenWhenRetriedConcurrently() throws Exception {
        Fixture fixture = fixtureWithPrices();
        SeatHoldView hold = seatHoldManagement.create(fixture.firstUserId(), new SeatHoldCommand(
                fixture.showtimeId(), List.of(fixture.standardSeatId()), "hold-for-ticket"));
        BookingView booking = bookingCheckout.checkout(fixture.firstUserId(), new BookingCheckoutCommand(hold.id(), "checkout-ticket"));
        assertThatThrownBy(() -> ticketIssuer.issueForPaidBooking(booking.id()))
                .isInstanceOf(ApplicationException.class)
                .extracting(error -> ((ApplicationException) error).code())
                .isEqualTo("BOOKING_PAYMENT_NOT_VERIFIED");
        jdbcTemplate.update("UPDATE bookings SET status='PAID' WHERE id=?", booking.id());

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<TicketIssuance> issued;
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<TicketIssuance> first = executor.submit(issue(booking.id(), ready, start));
            Future<TicketIssuance> second = executor.submit(issue(booking.id(), ready, start));
            ready.await();
            start.countDown();
            issued = List.of(first.get(), second.get());
            assertThat(issued).extracting(result -> result.ticket().id()).containsOnly(issued.getFirst().ticket().id());
            assertThat(issued.stream().filter(result -> result.rawQrToken().isPresent())).hasSize(1);
        }
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM tickets WHERE booking_id=?", Integer.class, booking.id())).isEqualTo(1);
        String storedHash = jdbcTemplate.queryForObject("SELECT qr_token_hash FROM tickets WHERE booking_id=?", String.class, booking.id());
        String rawToken = issued.stream().flatMap(result -> result.rawQrToken().stream()).findFirst().orElseThrow();
        assertThat(storedHash).isNotEqualTo(rawToken);
        assertThat(storedHash).hasSize(64);
    }

    private Callable<TicketIssuance> issue(UUID bookingId, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await();
            return ticketIssuer.issueForPaidBooking(bookingId);
        };
    }

    private Callable<String> checkoutVoucher(UUID userId, UUID holdId, String key, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await();
            try {
                bookingCheckout.checkout(userId, new BookingCheckoutCommand(holdId, key, "ONEUSE"));
                return "SUCCESS";
            }
            catch (ApplicationException exception) {
                return exception.code();
            }
        };
    }

    private Fixture fixtureWithPrices() {
        Fixture fixture = fixture();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        price(fixture.showtimeId(), "STANDARD", 90_000L, now);
        price(fixture.showtimeId(), "COUPLE", 180_000L, now);
        return fixture;
    }

    private void price(UUID showtimeId, String seatType, long amount, OffsetDateTime now) {
        jdbcTemplate.update("""
                INSERT INTO showtime_prices (id,showtime_id,seat_type,price,source,created_at,updated_at)
                VALUES (?,?,?,?,'SYSTEM_PROFILE',?,?)
                """, UUID.randomUUID(), showtimeId, seatType, amount, now, now);
    }

    private UUID voucher(
            String code, String discountType, long discountValue, Long maxDiscount, long minimumOrder,
            Integer usageLimit, Integer perUserLimit) {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update("""
                INSERT INTO vouchers (id,code,discount_type,discount_value,max_discount_amount,min_order_amount,usage_limit,
                                      usage_count,per_user_limit,starts_at,ends_at,status,created_at,updated_at)
                VALUES (?,?,?,?,?,?,?,0,?,?,?,'ACTIVE',?,?)
                """, id, code, discountType, discountValue, maxDiscount, minimumOrder, usageLimit, perUserLimit,
                now.minusHours(1), now.plusDays(1), now, now);
        return id;
    }
}
