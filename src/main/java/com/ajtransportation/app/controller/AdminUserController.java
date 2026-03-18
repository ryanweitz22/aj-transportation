package com.ajtransportation.app.controller;

import com.ajtransportation.app.model.User;
import com.ajtransportation.app.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String listUsers(Model model) {
        List<User> users = userRepository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("users", users);
        model.addAttribute("totalCount", users.size());
        model.addAttribute("blockedCount",    users.stream().filter(User::isBlocked).count());
        model.addAttribute("unverifiedCount", users.stream().filter(u -> !u.isEmailVerified()).count());
        return "admin/users";
    }

    @PostMapping("/block/{id}")
    public String blockUser(@PathVariable UUID id, RedirectAttributes ra) {
        userRepository.findById(id).ifPresent(user -> {
            if ("ADMIN".equals(user.getRole())) {
                ra.addFlashAttribute("errorMessage", "Cannot block an admin account.");
                return;
            }
            user.setBlocked(true);
            userRepository.save(user);
            ra.addFlashAttribute("successMessage", "User blocked.");
        });
        return "redirect:/admin/users";
    }

    @PostMapping("/unblock/{id}")
    public String unblockUser(@PathVariable UUID id, RedirectAttributes ra) {
        userRepository.findById(id).ifPresent(user -> {
            user.setBlocked(false);
            userRepository.save(user);
            ra.addFlashAttribute("successMessage", "User unblocked.");
        });
        return "redirect:/admin/users";
    }

    @PostMapping("/verify/{id}")
    public String verifyUser(@PathVariable UUID id, RedirectAttributes ra) {
        userRepository.findById(id).ifPresent(user -> {
            user.setEmailVerified(true);
            user.setVerificationToken(null);
            userRepository.save(user);
            ra.addFlashAttribute("successMessage", "Email verified for " + user.getEmail());
        });
        return "redirect:/admin/users";
    }

    @PostMapping("/delete/{id}")
    public String deleteUser(@PathVariable UUID id, RedirectAttributes ra) {
        userRepository.findById(id).ifPresent(user -> {
            if ("ADMIN".equals(user.getRole())) {
                ra.addFlashAttribute("errorMessage", "Cannot delete an admin account.");
                return;
            }
            userRepository.delete(user);
            ra.addFlashAttribute("successMessage", "User deleted.");
        });
        return "redirect:/admin/users";
    }
}