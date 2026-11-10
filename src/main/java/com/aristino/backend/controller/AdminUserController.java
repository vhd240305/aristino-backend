package com.aristino.backend.controller;

import com.aristino.backend.dto.admin.AdminUserDetail;
import com.aristino.backend.dto.admin.AdminUserSummary;
import com.aristino.backend.dto.admin.UserAdminRoleRequest;
import com.aristino.backend.dto.admin.UserStatusRequest;
import com.aristino.backend.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    // GET /api/admin/users?page=0&size=10&q=an&role=ADMIN&status=LOCKED
    @GetMapping
    public Page<AdminUserSummary> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status
    ) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "id"));
        return adminUserService.list(q, role, status, pageable);
    }

    // GET /api/admin/users/{id}
    @GetMapping("/{id}")
    public AdminUserDetail getById(@PathVariable Long id) {
        return adminUserService.getDetail(id);
    }

    // PUT /api/admin/users/{id}/status - khoa / mo khoa tai khoan
    @PutMapping("/{id}/status")
    public AdminUserDetail updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        return adminUserService.updateStatus(id, request.getStatus());
    }

    // PUT /api/admin/users/{id}/admin - cap / thu hoi quyen quan tri
    @PutMapping("/{id}/admin")
    public AdminUserDetail setAdmin(@PathVariable Long id, @Valid @RequestBody UserAdminRoleRequest request) {
        return adminUserService.setAdmin(id, request.getAdmin());
    }
}