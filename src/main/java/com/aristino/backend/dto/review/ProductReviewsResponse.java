package com.aristino.backend.dto.review;

import org.springframework.data.domain.Page;

import java.util.Map;

/** Tong hop danh gia cua mot san pham + mot trang danh sach danh gia. */
public record ProductReviewsResponse(
        double averageRating,                // diem trung binh, lam tron 1 chu so thap phan (0 neu chua co danh gia)
        long totalReviews,
        Map<Integer, Long> distribution,     // so danh gia theo tung muc sao: 5, 4, 3, 2, 1
        Page<ReviewResponse> reviews
) {
}