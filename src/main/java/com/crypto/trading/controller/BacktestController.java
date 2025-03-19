package com.crypto.trading.controller;

import com.crypto.trading.algorithm.AlgorithmRegistry;
import com.crypto.trading.algorithm.TradingAlgorithm;
import com.crypto.trading.backtest.BacktestResult;
import com.crypto.trading.backtest.BacktestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
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
     * Run a backtest with the specified parameters.
     * 
     * @param backtestRequest the backtest parameters
     * @return a ResponseEntity containing the backtest results
     */
    @PostMapping("/run")
    public Mono<ResponseEntity<BacktestResult>> runBacktest(
            @RequestBody Map<String, Object> backtestRequest) {
        
        try {
            String algorithmId = (String) backtestRequest.get("algorithmId");
            String exchange = (String) backtestRequest.get("exchange");
            String tradingPair = (String) backtestRequest.get("tradingPair");
            
            // Validate required string parameters
            if (algorithmId == null || exchange == null || tradingPair == null) {
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
                    return Mono.just(ResponseEntity.badRequest().build());
                }
                
                startTime = LocalDateTime.parse(startTimeStr);
                endTime = LocalDateTime.parse(endTimeStr);
            } catch (Exception e) {
                logger.error("Error parsing dates", e);
                return Mono.just(ResponseEntity.badRequest().build());
            }
            
            // Get initial capital
            double initialCapital = 10000.0; // Default
            if (backtestRequest.containsKey("initialCapital")) {
                try {
                    initialCapital = ((Number) backtestRequest.get("initialCapital")).doubleValue();
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
            
            // Validate algorithm parameters
            if (!algorithm.validateParameters(algorithmParams)) {
                logger.error("Invalid algorithm parameters: {}", algorithmParams);
                return Mono.just(ResponseEntity.badRequest().build());
            }
            
            logger.info("Starting backtest for algorithm {} on {} {} from {} to {}",
                    algorithmId, exchange, tradingPair, startTime, endTime);
            
            // Run the backtest
            return backtestService.runBacktest(
                    algorithm, exchange, tradingPair, startTime, endTime, 
                    initialCapital, algorithmParams)
                    .map(ResponseEntity::ok)
                    .defaultIfEmpty(ResponseEntity.notFound().build())
                    .onErrorResume(e -> {
                        logger.error("Error running backtest", e);
                        return Mono.just(ResponseEntity.badRequest().build());
                    });
                    
        } catch (Exception e) {
            logger.error("Error processing backtest request", e);
            return Mono.just(ResponseEntity.badRequest().build());
        }
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
}
