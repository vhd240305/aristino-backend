package com.aristino.backend.dto.admin;

import com.aristino.backend.entity.Product;
import com.aristino.backend.entity.ProductImage;
import com.aristino.backend.entity.ProductVariant;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;

/** Mot dong trong danh sach san pham o trang quan tri. */
public record AdminProductSummary(
        Long id,
        String name,
        String slug,
        Long categoryId,
        String categoryName,
        BigDecimal basePrice,
        String status,
        String thumbnailUrl,
        int variantCount,
        int totalStock,
        LocalDateTime createdAt
) {
    public static AdminProductSummary from(Product p) {
        String thumbnail = p.getImages().stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsThumbnail()))
                .findFirst()
                .or(() -> p.getImages().stream()
                        .min(Comparator.comparingInt((ProductImage i) -> i.getSortOrder() == null ? 0 : i.getSortOrder())))
                .map(ProductImage::getImageUrl)
                .orElse(null);

        return new AdminProductSummary(
                p.getId(),
                p.getName(),
                p.getSlug(),
                p.getCategory().getId(),
                p.getCategory().getName(),
                p.getBasePrice(),
                p.getStatus().name(),
                thumbnail,
                p.getVariants().size(),
                p.getVariants().stream().mapToInt((ProductVariant v) -> v.getStockQuantity()).sum(),
                p.getCreatedAt()
        );
    }
}