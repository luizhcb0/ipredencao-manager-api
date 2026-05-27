package org.ipredencao.ipredencao_manager.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ipredencao.ipredencao_manager.config.FirebaseConfig;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base for integration test suites. Boots a Postgres container once per test run,
 * applies the full Liquibase changelog, and exposes {@link DSLContext}, {@link MockMvc}
 * and {@link ObjectMapper} for subclasses.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public abstract class IntegrationTestBase {

    static {
        // Docker Engine 29+ requires MinAPIVersion 1.44; docker-java still defaults to 1.41.
        // Must run before any Testcontainers class is loaded.
        System.setProperty("api.version", "1.44");
    }

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("ipredencao_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(true);

    /** Neutralizes {@code @PostConstruct} that would try to load Firebase credentials. */
    @MockitoBean
    protected FirebaseConfig firebaseConfig;

    @Autowired protected DSLContext dsl;
    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
}
