package com.aristino.backend.service;

import com.aristino.backend.common.CurrentUserProvider;
import com.aristino.backend.dto.review.CreateReviewRequest;
import com.aristino.backend.dto.review.ProductReviewsResponse;
import com.aristino.backend.dto.review.ReviewEligibilityResponse;
import com.aristino.backend.dto.review.ReviewResponse;
import com.aristino.backend.entity.Order;
import com.aristino.backend.entity.OrderItem;
import com.aristino.backend.entity.Product;
import com.aristino.backend.entity.Review;
import com.aristino.backend.entity.User;
import com.aristino.backend.repository.OrderItemRepository;
import com.aristino.backend.repository.ProductRepository;
import com.aristino.backend.repository.ReviewRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Danh gia san pham (Phuong an 2 da chon o Chuong 1):
 * - Chi khach da MUA va NHAN HANG (don o trang thai COMPLETED) moi duoc danh gia san pham do.
 * - Moi khach chi danh gia MOT lan cho moi san pham (database cung co rang buoc UNIQUE(user_id, product_id)).
 * - Danh gia gom so sao (1-5, bat buoc) va nhan xet (khong bat buoc), lien ket toi dong don hang da mua.
 * - Moi nguoi xem duoc danh gia; ten nguoi danh gia duoc che bot.
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final int MAX_PAGE_SIZE = 20;

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final CurrentUserProvider currentUserProvider;

    // ===== Xem danh gia (cong khai) =====

    @Transactional(readOnly = true)
    public ProductReviewsResponse getProductReviews(String slug, int page, int size) {
        Product product = productRepository.findBySlug(slug)
                .filter(p -> p.getStatus() != Product.Status.HIDDEN)
                .orElseThrow(() -> new EntityNotFoundException("Khong tim thay san pham: " + slug));

        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int stars = 5; stars >= 1; stars--) {
            distribution.put(stars, 0L);
        }
        long total = 0;
        long starSum = 0;
        for (Object row : reviewRepository.countByRating(product.getId())) {
            Object[] r = (Object[]) row;
            int stars = ((Number) r[0]).intValue();
            long count = ((Number) r[1]).longValue();
            distribution.put(stars, count);
            total += count;
            starSum += stars * count;
        }
        double average = total == 0 ? 0 : Math.round(starSum * 10.0 / total) / 10.0;

        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<ReviewResponse> reviews = reviewRepository.findByProductId(product.getId(), pageable)
                .map(ReviewResponse::from);

        return new ProductReviewsResponse(average, total, distribution, reviews);
    }

    // ===== Khach dang nhap: co duoc danh gia san pham nay khong? =====

    @Transactional(readOnly = true)
    public ReviewEligibilityResponse eligibility(Long productId) {
        User user = currentUserProvider.getCurrentUser();
        if (reviewRepository.findByUserIdAndProductId(user.getId(), productId).isPresent()) {
            return new ReviewEligibilityResponse(false, "ALREADY_REVIEWED");
        }
        if (findPurchasedItem(user.getId(), productId) == null) {
            return new ReviewEligibilityResponse(false, "NOT_PURCHASED");
        }
        return new ReviewEligibilityResponse(true, "OK");
    }

    // ===== Viet danh gia =====

    @Transactional
    public ReviewResponse create(CreateReviewRequest request) {
        User user = currentUserProvider.getCurrentUser();
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Khong tim thay san pham"));

        if (reviewRepository.findByUserIdAndProductId(user.getId(), product.getId()).isPresent()) {
            throw new IllegalStateException("Ban da danh gia san pham nay roi");
        }
        OrderItem purchased = findPurchasedItem(user.getId(), product.getId());
        if (purchased == null) {
            throw new IllegalStateException("Chi khach hang da mua va nhan san pham moi duoc danh gia");
        }

        String comment = request.getComment() == null ? "" : request.getComment().trim();
        Review review = Review.builder()
                .product(product)
                .user(user)
                .orderItem(purchased)
                .rating(request.getRating().byteValue())
                .comment(comment.isEmpty() ? null : comment)
                .build();

        try {
            return ReviewResponse.from(reviewRepository.saveAndFlush(review));
        } catch (DataIntegrityViolationException ex) {
            // Hai yeu cau gui cung luc: rang buoc UNIQUE cua database chan yeu cau den sau
            throw new IllegalStateException("Ban da danh gia san pham nay roi");
        }
    }

    // Dong don hang gan nhat cua khach co san pham nay trong don da HOAN TAT (null neu chua mua / chua nhan hang)
    private OrderItem findPurchasedItem(Long userId, Long productId) {
        List<OrderItem> items = orderItemRepository.findPurchasedItems(
                userId, productId, Order.OrderStatus.COMPLETED, PageRequest.of(0, 1));
        return items.isEmpty() ? null : items.get(0);
    }
}