package com.aristino.backend.controller;

import com.aristino.backend.dto.admin.AdminProductDetail;
import com.aristino.backend.dto.admin.AdminProductRequest;
import com.aristino.backend.dto.admin.AdminProductSummary;
import com.aristino.backend.dto.admin.ProductStatusRequest;
import com.aristino.backend.service.AdminProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;

    // GET /api/admin/products?page=0&size=10&q=ao&categoryId=1&status=HIDDEN
    @GetMapping
    public Page<AdminProductSummary> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status
    ) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "id"));
        return adminProductService.list(q, categoryId, status, pageable);
    }

    // GET /api/admin/products/{id}
    @GetMapping("/{id}")
    public AdminProductDetail getById(@PathVariable Long id) {
        return adminProductService.getDetail(id);
    }

    // POST /api/admin/products
    @PostMapping
    public AdminProductDetail create(@Valid @RequestBody AdminProductRequest request) {
        return adminProductService.create(request);
    }

    // PUT /api/admin/products/{id}
    @PutMapping("/{id}")
    public AdminProductDetail update(@PathVariable Long id, @Valid @RequestBody AdminProductRequest request) {
        return adminProductService.update(id, request);
    }

    // PUT /api/admin/products/{id}/status - an / hien nhanh tu danh sach
    @PutMapping("/{id}/status")
    public AdminProductDetail updateStatus(@PathVariable Long id, @Valid @RequestBody ProductStatusRequest request) {
        return adminProductService.updateStatus(id, request.getStatus());
    }

    // DELETE /api/admin/products/{id} - khong xoa duoc san pham da co don hang
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        adminProductService.delete(id);
    }
}