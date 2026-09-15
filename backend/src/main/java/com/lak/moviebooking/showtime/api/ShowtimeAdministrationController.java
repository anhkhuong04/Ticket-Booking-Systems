package com.lak.moviebooking.showtime.api;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.lak.moviebooking.authorization.application.CinemaScopeAuthorizer;
import com.lak.moviebooking.cinema.application.CinemaShowtimeQuery;
import com.lak.moviebooking.common.security.AuthenticatedPrincipal;
import com.lak.moviebooking.showtime.application.PriceProfileCommand;
import com.lak.moviebooking.showtime.application.PriceProfileDetail;
import com.lak.moviebooking.showtime.application.PriceProfileView;
import com.lak.moviebooking.showtime.application.PriceRuleCommand;
import com.lak.moviebooking.showtime.application.PriceRuleView;
import com.lak.moviebooking.showtime.application.ShowtimeCreateCommand;
import com.lak.moviebooking.showtime.application.ShowtimeManagement;
import com.lak.moviebooking.showtime.application.ShowtimeView;
import com.lak.moviebooking.showtime.application.AdminShowtimeView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/admin")
public class ShowtimeAdministrationController {
    private final ShowtimeManagement showtimeManagement;
    private final CinemaShowtimeQuery cinemaShowtimeQuery;
    private final CinemaScopeAuthorizer cinemaScopeAuthorizer;
    public ShowtimeAdministrationController(ShowtimeManagement showtimeManagement, CinemaShowtimeQuery cinemaShowtimeQuery, CinemaScopeAuthorizer cinemaScopeAuthorizer) { this.showtimeManagement=showtimeManagement; this.cinemaShowtimeQuery=cinemaShowtimeQuery; this.cinemaScopeAuthorizer=cinemaScopeAuthorizer; }
    @GetMapping("/price-profiles") public List<PriceProfileDetail> profiles(@AuthenticationPrincipal AuthenticatedPrincipal actor,@RequestParam(required=false) UUID cinemaId){if(cinemaId==null)cinemaScopeAuthorizer.requireSuperAdmin(actor,"/api/admin/price-profiles");else cinemaScopeAuthorizer.requireAccess(actor,cinemaId);return showtimeManagement.findPriceProfiles(cinemaId);}
    @PostMapping("/price-profiles") public PriceProfileView createProfile(@AuthenticationPrincipal AuthenticatedPrincipal actor,@Valid @RequestBody PriceProfileRequest request){ if(request.cinemaId()==null)cinemaScopeAuthorizer.requireSuperAdmin(actor,"/api/admin/price-profiles");else cinemaScopeAuthorizer.requireAccess(actor,request.cinemaId());return showtimeManagement.createPriceProfile(actor.userId(),new PriceProfileCommand(request.cinemaId(),request.name(),request.effectiveFrom(),request.effectiveTo(),request.status())); }
    @PutMapping("/price-profiles/{profileId}") public PriceProfileView updateProfile(@AuthenticationPrincipal AuthenticatedPrincipal actor,@org.springframework.web.bind.annotation.PathVariable UUID profileId,@Valid @RequestBody PriceProfileRequest request){requireProfileAccess(actor,profileId);return showtimeManagement.updatePriceProfile(actor.userId(),profileId,new PriceProfileCommand(request.cinemaId(),request.name(),request.effectiveFrom(),request.effectiveTo(),request.status()));}
    @DeleteMapping("/price-profiles/{profileId}") public void deactivateProfile(@AuthenticationPrincipal AuthenticatedPrincipal actor,@org.springframework.web.bind.annotation.PathVariable UUID profileId){requireProfileAccess(actor,profileId);showtimeManagement.deactivatePriceProfile(actor.userId(),profileId);}
    @PostMapping("/price-profiles/{profileId}/rules") public PriceRuleView createRule(@AuthenticationPrincipal AuthenticatedPrincipal actor,@org.springframework.web.bind.annotation.PathVariable UUID profileId,@Valid @RequestBody PriceRuleRequest request){var cinemaId=showtimeManagement.findPriceProfileCinemaId(profileId);if(cinemaId.isPresent())cinemaScopeAuthorizer.requireAccess(actor,cinemaId.get());else cinemaScopeAuthorizer.requireSuperAdmin(actor,"/api/admin/price-profiles/"+profileId+"/rules");return showtimeManagement.createPriceRule(actor.userId(),profileId,new PriceRuleCommand(request.dayType(),request.timeFrom(),request.timeTo(),request.screenFormat(),request.seatType(),request.amount(),request.priority()));}
    @PutMapping("/price-rules/{ruleId}") public PriceRuleView updateRule(@AuthenticationPrincipal AuthenticatedPrincipal actor,@org.springframework.web.bind.annotation.PathVariable UUID ruleId,@Valid @RequestBody PriceRuleRequest request){requireRuleAccess(actor,ruleId);return showtimeManagement.updatePriceRule(actor.userId(),ruleId,new PriceRuleCommand(request.dayType(),request.timeFrom(),request.timeTo(),request.screenFormat(),request.seatType(),request.amount(),request.priority()));}
    @DeleteMapping("/price-rules/{ruleId}") public void deleteRule(@AuthenticationPrincipal AuthenticatedPrincipal actor,@org.springframework.web.bind.annotation.PathVariable UUID ruleId){requireRuleAccess(actor,ruleId);showtimeManagement.deletePriceRule(actor.userId(),ruleId);}
    @GetMapping("/showtimes") public List<AdminShowtimeView> showtimes(@AuthenticationPrincipal AuthenticatedPrincipal actor,@RequestParam @NotNull UUID cinemaId,@RequestParam @NotNull LocalDate date){cinemaScopeAuthorizer.requireAccess(actor,cinemaId);return showtimeManagement.findAdminShowtimes(cinemaId,date);}
    @PostMapping("/showtimes") public ShowtimeView createShowtime(@AuthenticationPrincipal AuthenticatedPrincipal actor,@Valid @RequestBody ShowtimeRequest request){var auditorium=cinemaShowtimeQuery.findAuditorium(request.auditoriumId());cinemaScopeAuthorizer.requireAccess(actor,auditorium.cinemaId());return showtimeManagement.createShowtime(actor.userId(),new ShowtimeCreateCommand(request.movieId(),request.auditoriumId(),request.startAt(),request.priceOverrides()==null?Map.of():request.priceOverrides()));}
    @DeleteMapping("/showtimes/{showtimeId}") public AdminShowtimeView cancelShowtime(@AuthenticationPrincipal AuthenticatedPrincipal actor,@org.springframework.web.bind.annotation.PathVariable UUID showtimeId){cinemaScopeAuthorizer.requireAccess(actor,showtimeManagement.findShowtimeCinemaId(showtimeId));return showtimeManagement.cancelShowtime(actor.userId(),showtimeId);}
    private void requireProfileAccess(AuthenticatedPrincipal actor,UUID profileId){var cinemaId=showtimeManagement.findPriceProfileCinemaId(profileId);if(cinemaId.isPresent())cinemaScopeAuthorizer.requireAccess(actor,cinemaId.get());else cinemaScopeAuthorizer.requireSuperAdmin(actor,"/api/admin/price-profiles/"+profileId);}
    private void requireRuleAccess(AuthenticatedPrincipal actor,UUID ruleId){var cinemaId=showtimeManagement.findPriceRuleCinemaId(ruleId);if(cinemaId.isPresent())cinemaScopeAuthorizer.requireAccess(actor,cinemaId.get());else cinemaScopeAuthorizer.requireSuperAdmin(actor,"/api/admin/price-rules/"+ruleId);}
    public record PriceProfileRequest(UUID cinemaId,@NotBlank String name,@NotNull LocalDate effectiveFrom,LocalDate effectiveTo,@NotNull @Pattern(regexp="ACTIVE|INACTIVE") String status){}
    public record PriceRuleRequest(@NotNull @Pattern(regexp="ANY|WEEKDAY|WEEKEND") String dayType,LocalTime timeFrom,LocalTime timeTo,String screenFormat,String seatType,@Min(1) long amount,@Min(0) int priority){}
    public record ShowtimeRequest(@NotNull UUID movieId,@NotNull UUID auditoriumId,@NotNull Instant startAt,Map<String,Long> priceOverrides){}
}
