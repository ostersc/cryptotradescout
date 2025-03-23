package com.crypto.trading.config;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${app.auth.allowed-emails:}")
    private List<String> allowedEmails;
    
    @Value("${app.auth.allowed-domains:}")
    private List<String> allowedDomains;

    private final ClientRegistrationRepository clientRegistrationRepository;

    @Autowired(required = false)
    public SecurityConfig(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        if (clientRegistrationRepository == null) {
            logger.warn("ClientRegistrationRepository is null. OAuth2 login will not be available.");
        } else {
            logger.info("ClientRegistrationRepository initialized successfully. OAuth2 login is available.");
        }
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for simplicity in development mode
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/login", "/error", "/css/**", "/js/**", "/img/**", "/favicon.ico").permitAll()
                .requestMatchers("/api/v1/public/**").permitAll()
                .requestMatchers("/webjars/**", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                // In development mode without OAuth2 configured, allow all access
                .requestMatchers("/**").permitAll()
            );
            
        // Only configure OAuth2 login if we have a client registration repository
        if (clientRegistrationRepository != null) {
            http.oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo
                    .userAuthoritiesMapper(this.userAuthoritiesMapper())
                )
                .defaultSuccessUrl("/dashboard", true)
            );
        } else {
            logger.warn("OAuth2 login is not configured. Development mode is enabled with all endpoints accessible.");
            // In development mode, we'll simply allow all access without login
            http.formLogin(form -> form
                .loginPage("/login")
                .permitAll()
                .defaultSuccessUrl("/dashboard", true)
            );
        }
        
        http.logout(logout -> logout
            .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
            .logoutSuccessHandler(oidcLogoutSuccessHandler())
            .invalidateHttpSession(true)
            .clearAuthentication(true)
            .deleteCookies("JSESSIONID")
        );

        return http.build();
    }

    @Bean
    public LogoutSuccessHandler oidcLogoutSuccessHandler() {
        if (this.clientRegistrationRepository != null) {
            OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler =
                    new OidcClientInitiatedLogoutSuccessHandler(this.clientRegistrationRepository);
            
            // Set the URL that the user is redirected to after logging out
            oidcLogoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}");
            
            return oidcLogoutSuccessHandler;
        } else {
            // Fallback to simple logout handler when OAuth2 is not configured
            SimpleUrlLogoutSuccessHandler logoutSuccessHandler = new SimpleUrlLogoutSuccessHandler();
            logoutSuccessHandler.setDefaultTargetUrl("/login?logout");
            return logoutSuccessHandler;
        }
    }

    private GrantedAuthoritiesMapper userAuthoritiesMapper() {
        return (authorities) -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
            
            authorities.forEach(authority -> {
                boolean isAuthorized = false;
                String email = null;
                String hd = null; // hosted domain
                
                if (authority instanceof OidcUserAuthority) {
                    OidcUserAuthority oidcUserAuthority = (OidcUserAuthority) authority;
                    OidcIdToken idToken = oidcUserAuthority.getIdToken();
                    OidcUserInfo userInfo = oidcUserAuthority.getUserInfo();
                    
                    // Extract email and domain information
                    if (userInfo != null) {
                        email = userInfo.getEmail();
                        hd = userInfo.getClaimAsString("hd"); // Google Workspace domain
                    } else if (idToken != null) {
                        email = idToken.getEmail();
                        hd = idToken.getClaimAsString("hd");
                    }
                    
                    // Check if MFA/2FA was used
                    boolean usedMfa = idToken != null && 
                                      idToken.getClaimAsBoolean("amr") != null && 
                                      Arrays.asList(idToken.getClaimAsStringList("amr")).contains("mfa");
                    
                    // Only authorize if MFA was used
                    if (usedMfa && isAllowedUser(email, hd)) {
                        isAuthorized = true;
                        mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                    }
                    
                } else if (authority instanceof OAuth2UserAuthority) {
                    OAuth2UserAuthority oauth2UserAuthority = (OAuth2UserAuthority) authority;
                    
                    email = oauth2UserAuthority.getAttributes().get("email").toString();
                    if (oauth2UserAuthority.getAttributes().containsKey("hd")) {
                        hd = oauth2UserAuthority.getAttributes().get("hd").toString();
                    }
                    
                    // For OAuth2 users, we can't easily check MFA, 
                    // so we rely only on email allowlist
                    if (isAllowedUser(email, hd)) {
                        isAuthorized = true;
                        mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                    }
                }
                
                // If not authorized by email or missing MFA, don't add ROLE_USER
                if (!isAuthorized && mappedAuthorities.isEmpty()) {
                    // Add a limited role to show access denied page
                    mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_UNAUTHORIZED"));
                }
            });
            
            return mappedAuthorities;
        };
    }
    
    private boolean isAllowedUser(String email, String domain) {
        if (email == null) {
            return false;
        }
        
        // Check if email directly in allowed list
        if (allowedEmails != null && !allowedEmails.isEmpty() && allowedEmails.contains(email)) {
            return true;
        }
        
        // Check if domain in allowed domains
        if (domain != null && allowedDomains != null && !allowedDomains.isEmpty() && allowedDomains.contains(domain)) {
            return true;
        }
        
        // Check if email domain matches any in allowed domains
        if (allowedDomains != null && !allowedDomains.isEmpty()) {
            String emailDomain = email.substring(email.indexOf('@') + 1);
            return allowedDomains.contains(emailDomain);
        }
        
        return false;
    }
    
    /**
     * Bean to configure a UserDetailsService for development mode
     */
    @Bean
    public UserDetailsService userDetailsService() {
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
        
        if (clientRegistrationRepository == null) {
            // For development mode, create a test user that works with any password
            PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
            manager.createUser(User.withUsername("dev")
                .password(encoder.encode("dev"))
                .roles("USER")
                .build());
                
            logger.info("Created development user 'dev' with password 'dev'");
        }
        
        return manager;
    }
}