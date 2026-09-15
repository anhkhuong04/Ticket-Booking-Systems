package com.lak.moviebooking.support.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {

	private static final String REDIS_PASSWORD = "integration-test-password";
	@Container
	private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
			DockerImageName.parse("postgres:17-alpine"))
			.withDatabaseName("lak_test")
			.withUsername("lak_test")
			.withPassword("lak_test_password");
	@Container
	private static final GenericContainer<?> REDIS = new GenericContainer<>(
			DockerImageName.parse("redis:7-alpine"))
			.withCommand("redis-server", "--requirepass", REDIS_PASSWORD)
			.withExposedPorts(6379);

	@DynamicPropertySource
	static void registerContainerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.data.redis.host", REDIS::getHost);
		registry.add("spring.data.redis.port", REDIS::getFirstMappedPort);
		registry.add("spring.data.redis.password", () -> REDIS_PASSWORD);
	}
}
