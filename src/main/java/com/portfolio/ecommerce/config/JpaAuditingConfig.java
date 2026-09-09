package com.portfolio.ecommerce.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Turns on Spring Data JPA's auditing so @CreatedDate/@LastModifiedDate in
 * BaseEntity actually get populated on persist/update.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
