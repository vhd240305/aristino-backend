package com.aristino.backend.controller;

import com.aristino.backend.dto.review.CreateReviewRequest;
import com.aristino.backend.dto.review.ProductReviewsResponse;
import com.aristino.backend.dto.review.ReviewEligibilityResponse;
import com.aristino.backend.dto.review.ReviewResponse;
import com.aristino.backend.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // GET /api/products/{slug}/reviews?page=0&size=5 - tong hop va danh sach danh gia (cong khai, khong can dang nhap)
    @GetMapping("/products/{slug}/reviews")
    public ProductReviewsResponse list(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        return reviewService.getProductReviews(slug, page, size);
    }

    // GET /api/reviews/eligibility?productId=1 - khach dang nhap co duoc danh gia san pham nay khong
    @GetMapping("/reviews/eligibility")
    public ReviewEligibilityResponse eligibility(@RequestParam Long productId) {
        return reviewService.eligibility(productId);
    }

    // POST /api/reviews - viet danh gia (can dang nhap, da mua va nhan san pham)
    @PostMapping("/reviews")
    public ReviewResponse create(@Valid @RequestBody CreateReviewRequest request) {
        return reviewService.create(request);
    }
}