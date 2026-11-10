package com.aristino.backend.service;

import com.aristino.backend.common.CurrentUserProvider;
import com.aristino.backend.dto.user.ChangePasswordRequest;
import com.aristino.backend.dto.user.UpdateProfileRequest;
import com.aristino.backend.dto.user.UserProfileResponse;
import com.aristino.backend.entity.User;
import com.aristino.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile() {
        return UserProfileResponse.from(currentUserProvider.getCurrentUser());
    }

    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        User user = currentUserProvider.getCurrentUser();

        user.setFullName(request.getFullName().trim());
        String phone = request.getPhone() == null ? "" : request.getPhone().trim();
        user.setPhone(phone.isEmpty() ? null : phone);

        return UserProfileResponse.from(userRepository.save(user));
    }

    public void changePassword(ChangePasswordRequest request) {
        User user = currentUserProvider.getCurrentUser();

        // Tai khoan dang nhap bang Google co the khong co mat khau -> khong doi duoc
        // Dung IllegalArgumentException (-> 400), KHONG dung BadCredentialsException (-> 401)
        // vi frontend coi 401 la "token het han" va se tu dang xuat nguoi dung
        if (user.getPassword() == null
                || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mat khau hien tai khong dung");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mat khau moi phai khac mat khau hien tai");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}