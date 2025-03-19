package com.crypto.trading.notification;

import com.crypto.trading.exchange.model.Order;
import reactor.core.publisher.Mono;

/**
 * Interface for notification services.
 * Defines operations for sending notifications about trading activities.
 */
public interface NotificationService {

    /**
     * Get the name of this notification service.
     * 
     * @return the service name
     */
    String getServiceName();
    
    /**
     * Send a notification about a trade execution.
     * 
     * @param order the executed order
     * @return a Mono that completes when the notification is sent
     */
    Mono<Void> sendTradeNotification(Order order);
    
    /**
     * Send a notification about a system alert.
     * 
     * @param alertLevel the severity level of the alert
     * @param message the alert message
     * @return a Mono that completes when the notification is sent
     */
    Mono<Void> sendAlertNotification(AlertLevel alertLevel, String message);
    
    /**
     * Send a notification with a custom subject and message.
     * 
     * @param subject the notification subject
     * @param message the notification message
     * @return a Mono that completes when the notification is sent
     */
    Mono<Void> sendCustomNotification(String subject, String message);
    
    /**
     * Check if the notification service is properly configured and operational.
     * 
     * @return true if the service is ready, false otherwise
     */
    boolean isOperational();
    
    /**
     * Enum representing different levels of alert severity.
     */
    enum AlertLevel {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }
}
