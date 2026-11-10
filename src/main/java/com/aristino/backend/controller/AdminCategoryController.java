package com.aristino.backend.controller;

import com.aristino.backend.dto.admin.AdminCategoryResponse;
import com.aristino.backend.dto.admin.CategoryRequest;
import com.aristino.backend.service.AdminCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    // GET /api/admin/categories - kem so san pham trong tung danh muc
    @GetMapping
    public List<AdminCategoryResponse> list() {
        return adminCategoryService.list();
    }

    // POST /api/admin/categories
    @PostMapping
    public AdminCategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        return adminCategoryService.create(request);
    }

    // PUT /api/admin/categories/{id}
    @PutMapping("/{id}")
    public AdminCategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return adminCategoryService.update(id, request);
    }

    // DELETE /api/admin/categories/{id} - khong xoa duoc danh muc dang co san pham
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        adminCategoryService.delete(id);
    }
}