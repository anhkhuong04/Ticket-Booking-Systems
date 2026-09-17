package com.lak.moviebooking.operations.api;
import java.time.LocalDate; import java.util.List; import java.util.UUID;
import com.lak.moviebooking.authorization.application.CinemaScopeAuthorizer; import com.lak.moviebooking.common.security.AuthenticatedPrincipal; import com.lak.moviebooking.operations.application.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin") public class AdminOperationsController {
 private final AdminOperations operations; private final CinemaScopeAuthorizer scope;
 public AdminOperationsController(AdminOperations operations,CinemaScopeAuthorizer scope){this.operations=operations;this.scope=scope;}
 @GetMapping("/bookings") public List<AdminBookingView> bookings(@AuthenticationPrincipal AuthenticatedPrincipal actor,@RequestParam(required=false)String q,@RequestParam(required=false)String status,@RequestParam(required=false)String exception,@RequestParam(required=false)UUID cinemaId,@RequestParam(required=false)LocalDate date){check(actor,cinemaId);return operations.bookings(q,status,exception,cinemaId,date,scope.accessibleCinemaIds(actor));}
 @GetMapping("/payments") public List<AdminPaymentView> payments(@AuthenticationPrincipal AuthenticatedPrincipal actor,@RequestParam(required=false)String q,@RequestParam(required=false)String status,@RequestParam(required=false)UUID cinemaId,@RequestParam(required=false)LocalDate date){check(actor,cinemaId);return operations.payments(q,status,cinemaId,date,scope.accessibleCinemaIds(actor));}
 @GetMapping("/refunds") public List<AdminRefundView> refunds(@AuthenticationPrincipal AuthenticatedPrincipal actor,@RequestParam(required=false)String status,@RequestParam(defaultValue="false")boolean overdue,@RequestParam(required=false)UUID cinemaId,@RequestParam(required=false)LocalDate date){check(actor,cinemaId);return operations.refunds(status,overdue,cinemaId,date,scope.accessibleCinemaIds(actor));}
 @PostMapping("/refunds/{refundId}/retry") public void retry(@AuthenticationPrincipal AuthenticatedPrincipal actor,@PathVariable UUID refundId){operations.retryFailedRefund(actor.userId(),refundId,scope.accessibleCinemaIds(actor));}
 @GetMapping("/users") public List<AdminUserView> users(@AuthenticationPrincipal AuthenticatedPrincipal actor,@RequestParam(required=false)String q){scope.requireSuperAdmin(actor,"/api/admin/users");return operations.users(q);}
 @PostMapping("/users/{userId}/lock") public void lock(@AuthenticationPrincipal AuthenticatedPrincipal actor,@PathVariable UUID userId){scope.requireSuperAdmin(actor,"/api/admin/users/"+userId+"/lock");operations.setUserLocked(actor.userId(),userId,true);}
 @PostMapping("/users/{userId}/unlock") public void unlock(@AuthenticationPrincipal AuthenticatedPrincipal actor,@PathVariable UUID userId){scope.requireSuperAdmin(actor,"/api/admin/users/"+userId+"/unlock");operations.setUserLocked(actor.userId(),userId,false);}
 private void check(AuthenticatedPrincipal actor,UUID cinemaId){if(cinemaId!=null)scope.requireAccess(actor,cinemaId);}
}
