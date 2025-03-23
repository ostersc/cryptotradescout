package com.crypto.trading.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    private final Environment environment;

    @Autowired
    public AuthController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/login")
    public String login(Model model) {
        // Check if Google OAuth2 is configured
        boolean oauthConfigured = environment.getProperty("spring.security.oauth2.client.registration.google.client-id") != null &&
                                 !environment.getProperty("spring.security.oauth2.client.registration.google.client-id").equals("your-client-id");
        
        model.addAttribute("oauthConfigured", oauthConfigured);
        return "login";
    }
    
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
    
    @GetMapping("/dashboard") 
    public String dashboard() {
        // This is just a placeholder. The actual dashboard would have more logic.
        return "redirect:/";
    }
}