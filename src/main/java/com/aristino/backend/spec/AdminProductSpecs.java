package com.aristino.backend.spec;

import com.aristino.backend.entity.Product;
import org.springframework.data.jpa.domain.Specification;

/** Dieu kien loc san pham rieng cho trang quan tri (xem them ProductSpecs dung chung). */
public final class AdminProductSpecs {

    private AdminProductSpecs() {
    }

    // Khac voi ProductSpecs.active(): quan tri xem duoc ca san pham dang an
    public static Specification<Product> hasStatus(Product.Status status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }
}