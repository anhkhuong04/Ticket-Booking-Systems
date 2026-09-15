package com.lak.moviebooking.identity.api;

import com.lak.moviebooking.common.security.CsrfCookieService;
import com.lak.moviebooking.common.security.RefreshTokenCookieService;
import com.lak.moviebooking.common.security.AuthenticatedPrincipal;
import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.identity.application.AuthenticatedIdentity;
import com.lak.moviebooking.identity.application.IdentityAuthenticationService;
import com.lak.moviebooking.identity.application.LoginRateLimit;
import com.lak.moviebooking.identity.application.RefreshSessionResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final IdentityAuthenticationService authenticationService;
    private final RefreshTokenCookieService refreshTokenCookieService;
    private final CsrfCookieService csrfCookieService;
    private final LoginRateLimit loginRateLimit;

    public AuthController(
            IdentityAuthenticationService authenticationService,
            RefreshTokenCookieService refreshTokenCookieService,
            CsrfCookieService csrfCookieService,
            LoginRateLimit loginRateLimit) {
        this.authenticationService = authenticationService;
        this.refreshTokenCookieService = refreshTokenCookieService;
        this.csrfCookieService = csrfCookieService;
        this.loginRateLimit = loginRateLimit;
    }

    @PostMapping("/register")
    ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        return authenticatedResponse(authenticationService.register(request.fullName(), request.email(), request.password()), response,
                HttpStatus.CREATED);
    }

    @PostMapping("/login")
    AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response) {
        loginRateLimit.check(request.email().trim().toLowerCase(java.util.Locale.ROOT), clientAddress(servletRequest));
        RefreshSessionResult result = authenticationService.login(request.email(), request.password());
        writeCookies(result, response);
        return AuthResponse.from(result.session().accessToken(), result.session().user());
    }

    @PostMapping("/refresh")
    AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = refreshTokenCookieService.read(request);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw ApplicationException.unauthenticated("REFRESH_TOKEN_INVALID", "Your session is no longer valid");
        }
        RefreshSessionResult result = authenticationService.refresh(refreshToken);
        writeCookies(result, response);
        return AuthResponse.from(result.session().accessToken(), result.session().user());
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = refreshTokenCookieService.read(request);
        if (refreshToken != null && !refreshToken.isBlank()) {
            authenticationService.logout(refreshToken);
        }
        refreshTokenCookieService.clear(response);
        csrfCookieService.issue(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/csrf")
    ResponseEntity<Void> csrf(HttpServletResponse response) {
        csrfCookieService.issue(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/session")
    SessionUserResponse session(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return new SessionUserResponse(principal.userId(), principal.email(), principal.fullName(), principal.roles());
    }

    @PostMapping("/forgot-password")
    ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authenticationService.requestPasswordReset(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authenticationService.resetPassword(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<AuthResponse> authenticatedResponse(
            RefreshSessionResult result, HttpServletResponse response, HttpStatus status) {
        writeCookies(result, response);
        return ResponseEntity.status(status).body(AuthResponse.from(result.session().accessToken(), result.session().user()));
    }

    private void writeCookies(RefreshSessionResult result, HttpServletResponse response) {
        refreshTokenCookieService.write(response, result.refreshToken());
        csrfCookieService.issue(response);
    }

    private String clientAddress(HttpServletRequest request) {
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    record RegisterRequest(
            @NotBlank @Size(max = 150) String fullName,
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 12, max = 128) String password) {
    }

    record LoginRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(max = 128) String password) {
    }

    record ForgotPasswordRequest(@NotBlank @Email @Size(max = 320) String email) {
    }

    record ResetPasswordRequest(
            @NotBlank @Size(min = 32, max = 128) String token,
            @NotBlank @Size(min = 12, max = 128) String password) {
    }

    record AuthResponse(String accessToken, SessionUserResponse user) {
        static AuthResponse from(String accessToken, AuthenticatedIdentity user) {
            return new AuthResponse(accessToken, new SessionUserResponse(user.id(), user.email(), user.fullName(), user.roles()));
        }
    }

    record SessionUserResponse(java.util.UUID id, String email, String fullName, java.util.Set<String> roles) {
    }
}
