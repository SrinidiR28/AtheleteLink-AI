package com.athletelink.service;

import com.athletelink.dto.UpdateProfileRequest;
import com.athletelink.dto.UserProfileResponse;
import com.athletelink.exception.UserNotFoundException;
import com.athletelink.model.User;
import com.athletelink.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return new UserProfileResponse(
                user.getId(), user.getFullName(), user.getEmail(),
                user.getSport(), user.getLocation(), user.getCreatedAt());
    }

    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        userRepository.updateProfile(userId, request.getFullName().trim(),
                request.getSport(), request.getLocation());

        return getProfile(userId);
    }
}
