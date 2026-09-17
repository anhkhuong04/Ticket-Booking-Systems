package com.lak.moviebooking.authorization.infrastructure;

import java.util.UUID;
import java.util.Set;

import com.lak.moviebooking.audit.application.DeniedAccessAudit;
import com.lak.moviebooking.authorization.application.CinemaScopeAuthorizer;
import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.common.security.AuthenticatedPrincipal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class JdbcCinemaScopeAuthorizer implements CinemaScopeAuthorizer {

    private final JdbcTemplate jdbcTemplate;
    private final DeniedAccessAudit deniedAccessAudit;

    JdbcCinemaScopeAuthorizer(JdbcTemplate jdbcTemplate, DeniedAccessAudit deniedAccessAudit) {
        this.jdbcTemplate = jdbcTemplate;
        this.deniedAccessAudit = deniedAccessAudit;
    }

    @Override
    public void requireAccess(AuthenticatedPrincipal actor, UUID cinemaId) {
        if (actor.roles().contains("SUPER_ADMIN")) {
            return;
        }
        boolean scopedRole = actor.roles().contains("CINEMA_MANAGER") || actor.roles().contains("TICKET_STAFF");
        boolean assigned = scopedRole && Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM staff_cinema_assignments
                    WHERE user_id = ? AND cinema_id = ?
                )
                """, Boolean.class, actor.userId(), cinemaId));
        if (assigned) {
            return;
        }
        deniedAccessAudit.record(actor.userId(), "/api/admin/cinemas/" + cinemaId, "CINEMA_SCOPE");
        throw ApplicationException.forbidden("CINEMA_SCOPE_DENIED", "You are not assigned to this cinema");
    }

    @Override
    public void requireSuperAdmin(AuthenticatedPrincipal actor, String path) {
        if (actor.roles().contains("SUPER_ADMIN")) {
            return;
        }
        deniedAccessAudit.record(actor.userId(), path, "SUPER_ADMIN");
        throw ApplicationException.forbidden("SUPER_ADMIN_REQUIRED", "This operation requires system administration permission");
    }

    @Override
    public Set<UUID> accessibleCinemaIds(AuthenticatedPrincipal actor) {
        if (actor.roles().contains("SUPER_ADMIN")) {
            return Set.of();
        }
        Set<UUID> assigned = Set.copyOf(jdbcTemplate.queryForList(
                "SELECT cinema_id FROM staff_cinema_assignments WHERE user_id = ?", UUID.class, actor.userId()));
        if (assigned.isEmpty()) {
            deniedAccessAudit.record(actor.userId(), "/api/admin", "CINEMA_SCOPE");
            throw ApplicationException.forbidden("CINEMA_SCOPE_DENIED", "You are not assigned to a cinema");
        }
        return assigned;
    }
}
