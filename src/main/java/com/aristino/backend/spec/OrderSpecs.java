package com.aristino.backend.spec;

import com.aristino.backend.entity.Order;
import org.springframework.data.jpa.domain.Specification;

/** Cac dieu kien loc don hang cho trang quan tri. Dieu kien null = bo qua. */
public final class OrderSpecs {

    private OrderSpecs() {
    }

    public static Specification<Order> hasStatus(Order.OrderStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("orderStatus"), status);
    }

    // Tim theo ma don, ten nguoi nhan hoac so dien thoai nguoi nhan
    public static Specification<Order> matches(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String kw = keyword.trim().toLowerCase();
            String like = "%" + kw + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("orderCode")), like),
                    cb.like(cb.lower(root.get("recipientName")), like),
                    cb.like(root.get("recipientPhone"), "%" + keyword.trim() + "%")
            );
        };
    }
}