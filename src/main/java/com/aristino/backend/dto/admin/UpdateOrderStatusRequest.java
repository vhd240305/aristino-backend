package com.aristino.backend.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateOrderStatusRequest {

    // Cac buoc tien len cua don hang. Huy don di qua API rieng (/cancel)
    @NotBlank(message = "Vui long chon trang thai don hang")
    @Pattern(regexp = "CONFIRMED|SHIPPING|COMPLETED", message = "Trang thai don hang khong hop le")
    private String status;
}