package com.lak.moviebooking.catalog.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.lak.moviebooking.audit.application.AuditLogWriter;
import com.lak.moviebooking.catalog.application.MediaUploadRequest;
import com.lak.moviebooking.catalog.application.MediaUploadSignature;
import com.lak.moviebooking.catalog.application.MediaUploadSignatureIssuer;
import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.common.config.CloudinaryProperties;
import org.springframework.stereotype.Service;

@Service
class CloudinaryMediaUploadSignatureIssuer implements MediaUploadSignatureIssuer {

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private final CloudinaryProperties properties;
    private final AuditLogWriter auditLogWriter;
    private final Clock clock;

    CloudinaryMediaUploadSignatureIssuer(CloudinaryProperties properties, AuditLogWriter auditLogWriter, Clock clock) {
        this.properties = properties;
        this.auditLogWriter = auditLogWriter;
        this.clock = clock;
    }

    @Override
    public MediaUploadSignature issue(UUID actorId, MediaUploadRequest request) {
        if (!properties.enabled()) {
            throw ApplicationException.businessRule("MEDIA_UPLOAD_DISABLED", "Media upload is not configured");
        }
        long maxBytes = properties.maxUploadMegabytes() * 1024L * 1024L;
        if (request.filename() == null || request.filename().isBlank() || !IMAGE_TYPES.contains(request.contentType())
                || request.sizeBytes() <= 0 || request.sizeBytes() > maxBytes) {
            throw ApplicationException.businessRule("INVALID_MEDIA_UPLOAD", "Only JPEG, PNG, or WebP images up to the configured limit are allowed");
        }
        long timestamp = clock.instant().getEpochSecond();
        String values = "folder=" + properties.folder() + "&timestamp=" + timestamp + properties.apiSecret();
        auditLogWriter.record(actorId, "MEDIA_UPLOAD_SIGNATURE_ISSUED", "media_upload", null,
                Map.of("contentType", request.contentType(), "sizeBytes", Long.toString(request.sizeBytes())));
        return new MediaUploadSignature(properties.cloudName(), properties.apiKey(), properties.folder(), timestamp, sha1(values));
    }

    private String sha1(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        }
        catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1 is unavailable", exception);
        }
    }
}
