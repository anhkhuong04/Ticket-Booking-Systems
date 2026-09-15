package com.lak.moviebooking.authorization.infrastructure;

import java.util.UUID;

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
}
