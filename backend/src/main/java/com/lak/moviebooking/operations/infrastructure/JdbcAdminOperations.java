package com.lak.moviebooking.operations.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.lak.moviebooking.audit.application.AuditLogWriter;
import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.operations.application.AdminBookingView;
import com.lak.moviebooking.operations.application.AdminOperations;
import com.lak.moviebooking.operations.application.AdminPaymentView;
import com.lak.moviebooking.operations.application.AdminRefundView;
import com.lak.moviebooking.operations.application.AdminUserView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class JdbcAdminOperations implements AdminOperations {
    private final JdbcTemplate jdbc; private final AuditLogWriter audit; private final Clock clock;
    JdbcAdminOperations(JdbcTemplate jdbc, AuditLogWriter audit, Clock clock) { this.jdbc = jdbc; this.audit = audit; this.clock = clock; }

    @Override public List<AdminBookingView> bookings(String query, String status, UUID cinemaId, LocalDate date, Set<UUID> scope) {
        return jdbc.query("""
                SELECT b.id,b.booking_code,u.full_name,u.email,c.id cinema_id,c.name cinema_name,m.title,st.start_at,b.total_amount,b.status,b.created_at
                FROM bookings b JOIN users u ON u.id=b.user_id JOIN showtimes st ON st.id=b.showtime_id JOIN auditoriums a ON a.id=st.auditorium_id JOIN cinemas c ON c.id=a.cinema_id JOIN movies m ON m.id=st.movie_id
                WHERE (%s) AND (? IS NULL OR b.status=?) AND (? IS NULL OR c.id=?) AND (? IS NULL OR (st.start_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date=?)
                AND (? IS NULL OR b.booking_code ILIKE ? OR u.email ILIKE ? OR COALESCE(u.phone,'') ILIKE ?) ORDER BY b.created_at DESC LIMIT 200""".formatted(scopeSql(scope)), this::booking,
                scopeArray(scope),scopeArray(scope), status,status,cinemaId,cinemaId,date,date, blank(query),like(query),like(query),like(query)); }
    @Override public List<AdminPaymentView> payments(String query, String status, UUID cinemaId, LocalDate date, Set<UUID> scope) {
        return jdbc.query("""
                SELECT p.id,b.booking_code,c.id cinema_id,c.name cinema_name,p.provider,p.provider_transaction_id,p.amount,p.status,p.created_at,p.paid_at
                FROM payments p JOIN bookings b ON b.id=p.booking_id JOIN showtimes st ON st.id=b.showtime_id JOIN auditoriums a ON a.id=st.auditorium_id JOIN cinemas c ON c.id=a.cinema_id
                WHERE (%s) AND (? IS NULL OR p.status=?) AND (? IS NULL OR c.id=?) AND (? IS NULL OR (p.created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date=?)
                AND (? IS NULL OR b.booking_code ILIKE ? OR p.provider_transaction_id ILIKE ?) ORDER BY p.created_at DESC LIMIT 200""".formatted(scopeSql(scope)), this::payment,
                scopeArray(scope),scopeArray(scope), status,status,cinemaId,cinemaId,date,date,blank(query),like(query),like(query)); }
    @Override public List<AdminRefundView> refunds(String status, UUID cinemaId, LocalDate date, Set<UUID> scope) {
        return jdbc.query("""
                SELECT r.id,b.booking_code,c.id cinema_id,c.name cinema_name,r.amount,r.reason,r.status,r.attempt_count,r.requested_at,r.refunded_at
                FROM refunds r JOIN bookings b ON b.id=r.booking_id JOIN showtimes st ON st.id=b.showtime_id JOIN auditoriums a ON a.id=st.auditorium_id JOIN cinemas c ON c.id=a.cinema_id
                WHERE (%s) AND (? IS NULL OR r.status=?) AND (? IS NULL OR c.id=?) AND (? IS NULL OR (r.requested_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date=?) ORDER BY r.requested_at DESC LIMIT 200""".formatted(scopeSql(scope)), this::refund,
                scopeArray(scope),scopeArray(scope), status,status,cinemaId,cinemaId,date,date); }
    @Override public List<AdminUserView> users(String query) {
        return jdbc.query("""
                SELECT u.id,u.full_name,u.email,u.phone,u.status,u.created_at,
                COALESCE(array_agg(DISTINCT role.code) FILTER (WHERE role.code IS NOT NULL),'{}') roles,
                COALESCE(array_agg(DISTINCT c.name) FILTER (WHERE c.name IS NOT NULL),'{}') cinemas
                FROM users u LEFT JOIN user_roles ur ON ur.user_id=u.id LEFT JOIN roles role ON role.id=ur.role_id
                LEFT JOIN staff_cinema_assignments sca ON sca.user_id=u.id LEFT JOIN cinemas c ON c.id=sca.cinema_id
                WHERE (? IS NULL OR u.email ILIKE ? OR u.full_name ILIKE ? OR COALESCE(u.phone,'') ILIKE ?) GROUP BY u.id ORDER BY u.created_at DESC LIMIT 200""", this::user, blank(query),like(query),like(query),like(query)); }
    @Override public AdminRefundView refund(UUID refundId, Set<UUID> scope) { return refunds(null,null,null,scope).stream().filter(value -> value.id().equals(refundId)).findFirst().orElseThrow(() -> ApplicationException.notFound("ADMIN_REFUND_NOT_FOUND", "Refund was not found")); }
    @Override @Transactional public void retryFailedRefund(UUID actorId, UUID refundId, Set<UUID> scope) {
        AdminRefundView value=refund(refundId,scope); Instant now=clock.instant(); int updated=jdbc.update("UPDATE refunds SET status='REQUESTED',next_attempt_at=?,last_error_code=NULL,updated_at=? WHERE id=? AND status='REFUND_FAILED'", at(now),at(now),refundId);
        if(updated!=1) throw ApplicationException.businessRule("REFUND_RETRY_INVALID","Refund is not eligible for retry");
        audit.record(actorId,"REFUND_MANUAL_RETRY_REQUESTED","refund",refundId,Map.of("bookingCode",value.bookingCode())); }
    @Override @Transactional public void setUserLocked(UUID actorId, UUID userId, boolean locked) {
        if(actorId.equals(userId)) throw ApplicationException.businessRule("SELF_ACCOUNT_LOCK","You cannot change your own account status");
        Instant now=clock.instant(); int updated=jdbc.update("UPDATE users SET status=?,updated_at=? WHERE id=? AND status<>?",locked?"LOCKED":"ACTIVE",at(now),userId,locked?"LOCKED":"ACTIVE");
        if(updated!=1) throw ApplicationException.businessRule("USER_STATUS_UNCHANGED","User status cannot be changed");
        if(locked) jdbc.update("UPDATE refresh_tokens SET revoked_at=?,revocation_reason='REUSE_DETECTED',updated_at=? WHERE user_id=? AND revoked_at IS NULL",at(now),at(now),userId);
        audit.record(actorId,locked?"USER_LOCKED":"USER_UNLOCKED","user",userId,Map.of()); }
    private String scopeSql(Set<UUID> scope){return "(? = '{}' OR c.id = ANY(CAST(? AS uuid[])))";} private String scopeArray(Set<UUID> scope){return "{"+scope.stream().map(UUID::toString).collect(java.util.stream.Collectors.joining(","))+"}";} private String blank(String s){return s==null||s.isBlank()?null:s.trim();} private String like(String s){return "%"+(s==null?"":s.trim())+"%";}
    private AdminBookingView booking(ResultSet r,int n)throws SQLException{return new AdminBookingView(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getString(4),r.getObject(5,UUID.class),r.getString(6),r.getString(7),instant(r,8),r.getLong(9),r.getString(10),instant(r,11));}
    private AdminPaymentView payment(ResultSet r,int n)throws SQLException{return new AdminPaymentView(r.getObject(1,UUID.class),r.getString(2),r.getObject(3,UUID.class),r.getString(4),r.getString(5),r.getString(6),r.getLong(7),r.getString(8),instant(r,9),optional(r,10));}
    private AdminRefundView refund(ResultSet r,int n)throws SQLException{return new AdminRefundView(r.getObject(1,UUID.class),r.getString(2),r.getObject(3,UUID.class),r.getString(4),r.getLong(5),r.getString(6),r.getString(7),r.getInt(8),instant(r,9),optional(r,10));}
    private AdminUserView user(ResultSet r,int n)throws SQLException{return new AdminUserView(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getString(4),r.getString(5),List.of((String[])r.getArray(7).getArray()),List.of((String[])r.getArray(8).getArray()),instant(r,6));}
    private Instant instant(ResultSet r,int i)throws SQLException{return r.getObject(i,OffsetDateTime.class).toInstant();} private Instant optional(ResultSet r,int i)throws SQLException{OffsetDateTime v=r.getObject(i,OffsetDateTime.class);return v==null?null:v.toInstant();} private OffsetDateTime at(Instant v){return v.atOffset(ZoneOffset.UTC);}
}
