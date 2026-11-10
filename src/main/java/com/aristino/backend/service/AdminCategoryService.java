package com.aristino.backend.service;

import com.aristino.backend.common.SlugUtil;
import com.aristino.backend.dto.admin.AdminCategoryResponse;
import com.aristino.backend.dto.admin.CategoryRequest;
import com.aristino.backend.entity.Category;
import com.aristino.backend.repository.CategoryRepository;
import com.aristino.backend.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Quan ly danh muc (phang, khong phan cap cha-con vi cua hang chi hien danh sach phang).
 * Khong xoa duoc danh muc dang co san pham.
 */
@Service
@RequiredArgsConstructor
public class AdminCategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<AdminCategoryResponse> list() {
        return categoryRepository.findAll().stream()
                .sorted(Comparator.comparing(Category::getId))
                .map(c -> AdminCategoryResponse.from(c, productRepository.countByCategoryId(c.getId())))
                .toList();
    }

    @Transactional
    public AdminCategoryResponse create(CategoryRequest request) {
        String name = request.getName().trim();
        String slug = resolveSlug(request.getSlug(), name, null);

        Category saved = categoryRepository.save(Category.builder().name(name).slug(slug).build());
        return AdminCategoryResponse.from(saved, 0);
    }

    @Transactional
    public AdminCategoryResponse update(Long id, CategoryRequest request) {
        Category category = find(id);
        category.setName(request.getName().trim());

        // Khong nhap duong dan thi giu nguyen (doi ten khong lam hong cac duong dan cu)
        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            category.setSlug(resolveSlug(request.getSlug(), category.getName(), category.getId()));
        }
        Category saved = categoryRepository.save(category);
        return AdminCategoryResponse.from(saved, productRepository.countByCategoryId(id));
    }

    @Transactional
    public void delete(Long id) {
        Category category = find(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new IllegalStateException(
                    "Danh muc dang co san pham, khong the xoa. Hay chuyen san pham sang danh muc khac truoc");
        }
        if (!category.getChildren().isEmpty()) {
            throw new IllegalStateException("Danh muc dang co danh muc con, khong the xoa");
        }
        categoryRepository.delete(category);
    }

    // ===== Ham phu =====

    private Category find(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Khong tim thay danh muc"));
    }

    // Nguoi dung nhap slug thi chuan hoa va bao loi neu trung; khong nhap thi tu tao tu ten (trung thi them -2, -3...)
    private String resolveSlug(String requested, String name, Long selfId) {
        boolean custom = requested != null && !requested.isBlank();
        String base = SlugUtil.slugify(custom ? requested : name);
        if (base.isEmpty()) {
            base = "danh-muc";
        }
        if (base.length() > 110) {
            base = base.substring(0, 110);
        }

        if (custom) {
            if (isTaken(base, selfId)) {
                throw new IllegalArgumentException("Duong dan (slug) da ton tai");
            }
            return base;
        }
        String slug = base;
        int n = 2;
        while (isTaken(slug, selfId)) {
            slug = base + "-" + n++;
        }
        return slug;
    }

    private boolean isTaken(String slug, Long selfId) {
        return categoryRepository.findBySlug(slug)
                .map(existing -> selfId == null || !existing.getId().equals(selfId))
                .orElse(false);
    }
}