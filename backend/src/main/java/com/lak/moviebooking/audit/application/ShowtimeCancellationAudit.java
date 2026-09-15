package com.lak.moviebooking.audit.application;

import java.util.UUID;

public interface ShowtimeCancellationAudit {
    void recordRequested(UUID actorId, UUID showtimeId);
}
