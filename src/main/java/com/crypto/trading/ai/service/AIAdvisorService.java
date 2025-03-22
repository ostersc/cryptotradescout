package com.crypto.trading.ai.service;

import com.crypto.trading.ai.model.AIAnalysisRequest;
import com.crypto.trading.ai.model.AIAnalysisResponse;
import com.crypto.trading.ai.model.AlgorithmSuggestion;
import com.crypto.trading.algorithm.AlgorithmRegistry;
import com.crypto.trading.algorithm.TradingAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for AI-powered trading strategy recommendations.
 */
@Service
public class AIAdvisorService {
    private static final Logger logger = LoggerFactory.getLogger(AIAdvisorService.class);
    
    private final OpenAIService openAIService;
    private final AlgorithmRegistry algorithmRegistry;
    
    @Autowired
    public AIAdvisorService(OpenAIService openAIService, AlgorithmRegistry algorithmRegistry) {
        this.openAIService = openAIService;
        this.algorithmRegistry = algorithmRegistry;
        logger.info("AIAdvisorService initialized");
    }
    
    /**
     * Get AI-powered trading recommendations for the given market.
     *
     * @param exchange The exchange name
     * @param tradingPair The trading pair
     * @return AIAnalysisResponse with market analysis and algorithm recommendations
     */
    public AIAnalysisResponse getRecommendations(String exchange, String tradingPair) {
        logger.info("Getting AI recommendations for {}/{}", exchange, tradingPair);
        
        try {
            // Create request object
            AIAnalysisRequest request = new AIAnalysisRequest(exchange, tradingPair, "1h");
            
            // Get recommendations from OpenAI
            AIAnalysisResponse response = openAIService.generateMarketAnalysis(request);
            
            // Validate and enhance the recommendations
            validateAndEnhanceRecommendations(response);
            
            return response;
        } catch (Exception e) {
            logger.error("Error getting AI recommendations", e);
            throw new RuntimeException("Failed to get AI recommendations", e);
        }
    }
    
    /**
     * Validate and enhance the AI-generated recommendations.
     *
     * @param response The AI analysis response to validate
     */
    private void validateAndEnhanceRecommendations(AIAnalysisResponse response) {
        logger.info("Validating and enhancing AI recommendations");
        
        if (response == null) {
            logger.warn("AI response is null");
            return;
        }
        
        List<AlgorithmSuggestion> suggestions = response.getAlgorithmSuggestions();
        if (suggestions == null || suggestions.isEmpty()) {
            logger.warn("No algorithm suggestions in AI response");
            return;
        }
        
        // Check if the algorithm exists in our system and add additional information if needed
        suggestions.forEach(suggestion -> {
            try {
                if (algorithmRegistry.hasAlgorithm(suggestion.getAlgorithmId())) {
                    TradingAlgorithm algorithm = algorithmRegistry.getAlgorithm(suggestion.getAlgorithmId());
                    
                    // Update with accurate algorithm name if needed
                    if (!algorithm.getName().equals(suggestion.getAlgorithmName())) {
                        logger.info("Updating algorithm name from {} to {}", 
                            suggestion.getAlgorithmName(), algorithm.getName());
                        suggestion.setAlgorithmName(algorithm.getName());
                    }
                    
                    // Validate parameters against algorithm definition
                    validateParameters(suggestion, algorithm);
                } else {
                    logger.warn("Algorithm not found: {}", suggestion.getAlgorithmId());
                }
            } catch (Exception e) {
                logger.error("Error validating algorithm suggestion: {}", suggestion.getAlgorithmId(), e);
            }
        });
    }
    
    /**
     * Validate algorithm parameters against the algorithm definition.
     *
     * @param suggestion The algorithm suggestion to validate
     * @param algorithm The algorithm definition
     */
    private void validateParameters(AlgorithmSuggestion suggestion, TradingAlgorithm algorithm) {
        // Check if the recommended parameters are valid for this algorithm
        if (suggestion.getRecommendedParameters() != null && !suggestion.getRecommendedParameters().isEmpty()) {
            boolean validParams = algorithm.validateParameters(suggestion.getRecommendedParameters());
            if (!validParams) {
                logger.warn("Invalid parameters for algorithm {}: {}", 
                    suggestion.getAlgorithmId(), suggestion.getRecommendedParameters());
                
                // If invalid, replace with default parameters
                suggestion.setRecommendedParameters(algorithm.getDefaultParameters());
                logger.info("Replaced with default parameters: {}", suggestion.getRecommendedParameters());
            } else {
                logger.info("Validated parameters for algorithm: {}", suggestion.getAlgorithmId());
            }
        } else {
            // If no parameters provided, use defaults
            suggestion.setRecommendedParameters(algorithm.getDefaultParameters());
            logger.info("Using default parameters for algorithm: {}", suggestion.getAlgorithmId());
        }
    }
}