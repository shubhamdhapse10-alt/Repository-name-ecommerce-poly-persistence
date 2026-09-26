package com.example.ecommerce.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Product catalog lives in MongoDB because different product categories
 * have wildly different attributes (a phone has RAM/storage; a shirt has
 * size/color/fabric). Modeling that in a fixed relational schema means
 * either a huge sparse table or an EAV pattern - Mongo's document model
 * fits naturally instead.
 */
@Document(collection = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    private String id;

    @Indexed
    private String name;

    @Indexed
    private String category;

    private String description;

    private BigDecimal price;

    private List<String> imageUrls;

    // Category-specific, schema-less attributes, e.g.
    // { "ram": "8GB", "storage": "128GB" } or { "size": "M", "color": "Blue" }
    private Map<String, Object> attributes;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}
