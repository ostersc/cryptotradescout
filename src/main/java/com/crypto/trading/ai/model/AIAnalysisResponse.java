package com.crypto.trading.ai.model;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response model for AI market analysis results.
 */
@Schema(
    description = "AI market analysis results with algorithm recommendations and trading insights",
    name = "AIAnalysisResponse"
)
public class AIAnalysisResponse {
    private String marketTrend;
    private String marketSentiment;
    private double volatilityScore;
    private String analysisExplanation;
    private LocalDateTime timestamp;
    private List<AlgorithmSuggestion> algorithmSuggestions;

    public AIAnalysisResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public String getMarketTrend() {
        return marketTrend;
    }

    public void setMarketTrend(String marketTrend) {
        this.marketTrend = marketTrend;
    }

    public String getMarketSentiment() {
        return marketSentiment;
    }

    public void setMarketSentiment(String marketSentiment) {
        this.marketSentiment = marketSentiment;
    }

    public double getVolatilityScore() {
        return volatilityScore;
    }

    public void setVolatilityScore(double volatilityScore) {
        this.volatilityScore = volatilityScore;
    }

    public String getAnalysisExplanation() {
        return analysisExplanation;
    }

    public void setAnalysisExplanation(String analysisExplanation) {
        this.analysisExplanation = analysisExplanation;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public List<AlgorithmSuggestion> getAlgorithmSuggestions() {
        return algorithmSuggestions;
    }

    public void setAlgorithmSuggestions(List<AlgorithmSuggestion> algorithmSuggestions) {
        this.algorithmSuggestions = algorithmSuggestions;
    }

    @Override
    public String toString() {
        return "AIAnalysisResponse{" +
                "marketTrend='" + marketTrend + '\'' +
                ", marketSentiment='" + marketSentiment + '\'' +
                ", volatilityScore=" + volatilityScore +
                ", analysisExplanation='" + analysisExplanation + '\'' +
                ", timestamp=" + timestamp +
                ", algorithmSuggestions=" + algorithmSuggestions +
                '}';
    }
}