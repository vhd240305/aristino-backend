package com.aristino.backend.dto.address;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressRequest {

    @NotBlank(message = "Ten nguoi nhan khong duoc de trong")
    private String recipientName;

    @NotBlank(message = "So dien thoai khong duoc de trong")
    private String phone;

    @NotBlank(message = "Tinh/thanh pho khong duoc de trong")
    private String province;

    @NotBlank(message = "Quan/huyen khong duoc de trong")
    private String district;

    @NotBlank(message = "Phuong/xa khong duoc de trong")
    private String ward;

    @NotBlank(message = "Dia chi chi tiet khong duoc de trong")
    private String detailAddress;

    private Boolean isDefault = false;
}