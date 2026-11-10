package com.aristino.backend.common;

import com.aristino.backend.entity.User;
import com.aristino.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final UserRepository userRepository;

    // JwtAuthFilter da luu email vao SecurityContext khi token hop le,
    // ham nay dung lai email do de lay ra User entity day du
    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Khong tim thay user dang dang nhap"));
    }
}