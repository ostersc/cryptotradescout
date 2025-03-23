package com.crypto.trading.ai.model;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Model for algorithm suggestions from AI analysis.
 */
@Schema(
    description = "Algorithm suggestion with confidence scores and optimized parameters",
    name = "AlgorithmSuggestion"
)
public class AlgorithmSuggestion {
    private String algorithmId;
    private String algorithmName;
    private double confidenceScore;
    private double expectedReturnPercent;
    private String reasoning;
    private Map<String, Object> recommendedParameters;

    public AlgorithmSuggestion() {
    }

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

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public double getExpectedReturnPercent() {
        return expectedReturnPercent;
    }

    public void setExpectedReturnPercent(double expectedReturnPercent) {
        this.expectedReturnPercent = expectedReturnPercent;
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

    @Override
    public String toString() {
        return "AlgorithmSuggestion{" +
                "algorithmId='" + algorithmId + '\'' +
                ", algorithmName='" + algorithmName + '\'' +
                ", confidenceScore=" + confidenceScore +
                ", expectedReturnPercent=" + expectedReturnPercent +
                ", reasoning='" + reasoning + '\'' +
                ", recommendedParameters=" + recommendedParameters +
                '}';
    }
}