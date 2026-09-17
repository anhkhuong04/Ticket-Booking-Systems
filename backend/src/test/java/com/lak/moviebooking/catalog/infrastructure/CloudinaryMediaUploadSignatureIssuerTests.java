package com.lak.moviebooking.catalog.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import com.lak.moviebooking.audit.application.AuditLogWriter;
import com.lak.moviebooking.catalog.application.MediaUploadRequest;
import com.lak.moviebooking.catalog.application.MediaUploadSignature;
import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.common.config.CloudinaryProperties;
import org.junit.jupiter.api.Test;

class CloudinaryMediaUploadSignatureIssuerTests {

    @Test
    void signsOnlyAllowedImageMetadataWithoutExposingTheSecret() {
        UUID actorId = UUID.randomUUID();
        RecordingAuditLog audit = new RecordingAuditLog();
        CloudinaryMediaUploadSignatureIssuer issuer = new CloudinaryMediaUploadSignatureIssuer(
                new CloudinaryProperties(true, "demo-cloud", "key", "secret", "lak-movies", 3), audit,
                Clock.fixed(Instant.ofEpochSecond(1_700_000_000L), ZoneOffset.UTC));

        MediaUploadSignature signature = issuer.issue(actorId, new MediaUploadRequest("poster.webp", "image/webp", 1024));

        assertThat(signature).isEqualTo(new MediaUploadSignature("demo-cloud", "key", "lak-movies", "image", 1_700_000_000L,
                "fe4d59bde9a407223a437af67ed24862c33a865d"));
        assertThat(audit.action).isEqualTo("MEDIA_UPLOAD_SIGNATURE_ISSUED");
        assertThat(audit.metadata).doesNotContainValue("secret");
    }

    @Test
    void rejectsDisallowedFileTypesAndOversizedFiles() {
        CloudinaryMediaUploadSignatureIssuer issuer = new CloudinaryMediaUploadSignatureIssuer(
                new CloudinaryProperties(true, "demo-cloud", "key", "secret", "lak-movies", 3), new RecordingAuditLog(), Clock.systemUTC());

        assertThatThrownBy(() -> issuer.issue(UUID.randomUUID(), new MediaUploadRequest("payload.svg", "image/svg+xml", 1)))
                .isInstanceOf(ApplicationException.class)
                .extracting(error -> ((ApplicationException) error).code()).isEqualTo("INVALID_MEDIA_UPLOAD");
        assertThatThrownBy(() -> issuer.issue(UUID.randomUUID(), new MediaUploadRequest("large.png", "image/png", 3L * 1024 * 1024 + 1)))
                .isInstanceOf(ApplicationException.class);
    }

    @Test
    void signsAllowedVideoMetadata() {
        CloudinaryMediaUploadSignatureIssuer issuer = new CloudinaryMediaUploadSignatureIssuer(
                new CloudinaryProperties(true, "demo-cloud", "key", "secret", "lak-movies", 3), new RecordingAuditLog(), Clock.systemUTC());

        MediaUploadSignature signature = issuer.issue(UUID.randomUUID(), new MediaUploadRequest("trailer.mp4", "video/mp4", 1024));

        assertThat(signature.resourceType()).isEqualTo("video");
    }

    private static final class RecordingAuditLog implements AuditLogWriter {
        private String action;
        private Map<String, String> metadata = Map.of();
        @Override public void record(UUID actorId, String action, String entityType, UUID entityId, Map<String, String> metadata) { this.action = action; this.metadata = metadata; }
    }
}
