package com.example.ecommerce.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * High-volume, append-only, no-joins-needed data (page views, order status
 * changes, login events). This is the classic "Mongo as a write-heavy
 * event/log store" use case - we don't want every user click hammering
 * Postgres with inserts and bloating a transactional table.
 */
@Document(collection = "activity_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLog {

    @Id
    private String id;

    @Indexed
    private Long userId;

    @Indexed
    private String eventType; // ORDER_PLACED, PRODUCT_VIEWED, LOGIN, etc.

    private Map<String, Object> metadata; // flexible payload per event type

    @Indexed
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
