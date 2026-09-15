package com.lak.moviebooking.ticketing.infrastructure;

import java.util.List;
import java.util.UUID;

import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.ticketing.application.TicketEmailResendRateLimit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
class RedisTicketEmailResendRateLimit implements TicketEmailResendRateLimit {

    private static final DefaultRedisScript<Long> INCREMENT_WITH_EXPIRY = new DefaultRedisScript<>(
            "local current = redis.call('INCR', KEYS[1]); "
                    + "if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end; return current;",
            Long.class);
    private final StringRedisTemplate redisTemplate;
    private final TicketingProperties properties;

    RedisTicketEmailResendRateLimit(StringRedisTemplate redisTemplate, TicketingProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public void check(UUID ownerId, UUID ticketId) {
        Long attempts = redisTemplate.execute(INCREMENT_WITH_EXPIRY,
                List.of("rate-limit:ticket-email:" + ownerId + ":" + ticketId),
                Long.toString(properties.resendRateLimitWindow().toSeconds()));
        if (attempts == null || attempts > properties.resendRateLimit()) {
            throw ApplicationException.rateLimited("TICKET_EMAIL_RATE_LIMITED", "Please try again later");
        }
    }
}
