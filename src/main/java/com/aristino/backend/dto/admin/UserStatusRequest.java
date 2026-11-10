package com.aristino.backend.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserStatusRequest {

    // ACTIVE = hoat dong, LOCKED = bi khoa (khong dang nhap / dung duoc nua)
    @NotBlank(message = "Trang thai tai khoan khong hop le")
    @Pattern(regexp = "ACTIVE|LOCKED", message = "Trang thai tai khoan khong hop le")
    private String status;
}