package com.example.ecommerce.service;

import com.example.ecommerce.document.Review;
import com.example.ecommerce.repository.mongo.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public Review addReview(Review review) {
        return reviewRepository.save(review);
    }

    public Review addReply(String reviewId, Review.Reply reply) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));
        review.getReplies().add(reply);
        return reviewRepository.save(review);
    }

    public List<Review> getReviewsForProduct(String productId) {
        return reviewRepository.findByProductId(productId);
    }
}
