package com.portfolio.ecommerce;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for full-stack integration tests: a real Spring context, real
 * MockMvc dispatch through the actual controllers -> services -> repositories,
 * and a real Postgres instance via Testcontainers rather than mocks or an
 * in-memory substitute like H2 that doesn't actually behave like Postgres.
 * This is what catches what Day 9's mocked-repository unit tests structurally
 * cannot: real constraint violations, real cascades, and real transactional
 * rollback under an actual failure.
 *
 * {@code @ServiceConnection} (Spring Boot 3.1+) wires the running container's
 * JDBC URL and credentials into the "test" profile's DataSource on its own -
 * no manual {@code @DynamicPropertySource} block needed.
 *
 * The container is a {@code static} field, so Testcontainers starts it once
 * per JVM and every subclass shares that one instance (the "singleton
 * container" pattern) instead of paying container-startup cost per test
 * class.
 *
 * {@code @Transactional} on the base class wraps every test method in a
 * transaction that Spring's test support rolls back automatically once the
 * method finishes - including whatever the service layer did through its
 * own {@code @Transactional} methods, since those simply join the
 * surrounding test transaction rather than starting a second one. That gives
 * every test method a clean slate with no hand-written cleanup, and it's
 * also exactly what makes {@code OrderIntegrationTest}'s real-rollback
 * assertions meaningful: a service-layer method that fails partway through
 * really does undo everything it had done so far, in a real database, not
 * just "the exception was thrown" as a mocked-repository test could only
 * ever claim.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Transactional
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

}
