package com.aristino.backend.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelOrderRequest {

    // Khach chi duoc chon cac ly do nay (PAYMENT_EXPIRED chi danh cho he thong tu huy)
    @NotBlank(message = "Vui long chon ly do huy don")
    @Pattern(regexp = "CHANGED_MIND|WRONG_ITEM|CHANGE_ADDRESS|BETTER_PRICE|PAYMENT_PROBLEM|OTHER",
            message = "Ly do huy don khong hop le")
    private String reason;

    // Bat buoc khi reason = OTHER
    @Size(max = 255, message = "Ghi chu khong duoc qua 255 ky tu")
    private String note;
}