package com.aristino.backend.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequest {

    @NotBlank(message = "Ten danh muc khong duoc de trong")
    @Size(max = 100, message = "Ten danh muc khong duoc qua 100 ky tu")
    private String name;

    // De trong = tu tao tu ten. Co nhap thi duoc chuan hoa thanh chu thuong khong dau, noi bang dau gach ngang
    @Size(max = 120, message = "Duong dan khong duoc qua 120 ky tu")
    private String slug;
}