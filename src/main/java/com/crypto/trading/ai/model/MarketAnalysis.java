package com.crypto.trading.ai.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a market analysis with AI-generated trading suggestions
 */
public class MarketAnalysis {
    private String tradingPair;
    private String exchange;
    private LocalDateTime timestamp;
    private String marketTrend;
    private String marketSentiment;
    private Double volatilityScore;
    private List<AlgorithmSuggestion> algorithmSuggestions;
    private String analysisExplanation;
    
    // Getters and setters
    public String getTradingPair() {
        return tradingPair;
    }
    
    public void setTradingPair(String tradingPair) {
        this.tradingPair = tradingPair;
    }
    
    public String getExchange() {
        return exchange;
    }
    
    public void setExchange(String exchange) {
        this.exchange = exchange;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
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
    
    public Double getVolatilityScore() {
        return volatilityScore;
    }
    
    public void setVolatilityScore(Double volatilityScore) {
        this.volatilityScore = volatilityScore;
    }
    
    public List<AlgorithmSuggestion> getAlgorithmSuggestions() {
        return algorithmSuggestions;
    }
    
    public void setAlgorithmSuggestions(List<AlgorithmSuggestion> algorithmSuggestions) {
        this.algorithmSuggestions = algorithmSuggestions;
    }
    
    public String getAnalysisExplanation() {
        return analysisExplanation;
    }
    
    public void setAnalysisExplanation(String analysisExplanation) {
        this.analysisExplanation = analysisExplanation;
    }
}