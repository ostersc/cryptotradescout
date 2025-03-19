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
     * @return the number of trades
     */
    public int getNumberOfTrades() {
        return generatedOrders.size();
    }

    /**
     * Get the timespan of the backtest in days.
     *
     * @return the backtest timespan in days
     */
    public long getBacktestTimeSpanDays() {
        return java.time.Duration.between(startTime, endTime).toDays();
    }

    @Override
    public String toString() {
        return "BacktestResult{" +
                "algorithmId='" + algorithmId + '\'' +
                ", exchange='" + exchange + '\'' +
                ", tradingPair='" + tradingPair + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", initialCapital=" + initialCapital +
                ", numberOfTrades=" + generatedOrders.size() +
                ", executionTimeMs=" + executionTimeMs +
                ", metrics=" + metrics +
                '}';
    }
}
