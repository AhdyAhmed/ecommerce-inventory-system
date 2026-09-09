package com.portfolio.ecommerce.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Common identity + audit fields for every entity. Kept as a @MappedSuperclass
 * (not @Entity) so it doesn't get its own table - fields are flattened into
 * each subclass's table instead.
 *
 * equals()/hashCode() are based on id only (see @EqualsAndHashCode.Include),
 * which is the standard-safe approach for JPA entities: it avoids pulling in
 * mutable/lazy fields and avoids infinite recursion on bidirectional
 * relationships. It does mean two brand-new, unsaved entities (id == null)
 * are only equal to themselves - which is the correct behavior.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
