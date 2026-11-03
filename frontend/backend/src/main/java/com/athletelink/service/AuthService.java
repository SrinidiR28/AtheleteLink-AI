package com.athletelink.service;

import com.athletelink.dto.AuthResponse;
import com.athletelink.dto.LoginRequest;
import com.athletelink.dto.RegisterRequest;
import com.athletelink.exception.EmailAlreadyExistsException;
import com.athletelink.exception.InvalidCredentialsException;
import com.athletelink.exception.PasswordMismatchException;
import com.athletelink.model.User;
import com.athletelink.repository.UserRepository;
import com.athletelink.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new PasswordMismatchException("Password and confirm password do not match");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException("An account with this email already exists");
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setSport(request.getSport());
        user.setLocation(request.getLocation());
        user.setCreatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);

        String role = saved.getRole() != null ? saved.getRole() : "ATHLETE";
        String token = jwtUtil.generateToken(saved.getId(), saved.getEmail(), role);
        return new AuthResponse(token, saved.getId(), saved.getFullName(), saved.getEmail(), role);
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if ("INACTIVE".equalsIgnoreCase(user.getStatus()) || "DEACTIVATED".equalsIgnoreCase(user.getStatus())) {
            throw new InvalidCredentialsException("Your account is deactivated. Please contact an administrator.");
        }

        String role = user.getRole() != null ? user.getRole() : "ATHLETE";
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), role);
        return new AuthResponse(token, user.getId(), user.getFullName(), user.getEmail(), role);
    }
}
