package com.lak.moviebooking.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.Cookie;

import com.lak.moviebooking.identity.application.AccessTokenIssuer;
import com.lak.moviebooking.payment.application.PaymentManagement;
import com.lak.moviebooking.refund.application.RefundManagement;
import com.lak.moviebooking.reservation.application.SeatHoldManagement;
import com.lak.moviebooking.support.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Exercises the revenue journey through the HTTP boundary against PostgreSQL and Redis Testcontainers.
 * Focused component tests cover the isolated Redis/WebSocket failure behavior used by this journey.
 */
@AutoConfigureMockMvc
class RevenueJourneyE2EIT extends AbstractIntegrationTest {

    private static final String SANDBOX_SECRET = "local-development-secret-must-be-replaced";
    private static final UUID TICKET_STAFF_ROLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private AccessTokenIssuer accessTokens;
    @Autowired private SeatHoldManagement seatHolds;
    @Autowired private PaymentManagement payments;
    @Autowired private RefundManagement refunds;

    @Test
    void customerLogsInRefreshesAndCompletesTheRevenueJourneyThroughTicketScan() throws Exception {
        Customer customer = registerLoginAndRefresh("journey-customer");
        Fixture fixture = fixture();

        mockMvc.perform(get("/api/showtimes")
                        .param("movieId", fixture.movieId().toString())
                        .param("cinemaId", fixture.cinemaId().toString())
                        .param("date", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(fixture.showtimeId().toString()));
        mockMvc.perform(get("/api/showtimes/{showtimeId}/seats", fixture.showtimeId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seats[0].id").value(fixture.showtimeSeatId().toString()))
                .andExpect(jsonPath("$.seats[0].status").value("AVAILABLE"));

        JsonNode hold = postJson("/api/seat-holds", customer.accessToken(), "hold-journey", """
                {"showtimeId":"%s","showtimeSeatIds":["%s"]}
                """.formatted(fixture.showtimeId(), fixture.showtimeSeatId()), 201);
        JsonNode heldAgain = postJson("/api/seat-holds", customer.accessToken(), "hold-journey", """
                {"showtimeId":"%s","showtimeSeatIds":["%s"]}
                """.formatted(fixture.showtimeId(), fixture.showtimeSeatId()), 201);
        assertThat(heldAgain.path("id").asText()).isEqualTo(hold.path("id").asText());

        JsonNode booking = postJson("/api/bookings/checkout", customer.accessToken(), "checkout-journey", """
                {"holdId":"%s"}
                """.formatted(hold.path("id").asText()), 201);
        JsonNode checkedOutAgain = postJson("/api/bookings/checkout", customer.accessToken(), "checkout-journey", """
                {"holdId":"%s"}
                """.formatted(hold.path("id").asText()), 201);
        assertThat(checkedOutAgain.path("id").asText()).isEqualTo(booking.path("id").asText());
        assertThat(booking.path("totalAmount").asLong()).isEqualTo(90_000L);

        JsonNode payment = postJson("/api/payments", customer.accessToken(), null, """
                {"bookingCode":"%s","provider":"sandbox"}
                """.formatted(booking.path("bookingCode").asText()), 201);
        JsonNode paymentReplay = postJson("/api/payments", customer.accessToken(), null, """
                {"bookingCode":"%s","provider":"sandbox"}
                """.formatted(booking.path("bookingCode").asText()), 201);
        assertThat(paymentReplay.path("id").asText()).isEqualTo(payment.path("id").asText());

        String webhook = webhookPayload(payment.path("id").asText(), UUID.randomUUID().toString(),
                transaction(UUID.fromString(payment.path("id").asText())), payment.path("amount").asLong());
        sendWebhook(webhook);
        sendWebhook(webhook);

        mockMvc.perform(get("/api/bookings/{bookingCode}", booking.path("bookingCode").asText())
                        .header("Authorization", bearer(customer.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
        JsonNode history = getJson("/api/me/bookings", customer.accessToken());
        String ticketCode = history.get(0).path("ticketCode").asText();
        JsonNode ticket = getJson("/api/tickets/" + ticketCode, customer.accessToken());
        String qrPayload = ticket.path("qrPayload").asText();
        assertThat(qrPayload).isNotBlank().isNotEqualTo(ticketCode);

        makeShowtimeScannable(fixture.showtimeId());
        String staffToken = staffToken(fixture.cinemaId());
        JsonNode firstScan = postJson("/api/tickets/validate", staffToken, null,
                "{\"code\":\"%s\"}".formatted(qrPayload), 200);
        JsonNode replayScan = postJson("/api/tickets/validate", staffToken, null,
                "{\"code\":\"%s\"}".formatted(qrPayload), 200);
        assertThat(firstScan.path("result").asText()).isEqualTo("VALID");
        assertThat(replayScan.path("result").asText()).isEqualTo("USED");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM ticket_scan_logs scan
                JOIN tickets ticket ON ticket.id=scan.ticket_id
                WHERE ticket.ticket_code=?
                """, Integer.class, ticketCode))
                .isEqualTo(1);
    }

    @Test
    void preservesRevenueIntegrityForConflictExpiryLatePaymentRefundAndShowtimeCancellation() throws Exception {
        Customer owner = registerLoginAndRefresh("failure-owner");
        Customer otherCustomer = registerLoginAndRefresh("failure-other");
        Fixture conflictFixture = fixture();
        JsonNode firstHold = postJson("/api/seat-holds", owner.accessToken(), "conflict-owner", """
                {"showtimeId":"%s","showtimeSeatIds":["%s"]}
                """.formatted(conflictFixture.showtimeId(), conflictFixture.showtimeSeatId()), 201);
        mockMvc.perform(post("/api/seat-holds")
                        .header("Authorization", bearer(otherCustomer.accessToken()))
                        .header("Idempotency-Key", "conflict-other")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"showtimeId":"%s","showtimeSeatIds":["%s"]}
                                """.formatted(conflictFixture.showtimeId(), conflictFixture.showtimeSeatId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SEAT_UNAVAILABLE"));

        expireHold(UUID.fromString(firstHold.path("id").asText()));
        assertThat(seatHolds.expireDueHolds(Instant.now())).isEqualTo(1);
        JsonNode secondHold = postJson("/api/seat-holds", otherCustomer.accessToken(), "expired-seat-retry", """
                {"showtimeId":"%s","showtimeSeatIds":["%s"]}
                """.formatted(conflictFixture.showtimeId(), conflictFixture.showtimeSeatId()), 201);
        JsonNode lateBooking = checkout(otherCustomer, secondHold, "late-payment");
        JsonNode latePayment = createPayment(otherCustomer, lateBooking);
        expirePayment(UUID.fromString(lateBooking.path("id").asText()), UUID.fromString(latePayment.path("id").asText()));
        assertThat(payments.expireDuePayments(Instant.now())).isEqualTo(1);
        sendWebhook(webhookPayload(latePayment.path("id").asText(), UUID.randomUUID().toString(),
                transaction(UUID.fromString(latePayment.path("id").asText())), latePayment.path("amount").asLong()));
        mockMvc.perform(get("/api/payments/{paymentId}/status", latePayment.path("id").asText())
                        .header("Authorization", bearer(otherCustomer.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingStatus").value("PAYMENT_REVIEW"));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM tickets WHERE booking_id=?", Integer.class,
                UUID.fromString(lateBooking.path("id").asText()))).isZero();

        Fixture refundFixture = fixture();
        JsonNode refundableBooking = paidBooking(owner, refundFixture, "customer-refund");
        JsonNode refund = postJson("/api/bookings/%s/refunds".formatted(refundableBooking.path("id").asText()),
                owner.accessToken(), "customer-refund", "", 202);
        JsonNode refundReplay = postJson("/api/bookings/%s/refunds".formatted(refundableBooking.path("id").asText()),
                owner.accessToken(), "customer-refund", "", 202);
        assertThat(refundReplay.path("id").asText()).isEqualTo(refund.path("id").asText());
        assertThat(refund.path("amount").asLong()).isEqualTo(90_000L);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM tickets WHERE booking_id=?", String.class,
                UUID.fromString(refundableBooking.path("id").asText()))).isEqualTo("CANCELLED");
        assertThat(refunds.processRequestedRefunds(Instant.now())).isEqualTo(1);
        mockMvc.perform(get("/api/refunds/{refundId}", refund.path("id").asText())
                        .header("Authorization", bearer(owner.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));

        Fixture cancelledFixture = fixture();
        JsonNode cancelledBooking = paidBooking(owner, cancelledFixture, "showtime-cancel");
        mockMvc.perform(delete("/api/admin/showtimes/{showtimeId}", cancelledFixture.showtimeId())
                        .header("Authorization", bearer(managerToken(cancelledFixture.cinemaId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM outbox_events WHERE event_type='refund.showtime_cancellation_requested' AND aggregate_id=?",
                Integer.class, cancelledFixture.showtimeId())).isEqualTo(1);
        refunds.processShowtimeCancellation(cancelledFixture.showtimeId(), Instant.now());
        assertThat(refunds.processRequestedRefunds(Instant.now())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM bookings WHERE id=?", String.class,
                UUID.fromString(cancelledBooking.path("id").asText()))).isEqualTo("REFUNDED");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM tickets WHERE booking_id=?", String.class,
                UUID.fromString(cancelledBooking.path("id").asText()))).isEqualTo("CANCELLED");
    }

    private Customer registerLoginAndRefresh(String prefix) throws Exception {
        String email = prefix + "-" + UUID.randomUUID() + "@example.test";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"E2E Customer","email":"%s","password":"a-secure-password"}
                                """.formatted(email)))
                .andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"a-secure-password"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refreshCookie = login.getResponse().getCookie("lak_refresh_token");
        Cookie csrfCookie = login.getResponse().getCookie("lak_csrf_token");
        assertThat(refreshCookie).isNotNull();
        assertThat(csrfCookie).isNotNull();
        MvcResult refreshed = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie, csrfCookie)
                        .header("X-CSRF-Token", csrfCookie.getValue()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode session = objectMapper.readTree(refreshed.getResponse().getContentAsString());
        return new Customer(session.path("user").path("id").asText(), session.path("accessToken").asText());
    }

    private JsonNode paidBooking(Customer customer, Fixture fixture, String key) throws Exception {
        JsonNode hold = postJson("/api/seat-holds", customer.accessToken(), "hold-" + key, """
                {"showtimeId":"%s","showtimeSeatIds":["%s"]}
                """.formatted(fixture.showtimeId(), fixture.showtimeSeatId()), 201);
        JsonNode booking = checkout(customer, hold, key);
        JsonNode payment = createPayment(customer, booking);
        sendWebhook(webhookPayload(payment.path("id").asText(), UUID.randomUUID().toString(),
                transaction(UUID.fromString(payment.path("id").asText())), payment.path("amount").asLong()));
        return booking;
    }

    private JsonNode checkout(Customer customer, JsonNode hold, String key) throws Exception {
        return postJson("/api/bookings/checkout", customer.accessToken(), "checkout-" + key,
                "{\"holdId\":\"%s\"}".formatted(hold.path("id").asText()), 201);
    }

    private JsonNode createPayment(Customer customer, JsonNode booking) throws Exception {
        return postJson("/api/payments", customer.accessToken(), null,
                "{\"bookingCode\":\"%s\",\"provider\":\"sandbox\"}".formatted(booking.path("bookingCode").asText()), 201);
    }

    private JsonNode postJson(String path, String token, String idempotencyKey, String body, int expectedStatus) throws Exception {
        MockHttpServletRequestBuilder request = post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (token != null) request.header("Authorization", bearer(token));
        if (idempotencyKey != null) request.header("Idempotency-Key", idempotencyKey);
        MvcResult result = mockMvc.perform(request).andExpect(status().is(expectedStatus)).andReturn();
        String responseBody = result.getResponse().getContentAsString();
        return responseBody.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(responseBody);
    }

    private JsonNode getJson(String path, String token) throws Exception {
        MvcResult result = mockMvc.perform(get(path).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private void sendWebhook(String payload) throws Exception {
        mockMvc.perform(post("/api/payments/sandbox/webhook")
                        .header("X-Payment-Signature", sign(payload))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNoContent());
    }

    private Fixture fixture() {
        UUID cinemaId = UUID.randomUUID();
        UUID auditoriumId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();
        UUID showtimeId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        UUID showtimeSeatId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Instant startAt = Instant.now().plusSeconds(86_400);
        jdbcTemplate.update("INSERT INTO cinemas (id,name,address,city,timezone,status,created_at,updated_at) VALUES (?,?,?,?,?,'ACTIVE',?,?)",
                cinemaId, "E2E Cinema " + cinemaId, "1 Test Street", "HCM", "Asia/Ho_Chi_Minh", now, now);
        jdbcTemplate.update("INSERT INTO auditoriums (id,cinema_id,name,screen_format,cleanup_minutes,status,created_at,updated_at) VALUES (?,?,?,'2D',0,'ACTIVE',?,?)",
                auditoriumId, cinemaId, "E2E Room", now, now);
        jdbcTemplate.update("INSERT INTO seats (id,auditorium_id,row_label,seat_number,seat_type,status,created_at,updated_at) VALUES (?,?, 'A',1,'STANDARD','ACTIVE',?,?)",
                seatId, auditoriumId, now, now);
        jdbcTemplate.update("INSERT INTO movies (id,title,duration_minutes,age_rating,release_date,status,created_at,updated_at) VALUES (?,?,90,'P',?,'NOW_SHOWING',?,?)",
                movieId, "E2E Movie " + movieId, LocalDate.now(), now, now);
        jdbcTemplate.update("INSERT INTO showtimes (id,movie_id,auditorium_id,start_at,end_at,sales_close_at,status,created_at,updated_at) VALUES (?,?,?,?,?,?,'SCHEDULED',?,?)",
                showtimeId, movieId, auditoriumId, atUtc(startAt), atUtc(startAt.plusSeconds(5_400)), atUtc(startAt.minusSeconds(300)), now, now);
        jdbcTemplate.update("INSERT INTO showtime_seats (id,showtime_id,seat_id,status,created_at,updated_at) VALUES (?,?,?,'AVAILABLE',?,?)",
                showtimeSeatId, showtimeId, seatId, now, now);
        jdbcTemplate.update("INSERT INTO showtime_prices (id,showtime_id,seat_type,price,source,created_at,updated_at) VALUES (?,?, 'STANDARD',90000,'SYSTEM_PROFILE',?,?)",
                UUID.randomUUID(), showtimeId, now, now);
        return new Fixture(cinemaId, movieId, showtimeId, showtimeSeatId);
    }

    private void expireHold(UUID holdId) {
        OffsetDateTime expired = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1);
        jdbcTemplate.update("UPDATE seat_holds SET created_at=?,expires_at=?,updated_at=? WHERE id=?",
                expired.minusMinutes(5), expired, expired, holdId);
    }

    private void expirePayment(UUID bookingId, UUID paymentId) {
        OffsetDateTime expired = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1);
        jdbcTemplate.update("UPDATE bookings SET payment_deadline=?,hard_deadline=?,updated_at=? WHERE id=?",
                expired.minusMinutes(2), expired, expired, bookingId);
        jdbcTemplate.update("UPDATE payments SET expires_at=?,updated_at=? WHERE id=?", expired, expired, paymentId);
    }

    private void makeShowtimeScannable(UUID showtimeId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update("UPDATE showtimes SET start_at=?,end_at=?,sales_close_at=?,updated_at=? WHERE id=?",
                now.minusMinutes(10), now.plusHours(1), now.minusMinutes(15), now, showtimeId);
    }

    private String staffToken(UUID cinemaId) {
        return scopedStaffToken(cinemaId, "TICKET_STAFF", TICKET_STAFF_ROLE_ID);
    }

    private String managerToken(UUID cinemaId) {
        return scopedStaffToken(cinemaId, "CINEMA_MANAGER", UUID.fromString("00000000-0000-0000-0000-000000000103"));
    }

    private String scopedStaffToken(UUID cinemaId, String role, UUID roleId) {
        UUID userId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update("INSERT INTO users (id,email,password_hash,full_name,status,created_at,updated_at) VALUES (?,?,?,'E2E Staff','ACTIVE',?,?)",
                userId, role.toLowerCase() + "-" + userId + "@example.test", "not-used", now, now);
        jdbcTemplate.update("INSERT INTO user_roles (user_id,role_id,assigned_at) VALUES (?,?,?)", userId, roleId, now);
        jdbcTemplate.update("INSERT INTO staff_cinema_assignments (user_id,cinema_id,assigned_at) VALUES (?,?,?)", userId, cinemaId, now);
        return accessTokens.issue(userId, Set.of(role), Instant.now());
    }

    private String transaction(UUID paymentId) {
        return jdbcTemplate.queryForObject("SELECT provider_transaction_id FROM payments WHERE id=?", String.class, paymentId);
    }

    private String webhookPayload(String paymentId, String eventId, String transactionId, long amount) {
        return """
                {"paymentId":"%s","eventId":"%s","transactionId":"%s","amount":%d,"currency":"VND","status":"SUCCESS","paidAt":"%s"}
                """.formatted(paymentId, eventId, transactionId, amount, Instant.now());
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

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private OffsetDateTime atUtc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private record Customer(String id, String accessToken) { }
    private record Fixture(UUID cinemaId, UUID movieId, UUID showtimeId, UUID showtimeSeatId) { }
}
