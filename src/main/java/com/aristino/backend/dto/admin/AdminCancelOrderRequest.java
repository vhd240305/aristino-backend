package com.aristino.backend.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminCancelOrderRequest {

    @NotBlank(message = "Vui long chon ly do huy don")
    @Pattern(regexp = "SHOP_OUT_OF_STOCK|SHOP_CUSTOMER_REQUEST|SHOP_OTHER", message = "Ly do huy don khong hop le")
    private String reason;

    // Bat buoc khi reason = SHOP_OTHER
    @Size(max = 255, message = "Ghi chu khong duoc qua 255 ky tu")
    private String note;
}