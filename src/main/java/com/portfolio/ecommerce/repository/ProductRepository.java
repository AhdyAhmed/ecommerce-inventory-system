package com.portfolio.ecommerce.repository;

import com.portfolio.ecommerce.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    List<Product> findByCategory_Name(String categoryName);

    /**
     * Products at or below a stock threshold, cheapest-on-shelf-space-first
     * (lowest stock first). The category is fetch-joined so callers can read
     * `category.getName()` without an extra lazy-load query per product -
     * safe here because ManyToOne joins don't multiply result rows the way a
     * collection fetch join would.
     */
    @Query("""
            SELECT p FROM Product p
            LEFT JOIN FETCH p.category
            WHERE p.stockQuantity <= :threshold
            ORDER BY p.stockQuantity ASC
            """)
    List<Product> findLowStock(@Param("threshold") int threshold);

}
