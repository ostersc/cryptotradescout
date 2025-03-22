package com.crypto.trading.controller;

import com.crypto.trading.algorithm.AlgorithmRegistry;
import com.crypto.trading.algorithm.TradingAlgorithm;
import com.crypto.trading.backtest.BacktestResult;
import com.crypto.trading.backtest.BacktestService;
import com.crypto.trading.exchange.ExchangeService;
import com.crypto.trading.exchange.model.MarketData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST controller for backtesting trading algorithms.
 * Provides endpoints for running backtests and retrieving results.
 */
@RestController
@RequestMapping("/api/backtest")
public class BacktestController {
    private static final Logger logger = LoggerFactory.getLogger(BacktestController.class);
    
    private final BacktestService backtestService;
    private final AlgorithmRegistry algorithmRegistry;
    
    /**
     * Constructor with dependencies.
     * 
     * @param backtestService the backtest service
     * @param algorithmRegistry the algorithm registry
     */
    public BacktestController(BacktestService backtestService, AlgorithmRegistry algorithmRegistry) {
        this.backtestService = backtestService;
        this.algorithmRegistry = algorithmRegistry;
    }
    
    /**
     * Run a backtest with the specified parameters (endpoint removed to simplify API).
     * All functionality has been consolidated into the root endpoint.
     * 
     * This method is kept as a placeholder to indicate that this endpoint was 
     * intentionally removed as part of code simplification.
     */
    @PostMapping("/run")
    public Mono<ResponseEntity<String>> removedEndpoint() {
        logger.warn("The /run endpoint has been removed. Please use the root endpoint (/api/backtest) instead.");
        return Mono.just(ResponseEntity
                .status(301)
                .header("Location", "/api/backtest")
                .body("This endpoint has been deprecated. Please use /api/backtest instead."));
    }
    
    /**
     * Run a backtest with URL parameters for simpler use cases.
     * 
     * @param algorithmId the algorithm ID
     * @param exchange the exchange name
     * @param tradingPair the trading pair
     * @param startTime the start time
     * @param endTime the end time
     * @param initialCapital the initial capital
     * @return a ResponseEntity containing the backtest results
     */
    @GetMapping("/run-simple")
    public Mono<ResponseEntity<BacktestResult>> runSimpleBacktest(
            @RequestParam String algorithmId,
            @RequestParam String exchange,
            @RequestParam String tradingPair,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "10000.0") double initialCapital) {
        
        // Get the algorithm
        if (!algorithmRegistry.hasAlgorithm(algorithmId)) {
            logger.error("Algorithm not found: {}", algorithmId);
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        TradingAlgorithm algorithm = algorithmRegistry.getAlgorithm(algorithmId);
        
        logger.info("Starting simple backtest for algorithm {} on {} {} from {} to {}",
                algorithmId, exchange, tradingPair, startTime, endTime);
        
        // Use default parameters for the algorithm
        Map<String, Object> algorithmParams = Map.of();
        
        // Run the backtest
        return backtestService.runBacktest(
                algorithm, exchange, tradingPair, startTime, endTime, 
                initialCapital, algorithmParams)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .onErrorResume(e -> {
                    logger.error("Error running simple backtest", e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }
    
    /**
     * Primary endpoint for running a backtest with the specified parameters.
     * 
     * @param backtestRequest the backtest parameters
     * @return a ResponseEntity containing the backtest results
     */
    @PostMapping("")
    public Mono<ResponseEntity<BacktestResult>> runBacktest(
            @RequestBody Map<String, Object> backtestRequest) {
        
        try {
            String algorithmId = (String) backtestRequest.get("algorithmId");
            if (algorithmId == null) {
                // Try alternate field name
                algorithmId = (String) backtestRequest.get("algorithm");
            }
            
            String exchange = (String) backtestRequest.get("exchange");
            String tradingPair = (String) backtestRequest.get("tradingPair");
            if (tradingPair == null) {
                // Try alternate field name
                tradingPair = (String) backtestRequest.get("pair");
            }
            
            // Validate required string parameters
            if (algorithmId == null || exchange == null || tradingPair == null) {
                logger.error("Missing required parameters: algorithmId={}, exchange={}, tradingPair={}", 
                        algorithmId, exchange, tradingPair);
                return Mono.just(ResponseEntity.badRequest().build());
            }
            
            // Get the algorithm
            if (!algorithmRegistry.hasAlgorithm(algorithmId)) {
                logger.error("Algorithm not found: {}", algorithmId);
                return Mono.just(ResponseEntity.badRequest().build());
            }
            
            TradingAlgorithm algorithm = algorithmRegistry.getAlgorithm(algorithmId);
            
            // Parse dates
            LocalDateTime startTime;
            LocalDateTime endTime;
            
            try {
                String startTimeStr = (String) backtestRequest.get("startTime");
                String endTimeStr = (String) backtestRequest.get("endTime");
                
                if (startTimeStr == null || endTimeStr == null) {
                    logger.error("Missing start or end time");
                    return Mono.just(ResponseEntity.badRequest().build());
                }
                
                // Try to parse in different formats
                startTime = parseDateTime(startTimeStr);
                endTime = parseDateTime(endTimeStr);
                
                if (startTime == null || endTime == null) {
                    logger.error("Could not parse dates: startTime={}, endTime={}", startTimeStr, endTimeStr);
                    return Mono.just(ResponseEntity.badRequest().build());
                }
            } catch (Exception e) {
                logger.error("Error parsing dates", e);
                return Mono.just(ResponseEntity.badRequest().build());
            }
            
            // Get initial capital
            double initialCapital = 10000.0; // Default
            if (backtestRequest.containsKey("initialCapital")) {
                try {
                    Object capValue = backtestRequest.get("initialCapital");
                    if (capValue instanceof Number) {
                        initialCapital = ((Number) capValue).doubleValue();
                    } else if (capValue instanceof String) {
                        initialCapital = Double.parseDouble((String) capValue);
                    }
                } catch (Exception e) {
                    logger.error("Error parsing initialCapital", e);
                    return Mono.just(ResponseEntity.badRequest().build());
                }
            }
            
            // Get algorithm parameters
            @SuppressWarnings("unchecked")
            Map<String, Object> algorithmParams = 
                    (Map<String, Object>) backtestRequest.get("algorithmParams");
            
            if (algorithmParams == null) {
                algorithmParams = Map.of(); // Empty map if not provided
            }
            
            // Validate algorithm parameters (but be lenient if they're missing or incompatible)
            if (!algorithm.validateParameters(algorithmParams)) {
                logger.warn("Invalid algorithm parameters: {}, using defaults", algorithmParams);
                algorithmParams = Map.of(); // Use defaults
            }
            
            // Store variables that need to be final for the lambda in truly final variables
            final String capturedAlgorithmId = algorithmId;
            final String capturedExchange = exchange;
            final String capturedTradingPair = tradingPair;
            final LocalDateTime capturedStartTime = startTime;
            final LocalDateTime capturedEndTime = endTime;
            final double capturedInitialCapital = initialCapital;
            final Map<String, Object> capturedAlgorithmParams = algorithmParams;
            
            logger.info("Starting backtest for algorithm {} on {} {} from {} to {}",
                    capturedAlgorithmId, capturedExchange, capturedTradingPair, capturedStartTime, capturedEndTime);
            
            // Run the backtest
            return backtestService.runBacktest(
                    algorithm, capturedExchange, capturedTradingPair, capturedStartTime, capturedEndTime, 
                    capturedInitialCapital, capturedAlgorithmParams)
                    .map(ResponseEntity::ok)
                    .defaultIfEmpty(ResponseEntity.notFound().build())
                    .onErrorResume(e -> {
                        logger.error("Error running backtest: {}", e.getMessage(), e);
                        
                        // Return a more detailed error response instead of just a bad request
                        BacktestResult errorResult = new BacktestResult(
                            capturedAlgorithmId, capturedExchange, capturedTradingPair, 
                            capturedStartTime, capturedEndTime, capturedInitialCapital,
                            null, 0, null);
                        errorResult.setErrorMessage("Failed to complete backtest: " + e.getMessage());
                        return Mono.just(ResponseEntity.status(500).body(errorResult));
                    });
                    
        } catch (Exception e) {
            logger.error("Error processing backtest request", e);
            return Mono.just(ResponseEntity.badRequest().build());
        }
    }
    
    /**
     * Test endpoint to get historical market data directly for diagnostic purposes.
     * 
     * @param exchange the exchange name
     * @param tradingPair the trading pair
     * @param startTime the start time
     * @param endTime the end time
     * @return a ResponseEntity with the historical data
     */
    @GetMapping("/test-historical-data")
    public Mono<ResponseEntity<List<MarketData>>> testHistoricalData(
            @RequestParam String exchange,
            @RequestParam String tradingPair,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        logger.info("Testing historical data fetch for {} {} from {} to {}", 
            exchange, tradingPair, startTime, endTime);
        
        // Find the exchange service
        ExchangeService exchangeService = null;
        for (ExchangeService service : backtestService.getExchangeServices().values()) {
            if (service.getExchangeName().equalsIgnoreCase(exchange)) {
                exchangeService = service;
                break;
            }
        }
        
        if (exchangeService == null) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        
        // Fetch the historical data directly
        return exchangeService.getHistoricalMarketData(tradingPair, startTime, endTime)
                .map(data -> {
                    logger.info("Retrieved {} historical data points", data.size());
                    return ResponseEntity.ok(data);
                })
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .onErrorResume(e -> {
                    logger.error("Error fetching historical market data: {}", e.getMessage(), e);
                    return Mono.just(ResponseEntity.status(500).build());
                });
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            // Try direct ISO format parsing (default)
            return LocalDateTime.parse(dateTimeStr);
        } catch (Exception e1) {
            try {
                // Try adding seconds if missing
                if (dateTimeStr.length() == 16) { // Missing seconds
                    return LocalDateTime.parse(dateTimeStr + ":00");
                }
            } catch (Exception e2) {
                // Ignore and try next format
            }
            
            try {
                // Try with T separator
                if (!dateTimeStr.contains("T") && dateTimeStr.contains(" ")) {
                    return LocalDateTime.parse(dateTimeStr.replace(" ", "T"));
                }
            } catch (Exception e3) {
                // Ignore and return null
            }
        }
        return null;
    }
}
