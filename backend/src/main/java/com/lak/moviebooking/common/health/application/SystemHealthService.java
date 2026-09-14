package com.lak.moviebooking.common.health.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SystemHealthService implements SystemHealthQuery {

	private static final Logger LOGGER = LoggerFactory.getLogger(SystemHealthService.class);

	private final JdbcTemplate jdbcTemplate;
	private final RedisConnectionFactory redisConnectionFactory;
	private final Clock clock;

	public SystemHealthService(
			JdbcTemplate jdbcTemplate,
			RedisConnectionFactory redisConnectionFactory,
			Clock clock) {
		this.jdbcTemplate = jdbcTemplate;
		this.redisConnectionFactory = redisConnectionFactory;
		this.clock = clock;
	}

	@Override
	public SystemHealthResponse check() {
		Map<String, ServiceHealth> services = new LinkedHashMap<>();
		services.put("backend", ServiceHealth.up());
		services.put("database", checkDatabase());
		services.put("redis", checkRedis());

		boolean healthy = services.values().stream().allMatch(ServiceHealth::matchesUpStatus);
		return new SystemHealthResponse(
				healthy ? "UP" : "DOWN",
				Instant.now(clock),
				Collections.unmodifiableMap(services));
	}

	private ServiceHealth checkDatabase() {
		try {
			Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
			return Integer.valueOf(1).equals(result) ? ServiceHealth.up() : ServiceHealth.down();
		} catch (RuntimeException exception) {
			LOGGER.warn("Database health check failed: {}", exception.getClass().getSimpleName());
			return ServiceHealth.down();
		}
	}

	private ServiceHealth checkRedis() {
		try (RedisConnection connection = redisConnectionFactory.getConnection()) {
			return "PONG".equalsIgnoreCase(connection.ping())
					? ServiceHealth.up()
					: ServiceHealth.down();
		} catch (RuntimeException exception) {
			LOGGER.warn("Redis health check failed: {}", exception.getClass().getSimpleName());
			return ServiceHealth.down();
		}
	}
}
