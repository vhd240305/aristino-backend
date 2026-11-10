package com.aristino.backend.controller;

import com.aristino.backend.dto.order.OrderResponse;
import com.aristino.backend.dto.payment.PaymentResultRequest;
import com.aristino.backend.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // POST /api/payments/{orderId}/confirm - cong thanh toan GIA LAP bao ket qua (SUCCESS / FAILED)
    @PostMapping("/{orderId}/confirm")
    public OrderResponse confirm(@PathVariable Long orderId, @Valid @RequestBody PaymentResultRequest request) {
        return paymentService.confirmMockPayment(orderId, request);
    }
}