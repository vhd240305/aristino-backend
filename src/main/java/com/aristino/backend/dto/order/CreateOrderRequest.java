package com.aristino.backend.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrderRequest {

    @NotNull(message = "Phai chon dia chi giao hang")
    private Long addressId;

    // "COD", "VNPAY", hoac "MOMO" - khop voi enum Order.PaymentMethod
    @NotBlank(message = "Phai chon hinh thuc thanh toan")
    private String paymentMethod;
}