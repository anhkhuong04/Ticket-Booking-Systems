package com.lak.moviebooking.identity.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.notification.application.EmailNotificationQueue;
import com.lak.moviebooking.notification.application.EmailOutboxPayload;
import com.lak.moviebooking.notification.application.EmailTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityAuthenticationService implements IdentityUserQuery {

    private static final String CUSTOMER_ROLE = "CUSTOMER";
    private final IdentityRepository identityRepository;
    private final PasswordHasher passwordHasher;
    private final OpaqueTokenService opaqueTokenService;
    private final AccessTokenIssuer accessTokenIssuer;
    private final IdentityAuthenticationPolicy policy;
    private final EmailNotificationQueue emailNotificationQueue;
    private final Clock clock;

    public IdentityAuthenticationService(
            IdentityRepository identityRepository,
            PasswordHasher passwordHasher,
            OpaqueTokenService opaqueTokenService,
            AccessTokenIssuer accessTokenIssuer,
            IdentityAuthenticationPolicy policy,
            EmailNotificationQueue emailNotificationQueue,
            Clock clock) {
        this.identityRepository = identityRepository;
        this.passwordHasher = passwordHasher;
        this.opaqueTokenService = opaqueTokenService;
        this.accessTokenIssuer = accessTokenIssuer;
        this.policy = policy;
        this.emailNotificationQueue = emailNotificationQueue;
        this.clock = clock;
    }

    @Transactional
    public RefreshSessionResult register(String fullName, String email, String password) {
        Instant now = clock.instant();
        String normalizedEmail = normalizeEmail(email);
        UUID userId = UUID.randomUUID();
        try {
            if (!identityRepository.createCustomer(userId, normalizedEmail, fullName.trim(), passwordHasher.hash(password), now)) {
                throw ApplicationException.conflict("EMAIL_ALREADY_REGISTERED", "An account already uses this email");
            }
        }
        catch (DataIntegrityViolationException exception) {
            throw ApplicationException.conflict("EMAIL_ALREADY_REGISTERED", "An account already uses this email");
        }
        IdentityAccount account = new IdentityAccount(
                userId, normalizedEmail, fullName.trim(), "", true, java.util.Set.of(CUSTOMER_ROLE));
        return issueSession(account, now);
    }

    @Transactional
    public RefreshSessionResult login(String email, String password) {
        IdentityAccount account = identityRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> ApplicationException.unauthenticated(
                        "INVALID_CREDENTIALS", "Email or password is incorrect"));
        if (!passwordHasher.matches(password, account.passwordHash())) {
            throw ApplicationException.unauthenticated("INVALID_CREDENTIALS", "Email or password is incorrect");
        }
        if (!account.active()) {
            throw ApplicationException.forbidden("ACCOUNT_LOCKED", "This account is locked");
        }
        return issueSession(account, clock.instant());
    }

    @Transactional(noRollbackFor = ApplicationException.class)
    public RefreshSessionResult refresh(String rawRefreshToken) {
        Instant now = clock.instant();
        String tokenHash = opaqueTokenService.hash(rawRefreshToken);
        StoredRefreshToken storedToken = identityRepository.findRefreshTokenForUpdate(tokenHash)
                .orElseThrow(() -> ApplicationException.unauthenticated(
                        "REFRESH_TOKEN_INVALID", "Your session is no longer valid"));
        if (storedToken.revokedAt() != null) {
            identityRepository.revokeActiveRefreshTokens(storedToken.userId(), "REUSE_DETECTED", now);
            throw ApplicationException.unauthenticated("REFRESH_TOKEN_REUSED", "Your session is no longer valid");
        }
        if (!storedToken.expiresAt().isAfter(now)) {
            identityRepository.rotateRefreshToken(storedToken.id(), null, now);
            throw ApplicationException.unauthenticated("REFRESH_TOKEN_EXPIRED", "Your session has expired");
        }
        IdentityAccount account = identityRepository.findById(storedToken.userId())
                .orElseThrow(() -> ApplicationException.unauthenticated(
                        "REFRESH_TOKEN_INVALID", "Your session is no longer valid"));
        if (!account.active()) {
            identityRepository.revokeActiveRefreshTokens(account.id(), "REUSE_DETECTED", now);
            throw ApplicationException.forbidden("ACCOUNT_LOCKED", "This account is locked");
        }

        RefreshSessionResult replacement = issueSession(account, now);
        identityRepository.rotateRefreshToken(storedToken.id(), replacement.refreshTokenId(), now);
        return replacement;
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        identityRepository.revokeRefreshToken(opaqueTokenService.hash(rawRefreshToken), "LOGOUT", clock.instant());
    }

    @Transactional
    public void requestPasswordReset(String email) {
        Optional<IdentityAccount> account = identityRepository.findByEmail(normalizeEmail(email));
        if (account.isEmpty() || !account.get().active()) {
            return;
        }
        Instant now = clock.instant();
        IdentityAccount user = account.get();
        identityRepository.consumeOtherPasswordResetTokens(user.id(), now);
        String rawToken = opaqueTokenService.generate();
        identityRepository.createPasswordResetToken(
                UUID.randomUUID(), user.id(), opaqueTokenService.hash(rawToken), now.plus(policy.passwordResetTokenTtl()), now);
        String separator = policy.passwordResetUrl().contains("?") ? "&" : "?";
        emailNotificationQueue.enqueue(user.id(), new EmailOutboxPayload(
                user.email(),
                EmailTemplate.PASSWORD_RESET,
                Map.of("resetUrl", policy.passwordResetUrl() + separator + "token=" + rawToken)));
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        Instant now = clock.instant();
        StoredPasswordResetToken storedToken = identityRepository.findPasswordResetTokenForUpdate(opaqueTokenService.hash(rawToken))
                .orElseThrow(() -> ApplicationException.unauthenticated(
                        "PASSWORD_RESET_TOKEN_INVALID", "The password reset link is invalid or expired"));
        if (storedToken.usedAt() != null || !storedToken.expiresAt().isAfter(now)) {
            throw ApplicationException.unauthenticated(
                    "PASSWORD_RESET_TOKEN_INVALID", "The password reset link is invalid or expired");
        }
        identityRepository.updatePassword(storedToken.userId(), passwordHasher.hash(newPassword), now);
        identityRepository.consumePasswordResetToken(storedToken.id(), now);
        identityRepository.consumeOtherPasswordResetTokens(storedToken.userId(), now);
        identityRepository.revokeActiveRefreshTokens(storedToken.userId(), "REUSE_DETECTED", now);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthenticatedIdentity> findActiveById(UUID userId) {
        return identityRepository.findById(userId)
                .filter(IdentityAccount::active)
                .map(account -> new AuthenticatedIdentity(
                        account.id(), account.email(), account.fullName(), account.roles()));
    }

    private RefreshSessionResult issueSession(IdentityAccount account, Instant now) {
        String rawRefreshToken = opaqueTokenService.generate();
        UUID refreshTokenId = UUID.randomUUID();
        identityRepository.createRefreshToken(
                refreshTokenId, account.id(), opaqueTokenService.hash(rawRefreshToken), now.plus(policy.refreshTokenTtl()), now);
        AuthenticatedIdentity user = new AuthenticatedIdentity(account.id(), account.email(), account.fullName(), account.roles());
        return new RefreshSessionResult(new AuthenticatedSession(
                accessTokenIssuer.issue(account.id(), account.roles(), now), user), rawRefreshToken, refreshTokenId);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
