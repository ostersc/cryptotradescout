package com.crypto.trading.backtest;

/**
 * Contains various performance metrics for evaluating trading algorithm performance.
 * Used to assess the quality of a trading strategy during backtesting.
 */
public class PerformanceMetrics {
    private final double totalProfit;
    private final double totalReturnPercentage;
    private final double maxDrawdownPercentage;
    private final int numberOfTrades;
    private final double volatilityPercentage;
    private final double sharpeRatio;

    /**
     * Default constructor with zero values.
     */
    public PerformanceMetrics() {
        this(0, 0, 0, 0, 0, 0);
    }

    /**
     * Full constructor with all metrics.
     *
     * @param totalProfit the total profit in absolute terms
     * @param totalReturnPercentage the total return as a percentage
     * @param maxDrawdownPercentage the maximum drawdown as a percentage
     * @param numberOfTrades the total number of trades executed
     * @param volatilityPercentage the volatility as a percentage
     * @param sharpeRatio the Sharpe ratio (reward-to-risk)
     */
    public PerformanceMetrics(double totalProfit, double totalReturnPercentage,
                             double maxDrawdownPercentage, int numberOfTrades,
                             double volatilityPercentage, double sharpeRatio) {
        this.totalProfit = totalProfit;
        this.totalReturnPercentage = totalReturnPercentage;
        this.maxDrawdownPercentage = maxDrawdownPercentage;
        this.numberOfTrades = numberOfTrades;
        this.volatilityPercentage = volatilityPercentage;
        this.sharpeRatio = sharpeRatio;
    }

    /**
     * Get the total profit.
     *
     * @return the total profit in the base currency
     */
    public double getTotalProfit() {
        return totalProfit;
    }

    /**
     * Get the total return percentage.
     *
     * @return the total return as a percentage
     */
    public double getTotalReturnPercentage() {
        return totalReturnPercentage;
    }

    /**
     * Get the maximum drawdown percentage.
     *
     * @return the maximum drawdown as a percentage
     */
    public double getMaxDrawdownPercentage() {
        return maxDrawdownPercentage;
    }

    /**
     * Get the number of trades.
     *
     * @return the total number of trades
     */
    public int getNumberOfTrades() {
        return numberOfTrades;
    }

    /**
     * Get the volatility percentage.
     *
     * @return the volatility as a percentage
     */
    public double getVolatilityPercentage() {
        return volatilityPercentage;
    }

    /**
     * Get the Sharpe ratio.
     *
     * @return the Sharpe ratio
     */
    public double getSharpeRatio() {
        return sharpeRatio;
    }

    /**
     * Calculate a simplified return-to-drawdown ratio.
     * Higher values indicate better risk-adjusted performance.
     *
     * @return the return-to-drawdown ratio
     */
    public double getReturnToDrawdownRatio() {
        if (maxDrawdownPercentage <= 0) {
            return 0; // Avoid division by zero
        }
        return totalReturnPercentage / maxDrawdownPercentage;
    }

    /**
     * Calculate the average profit per trade.
     *
     * @return the average profit per trade
     */
    public double getAverageProfitPerTrade() {
        if (numberOfTrades <= 0) {
            return 0;
        }
        return totalProfit / numberOfTrades;
    }

    @Override
    public String toString() {
        return "PerformanceMetrics{" +
                "totalProfit=" + totalProfit +
                ", totalReturnPercentage=" + totalReturnPercentage + "%" +
                ", maxDrawdownPercentage=" + maxDrawdownPercentage + "%" +
                ", numberOfTrades=" + numberOfTrades +
                ", volatilityPercentage=" + volatilityPercentage + "%" +
                ", sharpeRatio=" + sharpeRatio +
                '}';
    }
}
