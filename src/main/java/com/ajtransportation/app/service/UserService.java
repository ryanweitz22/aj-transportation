package com.ajtransportation.app.service;

import com.ajtransportation.app.model.RegisterRequest;
import com.ajtransportation.app.model.User;
import com.ajtransportation.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    public String register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return "Passwords do not match";
        }
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            return "An account with this email address already exists";
        }
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            return "This username is already taken — please choose another";
        }

        String token = UUID.randomUUID().toString();
        User user = new User();
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setUsername(request.getUsername().trim());
        user.setPhoneNumber(
            request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()
                ? request.getPhoneNumber().trim() : null);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");
        user.setEmailVerified(false);
        user.setVerificationToken(token);
        userRepository.save(user);

        try {
            sendVerificationEmail(user.getEmail(), token);
        } catch (Exception e) {
            System.err.println("Warning: could not send verification email: " + e.getMessage());
        }

        return null;
    }

    public boolean verifyEmail(String token) {
        return userRepository.findByVerificationToken(token).map(user -> {
            user.setEmailVerified(true);
            user.setVerificationToken(null);
            userRepository.save(user);
            return true;
        }).orElse(false);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public User findById(java.util.UUID id) {
        return userRepository.findById(id).orElse(null);
    }

    private void sendVerificationEmail(String toEmail, String token) {
        String verifyUrl = baseUrl + "/verify-email?token=" + token;
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(toEmail);
        msg.setSubject("Confirm your AJ Transportation account");
        msg.setText(
            "Thank you for registering with AJ Transportation.\n\n" +
            "Click this link to verify your email and activate your account:\n\n" +
            verifyUrl + "\n\n— AJ Transportation"
        );
        mailSender.send(msg);
    }
}