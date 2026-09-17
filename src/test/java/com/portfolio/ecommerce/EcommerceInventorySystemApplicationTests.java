package com.portfolio.ecommerce;

import org.junit.jupiter.api.Test;

/**
 * Day 1 smoke test: confirms the Spring context wires up correctly and the
 * application can connect to a database.
 *
 * Originally required `docker-compose up -d` against the real dev Postgres
 * on localhost:5433. As of Day 10, it extends {@link AbstractIntegrationTest}
 * instead, so it runs against a disposable Testcontainers-managed Postgres
 * the same way every other integration test does - `mvn test` now needs
 * nothing running locally beyond a Docker daemon, not a manually-started
 * docker-compose stack.
 */
class EcommerceInventorySystemApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }

}
