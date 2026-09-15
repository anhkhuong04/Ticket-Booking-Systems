package com.lak.moviebooking.audit.application;

import java.util.Map;
import java.util.UUID;

/** Cross-module contract for append-only records of sensitive state changes. */
public interface AuditLogWriter {

    void record(UUID actorId, String action, String entityType, UUID entityId, Map<String, String> metadata);
}
