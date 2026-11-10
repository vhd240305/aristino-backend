package com.aristino.backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private UserInfo user;

    // Thong tin user rut gon tra ve cho frontend, khong bao gio tra ve password
    @Getter
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String fullName;
        private String email;
        private List<String> roles;
    }
}
