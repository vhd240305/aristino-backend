package com.aristino.backend.service;

import com.aristino.backend.dto.auth.AuthResponse;
import com.aristino.backend.dto.auth.LoginRequest;
import com.aristino.backend.dto.auth.RegisterRequest;
import com.aristino.backend.entity.Cart;
import com.aristino.backend.entity.Role;
import com.aristino.backend.entity.User;
import com.aristino.backend.repository.CartRepository;
import com.aristino.backend.repository.RoleRepository;
import com.aristino.backend.repository.UserRepository;
import com.aristino.backend.security.JwtUtil;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EntityExistsException("Email da duoc su dung: " + request.getEmail());
        }

        // Moi user moi dang ky mac dinh duoc gan ROLE_CUSTOMER
        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new IllegalStateException("Chua seed du lieu ROLE_CUSTOMER trong bang roles"));

        Set<Role> roles = new HashSet<>();
        roles.add(customerRole);

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .roles(roles)
                .build();

        user = userRepository.save(user);

        // Tao san 1 gio hang rong cho user moi, khop voi rang buoc carts.user_id UNIQUE
        Cart cart = Cart.builder().user(user).build();
        cartRepository.save(cart);

        String token = jwtUtil.generateToken(user.getEmail());
        return buildAuthResponse(token, user);
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (org.springframework.security.authentication.DisabledException ex) {
            // Tai khoan bi khoa. Chi noi ro khi mat khau DUNG, de nguoi la khong do duoc email nao dang ton tai
            boolean passwordOk = userRepository.findByEmail(request.getEmail())
                    .map(u -> u.getPassword() != null
                            && passwordEncoder.matches(request.getPassword(), u.getPassword()))
                    .orElse(false);
            throw new BadCredentialsException(passwordOk
                    ? "Tai khoan cua ban da bi khoa. Vui long lien he cua hang de duoc ho tro"
                    : "Email hoac mat khau khong dung");
        } catch (org.springframework.security.core.AuthenticationException ex) {
            throw new BadCredentialsException("Email hoac mat khau khong dung");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Email hoac mat khau khong dung"));

        String token = jwtUtil.generateToken(user.getEmail());
        return buildAuthResponse(token, user);
    }

    private AuthResponse buildAuthResponse(String token, User user) {
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRoles().stream().map(Role::getName).toList()
        );
        return new AuthResponse(token, userInfo);
    }
}