package com.lak.moviebooking.cinema.infrastructure;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.lak.moviebooking.audit.application.AuditLogWriter;
import com.lak.moviebooking.cinema.application.AuditoriumView;
import com.lak.moviebooking.cinema.application.AuditoriumWriteCommand;
import com.lak.moviebooking.cinema.application.CinemaAdminView;
import com.lak.moviebooking.cinema.application.CinemaAdministration;
import com.lak.moviebooking.cinema.application.CinemaDetail;
import com.lak.moviebooking.cinema.application.CinemaWriteCommand;
import com.lak.moviebooking.cinema.application.SeatView;
import com.lak.moviebooking.cinema.application.SeatWriteCommand;
import com.lak.moviebooking.common.application.error.ApplicationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class JdbcCinemaAdministration implements CinemaAdministration {
    private static final Set<String> ACTIVE_STATUSES = Set.of("ACTIVE", "INACTIVE");
    private static final Set<String> SEAT_TYPES = Set.of("STANDARD", "VIP", "COUPLE");
    private static final Set<String> SEAT_STATUSES = Set.of("ACTIVE", "LOCKED");
    private final JdbcTemplate jdbcTemplate;
    private final AuditLogWriter auditLogWriter;
    private final Clock clock;

    JdbcCinemaAdministration(JdbcTemplate jdbcTemplate, AuditLogWriter auditLogWriter, Clock clock) {
        this.jdbcTemplate = jdbcTemplate; this.auditLogWriter = auditLogWriter; this.clock = clock;
    }

    @Override public List<CinemaAdminView> cinemas(Set<UUID> cinemaIds) {
        if (cinemaIds == null) return jdbcTemplate.query("""
                SELECT c.id, c.name, c.address, c.city, c.timezone, c.status, count(a.id) AS auditorium_count
                FROM cinemas c LEFT JOIN auditoriums a ON a.cinema_id = c.id GROUP BY c.id ORDER BY c.city, c.name
                """, (rs, row) -> cinemaView(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("address"), rs.getString("city"), rs.getString("timezone"), rs.getString("status"), rs.getInt("auditorium_count")));
        if (cinemaIds.isEmpty()) return List.of();
        String placeholders = String.join(",", java.util.Collections.nCopies(cinemaIds.size(), "?"));
        return jdbcTemplate.query("SELECT c.id, c.name, c.address, c.city, c.timezone, c.status, count(a.id) AS auditorium_count "
                + "FROM cinemas c LEFT JOIN auditoriums a ON a.cinema_id = c.id WHERE c.id IN (" + placeholders
                + ") GROUP BY c.id ORDER BY c.city, c.name", (rs, row) -> cinemaView(
                rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("address"), rs.getString("city"),
                rs.getString("timezone"), rs.getString("status"), rs.getInt("auditorium_count")), cinemaIds.toArray());
    }

    @Override @Transactional public CinemaDetail createCinema(UUID actorId, CinemaWriteCommand command) {
        validate(command); UUID id = UUID.randomUUID(); OffsetDateTime now = now();
        jdbcTemplate.update("INSERT INTO cinemas (id,name,address,city,timezone,status,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?)", id, command.name().trim(), command.address().trim(), command.city().trim(), command.timezone().trim(), command.status(), now, now);
        auditLogWriter.record(actorId, "CINEMA_CREATED", "cinema", id, Map.of()); return cinema(id);
    }
    @Override @Transactional public CinemaDetail updateCinema(UUID actorId, UUID id, CinemaWriteCommand command) {
        validate(command); if (jdbcTemplate.update("UPDATE cinemas SET name=?,address=?,city=?,timezone=?,status=?,updated_at=? WHERE id=?", command.name().trim(), command.address().trim(), command.city().trim(), command.timezone().trim(), command.status(), now(), id) != 1) notFound("CINEMA");
        auditLogWriter.record(actorId, "CINEMA_UPDATED", "cinema", id, Map.of("status", command.status())); return cinema(id);
    }
    @Override @Transactional public void deactivateCinema(UUID actorId, UUID id) { if (jdbcTemplate.update("UPDATE cinemas SET status='INACTIVE', updated_at=? WHERE id=?", now(), id) != 1) notFound("CINEMA"); auditLogWriter.record(actorId, "CINEMA_DEACTIVATED", "cinema", id, Map.of()); }
    @Override public List<AuditoriumView> auditoriums(UUID cinemaId) { cinema(cinemaId); return jdbcTemplate.query("SELECT id,cinema_id,name,screen_format,cleanup_minutes,status FROM auditoriums WHERE cinema_id=? ORDER BY name", (rs,row)->auditoriumView(rs.getObject("id",UUID.class),rs.getObject("cinema_id",UUID.class),rs.getString("name"),rs.getString("screen_format"),rs.getInt("cleanup_minutes"),rs.getString("status")), cinemaId); }
    @Override @Transactional public AuditoriumView createAuditorium(UUID actorId, UUID cinemaId, AuditoriumWriteCommand command) { validate(command); cinema(cinemaId); UUID id=UUID.randomUUID(); jdbcTemplate.update("INSERT INTO auditoriums (id,cinema_id,name,screen_format,cleanup_minutes,status,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?)",id,cinemaId,command.name().trim(),command.screenFormat().trim(),command.cleanupMinutes(),command.status(),now(),now()); auditLogWriter.record(actorId,"AUDITORIUM_CREATED","auditorium",id,Map.of("cinemaId",cinemaId.toString())); return auditorium(id); }
    @Override @Transactional public AuditoriumView updateAuditorium(UUID actorId, UUID id, AuditoriumWriteCommand command) { validate(command); ensureLayoutMutable(id); if(jdbcTemplate.update("UPDATE auditoriums SET name=?,screen_format=?,cleanup_minutes=?,status=?,updated_at=? WHERE id=?",command.name().trim(),command.screenFormat().trim(),command.cleanupMinutes(),command.status(),now(),id)!=1) notFound("AUDITORIUM"); auditLogWriter.record(actorId,"AUDITORIUM_UPDATED","auditorium",id,Map.of()); return auditorium(id); }
    @Override @Transactional public void deactivateAuditorium(UUID actorId, UUID id) { ensureLayoutMutable(id); if(jdbcTemplate.update("UPDATE auditoriums SET status='INACTIVE',updated_at=? WHERE id=?",now(),id)!=1) notFound("AUDITORIUM"); auditLogWriter.record(actorId,"AUDITORIUM_DEACTIVATED","auditorium",id,Map.of()); }
    @Override public AuditoriumView auditorium(UUID id) { return jdbcTemplate.query("SELECT id,cinema_id,name,screen_format,cleanup_minutes,status FROM auditoriums WHERE id=?",(rs,row)->auditoriumView(rs.getObject("id",UUID.class),rs.getObject("cinema_id",UUID.class),rs.getString("name"),rs.getString("screen_format"),rs.getInt("cleanup_minutes"),rs.getString("status")),id).stream().findFirst().orElseThrow(()->ApplicationException.notFound("AUDITORIUM_NOT_FOUND","Auditorium was not found")); }
    @Override public List<SeatView> seats(UUID id) { auditorium(id); return jdbcTemplate.query("SELECT id,row_label,seat_number,seat_type,pair_key,status FROM seats WHERE auditorium_id=? ORDER BY row_label,seat_number",(rs,row)->new SeatView(rs.getObject("id",UUID.class),rs.getString("row_label"),rs.getInt("seat_number"),rs.getString("seat_type"),rs.getString("pair_key"),rs.getString("status")),id); }
    @Override @Transactional public List<SeatView> replaceSeats(UUID actorId, UUID id, List<SeatWriteCommand> seats) { ensureLayoutMutable(id); validateSeats(seats); jdbcTemplate.update("DELETE FROM seats WHERE auditorium_id=?",id); for(SeatWriteCommand seat:seats) jdbcTemplate.update("INSERT INTO seats (id,auditorium_id,row_label,seat_number,seat_type,pair_key,status,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),id,seat.rowLabel().trim(),seat.seatNumber(),seat.seatType(),blankToNull(seat.pairKey()),seat.status(),now(),now()); auditLogWriter.record(actorId,"SEAT_LAYOUT_REPLACED","auditorium",id,Map.of("seatCount",Integer.toString(seats.size()))); return seats(id); }
    private CinemaDetail cinema(UUID id){ return jdbcTemplate.query("SELECT id,name,address,city,timezone,status FROM cinemas WHERE id=?",(rs,row)->new CinemaDetail(rs.getObject("id",UUID.class),rs.getString("name"),rs.getString("address"),rs.getString("city"),rs.getString("timezone"),rs.getString("status")),id).stream().findFirst().orElseThrow(()->ApplicationException.notFound("CINEMA_NOT_FOUND","Cinema was not found")); }
    private CinemaAdminView cinemaView(UUID id,String name,String address,String city,String timezone,String status,int count){return new CinemaAdminView(id,name,address,city,timezone,status,count);}
    private AuditoriumView auditoriumView(UUID id,UUID cinemaId,String name,String format,int cleanup,String status){return new AuditoriumView(id,cinemaId,name,format,cleanup,status);}
    private void ensureLayoutMutable(UUID auditoriumId){ auditorium(auditoriumId); if(tableExists("showtimes") && Boolean.TRUE.equals(jdbcTemplate.queryForObject("SELECT EXISTS (SELECT 1 FROM showtimes WHERE auditorium_id=?)",Boolean.class,auditoriumId))) throw ApplicationException.businessRule("LAYOUT_LOCKED_BY_SHOWTIME","Auditorium layout cannot change after showtimes are created"); }
    private boolean tableExists(String table){return Boolean.TRUE.equals(jdbcTemplate.queryForObject("SELECT to_regclass(?) IS NOT NULL",Boolean.class,table));}
    private void validate(CinemaWriteCommand c){if(blank(c.name())||blank(c.address())||blank(c.city())||blank(c.timezone())||!ACTIVE_STATUSES.contains(c.status())) throw ApplicationException.businessRule("INVALID_CINEMA","Cinema data is invalid");}
    private void validate(AuditoriumWriteCommand a){if(blank(a.name())||blank(a.screenFormat())||a.cleanupMinutes()<0||!ACTIVE_STATUSES.contains(a.status())) throw ApplicationException.businessRule("INVALID_AUDITORIUM","Auditorium data is invalid");}
    private void validateSeats(List<SeatWriteCommand> seats){Map<String,Integer> pairs=new HashMap<>(); Set<String> positions=new java.util.HashSet<>(); for(SeatWriteCommand s:seats){if(blank(s.rowLabel())||s.seatNumber()<=0||!SEAT_TYPES.contains(s.seatType())||!SEAT_STATUSES.contains(s.status())||!positions.add(s.rowLabel().trim()+"#"+s.seatNumber())) throw ApplicationException.businessRule("INVALID_SEAT_LAYOUT","Seat layout has invalid or duplicate positions"); if("COUPLE".equals(s.seatType())){if(blank(s.pairKey())) throw ApplicationException.businessRule("INVALID_SEAT_LAYOUT","Couple seats require a pair key");pairs.merge(s.pairKey().trim(),1,Integer::sum);}else if(!blank(s.pairKey())) throw ApplicationException.businessRule("INVALID_SEAT_LAYOUT","Only couple seats may have a pair key");} if(pairs.values().stream().anyMatch(count->count!=2)) throw ApplicationException.businessRule("INVALID_SEAT_LAYOUT","Every couple-seat pair must contain exactly two seats");}
    private void notFound(String entity){throw ApplicationException.notFound(entity+"_NOT_FOUND",entity.substring(0,1)+entity.substring(1).toLowerCase()+" was not found");}
    private OffsetDateTime now(){return clock.instant().atOffset(ZoneOffset.UTC);} private boolean blank(String v){return v==null||v.isBlank();} private String blankToNull(String v){return blank(v)?null:v.trim();}
}
