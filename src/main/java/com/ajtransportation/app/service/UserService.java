package com.ajtransportation.app.service;

import com.ajtransportation.app.model.RegisterRequest;
import com.ajtransportation.app.model.User;
import com.ajtransportation.app.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user after validating email/username uniqueness
     * and that the two passwords match.
     * Returns null on success, or an error message string on failure.
     */
    public String register(RegisterRequest request) {

        // Check passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return "Passwords do not match";
        }

        // Check email not already taken
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            return "An account with this email address already exists";
        }

        // Check username not already taken
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            return "This username is already taken — please choose another";
        }

        // Build and save the new user
        User user = new User();
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setUsername(request.getUsername().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");

        userRepository.save(user);
        return null; // null = success
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
}