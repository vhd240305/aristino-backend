package com.aristino.backend.dto.admin;

import com.aristino.backend.entity.Category;

public record AdminCategoryResponse(Long id, String name, String slug, long productCount) {

    public static AdminCategoryResponse from(Category c, long productCount) {
        return new AdminCategoryResponse(c.getId(), c.getName(), c.getSlug(), productCount);
    }
}