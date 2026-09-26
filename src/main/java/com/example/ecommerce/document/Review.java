package com.example.ecommerce.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reviews are a natural fit for a document store: each review can have a
 * variable number of nested replies, no join needed to fetch a review with
 * all its replies in one read - exactly the kind of nested/variable-depth
 * data Mongo handles more naturally than a relational join.
 */
@Document(collection = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    private String id;

    @Indexed
    private String productId; // references Mongo Product._id

    private Long userId;      // references Postgres users.id
    private String userName;  // denormalized snapshot for fast display

    private Integer rating;   // 1-5
    private String comment;

    @Builder.Default
    private List<Reply> replies = new ArrayList<>();

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Reply {
        private Long userId;
        private String userName;
        private String comment;
        @Builder.Default
        private LocalDateTime createdAt = LocalDateTime.now();
    }
}
