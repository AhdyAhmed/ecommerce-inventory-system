package com.portfolio.ecommerce.seed;

import com.portfolio.ecommerce.domain.Category;
import com.portfolio.ecommerce.domain.Order;
import com.portfolio.ecommerce.domain.OrderItem;
import com.portfolio.ecommerce.domain.Product;
import com.portfolio.ecommerce.domain.Tag;
import com.portfolio.ecommerce.domain.User;
import com.portfolio.ecommerce.domain.enums.OrderStatus;
import com.portfolio.ecommerce.repository.CategoryRepository;
import com.portfolio.ecommerce.repository.OrderRepository;
import com.portfolio.ecommerce.repository.ProductRepository;
import com.portfolio.ecommerce.repository.TagRepository;
import com.portfolio.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Populates a small, realistic dataset for local development so there's
 * something to query as soon as the app starts, without needing controllers
 * yet (those land Day 4+).
 *
 * Guarded by the "dev" profile so it never runs during tests or in a
 * production-like environment, and guarded by a row-count check so restarting
 * the app doesn't keep re-inserting duplicates.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final TagRepository tagRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (productRepository.count() > 0) {
            log.info("Seed data already present - skipping dev data seeding.");
            return;
        }

        log.info("Seeding dev data...");

        Category electronics = categoryRepository.save(Category.builder().name("Electronics").build());
        Category books = categoryRepository.save(Category.builder().name("Books").build());

        Tag bestseller = tagRepository.save(Tag.builder().name("bestseller").build());
        Tag newArrival = tagRepository.save(Tag.builder().name("new-arrival").build());
        Tag onSale = tagRepository.save(Tag.builder().name("sale").build());

        Product laptop = productRepository.save(Product.builder()
                .name("14-inch Laptop")
                .description("Lightweight laptop for everyday use")
                .sku("ELEC-LAPTOP-001")
                .price(new BigDecimal("999.99"))
                .stockQuantity(25)
                .category(electronics)
                .tags(Set.of(bestseller))
                .build());

        Product headphones = productRepository.save(Product.builder()
                .name("Wireless Headphones")
                .description("Over-ear noise-cancelling headphones")
                .sku("ELEC-HEADPHONE-001")
                .price(new BigDecimal("149.50"))
                .stockQuantity(60)
                .category(electronics)
                .tags(Set.of(newArrival, onSale))
                .build());

        Product programmingBook = productRepository.save(Product.builder()
                .name("The Pragmatic Programmer")
                .description("Classic software craftsmanship book")
                .sku("BOOK-PRAGPROG-001")
                .price(new BigDecimal("39.99"))
                .stockQuantity(100)
                .category(books)
                .tags(Set.of(bestseller))
                .build());

        productRepository.save(Product.builder()
                .name("The Joy of Cooking")
                .description("Comprehensive home cooking reference")
                .sku("BOOK-COOKING-001")
                .price(new BigDecimal("24.99"))
                .stockQuantity(40)
                .category(books)
                .build());

        User alice = userRepository.save(User.builder()
                .fullName("Alice Johnson")
                .email("alice@example.com")
                .build());

        User bob = userRepository.save(User.builder()
                .fullName("Bob Smith")
                .email("bob@example.com")
                .build());

        Order aliceOrder = Order.builder()
                .user(alice)
                .status(OrderStatus.CONFIRMED)
                .totalAmount(BigDecimal.ZERO)
                .build();
        aliceOrder.addItem(OrderItem.builder()
                .product(laptop)
                .quantity(1)
                .unitPrice(laptop.getPrice())
                .build());
        aliceOrder.addItem(OrderItem.builder()
                .product(programmingBook)
                .quantity(2)
                .unitPrice(programmingBook.getPrice())
                .build());
        aliceOrder.setTotalAmount(calculateTotal(aliceOrder));
        orderRepository.save(aliceOrder);

        Order bobOrder = Order.builder()
                .user(bob)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .build();
        bobOrder.addItem(OrderItem.builder()
                .product(headphones)
                .quantity(1)
                .unitPrice(headphones.getPrice())
                .build());
        bobOrder.setTotalAmount(calculateTotal(bobOrder));
        orderRepository.save(bobOrder);

        log.info("Seed complete: {} categories, {} tags, {} products, {} users, {} orders",
                categoryRepository.count(), tagRepository.count(), productRepository.count(),
                userRepository.count(), orderRepository.count());
    }

    private BigDecimal calculateTotal(Order order) {
        return order.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
