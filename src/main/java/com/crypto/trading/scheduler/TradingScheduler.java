package com.crypto.trading.scheduler;

import com.crypto.trading.algorithm.AlgorithmRegistry;
import com.crypto.trading.algorithm.TradingAlgorithm;
import com.crypto.trading.exchange.ExchangeService;
import com.crypto.trading.exchange.model.MarketData;
import com.crypto.trading.exchange.model.Order;
import com.crypto.trading.notification.NotificationService;
import com.crypto.trading.service.TradingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduler for automated trading tasks.
 * Periodically fetches market data and runs trading algorithms.
 */
@Component
public class TradingScheduler {
    private static final Logger logger = LoggerFactory.getLogger(TradingScheduler.class);
    
    private final Map<String, ExchangeService> exchangeServices;
    private final AlgorithmRegistry algorithmRegistry;
    private final TradingService tradingService;
    private final NotificationService notificationService;
    
    private final AtomicBoolean tradingEnabled = new AtomicBoolean(false);
    private final Map<String, TradingAlgorithm> activeAlgorithms = new ConcurrentHashMap<>();
    private final Map<String, String> algorithmExchanges = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastRuns = new ConcurrentHashMap<>();
    
    @Value("${trading.scheduler.enabled:false}")
    private boolean schedulerEnabled;
    
    @Value("${trading.default-exchange:Coinbase}")
    private String defaultExchange;
    
    @Value("${trading.default-algorithm:simple-moving-average}")
    private String defaultAlgorithm;
    
    @Value("${trading.default-trading-pair:BTC-USD}")
    private String defaultTradingPair;
    
    /**
     * Constructor with dependencies.
     * 
     * @param exchangeServices map of exchange services
     * @param algorithmRegistry algorithm registry
     * @param tradingService trading service
     * @param notificationService notification service
     */
    public TradingScheduler(
            Map<String, ExchangeService> exchangeServices,
            AlgorithmRegistry algorithmRegistry,
            TradingService tradingService,
            NotificationService notificationService) {
        this.exchangeServices = exchangeServices;
        this.algorithmRegistry = algorithmRegistry;
        this.tradingService = tradingService;
        this.notificationService = notificationService;
    }
    
    /**
     * Start the scheduler with a specific algorithm configuration.
     * 
     * @param algorithmId the algorithm ID
     * @param exchange the exchange name
     * @param tradingPair the trading pair
     * @param parameters the algorithm parameters
     * @return true if started successfully
     */
    public boolean startTrading(String algorithmId, String exchange, 
                               String tradingPair, Map<String, Object> parameters) {
        if (tradingEnabled.get()) {
            logger.warn("Trading already enabled, stop first before reconfiguring");
            return false;
        }
        
        // Validate algorithm
        TradingAlgorithm algorithm = algorithmRegistry.getAlgorithm(algorithmId);
        if (algorithm == null) {
            logger.error("Algorithm not found: {}", algorithmId);
            return false;
        }
        
        // Validate exchange
        ExchangeService exchangeService = null;
        for (ExchangeService service : exchangeServices.values()) {
            if (service.getExchangeName().equalsIgnoreCase(exchange)) {
                exchangeService = service;
                break;
            }
        }
        
        if (exchangeService == null) {
            logger.error("Exchange not found: {}", exchange);
            return false;
        }
        
        // Initialize algorithm
        try {
            algorithm.initialize(parameters);
        } catch (Exception e) {
            logger.error("Error initializing algorithm: {}", e.getMessage());
            return false;
        }
        
        // Register algorithm
        String key = tradingPair + ":" + algorithmId;
        activeAlgorithms.put(key, algorithm);
        algorithmExchanges.put(key, exchange);
        
        // Enable trading
        tradingEnabled.set(true);
        
        // Send notification
        notificationService.sendAlertNotification(
                NotificationService.AlertLevel.INFO,
                "Automated trading started with algorithm " + algorithmId + 
                " on " + exchange + " for " + tradingPair)
                .subscribe();
        
        logger.info("Started trading with algorithm {} on {} for {}", 
                algorithmId, exchange, tradingPair);
        
        return true;
    }
    
    /**
     * Stop all automated trading.
     * 
     * @return true if stopped successfully
     */
    public boolean stopTrading() {
        if (tradingEnabled.compareAndSet(true, false)) {
            activeAlgorithms.clear();
            algorithmExchanges.clear();
            
            // Send notification
            notificationService.sendAlertNotification(
                    NotificationService.AlertLevel.INFO,
                    "Automated trading stopped")
                    .subscribe();
            
            logger.info("Stopped all trading activities");
            return true;
        }
        
        return false;
    }
    
    /**
     * Scheduled task to fetch market data and run algorithms.
     * Runs every minute by default.
     */
    @Scheduled(fixedDelayString = "${trading.scheduler.interval:60000}")
    public void scheduledTrading() {
        if (!schedulerEnabled || !tradingEnabled.get()) {
            return;
        }
        
        logger.debug("Running scheduled trading task");
        
        // Process each active algorithm
        for (Map.Entry<String, TradingAlgorithm> entry : activeAlgorithms.entrySet()) {
            String key = entry.getKey();
            String[] parts = key.split(":");
            String tradingPair = parts[0];
            
            TradingAlgorithm algorithm = entry.getValue();
            String exchange = algorithmExchanges.get(key);
            
            // Find the exchange service
            ExchangeService exchangeService = null;
            for (ExchangeService service : exchangeServices.values()) {
                if (service.getExchangeName().equalsIgnoreCase(exchange)) {
                    exchangeService = service;
                    break;
                }
            }
            
            if (exchangeService == null) {
                logger.error("Exchange not found: {}", exchange);
                continue;
            }
            
            // Update last run time
            lastRuns.put(key, LocalDateTime.now());
            
            // Fetch market data and process with algorithm
            try {
                // Create final copies of variables used in lambda
                final TradingAlgorithm finalAlgorithm = algorithm;
                final ExchangeService finalExchangeService = exchangeService;
                final String finalTradingPair = tradingPair;
                
                finalExchangeService.getCurrentMarketData(finalTradingPair)
                        .flatMap(marketData -> processMarketData(finalAlgorithm, finalExchangeService, marketData))
                        .subscribe(
                            order -> logger.info("Generated order: {}", order),
                            error -> logger.error("Error processing market data: {}", error.getMessage())
                        );
            } catch (Exception e) {
                logger.error("Error in scheduled trading for {}: {}", tradingPair, e.getMessage());
            }
        }
    }
    
    /**
     * Process market data with an algorithm and execute any generated orders.
     * 
     * @param algorithm the trading algorithm
     * @param exchangeService the exchange service
     * @param marketData the market data
     * @return a Mono containing the executed order, or empty
     */
    private reactor.core.publisher.Mono<Order> processMarketData(
            TradingAlgorithm algorithm, 
            ExchangeService exchangeService, 
            MarketData marketData) {
        
        return algorithm.processMarketData(marketData)
                .flatMap(order -> {
                    // Execute the order on the exchange
                    if (order.getType().toString().contains("BUY")) {
                        return exchangeService.executeBuyOrder(order);
                    } else {
                        return exchangeService.executeSellOrder(order);
                    }
                })
                .doOnNext(executedOrder -> {
                    // Send notification about the trade
                    notificationService.sendTradeNotification(executedOrder).subscribe();
                });
    }
    
    /**
     * Get the status of the trading scheduler.
     * 
     * @return a map containing status information
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", tradingEnabled.get());
        status.put("schedulerEnabled", schedulerEnabled);
        status.put("activeAlgorithms", activeAlgorithms.size());
        
        Map<String, Object> algorithms = new HashMap<>();
        for (Map.Entry<String, TradingAlgorithm> entry : activeAlgorithms.entrySet()) {
            String key = entry.getKey();
            TradingAlgorithm algorithm = entry.getValue();
            
            Map<String, Object> algoInfo = new HashMap<>();
            algoInfo.put("name", algorithm.getName());
            algoInfo.put("exchange", algorithmExchanges.get(key));
            algoInfo.put("lastRun", lastRuns.getOrDefault(key, null));
            
            algorithms.put(key, algoInfo);
        }
        
        status.put("algorithms", algorithms);
        
        return status;
    }
}
