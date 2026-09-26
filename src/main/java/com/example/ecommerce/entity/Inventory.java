package com.example.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Stock is kept in Postgres (not Mongo) because it needs strong consistency:
 * concurrent order placements must never oversell a product.
 * @Version enables optimistic locking - a concurrent update will throw
 * OptimisticLockException instead of silently corrupting stock counts.
 */
@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, unique = true)
    private String productId; // references the Mongo Product._id

    @Column(nullable = false)
    private Integer quantity;

    @Version
    private Long version;

    public void deduct(int amount) {
        if (this.quantity < amount) {
            throw new IllegalStateException("Insufficient stock for product " + productId);
        }
        this.quantity -= amount;
    }
}
