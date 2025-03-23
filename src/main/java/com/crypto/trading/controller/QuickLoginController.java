package com.crypto.trading.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for handling quick login functionality
 */
@Controller
public class QuickLoginController {

    private static final Logger logger = LoggerFactory.getLogger(QuickLoginController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * Shows the quick login page
     */
    @GetMapping("/quicklogin")
    public String showQuickLoginPage() {
        return "quicklogin";
    }

    /**
     * Performs a developer login with a fixed account
     */
    @GetMapping("/auth/dev-login")
    public String devLogin(HttpServletRequest request, HttpServletResponse response) {
        try {
            logger.info("Attempting dev login");
            
            // Create authentication token with dev credentials
            UsernamePasswordAuthenticationToken authRequest = 
                new UsernamePasswordAuthenticationToken("dev", "dev");
            
            // Authenticate with the token
            Authentication auth = authenticationManager.authenticate(authRequest);
            
            // Set the authentication in the security context
            SecurityContextHolder.getContext().setAuthentication(auth);
            
            // Create a new session and add the security context to it
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, 
                                SecurityContextHolder.getContext());
            
            logger.info("Dev login successful: {}", auth.isAuthenticated());
            
            // Redirect to the dashboard on successful login
            return "redirect:/";
        } catch (Exception e) {
            logger.error("Error during dev login", e);
            return "redirect:/login?error";
        }
    }
}