package com.crypto.trading.notification;

import com.crypto.trading.exchange.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;

/**
 * Email-based implementation of the NotificationService interface.
 * Sends notifications about trades and alerts via email.
 */
@Service
public class EmailNotificationService implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);
    
    private final JavaMailSender mailSender;
    
    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;
    
    @Value("${notification.email.from:crypto-trading@example.com}")
    private String fromEmail;
    
    @Value("${notification.email.to:user@example.com}")
    private String toEmail;
    
    /**
     * Format for currency values.
     */
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
    
    /**
     * Format for date and time.
     */
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Constructor with JavaMailSender.
     * 
     * @param mailSender the JavaMailSender for sending emails
     */
    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    @Override
    public String getServiceName() {
        return "Email Notification Service";
    }
    
    @Override
    public Mono<Void> sendTradeNotification(Order order) {
        if (!emailEnabled) {
            logger.info("Email notifications are disabled, skipping trade notification");
            return Mono.empty();
        }
        
        String subject = String.format(
                "Trade Notification: %s %s on %s", 
                order.getType().toString(), 
                order.getTradingPair(), 
                order.getExchange());
        
        String message = buildTradeEmailBody(order);
        
        return sendEmail(subject, message);
    }
    
    @Override
    public Mono<Void> sendAlertNotification(AlertLevel alertLevel, String message) {
        if (!emailEnabled) {
            logger.info("Email notifications are disabled, skipping alert notification");
            return Mono.empty();
        }
        
        String subject = String.format("[%s] Trading System Alert", alertLevel.name());
        
        String emailBody = String.format(
                "<html><body>" +
                "<h2>Trading System Alert: %s</h2>" +
                "<p>Alert Level: <strong>%s</strong></p>" +
                "<p>Time: %s</p>" +
                "<p>Message: %s</p>" +
                "<p>This is an automated notification from your cryptocurrency trading system.</p>" +
                "</body></html>",
                alertLevel.name(),
                alertLevel.name(),
                java.time.LocalDateTime.now().format(dateTimeFormatter),
                message);
        
        return sendEmail(subject, emailBody);
    }
    
    @Override
    public Mono<Void> sendCustomNotification(String subject, String message) {
        if (!emailEnabled) {
            logger.info("Email notifications are disabled, skipping custom notification");
            return Mono.empty();
        }
        
        String emailBody = String.format(
                "<html><body>" +
                "<h2>%s</h2>" +
                "<p>%s</p>" +
                "<p>Time: %s</p>" +
                "<p>This is an automated notification from your cryptocurrency trading system.</p>" +
                "</body></html>",
                subject,
                message,
                java.time.LocalDateTime.now().format(dateTimeFormatter));
        
        return sendEmail(subject, emailBody);
    }
    
    @Override
    public boolean isOperational() {
        return emailEnabled && mailSender != null;
    }
    
    /**
     * Build an HTML email body for a trade notification.
     * 
     * @param order the order to include in the notification
     * @return the formatted HTML email body
     */
    private String buildTradeEmailBody(Order order) {
        return String.format(
                "<html><body>" +
                "<h2>Trade Execution Notification</h2>" +
                "<p>A trade has been executed by your cryptocurrency trading system:</p>" +
                "<table border='1' cellpadding='5' cellspacing='0'>" +
                "<tr><td><strong>Order ID</strong></td><td>%s</td></tr>" +
                "<tr><td><strong>Trading Pair</strong></td><td>%s</td></tr>" +
                "<tr><td><strong>Type</strong></td><td>%s</td></tr>" +
                "<tr><td><strong>Amount</strong></td><td>%s</td></tr>" +
                "<tr><td><strong>Price</strong></td><td>%s</td></tr>" +
                "<tr><td><strong>Total Value</strong></td><td>%s</td></tr>" +
                "<tr><td><strong>Status</strong></td><td>%s</td></tr>" +
                "<tr><td><strong>Exchange</strong></td><td>%s</td></tr>" +
                "<tr><td><strong>Time</strong></td><td>%s</td></tr>" +
                "</table>" +
                "<p>This is an automated notification from your cryptocurrency trading system.</p>" +
                "</body></html>",
                order.getId(),
                order.getTradingPair(),
                order.getType().toString(),
                order.getAmount(),
                currencyFormat.format(order.getPrice()),
                currencyFormat.format(order.getTotalValue()),
                order.getStatus(),
                order.getExchange(),
                order.getCreatedAt().format(dateTimeFormatter));
    }
    
    /**
     * Send an email with the given subject and body.
     * 
     * @param subject the email subject
     * @param body the email body (HTML)
     * @return a Mono that completes when the email is sent
     */
    private Mono<Void> sendEmail(String subject, String body) {
        return Mono.fromRunnable(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                
                helper.setFrom(fromEmail);
                helper.setTo(toEmail);
                helper.setSubject(subject);
                helper.setText(body, true); // true indicates HTML content
                
                mailSender.send(message);
                logger.info("Sent email notification: {}", subject);
            } catch (MessagingException e) {
                logger.error("Failed to send email notification", e);
                throw new RuntimeException("Failed to send email notification", e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
}
