package com.lak.moviebooking.audit.application;

import java.util.UUID;

/** Cross-module contract for recording security-relevant authorization denials. */
public interface DeniedAccessAudit {

    void record(UUID actorId, String path, String requiredAuthority);
}
