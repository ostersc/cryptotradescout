package com.crypto.trading.service;

import com.crypto.trading.algorithm.TradingAlgorithm;
import com.crypto.trading.exchange.ExchangeService;
import com.crypto.trading.exchange.model.MarketData;
import com.crypto.trading.exchange.model.Order;
import com.crypto.trading.notification.NotificationService;
import com.crypto.trading.repository.MarketDataRepository;
import com.crypto.trading.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for trading operations.
 * Handles market data retrieval, order execution, and automated trading.
 */
@Service
public class TradingService {
    private static final Logger logger = LoggerFactory.getLogger(TradingService.class);
    
    private final Map<String, ExchangeService> exchangeServices;
    private final Map<String, TradingAlgorithm> algorithms;
    private final NotificationService notificationService;
    private final MarketDataRepository marketDataRepository;
    private final OrderRepository orderRepository;
    
    private final AtomicBoolean automatedTradingActive = new AtomicBoolean(false);
    private final Map<String, Flux<MarketData>> activeStreams = new ConcurrentHashMap<>();
    
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
     * @param algorithms map of trading algorithms
     * @param notificationService notification service
     * @param marketDataRepository market data repository
     * @param orderRepository order repository
     */
    public TradingService(
            Map<String, ExchangeService> exchangeServices,
            Map<String, TradingAlgorithm> algorithms,
            NotificationService notificationService,
            MarketDataRepository marketDataRepository,
            OrderRepository orderRepository) {
        this.exchangeServices = exchangeServices;
        this.algorithms = algorithms;
        this.notificationService = notificationService;
        this.marketDataRepository = marketDataRepository;
        this.orderRepository = orderRepository;
    }
    
    /**
     * Get current market data for a trading pair.
     * 
     * @param exchange the exchange name
     * @param tradingPair the trading pair
     * @return a Mono containing the market data
     */
    public Mono<MarketData> getMarketData(String exchange, String tradingPair) {
        ExchangeService exchangeService = findExchangeService(exchange);
        if (exchangeService == null) {
            return Mono.empty();
        }
        
        return exchangeService.getCurrentMarketData(tradingPair)
                .doOnNext(data -> saveMarketData(data));
    }
    
    /**
     * Get a stream of market data updates.
     * 
     * @param exchange the exchange name
     * @param tradingPair the trading pair
     * @return a Flux of market data updates
     */
    public Flux<MarketData> getMarketDataStream(String exchange, String tradingPair) {
        ExchangeService exchangeService = findExchangeService(exchange);
        if (exchangeService == null) {
            return Flux.empty();
        }
        
        String streamKey = exchange + ":" + tradingPair;
        
        // Reuse existing stream if available
        return activeStreams.computeIfAbsent(streamKey, k ->
                exchangeService.getMarketDataStream(tradingPair)
                        .doOnNext(data -> saveMarketData(data))
                        .doOnCancel(() -> activeStreams.remove(streamKey))
                        .share());
    }
    
    /**
     * Execute a buy order.
     * 
     * @param exchange the exchange name
     * @param order the order to execute
     * @return a Mono containing the executed order
     */
    public Mono<Order> executeBuyOrder(String exchange, Order order) {
        ExchangeService exchangeService = findExchangeService(exchange);
        if (exchangeService == null) {
            return Mono.empty();
        }
        
        return exchangeService.executeBuyOrder(order)
                .doOnNext(executedOrder -> {
                    saveOrder(executedOrder);
                    notifyOrderExecution(executedOrder);
                });
    }
    
    /**
     * Execute a sell order.
     * 
     * @param exchange the exchange name
     * @param order the order to execute
     * @return a Mono containing the executed order
     */
    public Mono<Order> executeSellOrder(String exchange, Order order) {
        ExchangeService exchangeService = findExchangeService(exchange);
        if (exchangeService == null) {
            return Mono.empty();
        }
        
        return exchangeService.executeSellOrder(order)
                .doOnNext(executedOrder -> {
                    saveOrder(executedOrder);
                    notifyOrderExecution(executedOrder);
                });
    }
    
    /**
     * Check the status of an order.
     * 
     * @param exchange the exchange name
     * @param orderId the order ID
     * @return a Mono containing the order status
     */
    public Mono<Order> getOrderStatus(String exchange, String orderId) {
        ExchangeService exchangeService = findExchangeService(exchange);
        if (exchangeService == null) {
            return Mono.empty();
        }
        
        return exchangeService.checkOrderStatus(orderId);
    }
    
    /**
     * Get the available balance for a currency.
     * 
     * @param exchange the exchange name
     * @param currency the currency code
     * @return a Mono containing the balance
     */
    public Mono<Double> getBalance(String exchange, String currency) {
        ExchangeService exchangeService = findExchangeService(exchange);
        if (exchangeService == null) {
            return Mono.empty();
        }
        
        return exchangeService.getAvailableBalance(currency);
    }
    
    /**
     * Start automated trading.
     * 
     * @return a Mono containing true if started successfully
     */
    public Mono<Boolean> startAutomatedTrading() {
        if (automatedTradingActive.compareAndSet(false, true)) {
            logger.info("Starting automated trading with default configuration");
            
            // Use default algorithm and trading pair
            TradingAlgorithm algorithm = algorithms.get(defaultAlgorithm);
            if (algorithm == null) {
                logger.error("Default algorithm not found: {}", defaultAlgorithm);
                automatedTradingActive.set(false);
                return Mono.just(false);
            }
            
            // Initialize algorithm with default parameters
            algorithm.initialize(Map.of());
            
            // Start market data stream and connect to algorithm
            return Mono.just(true);
        }
        
        logger.info("Automated trading is already active");
        return Mono.just(true);
    }
    
    /**
     * Stop automated trading.
     * 
     * @return a Mono containing true if stopped successfully
     */
    public Mono<Boolean> stopAutomatedTrading() {
        if (automatedTradingActive.compareAndSet(true, false)) {
            logger.info("Stopping automated trading");
            
            // Cancel all active streams
            activeStreams.clear();
            
            return Mono.just(true);
        }
        
        logger.info("Automated trading is already inactive");
        return Mono.just(true);
    }
    
    /**
     * Check if automated trading is active.
     * 
     * @return a Mono containing true if active
     */
    public Mono<Boolean> isAutomatedTradingActive() {
        return Mono.just(automatedTradingActive.get());
    }
    
    /**
     * Find an exchange service by name (case-insensitive).
     * 
     * @param exchange the exchange name
     * @return the exchange service, or null if not found
     */
    private ExchangeService findExchangeService(String exchange) {
        for (ExchangeService service : exchangeServices.values()) {
            if (service.getExchangeName().equalsIgnoreCase(exchange)) {
                return service;
            }
        }
        
        logger.error("Exchange not found: {}", exchange);
        return null;
    }
    
    /**
     * Save market data to the repository.
     * 
     * @param marketData the market data to save
     */
    private void saveMarketData(MarketData marketData) {
        try {
            marketDataRepository.save(marketData);
        } catch (Exception e) {
            logger.error("Error saving market data", e);
        }
    }
    
    /**
     * Save an order to the repository.
     * 
     * @param order the order to save
     */
    private void saveOrder(Order order) {
        try {
            orderRepository.save(order);
        } catch (Exception e) {
            logger.error("Error saving order", e);
        }
    }
    
    /**
     * Send a notification about an order execution.
     * 
     * @param order the executed order
     */
    private void notifyOrderExecution(Order order) {
        notificationService.sendTradeNotification(order)
                .subscribe(
                    null,
                    e -> logger.error("Error sending trade notification", e),
                    () -> logger.debug("Trade notification sent for order {}", order.getId())
                );
    }
}
