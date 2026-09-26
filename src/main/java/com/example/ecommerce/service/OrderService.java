package com.example.ecommerce.service;

import com.example.ecommerce.document.ActivityLog;
import com.example.ecommerce.document.Product;
import com.example.ecommerce.dto.OrderRequest;
import com.example.ecommerce.dto.OrderResponse;
import com.example.ecommerce.entity.Inventory;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderItem;
import com.example.ecommerce.entity.Payment;
import com.example.ecommerce.repository.jpa.InventoryRepository;
import com.example.ecommerce.repository.jpa.OrderRepository;
import com.example.ecommerce.repository.jpa.PaymentRepository;
import com.example.ecommerce.repository.mongo.ActivityLogRepository;
import com.example.ecommerce.repository.mongo.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This service is the heart of the "why two databases" story.
 *
 * PostgreSQL (via @Transactional) owns: stock deduction, order creation,
 * order line items, payment record - all-or-nothing, ACID guarantees.
 * A crash halfway through rolls everything back; nobody gets charged for
 * an order that never got created, and stock never gets decremented
 * without a corresponding order.
 *
 * MongoDB is used to read product snapshot data (price/name at time of
 * order) and, after the Postgres transaction commits, to write an
 * activity-log event. That write is deliberately OUTSIDE the Postgres
 * transaction and fired asynchronously: it's fine if it lags a few
 * milliseconds behind, and we don't want a slow/unavailable Mongo to
 * block or fail an order that Postgres already committed successfully.
 * This is a small, honest example of eventual consistency between two
 * independent data stores - a very common real-world pattern, and one
 * interviewers like candidates to be able to explain in their own words.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final ActivityLogRepository activityLogRepository;

    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {

        // 1. Fetch product snapshots from Mongo (name + price at order time).
        //    We do this BEFORE opening/using the Postgres transaction resources
        //    so we're not holding DB locks while making a separate-store round trip.
        Map<String, Product> productsById = request.getItems().stream()
                .map(OrderRequest.Item::getProductId)
                .distinct()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> productRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id))
                ));

        // 2. Validate & deduct stock in Postgres, per item, with optimistic locking.
        //    If two requests race for the last unit, the loser's save() throws
        //    OptimisticLockException and the whole @Transactional method rolls back.
        Order order = Order.builder()
                .userId(request.getUserId())
                .status("CREATED")
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (OrderRequest.Item item : request.getItems()) {
            Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                    .orElseThrow(() -> new IllegalStateException("No inventory record for product " + item.getProductId()));

            inventory.deduct(item.getQuantity()); // throws if insufficient stock
            inventoryRepository.save(inventory);   // @Version column enforces optimistic lock on flush

            Product product = productsById.get(item.getProductId());
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(lineTotal);

            order.addItem(OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())   // snapshot, so later Mongo edits don't rewrite history
                    .unitPrice(product.getPrice())     // snapshot
                    .quantity(item.getQuantity())
                    .build());
        }

        order.setTotalAmount(total);
        order = orderRepository.save(order);

        // 3. Create the payment record in the SAME Postgres transaction.
        //    If this fails, stock deduction and order creation roll back too.
        Payment payment = Payment.builder()
                .orderId(order.getId())
                .amount(total)
                .status("SUCCESS") // simplified - a real gateway integration would be async/webhook-driven
                .method(request.getPaymentMethod() == null ? "COD" : request.getPaymentMethod())
                .build();
        payment = paymentRepository.save(payment);

        order.setStatus("PAID");
        orderRepository.save(order);

        // 4. Fire-and-forget write to Mongo AFTER the Postgres transaction's
        //    work is queued to commit. Kept out of @Transactional's scope on purpose.
        logActivityAsync(order.getUserId(), order.getId(), total);

        List<String> lineItems = order.getItems().stream()
                .map(i -> i.getQuantity() + " x " + i.getProductName())
                .toList();

        return OrderResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .paymentId(payment.getId())
                .paymentStatus(payment.getStatus())
                .lineItems(lineItems)
                .build();
    }

    @Async
    public void logActivityAsync(Long userId, Long orderId, BigDecimal amount) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("orderId", orderId);
            metadata.put("amount", amount);

            activityLogRepository.save(ActivityLog.builder()
                    .userId(userId)
                    .eventType("ORDER_PLACED")
                    .metadata(metadata)
                    .build());
        } catch (Exception e) {
            // Deliberately swallow: a logging failure must never fail an already-committed order.
            log.error("Failed to write activity log for order {}", orderId, e);
        }
    }
}
