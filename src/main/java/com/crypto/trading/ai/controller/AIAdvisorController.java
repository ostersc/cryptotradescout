package com.crypto.trading.ai.controller;

import com.crypto.trading.ai.model.MarketAnalysis;
import com.crypto.trading.ai.service.AIAdvisorService;
import com.crypto.trading.exchange.model.MarketData;
import com.crypto.trading.service.TradingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Controller for AI-powered trading strategy suggestion API endpoints
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AIAdvisorController {
    private static final Logger logger = LoggerFactory.getLogger(AIAdvisorController.class);
    
    @Autowired
    private AIAdvisorService aiAdvisorService;
    
    @Autowired
    private TradingService tradingService;
    
    /**
     * Get AI-powered trading strategy recommendations for a specific trading pair and exchange
     *
     * @param exchange The exchange name
     * @param tradingPair The trading pair
     * @return A market analysis with AI recommendations
     */
    @GetMapping("/recommendations")
    public Mono<ResponseEntity<MarketAnalysis>> getRecommendations(
            @RequestParam String exchange,
            @RequestParam String tradingPair) {
        
        logger.info("Received request for AI recommendations for {}-{}", exchange, tradingPair);
        
        return tradingService.getMarketData(exchange, tradingPair)
                .flatMap(marketData -> aiAdvisorService.generateMarketAnalysis(marketData)
                        .map(analysis -> {
                            logger.info("Generated AI recommendations for {}-{}", exchange, tradingPair);
                            return ResponseEntity.ok(analysis);
                        }))
                .onErrorResume(e -> {
                    logger.error("Error generating AI recommendations", e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }
    
    /**
     * Get AI-powered trading strategy recommendations based on provided market data
     *
     * @param marketData The market data to analyze
     * @return A market analysis with AI recommendations
     */
    @PostMapping("/analyze")
    public Mono<ResponseEntity<MarketAnalysis>> analyzeMarketData(@RequestBody MarketData marketData) {
        logger.info("Received request to analyze market data for {}-{}", 
                marketData.getExchange(), marketData.getTradingPair());
        
        return aiAdvisorService.generateMarketAnalysis(marketData)
                .map(analysis -> {
                    logger.info("Generated AI analysis for {}-{}", 
                            marketData.getExchange(), marketData.getTradingPair());
                    return ResponseEntity.ok(analysis);
                })
                .onErrorResume(e -> {
                    logger.error("Error analyzing market data", e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }
}