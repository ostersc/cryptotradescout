package com.crypto.trading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the cryptocurrency algorithmic trading system.
 * This Spring Boot application provides infrastructure for algorithmic trading,
 * backtesting, and trade notifications.
 */
@SpringBootApplication
@EnableScheduling
public class TradingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradingApplication.class, args);
    }
}
