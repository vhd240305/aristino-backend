package com.aristino.backend.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRequest {

    // Ma phien chat do server cap o lan nhan dau; frontend luu lai va gui kem cac lan sau
    @Size(max = 100, message = "Ma phien chat khong hop le")
    private String sessionToken;

    @NotBlank(message = "Tin nhan khong duoc de trong")
    @Size(max = 500, message = "Tin nhan khong duoc qua 500 ky tu")
    private String message;
}