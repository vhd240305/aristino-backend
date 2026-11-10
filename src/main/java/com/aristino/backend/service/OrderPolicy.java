package com.aristino.backend.service;

import com.aristino.backend.entity.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Cac quy tac nghiep vu ve thanh toan va huy don, dung chung o moi noi.
 *
 * - Don thanh toan online (VNPay/MoMo) phai duoc thanh toan trong "thoi han thanh toan"
 *   (mac dinh 30 phut ke tu luc dat). Qua han ma chua tra tien thi he thong tu huy don.
 * - Khach chi tu huy duoc don khi cua hang chua xac nhan (trang thai PENDING).
 */
@Component
public class OrderPolicy {

    @Value("${app.order.payment-timeout-minutes:30}")
    private long paymentTimeoutMinutes;

    public boolean isOnline(Order o) {
        return o.getPaymentMethod() != Order.PaymentMethod.COD;
    }

    // Thoi han thanh toan - chi co gia tri khi don dang cho thanh toan online
    public LocalDateTime paymentDeadline(Order o) {
        boolean awaitingPayment = isOnline(o)
                && o.getOrderStatus() == Order.OrderStatus.PENDING
                && (o.getPaymentStatus() == Order.PaymentStatus.PENDING
                    || o.getPaymentStatus() == Order.PaymentStatus.FAILED);
        return awaitingPayment ? o.getCreatedAt().plusMinutes(paymentTimeoutMinutes) : null;
    }

    public boolean isPaymentExpired(Order o) {
        LocalDateTime deadline = paymentDeadline(o);
        return deadline != null && !LocalDateTime.now().isBefore(deadline);
    }

    public boolean canPay(Order o) {
        LocalDateTime deadline = paymentDeadline(o);
        return deadline != null && LocalDateTime.now().isBefore(deadline);
    }

    // null = don khong cho thanh toan; 0 = da qua han (cho he thong tu huy)
    public Long paymentSecondsLeft(Order o) {
        LocalDateTime deadline = paymentDeadline(o);
        if (deadline == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        return now.isBefore(deadline) ? Duration.between(now, deadline).getSeconds() : 0L;
    }

    public boolean canCustomerCancel(Order o) {
        return o.getOrderStatus() == Order.OrderStatus.PENDING;
    }

    // Cac don online chua tra tien tao truoc moc nay deu da qua han
    public LocalDateTime expiryCutoff() {
        return LocalDateTime.now().minusMinutes(paymentTimeoutMinutes);
    }
}