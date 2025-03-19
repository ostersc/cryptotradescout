package com.crypto.trading.controller;

import java.time.LocalDateTime;
import java.util.HashMap;

import com.crypto.trading.exchange.model.MarketData;
import com.crypto.trading.exchange.model.Order;
import com.crypto.trading.exchange.model.OrderType;
import com.crypto.trading.service.TradingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * REST controller for trading operations.
 * Provides endpoints for retrieving market data, placing orders, and checking order status.
 */
@RestController
@RequestMapping("/api/trading")
public class TradingController {
    private static final Logger logger = LoggerFactory.getLogger(TradingController.class);
    
    private final TradingService tradingService;
    
    /**
     * Constructor with TradingService.
     * 
     * @param tradingService the trading service
     */
    public TradingController(TradingService tradingService) {
        this.tradingService = tradingService;
    }
    
    /**
     * Get current market data for a trading pair.
     * 
     * @param exchange the exchange name
     * @param tradingPair the trading pair
     * @return a ResponseEntity containing the market data
     */
    @GetMapping("/market-data")
    public Mono<ResponseEntity<MarketData>> getMarketData(
            @RequestParam String exchange,
            @RequestParam String tradingPair) {
        
        logger.info("Request for market data: {} on {}", tradingPair, exchange);
        
        return tradingService.getMarketData(exchange, tradingPair)
                .doOnNext(data -> logger.info("Received market data successfully: {}", data))
                .map(ResponseEntity::ok)
                .doOnError(e -> logger.error("Error getting market data: {}", e.getMessage(), e))
                .onErrorResume(e -> {
                    logger.error("Handling error in controller: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(500)
                        .body(new MarketData(tradingPair, 0, 0, 0, 0, 
                               java.time.LocalDateTime.now(), "Error: " + e.getMessage())));
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    /**
     * Get a stream of market data updates.
     * 
     * @param exchange the exchange name
     * @param tradingPair the trading pair
     * @return a Flux of server-sent events containing market data
     */
    @GetMapping(value = "/market-data-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MarketData> getMarketDataStream(
            @RequestParam String exchange,
            @RequestParam String tradingPair) {
        
        logger.info("Request for market data stream: {} on {}", tradingPair, exchange);
        
        return tradingService.getMarketDataStream(exchange, tradingPair)
                .delayElements(Duration.ofSeconds(1)); // Throttle updates
    }
    
    /**
     * Execute a market order.
     * 
     * @param orderRequest the order details
     * @return a ResponseEntity containing the executed order
     */
    @PostMapping("/execute-market-order")
    public Mono<ResponseEntity<Order>> executeMarketOrder(@RequestBody Map<String, Object> orderRequest) {
        logger.info("Request to execute market order: {}", orderRequest);
        
        String exchange = (String) orderRequest.get("exchange");
        String tradingPair = (String) orderRequest.get("tradingPair");
        String side = (String) orderRequest.get("side");
        Double amount = getDoubleValue(orderRequest, "amount");
        
        if (exchange == null || tradingPair == null || side == null || amount == null) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        boolean isBuy = "buy".equalsIgnoreCase(side);
        
        Order order = new Order(
                tradingPair,
                OrderType.MARKET,
                amount,
                0.0 // Price will be determined by the market
        );
        
        return (isBuy ? tradingService.executeBuyOrder(exchange, order) 
                      : tradingService.executeSellOrder(exchange, order))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .onErrorResume(e -> {
                    logger.error("Error executing market order", e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }
    
    /**
     * Execute a limit order.
     * 
     * @param orderRequest the order details
     * @return a ResponseEntity containing the executed order
     */
    @PostMapping("/execute-limit-order")
    public Mono<ResponseEntity<Order>> executeLimitOrder(@RequestBody Map<String, Object> orderRequest) {
        logger.info("Request to execute limit order: {}", orderRequest);
        
        String exchange = (String) orderRequest.get("exchange");
        String tradingPair = (String) orderRequest.get("tradingPair");
        String side = (String) orderRequest.get("side");
        Double amount = getDoubleValue(orderRequest, "amount");
        Double price = getDoubleValue(orderRequest, "price");
        
        if (exchange == null || tradingPair == null || side == null || 
            amount == null || price == null) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        boolean isBuy = "buy".equalsIgnoreCase(side);
        
        Order order = new Order(
                tradingPair,
                OrderType.LIMIT,
                amount,
                price
        );
        
        return (isBuy ? tradingService.executeBuyOrder(exchange, order) 
                      : tradingService.executeSellOrder(exchange, order))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .onErrorResume(e -> {
                    logger.error("Error executing limit order", e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }
    
    /**
     * Check the status of an order.
     * 
     * @param exchange the exchange name
     * @param orderId the order ID
     * @return a ResponseEntity containing the order status
     */
    @GetMapping("/order-status")
    public Mono<ResponseEntity<Order>> getOrderStatus(
            @RequestParam String exchange,
            @RequestParam String orderId) {
        
        logger.info("Request for order status: {} on {}", orderId, exchange);
        
        return tradingService.getOrderStatus(exchange, orderId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    /**
     * Get the available balance for a currency.
     * 
     * @param exchange the exchange name
     * @param currency the currency code
     * @return a ResponseEntity containing the balance
     */
    @GetMapping("/balance")
    public Mono<ResponseEntity<Double>> getBalance(
            @RequestParam String exchange,
            @RequestParam String currency) {
        
        logger.info("Request for balance: {} on {}", currency, exchange);
        
        return tradingService.getBalance(exchange, currency)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    /**
     * Control the automated trading system.
     * 
     * @param command the command (start, stop, status)
     * @return a ResponseEntity with the result
     */
    @PostMapping("/system-control")
    public Mono<ResponseEntity<Map<String, Object>>> controlTradingSystem(
            @RequestBody Map<String, String> request) {
        
        String command = request.get("command");
        
        if (command == null) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        logger.info("System control command received: {}", command);
        
        switch (command.toLowerCase()) {
            case "start":
                return tradingService.startAutomatedTrading()
                        .map(result -> ResponseEntity.ok(Map.of(
                                "status", "success",
                                "message", "Automated trading started",
                                "active", result)));
                
            case "stop":
                return tradingService.stopAutomatedTrading()
                        .map(result -> ResponseEntity.ok(Map.of(
                                "status", "success",
                                "message", "Automated trading stopped",
                                "active", !result)));
                
            case "status":
                return tradingService.isAutomatedTradingActive()
                        .map(active -> ResponseEntity.ok(Map.of(
                                "status", "success",
                                "active", active)));
                
            default:
                return Mono.just(ResponseEntity.badRequest()
                        .body(Map.of(
                                "status", "error",
                                "message", "Unknown command: " + command)));
        }
    }
    
    /**
     * Helper method to get a Double value from a map.
     * 
     * @param map the map
     * @param key the key
     * @return the Double value, or null if not present or not a number
     */
    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    @GetMapping("/system-status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "operational");
        status.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(status);
    }
}
