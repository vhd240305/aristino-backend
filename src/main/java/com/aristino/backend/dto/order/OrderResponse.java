package com.aristino.backend.dto.order;

import com.aristino.backend.entity.Order;
import com.aristino.backend.service.OrderPolicy;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String orderCode;
    private BigDecimal totalAmount;
    private BigDecimal shippingFee;
    private String paymentMethod;
    private String paymentStatus;
    private String orderStatus;
    private LocalDateTime createdAt;
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
    private List<OrderItemResponse> items;

    // Thong tin huy don
    private String cancelReason;
    private String cancelNote;
    private LocalDateTime cancelledAt;

    // Frontend dua vao 3 truong nay de hien thi nut "Huy don", "Thanh toan" va dong ho dem nguoc
    private boolean canCancel;
    private boolean canPay;
    // So giay con lai de thanh toan online (null neu don khong cho thanh toan, 0 neu da qua han)
    private Long paymentSecondsLeft;

    public static OrderResponse from(Order o, OrderPolicy policy) {
        String fullAddress = String.join(", ",
                o.getShippingDetailAddress(), o.getShippingWard(),
                o.getShippingDistrict(), o.getShippingProvince());

        return new OrderResponse(
                o.getId(),
                o.getOrderCode(),
                o.getTotalAmount(),
                o.getShippingFee(),
                o.getPaymentMethod().name(),
                o.getPaymentStatus().name(),
                o.getOrderStatus().name(),
                o.getCreatedAt(),
                o.getRecipientName(),
                o.getRecipientPhone(),
                fullAddress,
                o.getItems().stream().map(OrderItemResponse::from).toList(),
                o.getCancelReason(),
                o.getCancelNote(),
                o.getCancelledAt(),
                policy.canCustomerCancel(o),
                policy.canPay(o),
                policy.paymentSecondsLeft(o)
        );
    }
}