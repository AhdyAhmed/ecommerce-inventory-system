package com.portfolio.ecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Day 1 smoke test: confirms the Spring context wires up correctly and the
 * application can connect to Postgres.
 *
 * Requires `docker-compose up -d` to be running first (see README), since the
 * "dev" profile points at the real Postgres instance on localhost:5433.
 * From Day 10 onward this gets replaced by Testcontainers-backed integration
 * tests that don't depend on anything running locally.
 */
@SpringBootTest
class EcommerceInventorySystemApplicationTests {

    @Test
    void contextLoads() {
    }

}
