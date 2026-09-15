package com.lak.moviebooking.reservation.infrastructure;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Redis only mirrors the database hold and is deliberately best-effort. */
@Component
class RedisSeatHoldTtlStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisSeatHoldTtlStore.class);
    private final StringRedisTemplate redisTemplate;

    RedisSeatHoldTtlStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    void write(UUID showtimeId, List<UUID> seatIds, UUID holdId, Duration ttl) {
        try {
            for (UUID seatId : seatIds) {
                redisTemplate.opsForValue().set(key(showtimeId, seatId), holdId.toString(), ttl);
            }
        }
        catch (RuntimeException exception) {
            LOGGER.warn("Unable to mirror seat hold in Redis exception_type={}", exception.getClass().getSimpleName());
        }
    }

    void delete(UUID showtimeId, List<UUID> seatIds) {
        try {
            redisTemplate.delete(seatIds.stream().map(seatId -> key(showtimeId, seatId)).toList());
        }
        catch (RuntimeException exception) {
            LOGGER.warn("Unable to remove seat hold mirror from Redis exception_type={}", exception.getClass().getSimpleName());
        }
    }

    private String key(UUID showtimeId, UUID seatId) {
        return "hold:" + showtimeId + ":" + seatId;
    }
}
