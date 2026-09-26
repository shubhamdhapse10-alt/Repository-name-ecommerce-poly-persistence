package com.example.ecommerce;

import com.example.ecommerce.document.Product;
import com.example.ecommerce.dto.OrderRequest;
import com.example.ecommerce.dto.OrderResponse;
import com.example.ecommerce.entity.Inventory;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.jpa.InventoryRepository;
import com.example.ecommerce.repository.jpa.UserRepository;
import com.example.ecommerce.repository.mongo.ProductRepository;
import com.example.ecommerce.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that spins up REAL Postgres and Mongo instances via
 * Testcontainers instead of mocking the repositories. This exercises the
 * actual transactional behaviour (rollback on insufficient stock, optimistic
 * locking, etc.) that a mocked test would never catch.
 */
@Testcontainers
@SpringBootTest
class OrderServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("ecommerce_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired private OrderService orderService;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private InventoryRepository inventoryRepository;

    private Long userId;
    private String productId;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.builder()
                .email("test@example.com")
                .password("hashed")
                .fullName("Test User")
                .build());
        userId = user.getId();

        Product product = productRepository.save(Product.builder()
                .name("Test Widget")
                .category("gadgets")
                .price(BigDecimal.valueOf(25.00))
                .build());
        productId = product.getId();

        inventoryRepository.save(Inventory.builder()
                .productId(productId)
                .quantity(5)
                .build());
    }

    @Test
    void placeOrder_deductsStockAndCreatesOrderAndPayment() {
        OrderRequest request = new OrderRequest();
        request.setUserId(userId);
        request.setPaymentMethod("CARD");
        request.setItems(List.of(new OrderRequest.Item(productId, 2)));

        OrderResponse response = orderService.placeOrder(request);

        assertEquals("PAID", response.getStatus());
        assertEquals(BigDecimal.valueOf(50.00), response.getTotalAmount());

        Inventory updated = inventoryRepository.findByProductId(productId).orElseThrow();
        assertEquals(3, updated.getQuantity()); // 5 - 2
    }

    @Test
    void placeOrder_throwsAndRollsBackWhenStockInsufficient() {
        OrderRequest request = new OrderRequest();
        request.setUserId(userId);
        request.setPaymentMethod("CARD");
        request.setItems(List.of(new OrderRequest.Item(productId, 100))); // more than the 5 in stock

        assertThrows(IllegalStateException.class, () -> orderService.placeOrder(request));

        // Stock must be unchanged - the whole transaction rolled back.
        Inventory unchanged = inventoryRepository.findByProductId(productId).orElseThrow();
        assertEquals(5, unchanged.getQuantity());
    }
}
