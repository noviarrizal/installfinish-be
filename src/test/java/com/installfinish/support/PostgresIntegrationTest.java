package com.installfinish.support;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for tests against a real PostgreSQL database, with Flyway migrations and Hibernate schema validation
 * running exactly as in production.
 *
 * <p>Database source, in order of preference:
 * <ol>
 *   <li>{@code TEST_DATABASE_URL} (+ {@code TEST_DATABASE_USERNAME}/{@code TEST_DATABASE_PASSWORD}) — an existing,
 *       disposable database, for machines and CI runners without Docker;</li>
 *   <li>a Testcontainers PostgreSQL 17 container, shared by all test classes in the run.</li>
 * </ol>
 * If neither is available the tests are disabled and reported as skipped, never as passed.
 *
 * <p>With {@code INSTALLFINISH_REQUIRE_TESTCONTAINERS=true} (set in CI) only the Testcontainers path is allowed: a set
 * {@code TEST_DATABASE_URL} or an unavailable Docker daemon fails the run instead of falling back or skipping.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtConfig.class)
@ExtendWith(PostgresAvailableCondition.class)
public abstract class PostgresIntegrationTest {

    private static final boolean REQUIRE_TESTCONTAINERS =
            Boolean.parseBoolean(System.getenv("INSTALLFINISH_REQUIRE_TESTCONTAINERS"));
    private static final String EXTERNAL_URL = System.getenv("TEST_DATABASE_URL");
    private static final PostgreSQLContainer CONTAINER = startContainerIfNeeded();

    static boolean databaseAvailable() {
        return EXTERNAL_URL != null || CONTAINER != null;
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        if (EXTERNAL_URL != null) {
            registry.add("spring.datasource.url", () -> EXTERNAL_URL);
            registry.add("spring.datasource.username", () -> System.getenv("TEST_DATABASE_USERNAME"));
            registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("TEST_DATABASE_PASSWORD", ""));
        } else if (CONTAINER != null) {
            registry.add("spring.datasource.url", CONTAINER::getJdbcUrl);
            registry.add("spring.datasource.username", CONTAINER::getUsername);
            registry.add("spring.datasource.password", CONTAINER::getPassword);
        }
    }

    private static PostgreSQLContainer startContainerIfNeeded() {
        if (REQUIRE_TESTCONTAINERS) {
            if (EXTERNAL_URL != null) {
                throw new IllegalStateException(
                        "INSTALLFINISH_REQUIRE_TESTCONTAINERS is set, so TEST_DATABASE_URL must not be set");
            }
            if (!DockerClientFactory.instance().isDockerAvailable()) {
                throw new IllegalStateException("INSTALLFINISH_REQUIRE_TESTCONTAINERS is set but Docker is not available");
            }
        }
        if (EXTERNAL_URL != null) {
            System.out.println("[PostgresIntegrationTest] PostgreSQL source: TEST_DATABASE_URL");
            return null;
        }
        if (!DockerClientFactory.instance().isDockerAvailable()) {
            return null;
        }
        PostgreSQLContainer container = new PostgreSQLContainer("postgres:17-alpine");
        container.start();
        System.out.println("[PostgresIntegrationTest] PostgreSQL source: Testcontainers " + container.getDockerImageName());
        return container;
    }
}
