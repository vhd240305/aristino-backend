package com.aristino.backend.dto.review;

import com.aristino.backend.entity.Review;

import java.time.LocalDateTime;

/** Mot danh gia hien thi cong khai. Ten nguoi danh gia duoc che bot de bao ve rieng tu. */
public record ReviewResponse(Long id, int rating, String comment, String reviewerName, LocalDateTime createdAt) {

    // Goi trong @Transactional vi user la LAZY
    public static ReviewResponse from(Review r) {
        return new ReviewResponse(
                r.getId(),
                r.getRating() == null ? 0 : r.getRating().intValue(),
                r.getComment(),
                maskName(r.getUser().getFullName()),
                r.getCreatedAt()
        );
    }

    // "Nguyen Van Test" -> "Nguyen T."; mot tu duy nhat -> "N***"; rong -> "" (frontend hien "Khach hang")
    static String maskName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].charAt(0) + "***";
        }
        return parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
    }
}