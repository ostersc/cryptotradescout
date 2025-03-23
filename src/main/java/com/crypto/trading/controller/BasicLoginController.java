package com.crypto.trading.controller;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Basic controller to handle developer login during initial setup.
 * This bypasses the normal Spring Security flow for quick testing.
 */
@Controller
public class BasicLoginController {

    @GetMapping("/dev-login")
    public String devLoginPage() {
        return "dev-login";
    }
    
    @PostMapping("/auth/dev-login")
    public String processDevLogin(@RequestParam(defaultValue = "dev") String username) {
        // Create a simple authentication token with ROLE_USER
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            username, 
            "dev-password", 
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        
        // Set the authentication in the context
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        // Redirect to the dashboard after login
        return "redirect:/";
    }
}