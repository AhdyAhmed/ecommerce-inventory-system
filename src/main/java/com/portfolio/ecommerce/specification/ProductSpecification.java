package com.portfolio.ecommerce.specification;

import com.portfolio.ecommerce.domain.Product;
import com.portfolio.ecommerce.dto.product.ProductSearchCriteria;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * Builds a {@link Specification} for {@link Product} out of independent,
 * individually-nullable filters, then combines whichever ones the caller
 * actually supplied.
 *
 * Each factory method returns a {@code Specification} whose {@code toPredicate}
 * returns {@code null} when its own filter value is absent. That's not a bug -
 * Spring Data's {@code Specification.and(...)} composition treats a
 * {@code null} predicate as "no-op" and drops it from the final query, which
 * is exactly what makes these composable: {@code /products/search} with no
 * params at all degrades gracefully to "match everything", and any subset of
 * {@code name}/{@code categoryId}/{@code minPrice}/{@code maxPrice}/{@code inStock}
 * can be combined without a combinatorial explosion of hand-written queries.
 */
public final class ProductSpecification {

    private ProductSpecification() {
        // static factory holder - not meant to be instantiated
    }

    public static Specification<Product> hasCategory(Long categoryId) {
        return (root, query, criteriaBuilder) -> categoryId == null
                ? null
                : criteriaBuilder.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> nameContains(String name) {
        return (root, query, criteriaBuilder) -> (name == null || name.isBlank())
                ? null
                : criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Product> priceGreaterThanOrEqualTo(BigDecimal minPrice) {
        return (root, query, criteriaBuilder) -> minPrice == null
                ? null
                : criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Product> priceLessThanOrEqualTo(BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) -> maxPrice == null
                ? null
                : criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    /**
     * {@code true} -> stockQuantity > 0, {@code false} -> stockQuantity == 0,
     * {@code null} -> filter not applied.
     */
    public static Specification<Product> inStock(Boolean inStock) {
        return (root, query, criteriaBuilder) -> {
            if (inStock == null) {
                return null;
            }
            return inStock
                    ? criteriaBuilder.greaterThan(root.get("stockQuantity"), 0)
                    : criteriaBuilder.equal(root.get("stockQuantity"), 0);
        };
    }

    /**
     * Combines every filter in {@code criteria} into a single AND-ed
     * Specification. Starting from {@code Specification.where(null)} (rather
     * than one of the concrete filters) keeps this method symmetric - no
     * filter is treated as the "base" case that the others get bolted onto.
     */
    public static Specification<Product> fromCriteria(ProductSearchCriteria criteria) {
        return Specification.<Product>where(null)
                .and(nameContains(criteria.getName()))
                .and(hasCategory(criteria.getCategoryId()))
                .and(priceGreaterThanOrEqualTo(criteria.getMinPrice()))
                .and(priceLessThanOrEqualTo(criteria.getMaxPrice()))
                .and(inStock(criteria.getInStock()));
    }

}
