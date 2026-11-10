package com.aristino.backend.controller;

import com.aristino.backend.dto.admin.AdminCancelOrderRequest;
import com.aristino.backend.dto.admin.AdminOrderDetail;
import com.aristino.backend.dto.admin.AdminOrderSummary;
import com.aristino.backend.dto.admin.UpdateOrderStatusRequest;
import com.aristino.backend.service.AdminOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

// Toan bo /api/admin/** da duoc SecurityConfig gioi han cho ROLE_ADMIN
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    // GET /api/admin/orders?page=0&size=10&status=PENDING&q=DH2026
    @GetMapping
    public Page<AdminOrderSummary> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q
    ) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return adminOrderService.list(status, q, pageable);
    }

    // GET /api/admin/orders/{id}
    @GetMapping("/{id}")
    public AdminOrderDetail getById(@PathVariable Long id) {
        return adminOrderService.getDetail(id);
    }

    // PUT /api/admin/orders/{id}/status - xac nhan / giao hang / hoan tat
    @PutMapping("/{id}/status")
    public AdminOrderDetail updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return adminOrderService.updateStatus(id, request.getStatus());
    }

    // PUT /api/admin/orders/{id}/cancel - cua hang huy don
    @PutMapping("/{id}/cancel")
    public AdminOrderDetail cancel(@PathVariable Long id, @Valid @RequestBody AdminCancelOrderRequest request) {
        return adminOrderService.cancel(id, request);
    }
}