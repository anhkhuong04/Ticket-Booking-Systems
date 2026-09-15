package com.lak.moviebooking.support.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

class InfrastructureSmokeIT extends AbstractIntegrationTest {

	@Autowired
	private Flyway flyway;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private StringRedisTemplate redisTemplate;

	@Test
	@Sql("/fixtures/minimal.sql")
	void appliesMigrationsAndLoadsFixtureIntoEmptyPostgres() {
		Integer appliedMigrations = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM flyway_schema_history WHERE success",
				Integer.class);
		String fixtureLabel = jdbcTemplate.queryForObject(
				"SELECT label FROM integration_test_fixture WHERE id = '00000000-0000-0000-0000-000000000001'",
				String.class);

		assertThat(flyway.info().current()).isNotNull();
		assertThat(appliedMigrations).isPositive();
		assertThat(fixtureLabel).isEqualTo("minimal-fixture");
	}

	@Test
	void connectsToRealRedisContainer() {
		String key = "integration:smoke";
		redisTemplate.opsForValue().set(key, "ready", Duration.ofSeconds(30));

		assertThat(redisTemplate.opsForValue().get(key)).isEqualTo("ready");

		redisTemplate.delete(key);
	}
}
