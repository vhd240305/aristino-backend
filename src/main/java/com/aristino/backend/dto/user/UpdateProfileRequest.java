package com.aristino.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    @NotBlank(message = "Ho ten khong duoc de trong")
    @Size(max = 100, message = "Ho ten khong duoc qua 100 ky tu")
    private String fullName;

    // Cho phep de trong. Neu nhap thi phai gom 9-15 ky tu: so, dau +, khoang trang, dau cham, gach ngang
    @Pattern(regexp = "^$|^[0-9+\\s.-]{9,15}$", message = "So dien thoai khong hop le")
    private String phone;
}