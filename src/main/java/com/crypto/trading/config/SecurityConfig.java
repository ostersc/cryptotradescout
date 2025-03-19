package com.crypto.trading.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Security configuration for the application.
 * This class handles API security, authentication, and access control.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${trading.security.enabled:true}")
    private boolean securityEnabled;

    /**
     * Configures the security filter chain for HTTP requests.
     * For simplicity in a development environment, this may disable CSRF protection
     * and allow certain endpoints without authentication.
     * 
     * @param http The HttpSecurity to configure
     * @return The configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        if (!securityEnabled) {
            http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                    .anyRequest().permitAll()
                );
        } else {
            http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers("/api/public/**").permitAll()
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().authenticated()
                )
                .httpBasic(withDefaults());
        }
        
        return http.build();
    }
}
