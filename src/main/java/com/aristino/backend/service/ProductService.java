package com.aristino.backend.service;

import com.aristino.backend.dto.product.ProductDetailResponse;
import com.aristino.backend.dto.product.ProductSummaryResponse;
import com.aristino.backend.entity.Product;
import com.aristino.backend.repository.ProductRepository;
import com.aristino.backend.spec.ProductSpecs;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    // Tìm kiếm + lọc + sắp xếp + phân trang (page tính từ 0)
    public Page<ProductSummaryResponse> search(ProductSearchCriteria c, int page, int size) {
        Specification<Product> spec = Specification.where(ProductSpecs.active())
                .and(ProductSpecs.nameContains(c.q()))
                .and(ProductSpecs.inCategory(c.categoryId()))
                .and(ProductSpecs.priceAtLeast(c.minPrice()))
                .and(ProductSpecs.priceAtMost(c.maxPrice()))
                .and(ProductSpecs.hasStockedVariant(c.sizes(), c.colors()));

        Pageable pageable = PageRequest.of(page, size, resolveSort(c.sort()));
        return productRepository.findAll(spec, pageable).map(ProductSummaryResponse::from);
    }

    // Giữ lại 2 hàm cũ để CategoryController hiện có vẫn dùng được
    public Page<ProductSummaryResponse> getAll(Pageable pageable) {
        return search(ProductSearchCriteria.ofCategory(null), pageable.getPageNumber(), pageable.getPageSize());
    }

    public Page<ProductSummaryResponse> getByCategory(Long categoryId, Pageable pageable) {
        return search(ProductSearchCriteria.ofCategory(categoryId), pageable.getPageNumber(), pageable.getPageSize());
    }

    public ProductDetailResponse getBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Khong tim thay san pham: " + slug));
        // San pham dang bi cua hang an thi khong cho xem (du biet duong dan)
        if (product.getStatus() == Product.Status.HIDDEN) {
            throw new EntityNotFoundException("Khong tim thay san pham: " + slug);
        }
        return ProductDetailResponse.from(product);
    }

    // Chỉ cho phép các kiểu sắp xếp có trong danh sách (không nhận tên cột tùy ý từ người dùng)
    private Sort resolveSort(String sort) {
        Sort byIdDesc = Sort.by(Sort.Direction.DESC, "id");
        if (sort == null) sort = "newest";
        return switch (sort) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "basePrice").and(byIdDesc);
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "basePrice").and(byIdDesc);
            case "name_asc" -> Sort.by(Sort.Direction.ASC, "name").and(byIdDesc);
            default -> Sort.by(Sort.Direction.DESC, "createdAt").and(byIdDesc); // newest
        };
    }
}