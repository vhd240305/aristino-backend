package com.aristino.backend.dto.address;

import com.aristino.backend.entity.Address;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AddressResponse {
    private Long id;
    private String recipientName;
    private String phone;
    private String province;
    private String district;
    private String ward;
    private String detailAddress;
    private Boolean isDefault;

    public static AddressResponse from(Address a) {
        return new AddressResponse(
                a.getId(), a.getRecipientName(), a.getPhone(),
                a.getProvince(), a.getDistrict(), a.getWard(),
                a.getDetailAddress(), a.getIsDefault()
        );
    }
}