package com.lak.moviebooking.common.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.media.cloudinary")
public record CloudinaryProperties(
        boolean enabled,
        String cloudName,
        String apiKey,
        String apiSecret,
        String folder,
        @Min(1) @Max(3) int maxUploadMegabytes) {

    public CloudinaryProperties {
        if (maxUploadMegabytes > 3) {
            throw new IllegalArgumentException("Cloudinary uploads may not exceed 3 MiB");
        }
        if (enabled && (blank(cloudName) || blank(apiKey) || blank(apiSecret) || blank(folder))) {
            throw new IllegalArgumentException("Cloudinary credentials and folder are required when media upload is enabled");
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
