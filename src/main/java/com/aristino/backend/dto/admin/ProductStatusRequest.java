package com.aristino.backend.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductStatusRequest {

    @NotBlank(message = "Trang thai san pham khong hop le")
    @Pattern(regexp = "ACTIVE|HIDDEN", message = "Trang thai san pham khong hop le")
    private String status;
}