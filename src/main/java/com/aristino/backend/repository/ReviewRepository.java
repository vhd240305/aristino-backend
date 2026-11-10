package com.aristino.backend.repository;

import com.aristino.backend.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProductId(Long productId);

    // Danh sach danh gia cua mot san pham, co phan trang
    Page<Review> findByProductId(Long productId, Pageable pageable);

    // Dung de kiem tra user da review san pham nay chua truoc khi cho tao review moi
    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);

    // So danh gia theo tung muc sao: moi dong la [so sao, so luong]
    @Query("select r.rating, count(r) from Review r where r.product.id = :productId and r.rating is not null group by r.rating")
    List<Object[]> countByRating(@Param("productId") Long productId);
}