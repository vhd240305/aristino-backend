package com.aristino.backend.dto.review;

/**
 * Khach co duoc danh gia san pham nay khong?
 * reason: OK | NOT_PURCHASED (chua mua / chua nhan hang) | ALREADY_REVIEWED (da danh gia roi)
 */
public record ReviewEligibilityResponse(boolean canReview, String reason) {
}