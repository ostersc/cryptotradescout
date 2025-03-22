package com.crypto.trading.ai.model;

import java.util.Map;

/**
 * Represents an AI-generated trading algorithm suggestion with parameter recommendations
 */
public class AlgorithmSuggestion {
    private String algorithmId;
    private String algorithmName;
    private Double confidenceScore;
    private String reasoning;
    private Map<String, Object> recommendedParameters;
    private Double expectedReturnPercent;
    
    // Getters and setters
    public String getAlgorithmId() {
        return algorithmId;
    }
    
    public void setAlgorithmId(String algorithmId) {
        this.algorithmId = algorithmId;
    }
    
    public String getAlgorithmName() {
        return algorithmName;
    }
    
    public void setAlgorithmName(String algorithmName) {
        this.algorithmName = algorithmName;
    }
    
    public Double getConfidenceScore() {
        return confidenceScore;
    }
    
    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
    
    public String getReasoning() {
        return reasoning;
    }
    
    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }
    
    public Map<String, Object> getRecommendedParameters() {
        return recommendedParameters;
    }
    
    public void setRecommendedParameters(Map<String, Object> recommendedParameters) {
        this.recommendedParameters = recommendedParameters;
    }
    
    public Double getExpectedReturnPercent() {
        return expectedReturnPercent;
    }
    
    public void setExpectedReturnPercent(Double expectedReturnPercent) {
        this.expectedReturnPercent = expectedReturnPercent;
    }
}