package com.aristino.backend.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Chi tiet tai khoan: thong tin, tong tien da mua (don hoan tat) va 10 don gan nhat. */
public record AdminUserDetail(
        AdminUserSummary user,
        BigDecimal totalSpent,
        List<RecentOrder> recentOrders
) {
    public record RecentOrder(Long id, String orderCode, BigDecimal totalAmount, String orderStatus, LocalDateTime createdAt) {
    }
}