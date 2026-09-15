package com.lak.moviebooking.identity.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.common.config.AuthProperties;
import com.lak.moviebooking.identity.application.LoginRateLimit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
class RedisLoginRateLimit implements LoginRateLimit {

    private static final DefaultRedisScript<Long> INCREMENT_WITH_EXPIRY = new DefaultRedisScript<>(
            "local current = redis.call('INCR', KEYS[1]); "
                    + "if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end; return current;",
            Long.class);
    private final StringRedisTemplate redisTemplate;
    private final AuthProperties properties;

    RedisLoginRateLimit(StringRedisTemplate redisTemplate, AuthProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public void check(String email, String clientAddress) {
        Long attempts = redisTemplate.execute(
                INCREMENT_WITH_EXPIRY,
                List.of("rate-limit:login:" + sha256(email + ":" + clientAddress)),
                Long.toString(properties.loginRateLimitWindow().toSeconds()));
        if (attempts == null || attempts > properties.loginRateLimit()) {
            throw ApplicationException.rateLimited("LOGIN_RATE_LIMITED", "Too many login attempts. Please try again later");
        }
    }

    private String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
