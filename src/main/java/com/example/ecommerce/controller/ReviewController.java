package com.example.ecommerce.controller;

import com.example.ecommerce.document.Review;
import com.example.ecommerce.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Review addReview(@RequestBody Review review) {
        return reviewService.addReview(review);
    }

    @PostMapping("/{reviewId}/replies")
    public Review addReply(@PathVariable String reviewId, @RequestBody Review.Reply reply) {
        return reviewService.addReply(reviewId, reply);
    }

    @GetMapping("/product/{productId}")
    public List<Review> getForProduct(@PathVariable String productId) {
        return reviewService.getReviewsForProduct(productId);
    }
}
