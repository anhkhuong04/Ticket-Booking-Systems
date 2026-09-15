package com.lak.moviebooking.identity.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.lak.moviebooking.common.config.AuthProperties;
import com.lak.moviebooking.identity.application.AccessTokenIssuer;
import com.lak.moviebooking.identity.application.AccessTokenVerifier;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Minimal HMAC-SHA256 JWT implementation with an intentionally small claim set. */
@Component
class JwtAccessTokenService implements AccessTokenIssuer, AccessTokenVerifier {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private final ObjectMapper objectMapper;
    private final byte[] signingKey;
    private final long accessTokenTtlSeconds;

    JwtAccessTokenService(ObjectMapper objectMapper, AuthProperties properties) {
        this.objectMapper = objectMapper;
        this.signingKey = properties.jwtHmacSecret().getBytes(StandardCharsets.UTF_8);
        this.accessTokenTtlSeconds = properties.accessTokenTtl().toSeconds();
    }

    @Override
    public String issue(UUID userId, Set<String> roles, Instant issuedAt) {
        try {
            String header = encodeJson(Map.of("alg", "HS256", "typ", "JWT"));
            String payload = encodeJson(Map.of(
                    "iss", "lak",
                    "sub", userId.toString(),
                    "iat", issuedAt.getEpochSecond(),
                    "exp", issuedAt.plusSeconds(accessTokenTtlSeconds).getEpochSecond(),
                    "roles", List.copyOf(roles)));
            String signedContent = header + "." + payload;
            return signedContent + "." + URL_ENCODER.encodeToString(sign(signedContent));
        }
        catch (JacksonException exception) {
            throw new IllegalStateException("Unable to create access token", exception);
        }
    }

    @Override
    public UUID verify(String token, Instant now) {
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            throw new IllegalArgumentException("Access token is invalid");
        }
        String signedContent = parts[0] + "." + parts[1];
        byte[] suppliedSignature;
        try {
            suppliedSignature = URL_DECODER.decode(parts[2]);
        }
        catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Access token is invalid", exception);
        }
        if (!MessageDigest.isEqual(sign(signedContent), suppliedSignature)) {
            throw new IllegalArgumentException("Access token is invalid");
        }
        try {
            JsonNode payload = objectMapper.readTree(URL_DECODER.decode(parts[1]));
            if (!"lak".equals(payload.path("iss").asString()) || !payload.path("exp").canConvertToLong()
                    || payload.path("exp").asLong() <= now.getEpochSecond()) {
                throw new IllegalArgumentException("Access token is expired or invalid");
            }
            return UUID.fromString(payload.path("sub").asText());
        }
        catch (JacksonException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Access token is expired or invalid", exception);
        }
    }

    private String encodeJson(Map<String, ?> value) throws JacksonException {
        return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
    }

    private byte[] sign(String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            return mac.doFinal(content.getBytes(StandardCharsets.US_ASCII));
        }
        catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to sign access token", exception);
        }
    }
}
