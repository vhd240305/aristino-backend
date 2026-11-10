package com.aristino.backend.dto.admin;

import com.aristino.backend.entity.User;

import java.time.LocalDateTime;

/** Mot dong trong danh sach tai khoan o trang quan tri. Khong bao gio tra ve mat khau. */
public record AdminUserSummary(
        Long id,
        String fullName,
        String email,
        String phone,
        String status,
        String provider,
        boolean admin,
        LocalDateTime createdAt,
        long orderCount
) {
    // Goi trong @Transactional vi roles la LAZY
    public static AdminUserSummary from(User u, long orderCount) {
        return new AdminUserSummary(
                u.getId(),
                u.getFullName(),
                u.getEmail(),
                u.getPhone(),
                u.getStatus().name(),
                u.getProvider().name(),
                u.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName())),
                u.getCreatedAt(),
                orderCount
        );
    }
}