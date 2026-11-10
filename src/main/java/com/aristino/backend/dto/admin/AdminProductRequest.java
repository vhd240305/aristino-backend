package com.aristino.backend.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/** Du lieu them / sua san pham: thong tin chung + danh sach anh + danh sach bien the. */
@Getter
@Setter
public class AdminProductRequest {

    @NotNull(message = "Vui long chon danh muc")
    private Long categoryId;

    @NotBlank(message = "Ten san pham khong duoc de trong")
    @Size(max = 200, message = "Ten san pham khong duoc qua 200 ky tu")
    private String name;

    private String description;

    @Size(max = 150, message = "Chat lieu khong duoc qua 150 ky tu")
    private String material;

    @NotNull(message = "Gia san pham khong duoc de trong")
    @DecimalMin(value = "0", message = "Gia khong duoc am")
    private BigDecimal basePrice;

    // ACTIVE = dang ban, HIDDEN = an khoi cua hang
    @NotBlank(message = "Trang thai san pham khong hop le")
    @Pattern(regexp = "ACTIVE|HIDDEN", message = "Trang thai san pham khong hop le")
    private String status;

    @Valid
    private List<ImageInput> images;

    @Valid
    @NotEmpty(message = "Phai co it nhat mot bien the")
    private List<VariantInput> variants;

    @Getter
    @Setter
    public static class ImageInput {
        @NotBlank(message = "Duong dan anh khong duoc de trong")
        @Size(max = 255, message = "Duong dan anh khong duoc qua 255 ky tu")
        private String url;

        // Anh dai dien. Neu khong anh nao duoc chon thi lay anh dau tien
        private Boolean thumbnail;
    }

    @Getter
    @Setter
    public static class VariantInput {
        // null = bien the moi; co gia tri = sua bien the dang co cua san pham
        private Long id;

        @Size(max = 10, message = "Size khong duoc qua 10 ky tu")
        private String size;

        @Size(max = 50, message = "Mau khong duoc qua 50 ky tu")
        private String color;

        @NotBlank(message = "SKU khong duoc de trong")
        @Size(max = 50, message = "SKU khong duoc qua 50 ky tu")
        private String sku;

        @NotNull(message = "Gia bien the khong duoc de trong")
        @DecimalMin(value = "0", message = "Gia khong duoc am")
        private BigDecimal price;

        @NotNull(message = "Ton kho khong duoc de trong")
        @Min(value = 0, message = "Ton kho khong duoc am")
        private Integer stockQuantity;
    }
}