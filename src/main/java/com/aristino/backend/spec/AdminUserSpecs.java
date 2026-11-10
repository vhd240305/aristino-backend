package com.aristino.backend.spec;

import com.aristino.backend.entity.Role;
import com.aristino.backend.entity.User;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

/** Dieu kien loc tai khoan cho trang quan tri. Dieu kien null = bo qua. */
public final class AdminUserSpecs {

    private AdminUserSpecs() {
    }

    // Tim theo ho ten, email hoac so dien thoai
    public static Specification<User> matches(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String like = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("fullName")), like),
                    cb.like(cb.lower(root.get("email")), like),
                    cb.like(cb.lower(root.get("phone")), like)
            );
        };
    }

    public static Specification<User> hasStatus(User.Status status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    // roleName: "ROLE_ADMIN" hoac "ROLE_CUSTOMER"
    public static Specification<User> hasRole(String roleName) {
        return (root, query, cb) -> {
            if (roleName == null) {
                return null;
            }
            query.distinct(true);
            Join<User, Role> roles = root.join("roles");
            return cb.equal(roles.get("name"), roleName);
        };
    }
}