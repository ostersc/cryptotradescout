package com.crypto.trading.ai.model;

/**
 * Request model for AI market analysis.
 */
public class AIAnalysisRequest {
    private String exchange;
    private String tradingPair;
    private String timeframe;

    public AIAnalysisRequest() {
    }

    public AIAnalysisRequest(String exchange, String tradingPair, String timeframe) {
        this.exchange = exchange;
        this.tradingPair = tradingPair;
        this.timeframe = timeframe;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getTradingPair() {
        return tradingPair;
    }

    public void setTradingPair(String tradingPair) {
        this.tradingPair = tradingPair;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    @Override
    public String toString() {
        return "AIAnalysisRequest{" +
                "exchange='" + exchange + '\'' +
                ", tradingPair='" + tradingPair + '\'' +
                ", timeframe='" + timeframe + '\'' +
                '}';
    }
}