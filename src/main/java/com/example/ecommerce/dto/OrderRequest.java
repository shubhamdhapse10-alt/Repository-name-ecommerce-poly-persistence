package com.example.ecommerce.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    @NotNull
    private Long userId;

    @NotEmpty
    private List<@Valid Item> items;

    private String paymentMethod; // CARD, UPI, COD

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        @NotNull
        private String productId; // Mongo Product._id

        @NotNull
        private Integer quantity;
    }
}
