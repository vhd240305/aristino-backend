package com.aristino.backend.service;

import com.aristino.backend.common.SlugUtil;
import com.aristino.backend.dto.admin.AdminProductDetail;
import com.aristino.backend.dto.admin.AdminProductRequest;
import com.aristino.backend.dto.admin.AdminProductSummary;
import com.aristino.backend.entity.Category;
import com.aristino.backend.entity.Product;
import com.aristino.backend.entity.ProductImage;
import com.aristino.backend.entity.ProductVariant;
import com.aristino.backend.repository.CartItemRepository;
import com.aristino.backend.repository.CategoryRepository;
import com.aristino.backend.repository.OrderItemRepository;
import com.aristino.backend.repository.ProductRepository;
import com.aristino.backend.repository.ProductVariantRepository;
import com.aristino.backend.spec.AdminProductSpecs;
import com.aristino.backend.spec.ProductSpecs;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Quan ly san pham cho cua hang: them, sua (thong tin, anh, bien the), an/hien, xoa.
 *
 * Quy tac:
 * - Bien the (size/mau) da co don hang thi KHONG xoa duoc (don hang van tro toi no). Muon ngung ban thi dat ton kho = 0.
 * - San pham da co don hang thi KHONG xoa duoc, chi duoc chuyen sang trang thai An (HIDDEN).
 * - Go mot bien the chua tung ban thi tu dong xoa no khoi gio hang cua moi khach.
 * - Duong dan (slug) tao tu ten luc them moi va giu nguyen khi sua, de cac lien ket cu khong bi hong.
 */
@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository variantRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;

    @Transactional(readOnly = true)
    public Page<AdminProductSummary> list(String q, Long categoryId, String status, Pageable pageable) {
        Specification<Product> spec = Specification
                .where(ProductSpecs.nameContains(q))
                .and(ProductSpecs.inCategory(categoryId))
                .and(AdminProductSpecs.hasStatus(parseStatus(status)));
        return productRepository.findAll(spec, pageable).map(AdminProductSummary::from);
    }

    @Transactional(readOnly = true)
    public AdminProductDetail getDetail(Long id) {
        return AdminProductDetail.from(find(id));
    }

    @Transactional
    public AdminProductDetail create(AdminProductRequest request) {
        Category category = findCategory(request.getCategoryId());
        validateVariants(request.getVariants());

        Product product = Product.builder()
                .category(category)
                .name(request.getName().trim())
                .slug(uniqueSlug(request.getName()))
                .description(blankToNull(request.getDescription()))
                .material(blankToNull(request.getMaterial()))
                .basePrice(request.getBasePrice())
                .status(Product.Status.valueOf(request.getStatus()))
                .build();

        applyImages(product, request.getImages());
        for (AdminProductRequest.VariantInput input : request.getVariants()) {
            product.getVariants().add(buildVariant(product, input));
        }
        return AdminProductDetail.from(productRepository.save(product));
    }

    @Transactional
    public AdminProductDetail update(Long id, AdminProductRequest request) {
        Product product = find(id);
        Category category = findCategory(request.getCategoryId());
        validateVariants(request.getVariants());

        product.setCategory(category);
        product.setName(request.getName().trim());
        product.setDescription(blankToNull(request.getDescription()));
        product.setMaterial(blankToNull(request.getMaterial()));
        product.setBasePrice(request.getBasePrice());
        product.setStatus(Product.Status.valueOf(request.getStatus()));
        applyImages(product, request.getImages());

        // Doi chieu bien the theo id: co id -> sua, khong co id -> them moi, thieu so voi danh sach cu -> go bo
        Map<Long, ProductVariant> existing = product.getVariants().stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));
        Set<Long> keptIds = new HashSet<>();

        for (AdminProductRequest.VariantInput input : request.getVariants()) {
            if (input.getId() == null) {
                product.getVariants().add(buildVariant(product, input));
                continue;
            }
            ProductVariant variant = existing.get(input.getId());
            if (variant == null) {
                throw new IllegalArgumentException("Bien the khong thuoc san pham nay");
            }
            variant.setSize(blankToNull(input.getSize()));
            variant.setColor(blankToNull(input.getColor()));
            variant.setSku(input.getSku().trim());
            variant.setPrice(input.getPrice());
            variant.setStockQuantity(input.getStockQuantity());
            keptIds.add(variant.getId());
        }

        for (ProductVariant variant : new ArrayList<>(product.getVariants())) {
            if (variant.getId() == null || keptIds.contains(variant.getId())) {
                continue; // bien the moi them hoac bien the van giu
            }
            if (orderItemRepository.existsByVariantId(variant.getId())) {
                throw new IllegalStateException("Bien the " + variant.getSize() + "/" + variant.getColor()
                        + " da co don hang nen khong the xoa. Hay dat ton kho bang 0");
            }
            cartItemRepository.deleteByVariantId(variant.getId());
            product.getVariants().remove(variant);
        }

        return AdminProductDetail.from(productRepository.save(product));
    }

    @Transactional
    public AdminProductDetail updateStatus(Long id, String status) {
        Product product = find(id);
        product.setStatus(Product.Status.valueOf(status));
        return AdminProductDetail.from(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        Product product = find(id);
        if (orderItemRepository.existsByVariantProductId(id)) {
            throw new IllegalStateException("San pham da co don hang nen khong the xoa. Hay chuyen sang trang thai An");
        }
        cartItemRepository.deleteByProductId(id);
        productRepository.delete(product);
    }

    // ===== Ham phu =====

    private Product find(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Khong tim thay san pham"));
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Khong tim thay danh muc"));
    }

    // Kiem tra danh sach bien the gui len: khong trung SKU, khong trung cap size+mau, SKU chua bi san pham khac dung
    private void validateVariants(List<AdminProductRequest.VariantInput> variants) {
        Set<String> skus = new HashSet<>();
        Set<String> pairs = new HashSet<>();

        for (AdminProductRequest.VariantInput v : variants) {
            String sku = v.getSku().trim();
            if (!skus.add(sku.toLowerCase())) {
                throw new IllegalArgumentException("SKU bi trung trong danh sach bien the: " + sku);
            }
            String size = v.getSize() == null ? "" : v.getSize().trim();
            String color = v.getColor() == null ? "" : v.getColor().trim();
            if (!pairs.add(size.toLowerCase() + "|" + color.toLowerCase())) {
                throw new IllegalArgumentException("Bien the trung size va mau: " + size + " / " + color);
            }

            variantRepository.findBySku(sku).ifPresent(existing -> {
                if (v.getId() == null || !existing.getId().equals(v.getId())) {
                    throw new IllegalArgumentException("SKU da ton tai: " + sku);
                }
            });
        }
    }

    private ProductVariant buildVariant(Product product, AdminProductRequest.VariantInput input) {
        return ProductVariant.builder()
                .product(product)
                .size(blankToNull(input.getSize()))
                .color(blankToNull(input.getColor()))
                .sku(input.getSku().trim())
                .price(input.getPrice())
                .stockQuantity(input.getStockQuantity())
                .build();
    }

    // Thay toan bo danh sach anh. Thu tu trong danh sach = thu tu hien thi; dung 1 anh dai dien
    private void applyImages(Product product, List<AdminProductRequest.ImageInput> inputs) {
        product.getImages().clear();
        if (inputs == null || inputs.isEmpty()) {
            return;
        }
        int thumbnailIndex = 0;
        for (int i = 0; i < inputs.size(); i++) {
            if (Boolean.TRUE.equals(inputs.get(i).getThumbnail())) {
                thumbnailIndex = i;
                break;
            }
        }
        for (int i = 0; i < inputs.size(); i++) {
            product.getImages().add(ProductImage.builder()
                    .product(product)
                    .imageUrl(inputs.get(i).getUrl().trim())
                    .isThumbnail(i == thumbnailIndex)
                    .sortOrder(i)
                    .build());
        }
    }

    private String uniqueSlug(String name) {
        String base = SlugUtil.slugify(name);
        if (base.isEmpty()) {
            base = "san-pham";
        }
        if (base.length() > 200) {
            base = base.substring(0, 200);
        }
        String slug = base;
        int n = 2;
        while (productRepository.findBySlug(slug).isPresent()) {
            slug = base + "-" + n++;
        }
        return slug;
    }

    private Product.Status parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return Product.Status.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Trang thai san pham khong hop le");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}