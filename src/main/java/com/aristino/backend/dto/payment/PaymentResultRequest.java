package com.aristino.backend.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentResultRequest {

    // Ket qua ma "cong thanh toan gia lap" tra ve: SUCCESS hoac FAILED
    @NotBlank(message = "Thieu ket qua thanh toan")
    @Pattern(regexp = "SUCCESS|FAILED", message = "Ket qua thanh toan khong hop le")
    private String result;
}