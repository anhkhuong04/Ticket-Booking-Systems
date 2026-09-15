package com.lak.moviebooking.showtime.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.lak.moviebooking.audit.application.AuditLogWriter;
import com.lak.moviebooking.catalog.application.MovieCatalogQuery;
import com.lak.moviebooking.cinema.application.AuditoriumForShowtime;
import com.lak.moviebooking.cinema.application.CinemaShowtimeQuery;
import com.lak.moviebooking.cinema.application.SeatForShowtime;
import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.showtime.application.PriceProfileCommand;
import com.lak.moviebooking.showtime.application.PriceProfileView;
import com.lak.moviebooking.showtime.application.PriceRuleCommand;
import com.lak.moviebooking.showtime.application.PriceRuleView;
import com.lak.moviebooking.showtime.application.ShowtimeCreateCommand;
import com.lak.moviebooking.showtime.application.ShowtimeManagement;
import com.lak.moviebooking.showtime.application.ShowtimeSeatMap;
import com.lak.moviebooking.showtime.application.ShowtimeSeatView;
import com.lak.moviebooking.showtime.application.ShowtimeView;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class JdbcShowtimeManagement implements ShowtimeManagement {
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<String> PROFILE_STATUSES = Set.of("ACTIVE", "INACTIVE");
    private static final Set<String> DAY_TYPES = Set.of("ANY", "WEEKDAY", "WEEKEND");
    private final JdbcTemplate jdbcTemplate;
    private final MovieCatalogQuery movieCatalogQuery;
    private final CinemaShowtimeQuery cinemaShowtimeQuery;
    private final AuditLogWriter auditLogWriter;
    private final Clock clock;
    JdbcShowtimeManagement(JdbcTemplate jdbcTemplate, MovieCatalogQuery movieCatalogQuery, CinemaShowtimeQuery cinemaShowtimeQuery, AuditLogWriter auditLogWriter, Clock clock) { this.jdbcTemplate=jdbcTemplate; this.movieCatalogQuery=movieCatalogQuery; this.cinemaShowtimeQuery=cinemaShowtimeQuery; this.auditLogWriter=auditLogWriter; this.clock=clock; }

    @Override @Transactional public PriceProfileView createPriceProfile(UUID actorId, PriceProfileCommand command) {
        if (blank(command.name()) || command.effectiveFrom()==null || !PROFILE_STATUSES.contains(command.status()) || (command.effectiveTo()!=null && command.effectiveTo().isBefore(command.effectiveFrom()))) throw ApplicationException.businessRule("INVALID_PRICE_PROFILE","Price profile data is invalid");
        UUID id=UUID.randomUUID(); jdbcTemplate.update("INSERT INTO price_profiles (id,cinema_id,name,effective_from,effective_to,status,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?)",id,command.cinemaId(),command.name().trim(),command.effectiveFrom(),command.effectiveTo(),command.status(),now(),now()); auditLogWriter.record(actorId,"PRICE_PROFILE_CREATED","price_profile",id,Map.of("scope",command.cinemaId()==null?"SYSTEM":"CINEMA")); return new PriceProfileView(id,command.cinemaId(),command.name().trim(),command.effectiveFrom(),command.effectiveTo(),command.status());
    }
    @Override @Transactional public PriceRuleView createPriceRule(UUID actorId, UUID profileId, PriceRuleCommand command) {
        if (!DAY_TYPES.contains(command.dayType()) || command.amount()<=0 || command.priority()<0 || (command.timeFrom()==null)!=(command.timeTo()==null) || (command.timeFrom()!=null && !command.timeFrom().isBefore(command.timeTo()))) throw ApplicationException.businessRule("INVALID_PRICE_RULE","Price rule data is invalid");
        if (!Boolean.TRUE.equals(jdbcTemplate.queryForObject("SELECT EXISTS (SELECT 1 FROM price_profiles WHERE id=?)",Boolean.class,profileId))) throw ApplicationException.notFound("PRICE_PROFILE_NOT_FOUND","Price profile was not found");
        UUID id=UUID.randomUUID(); jdbcTemplate.update("INSERT INTO price_rules (id,profile_id,day_type,time_from,time_to,screen_format,seat_type,amount,priority,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?)",id,profileId,command.dayType(),command.timeFrom(),command.timeTo(),nullable(command.screenFormat()),nullable(command.seatType()),command.amount(),command.priority(),now(),now()); auditLogWriter.record(actorId,"PRICE_RULE_CREATED","price_rule",id,Map.of("profileId",profileId.toString())); return new PriceRuleView(id,profileId,command.dayType(),command.timeFrom(),command.timeTo(),nullable(command.screenFormat()),nullable(command.seatType()),command.amount(),command.priority());
    }
    @Override public java.util.Optional<UUID> findPriceProfileCinemaId(UUID profileId) {
        List<UUID> cinemaIds = jdbcTemplate.query("SELECT cinema_id FROM price_profiles WHERE id=?", (rs, row) -> rs.getObject("cinema_id", UUID.class), profileId);
        if (cinemaIds.isEmpty()) throw ApplicationException.notFound("PRICE_PROFILE_NOT_FOUND", "Price profile was not found");
        return java.util.Optional.ofNullable(cinemaIds.getFirst());
    }
    @Override @Transactional public ShowtimeView createShowtime(UUID actorId, ShowtimeCreateCommand command) {
        if (command.startAt()==null || command.startAt().isBefore(clock.instant().plusSeconds(300))) throw ApplicationException.businessRule("INVALID_SHOWTIME_START","Showtime must start at least five minutes in the future");
        var movie=movieCatalogQuery.findMovie(command.movieId()); AuditoriumForShowtime auditorium=cinemaShowtimeQuery.findAuditorium(command.auditoriumId()); List<SeatForShowtime> seats=cinemaShowtimeQuery.findSeats(command.auditoriumId()); if(seats.isEmpty()) throw ApplicationException.businessRule("EMPTY_AUDITORIUM","Auditorium has no seats");
        Instant endAt=command.startAt().plusSeconds((long)(movie.durationMinutes()+auditorium.cleanupMinutes())*60); Instant salesCloseAt=command.startAt().minusSeconds(300); Map<String,ResolvedPrice> prices=resolvePrices(auditorium, seats, command.startAt(), command.priceOverrides() == null ? Map.of() : command.priceOverrides()); UUID id=UUID.randomUUID();
        try { jdbcTemplate.update("INSERT INTO showtimes (id,movie_id,auditorium_id,start_at,end_at,sales_close_at,status,created_at,updated_at) VALUES (?,?,?,?,?,?, 'SCHEDULED',?,?)",id,command.movieId(),command.auditoriumId(),atUtc(command.startAt()),atUtc(endAt),atUtc(salesCloseAt),now(),now()); }
        catch (DataIntegrityViolationException exception) { throw ApplicationException.conflict("SHOWTIME_CONFLICT","Auditorium already has an overlapping showtime"); }
        for (ResolvedPrice price:prices.values()) jdbcTemplate.update("INSERT INTO showtime_prices (id,showtime_id,seat_type,price,source,created_at,updated_at) VALUES (?,?,?,?,?,?,?)",UUID.randomUUID(),id,price.seatType(),price.amount(),price.source(),now(),now());
        for (SeatForShowtime seat:seats) jdbcTemplate.update("INSERT INTO showtime_seats (id,showtime_id,seat_id,status,created_at,updated_at) VALUES (?,?,?,?,?,?)",UUID.randomUUID(),id,seat.id(),"LOCKED".equals(seat.status())?"BLOCKED":"AVAILABLE",now(),now());
        auditLogWriter.record(actorId,"SHOWTIME_CREATED","showtime",id,Map.of("cinemaId",auditorium.cinemaId().toString())); return new ShowtimeView(id,command.movieId(),auditorium.cinemaId(),auditorium.cinemaName(),auditorium.cinemaAddress(),auditorium.screenFormat(),command.startAt(),endAt,salesCloseAt,amounts(prices));
    }
    @Override public List<ShowtimeView> findOpenShowtimes(UUID movieId, LocalDate date, UUID cinemaId, Instant now) {
        Instant start=date.atStartOfDay(DISPLAY_ZONE).toInstant(); Instant end=date.plusDays(1).atStartOfDay(DISPLAY_ZONE).toInstant();
        String sql = "SELECT s.id,s.movie_id,c.id cinema_id,c.name cinema_name,c.address cinema_address,"
                + "a.screen_format,s.start_at,s.end_at,s.sales_close_at "
                + "FROM showtimes s JOIN auditoriums a ON a.id=s.auditorium_id "
                + "JOIN cinemas c ON c.id=a.cinema_id "
                + "WHERE s.movie_id=? AND s.status='SCHEDULED' AND s.sales_close_at>? "
                + "AND s.start_at>=? AND s.start_at<?"
                + (cinemaId == null ? "" : " AND c.id=?")
                + " ORDER BY c.name,a.screen_format,s.start_at";
        Object[] args=cinemaId==null?new Object[]{movieId,atUtc(now),atUtc(start),atUtc(end)}:new Object[]{movieId,atUtc(now),atUtc(start),atUtc(end),cinemaId};
        return jdbcTemplate.query(sql,(rs,row)->new ShowtimeView(rs.getObject("id",UUID.class),rs.getObject("movie_id",UUID.class),rs.getObject("cinema_id",UUID.class),rs.getString("cinema_name"),rs.getString("cinema_address"),rs.getString("screen_format"),instant(rs,"start_at"),instant(rs,"end_at"),instant(rs,"sales_close_at"),prices(rs.getObject("id",UUID.class))),args);
    }
    @Override public ShowtimeSeatMap findSeatMap(UUID id, Instant now) {
        String sql = "SELECT s.id,s.movie_id,m.title movie_title,c.id cinema_id,c.name cinema_name,"
                + "a.name auditorium_name,s.start_at FROM showtimes s "
                + "JOIN movies m ON m.id=s.movie_id JOIN auditoriums a ON a.id=s.auditorium_id "
                + "JOIN cinemas c ON c.id=a.cinema_id "
                + "WHERE s.id=? AND s.status='SCHEDULED' AND s.sales_close_at>?";
        return jdbcTemplate.query(sql, (rs, row) -> new ShowtimeSeatMap(
                        rs.getObject("id", UUID.class), rs.getObject("movie_id", UUID.class),
                        rs.getString("movie_title"), rs.getObject("cinema_id", UUID.class),
                        rs.getString("cinema_name"), rs.getString("auditorium_name"), instant(rs, "start_at"),
                        seats(id), prices(id)), id, atUtc(now))
                .stream().findFirst()
                .orElseThrow(() -> ApplicationException.notFound("SHOWTIME_NOT_OPEN", "Showtime was not found or is no longer open"));
    }
    private Map<String,ResolvedPrice> resolvePrices(AuditoriumForShowtime auditorium,List<SeatForShowtime> seats,Instant start,Map<String,Long> overrides){Map<String,ResolvedPrice> resolved=new HashMap<>(); for(String type:seats.stream().map(SeatForShowtime::seatType).collect(java.util.stream.Collectors.toSet())){Long override=overrides.get(type); if(override!=null){if(override<=0)throw ApplicationException.businessRule("INVALID_SHOWTIME_OVERRIDE","Showtime override must be positive");resolved.put(type,new ResolvedPrice(type,override,"SHOWTIME_OVERRIDE"));continue;} Profile profile=findProfile(auditorium.cinemaId(),start.atZone(DISPLAY_ZONE).toLocalDate()); Rule rule=findRule(profile.id(),auditorium.screenFormat(),type,start.atZone(DISPLAY_ZONE).toLocalDateTime().toLocalTime(),start.atZone(DISPLAY_ZONE).getDayOfWeek().getValue()); if(rule==null)throw ApplicationException.businessRule("MISSING_PRICE_RULE","No matching price rule for seat type "+type);resolved.put(type,new ResolvedPrice(type,rule.amount(),profile.cinemaId()==null?"SYSTEM_PROFILE":"CINEMA_PROFILE"));}return resolved;}
    private Profile findProfile(UUID cinemaId,LocalDate date){List<Profile> local=jdbcTemplate.query("SELECT id,cinema_id FROM price_profiles WHERE cinema_id=? AND status='ACTIVE' AND effective_from<=? AND (effective_to IS NULL OR effective_to>=?) ORDER BY effective_from DESC LIMIT 1",(rs,row)->new Profile(rs.getObject("id",UUID.class),rs.getObject("cinema_id",UUID.class)),cinemaId,date,date);if(!local.isEmpty())return local.getFirst();return jdbcTemplate.query("SELECT id,cinema_id FROM price_profiles WHERE cinema_id IS NULL AND status='ACTIVE' AND effective_from<=? AND (effective_to IS NULL OR effective_to>=?) ORDER BY effective_from DESC LIMIT 1",(rs,row)->new Profile(rs.getObject("id",UUID.class),rs.getObject("cinema_id",UUID.class)),date,date).stream().findFirst().orElseThrow(()->ApplicationException.businessRule("MISSING_PRICE_PROFILE","No active price profile applies to this showtime"));}
    private Rule findRule(UUID profileId,String format,String type,LocalTime time,int day){String dayType=day>=6?"WEEKEND":"WEEKDAY";return jdbcTemplate.query("SELECT amount FROM price_rules WHERE profile_id=? AND day_type IN ('ANY',?) AND (screen_format IS NULL OR screen_format=?) AND (seat_type IS NULL OR seat_type=?) AND (time_from IS NULL OR (time_from<=? AND ?<time_to)) ORDER BY priority DESC, amount DESC LIMIT 1",(rs,row)->new Rule(rs.getLong("amount")),profileId,dayType,format,type,time,time).stream().findFirst().orElse(null);}
    private Map<String,Long> prices(UUID id){return Map.copyOf(jdbcTemplate.query("SELECT seat_type,price FROM showtime_prices WHERE showtime_id=?",rs-> {Map<String,Long> p=new HashMap<>();while(rs.next())p.put(rs.getString("seat_type"),rs.getLong("price"));return p;},id));}
    private List<ShowtimeSeatView> seats(UUID id){Map<String,Long> p=prices(id);return jdbcTemplate.query("SELECT ss.id,seat.row_label,seat.seat_number,seat.seat_type,seat.pair_key,ss.status FROM showtime_seats ss JOIN seats seat ON seat.id=ss.seat_id WHERE ss.showtime_id=? ORDER BY seat.row_label,seat.seat_number",(rs,row)->new ShowtimeSeatView(rs.getObject("id",UUID.class),rs.getString("row_label"),rs.getInt("seat_number"),rs.getString("seat_type"),rs.getString("pair_key"),rs.getString("status"),p.get(rs.getString("seat_type"))),id);}
    private Map<String,Long> amounts(Map<String,ResolvedPrice> prices){return prices.values().stream().collect(java.util.stream.Collectors.toMap(ResolvedPrice::seatType,ResolvedPrice::amount));}
    private Instant instant(ResultSet rs,String column)throws SQLException{return rs.getObject(column,OffsetDateTime.class).toInstant();} private OffsetDateTime atUtc(Instant value){return value.atOffset(ZoneOffset.UTC);} private OffsetDateTime now(){return clock.instant().atOffset(ZoneOffset.UTC);} private boolean blank(String v){return v==null||v.isBlank();} private String nullable(String v){return blank(v)?null:v.trim();}
    private record Profile(UUID id,UUID cinemaId){} private record Rule(long amount){} private record ResolvedPrice(String seatType,long amount,String source){}
}
