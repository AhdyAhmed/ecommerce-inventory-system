package com.portfolio.ecommerce.repository;

import com.portfolio.ecommerce.domain.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUser_Email(String email, Pageable pageable);

    /**
     * A hand-written JPQL query rather than a derived method name: "orders
     * for this user within this date range" doesn't map cleanly onto a
     * `findByUser_EmailAndCreatedAtBetween` method name once you also want
     * control over fetch joins and ordering.
     *
     * SELECT DISTINCT matters here: fetch-joining a *collection*
     * (`o.items`) produces one result row per (order, item) pair, so without
     * DISTINCT an order with 3 items would appear 3 times in the returned
     * list. A ManyToOne fetch join (like the one in
     * ProductRepository.findLowStock) doesn't have this problem - only
     * collection fetch joins do.
     */
    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.items
            WHERE o.user.email = :email
            AND o.createdAt BETWEEN :from AND :to
            ORDER BY o.createdAt DESC
            """)
    List<Order> findByUserEmailAndDateRange(
            @Param("email") String email,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

}
