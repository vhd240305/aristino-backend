package com.aristino.backend.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReviewRequest {

    @NotNull(message = "Thieu ma san pham")
    private Long productId;

    @NotNull(message = "Vui long chon so sao danh gia")
    @Min(value = 1, message = "So sao danh gia phai tu 1 den 5")
    @Max(value = 5, message = "So sao danh gia phai tu 1 den 5")
    private Integer rating;

    // Khong bat buoc
    @Size(max = 1000, message = "Noi dung danh gia khong duoc qua 1000 ky tu")
    private String comment;
}