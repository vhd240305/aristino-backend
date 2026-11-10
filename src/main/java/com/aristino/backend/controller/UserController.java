package com.aristino.backend.controller;

import com.aristino.backend.dto.user.ChangePasswordRequest;
import com.aristino.backend.dto.user.UpdateProfileRequest;
import com.aristino.backend.dto.user.UserProfileResponse;
import com.aristino.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // GET /api/users/me - xem thong tin ca nhan
    @GetMapping("/me")
    public UserProfileResponse getMe() {
        return userService.getProfile();
    }

    // PUT /api/users/me - sua ho ten, so dien thoai
    @PutMapping("/me")
    public UserProfileResponse updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(request);
    }

    // PUT /api/users/me/password - doi mat khau
    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
    }
}