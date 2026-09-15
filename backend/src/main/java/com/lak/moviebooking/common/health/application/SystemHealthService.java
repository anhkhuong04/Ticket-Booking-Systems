package com.lak.moviebooking.common.health.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.lak.moviebooking.common.observability.DependencyHealthMetrics;
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
	private final DependencyHealthMetrics healthMetrics;

	public SystemHealthService(
			JdbcTemplate jdbcTemplate,
			RedisConnectionFactory redisConnectionFactory,
			Clock clock,
			DependencyHealthMetrics healthMetrics) {
		this.jdbcTemplate = jdbcTemplate;
		this.redisConnectionFactory = redisConnectionFactory;
		this.clock = clock;
		this.healthMetrics = healthMetrics;
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
			boolean healthy = Integer.valueOf(1).equals(result);
			healthMetrics.record("postgresql", healthy);
			return healthy ? ServiceHealth.up() : ServiceHealth.down();
		} catch (RuntimeException exception) {
			healthMetrics.record("postgresql", false);
			LOGGER.warn("Dependency health check failed dependency=postgresql exception_type={}",
					exception.getClass().getSimpleName());
			return ServiceHealth.down();
		}
	}

	private ServiceHealth checkRedis() {
		try (RedisConnection connection = redisConnectionFactory.getConnection()) {
			boolean healthy = "PONG".equalsIgnoreCase(connection.ping());
			healthMetrics.record("redis", healthy);
			return healthy ? ServiceHealth.up() : ServiceHealth.down();
		} catch (RuntimeException exception) {
			healthMetrics.record("redis", false);
			LOGGER.warn("Dependency health check failed dependency=redis exception_type={}",
					exception.getClass().getSimpleName());
			return ServiceHealth.down();
		}
	}
}
