package com.crypto.trading.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Configuration class for email-related beans.
 */
@Configuration
public class EmailConfig {
    private static final Logger logger = LoggerFactory.getLogger(EmailConfig.class);

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    /**
     * Creates a JavaMailSender bean.
     * 
     * @return the configured JavaMailSender
     */
    @Bean
    public JavaMailSender javaMailSender() {
        if (!emailEnabled) {
            logger.info("Email notifications are disabled, configuring a dummy JavaMailSender");
        }
        
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        
        if (username != null && !username.isEmpty()) {
            mailSender.setUsername(username);
        }
        
        if (password != null && !password.isEmpty()) {
            mailSender.setPassword(password);
        }
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false");
        
        return mailSender;
    }
}