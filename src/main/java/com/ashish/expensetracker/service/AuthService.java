package com.ashish.expensetracker.service;

import com.ashish.expensetracker.dto.AuthResponse;
import com.ashish.expensetracker.dto.LoginRequest;
import com.ashish.expensetracker.dto.RegisterRequest;
import com.ashish.expensetracker.model.Role;
import com.ashish.expensetracker.model.User;
import com.ashish.expensetracker.repository.UserRepository;
import com.ashish.expensetracker.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        User user = createUser(request.getUsername(), request.getEmail(), request.getPassword(), Role.ROLE_USER);
        return buildAuthResponse(user);
    }

    public void bootstrapAdmin(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        createUser(username, email, password, Role.ROLE_ADMIN);
    }

    private User createUser(String username, String email, String password, Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(role)
                .build();

        return userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole().name());
        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return buildAuthResponse(user);
    }
}
