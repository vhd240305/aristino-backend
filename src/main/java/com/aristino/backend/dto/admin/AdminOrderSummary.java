package com.aristino.backend.dto.admin;

import com.aristino.backend.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Mot dong trong danh sach don hang o trang quan tri. */
public record AdminOrderSummary(
        Long id,
        String orderCode,
        String customerName,
        String customerEmail,
        String recipientName,
        String recipientPhone,
        BigDecimal totalAmount,
        String paymentMethod,
        String paymentStatus,
        String orderStatus,
        LocalDateTime createdAt,
        int itemCount
) {
    public static AdminOrderSummary from(Order o) {
        return new AdminOrderSummary(
                o.getId(),
                o.getOrderCode(),
                o.getUser().getFullName(),
                o.getUser().getEmail(),
                o.getRecipientName(),
                o.getRecipientPhone(),
                o.getTotalAmount(),
                o.getPaymentMethod().name(),
                o.getPaymentStatus().name(),
                o.getOrderStatus().name(),
                o.getCreatedAt(),
                o.getItems().stream().mapToInt(i -> i.getQuantity()).sum()
        );
    }
}