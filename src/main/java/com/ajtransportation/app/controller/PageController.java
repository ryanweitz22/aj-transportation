package com.ajtransportation.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PageController {

    @GetMapping("/")
    public String home(@RequestParam(value = "logout", required = false) String logout, Model model) {
        if (logout != null) model.addAttribute("logoutMessage", "You have been logged out. See you next time!");
        return "index";
    }

    @GetMapping("/about")
    public String about() { return "about"; }

    @GetMapping("/contact")
    public String contact() { return "contact"; }

    @PostMapping("/contact")
    public String contactSubmit(
            @RequestParam(value = "message", required = false) String message,
            RedirectAttributes ra) {
        if (message == null || message.isBlank()) {
            ra.addFlashAttribute("errorMessage", "Please enter a message.");
            return "redirect:/contact";
        }
        ra.addFlashAttribute("successMessage", "Message received! We will get back to you within 24 hours.");
        return "redirect:/contact";
    }
}
