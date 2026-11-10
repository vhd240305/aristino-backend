package com.aristino.backend.dto.user;

import com.aristino.backend.entity.Role;
import com.aristino.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private List<String> roles;

    // Khong bao gio tra ve password. Goi ham nay trong @Transactional vi roles la LAZY
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRoles().stream().map(Role::getName).toList()
        );
    }
}