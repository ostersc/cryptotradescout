package com.crypto.trading.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class AuthController {

    private final Environment environment;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @Autowired
    public AuthController(Environment environment, AuthenticationManager authenticationManager) {
        this.environment = environment;
        this.authenticationManager = authenticationManager;
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
    
    @GetMapping("/simple-login")
    public String simpleLogin() {
        return "simple-login";
    }
    
    @PostMapping("/direct-login")
    public String directLogin(@RequestParam String username, 
                              @RequestParam String password,
                              HttpServletRequest request,
                              HttpServletResponse response,
                              RedirectAttributes redirectAttributes) {
        try {
            // Create the authentication token
            UsernamePasswordAuthenticationToken token = 
                new UsernamePasswordAuthenticationToken(username, password);
            
            // Authenticate the user
            Authentication auth = authenticationManager.authenticate(token);
            
            // Set the authentication in the SecurityContext
            SecurityContextHolder.getContext().setAuthentication(auth);
            
            // Save the context to the session
            securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
            
            // Add a success parameter to the redirect
            redirectAttributes.addAttribute("success", true);
            
            return "redirect:/simple-login";
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", true);
            return "redirect:/simple-login";
        }
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
    
    @GetMapping("/auth-test")
    public String authTest(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("authentication", auth);
        return "auth-test";
    }
}