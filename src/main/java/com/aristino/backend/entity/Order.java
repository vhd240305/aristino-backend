package com.aristino.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    public enum PaymentMethod { COD, VNPAY, MOMO }
    // REFUNDED: don da thanh toan online roi bi huy -> da hoan tien
    public enum PaymentStatus { PENDING, PAID, FAILED, REFUNDED }
    public enum OrderStatus {
        PENDING, CONFIRMED, SHIPPING, COMPLETED, CANCELLED,
        RETURN_REQUESTED, RETURNED, REFUNDED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "order_code", nullable = false, unique = true, length = 30)
    private String orderCode;

    // Tham chieu toi dia chi goc trong so dia chi - co the null neu dia chi bi xoa sau nay
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private Address address;

    // ===== Snapshot dia chi giao hang tai thoi diem dat don =====
    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 20)
    private String recipientPhone;

    @Column(name = "shipping_province", nullable = false, length = 100)
    private String shippingProvince;

    @Column(name = "shipping_district", nullable = false, length = 100)
    private String shippingDistrict;

    @Column(name = "shipping_ward", nullable = false, length = 100)
    private String shippingWard;

    @Column(name = "shipping_detail_address", nullable = false, length = 255)
    private String shippingDetailAddress;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "shipping_fee", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.COD;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", length = 30)
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.PENDING;

    // ===== Thong tin huy don (chi co gia tri khi orderStatus = CANCELLED) =====
    // Ma ly do: CHANGED_MIND, WRONG_ITEM, CHANGE_ADDRESS, BETTER_PRICE, PAYMENT_PROBLEM, OTHER,
    // hoac PAYMENT_EXPIRED (he thong tu huy do qua han thanh toan)
    @Column(name = "cancel_reason", length = 30)
    private String cancelReason;

    @Column(name = "cancel_note", length = 255)
    private String cancelNote;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<OrderItem> items = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}