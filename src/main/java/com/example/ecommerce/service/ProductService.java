package com.example.ecommerce.service;

import com.example.ecommerce.document.Product;
import com.example.ecommerce.entity.Inventory;
import com.example.ecommerce.repository.jpa.InventoryRepository;
import com.example.ecommerce.repository.mongo.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    /**
     * Creating a product touches BOTH stores: the flexible catalog data goes
     * to Mongo, and a corresponding stock row is created in Postgres.
     * This method is a good talking point for "how do you keep two
     * databases in sync" - here it's simple and synchronous because product
     * creation is a low-frequency, admin-driven operation (unlike checkout,
     * which is high-frequency and where we chose async instead).
     */
    @Transactional
    public Product createProduct(Product product, int initialStock) {
        product.setCreatedAt(LocalDateTime.now());
        Product saved = productRepository.save(product);

        inventoryRepository.save(Inventory.builder()
                .productId(saved.getId())
                .quantity(initialStock)
                .build());

        return saved;
    }

    public Product getProduct(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
    }

    public List<Product> getByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    public List<Product> searchByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    public List<Product> getAll() {
        return productRepository.findAll();
    }
}
