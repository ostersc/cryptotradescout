package com.crypto.trading.backtest;

import com.crypto.trading.exchange.model.Order;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents the results of a backtest run for a trading algorithm.
 * Contains the details of the backtest and its performance metrics.
 */
public class BacktestResult {
    private final String algorithmId;
    private final String exchange;
    private final String tradingPair;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final double initialCapital;
    private final List<Order> generatedOrders;
    private final long executionTimeMs;
    private final PerformanceMetrics metrics;
    private String errorMessage;
    private boolean hasError = false;


    /**
     * Constructor for BacktestResult.
     *
     * @param algorithmId the ID of the algorithm used
     * @param exchange the exchange used in the backtest
     * @param tradingPair the trading pair used in the backtest
     * @param startTime the start time of the backtest period
     * @param endTime the end time of the backtest period
     * @param initialCapital the initial capital used in the backtest
     * @param generatedOrders the orders generated during the backtest
     * @param executionTimeMs the execution time of the backtest in milliseconds
     * @param metrics the performance metrics calculated from the backtest
     */
    public BacktestResult(String algorithmId, String exchange, String tradingPair,
                         LocalDateTime startTime, LocalDateTime endTime, double initialCapital,
                         List<Order> generatedOrders, long executionTimeMs, PerformanceMetrics metrics) {
        this.algorithmId = algorithmId;
        this.exchange = exchange;
        this.tradingPair = tradingPair;
        this.startTime = startTime;
        this.endTime = endTime;
        this.initialCapital = initialCapital;
        this.generatedOrders = generatedOrders;
        this.executionTimeMs = executionTimeMs;
        this.metrics = metrics;
    }

    /**
     * Get the algorithm ID.
     *
     * @return the algorithm ID
     */
    public String getAlgorithmId() {
        return algorithmId;
    }

    /**
     * Get the exchange name.
     *
     * @return the exchange name
     */
    public String getExchange() {
        return exchange;
    }

    /**
     * Get the trading pair.
     *
     * @return the trading pair
     */
    public String getTradingPair() {
        return tradingPair;
    }

    /**
     * Get the start time of the backtest period.
     *
     * @return the start time
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }

    /**
     * Get the end time of the backtest period.
     *
     * @return the end time
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }

    /**
     * Get the initial capital.
     *
     * @return the initial capital
     */
    public double getInitialCapital() {
        return initialCapital;
    }

    /**
     * Get the orders generated during the backtest.
     *
     * @return the generated orders
     */
    public List<Order> getGeneratedOrders() {
        return generatedOrders;
    }

    /**
     * Get the execution time in milliseconds.
     *
     * @return the execution time
     */
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    /**
     * Get the performance metrics.
     *
     * @return the performance metrics
     */
    public PerformanceMetrics getMetrics() {
        return metrics;
    }

    /**
     * Get the number of trades executed during the backtest.
     *
     * @return the number of trades, or 0 if no orders were generated
     */
    public int getNumberOfTrades() {
        return generatedOrders != null ? generatedOrders.size() : 0;
    }

    /**
     * Get the timespan of the backtest in days.
     *
     * @return the backtest timespan in days
     */
    public long getBacktestTimeSpanDays() {
        return java.time.Duration.between(startTime, endTime).toDays();
    }
    
    /**
     * Get the error message if an error occurred during the backtest.
     *
     * @return the error message, or null if no error occurred
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Set the error message and mark this result as having an error.
     *
     * @param errorMessage the error message to set
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.hasError = true;
    }

    /**
     * Check if this result represents an error condition.
     *
     * @return true if an error occurred, false otherwise
     */
    public boolean hasError() {
        return hasError;
    }

    /**
     * Factory method to create a result object for an error condition.
     *
     * @param algorithmId the algorithm ID
     * @param exchange the exchange name
     * @param tradingPair the trading pair
     * @param startTime the start time
     * @param endTime the end time 
     * @param initialCapital the initial capital
     * @param errorMessage the error message
     * @return a BacktestResult instance with the error details
     */
    public static BacktestResult createErrorResult(
            String algorithmId, String exchange, String tradingPair,
            LocalDateTime startTime, LocalDateTime endTime, double initialCapital,
            String errorMessage) {
        BacktestResult result = new BacktestResult(
                algorithmId, exchange, tradingPair, startTime, endTime,
                initialCapital, null, 0, null);
        result.setErrorMessage(errorMessage);
        return result;
    }

    @Override
    public String toString() {
        if (hasError) {
            return "BacktestResult{" +
                    "algorithmId='" + algorithmId + '\'' +
                    ", exchange='" + exchange + '\'' +
                    ", tradingPair='" + tradingPair + '\'' +
                    ", ERROR='" + errorMessage + '\'' +
                    '}';
        }
        return "BacktestResult{" +
                "algorithmId='" + algorithmId + '\'' +
                ", exchange='" + exchange + '\'' +
                ", tradingPair='" + tradingPair + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", initialCapital=" + initialCapital +
                ", numberOfTrades=" + (generatedOrders != null ? generatedOrders.size() : 0) +
                ", executionTimeMs=" + executionTimeMs +
                ", metrics=" + metrics +
                '}';
    }
}
