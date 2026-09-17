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
    private static final Set<String> VIDEO_TYPES = Set.of("video/mp4", "video/webm");
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
        String resourceType = resourceType(request.contentType());
        if (request.filename() == null || request.filename().isBlank() || resourceType == null
                || request.sizeBytes() <= 0 || request.sizeBytes() > maxBytes) {
            throw ApplicationException.businessRule("INVALID_MEDIA_UPLOAD", "Only JPEG, PNG, WebP, MP4, or WebM files up to 3 MiB are allowed");
        }
        long timestamp = clock.instant().getEpochSecond();
        String values = "folder=" + properties.folder() + "&timestamp=" + timestamp + properties.apiSecret();
        auditLogWriter.record(actorId, "MEDIA_UPLOAD_SIGNATURE_ISSUED", "media_upload", null,
                Map.of("contentType", request.contentType(), "sizeBytes", Long.toString(request.sizeBytes())));
        return new MediaUploadSignature(properties.cloudName(), properties.apiKey(), properties.folder(), resourceType, timestamp, sha1(values));
    }

    private String resourceType(String contentType) {
        if (IMAGE_TYPES.contains(contentType)) return "image";
        if (VIDEO_TYPES.contains(contentType)) return "video";
        return null;
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
