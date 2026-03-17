package com.ajtransportation.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PageController {

    @GetMapping("/")
    public String home(
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {
        if (logout != null) {
            model.addAttribute("logoutMessage", "You've been logged out. See you next time!");
        }
        return "index";
    }

    @GetMapping("/about")
    public String about() { return "about"; }

    @GetMapping("/contact")
    public String contact() { return "contact"; }

    @GetMapping("/bookings")
    public String bookings() { return "user/bookings"; }
}