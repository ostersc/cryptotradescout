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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller for AI-powered trading strategy recommendations.
 */
@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI Advisor", description = "API for AI-powered trading strategy recommendations and market analysis")
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
    @Operation(
        summary = "Get AI trading recommendations",
        description = "Analyzes current market conditions and provides AI-powered trading strategy recommendations " +
                "with optimized parameters for the specified exchange and trading pair"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully generated trading recommendations",
            content = @Content(schema = @Schema(implementation = AIAnalysisResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid parameters provided"),
        @ApiResponse(responseCode = "500", description = "Error generating AI recommendations")
    })
    @GetMapping("/recommendations")
    public ResponseEntity<AIAnalysisResponse> getRecommendations(
            @Parameter(description = "The exchange name (e.g., 'Kraken', 'Coinbase')", required = true)
            @RequestParam String exchange,
            
            @Parameter(description = "The trading pair in the format 'BTC-USD'", required = true)
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