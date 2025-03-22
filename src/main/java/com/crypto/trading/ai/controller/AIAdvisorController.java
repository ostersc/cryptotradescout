package com.crypto.trading.ai.controller;

import com.crypto.trading.ai.model.AIAnalysisResponse;
import com.crypto.trading.ai.service.AIAdvisorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for AI-powered trading strategy recommendations.
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AIAdvisorController {
    private static final Logger logger = LoggerFactory.getLogger(AIAdvisorController.class);
    
    private final AIAdvisorService aiAdvisorService;
    
    @Autowired
    public AIAdvisorController(AIAdvisorService aiAdvisorService) {
        this.aiAdvisorService = aiAdvisorService;
        logger.info("AIAdvisorController initialized");
    }
    
    /**
     * Get AI-powered trading recommendations for the given market.
     *
     * @param exchange The exchange name
     * @param tradingPair The trading pair
     * @return AIAnalysisResponse with market analysis and algorithm recommendations
     */
    @GetMapping("/recommendations")
    public ResponseEntity<AIAnalysisResponse> getRecommendations(
            @RequestParam String exchange,
            @RequestParam String tradingPair) {
        
        logger.info("Received request for AI recommendations for {}/{}", exchange, tradingPair);
        
        try {
            AIAnalysisResponse response = aiAdvisorService.getRecommendations(exchange, tradingPair);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting AI recommendations", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}