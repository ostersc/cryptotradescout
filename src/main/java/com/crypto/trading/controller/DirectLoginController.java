package com.crypto.trading.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for direct login functionality
 */
@Controller
public class DirectLoginController {

    private static final Logger logger = LoggerFactory.getLogger(DirectLoginController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * Shows the direct login page
     */
    @GetMapping("/direct-login")
    public String showDirectLoginPage() {
        logger.info("Showing direct login page");
        return "direct-login";
    }

    /**
     * Processes the direct login request
     * This is a simplified approach that directly authenticates the user
     */
    @PostMapping("/process-direct-login")
    public String processDirectLogin(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        logger.info("Processing direct login for user: {}", username);
        
        try {
            // Create authentication token
            UsernamePasswordAuthenticationToken authRequest = 
                new UsernamePasswordAuthenticationToken(username, password);
            
            // Add details from the request
            authRequest.setDetails(new WebAuthenticationDetails(request));
            
            // Attempt authentication
            Authentication authentication = authenticationManager.authenticate(authRequest);
            
            // If we get here, authentication was successful
            logger.info("Authentication successful for user: {}", username);
            
            // Store the authentication in the security context
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Create a new session and add the security context to it
            HttpSession session = request.getSession(true);
            session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, 
                SecurityContextHolder.getContext()
            );
            
            logger.info("User authenticated and session created, redirecting to dashboard");
            
            // Redirect to the dashboard
            return "redirect:/";
        } catch (Exception e) {
            logger.error("Authentication failed", e);
            return "redirect:/direct-login?error=true";
        }
    }
}