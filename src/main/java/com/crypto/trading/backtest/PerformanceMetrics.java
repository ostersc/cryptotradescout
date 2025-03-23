package com.crypto.trading.backtest;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contains various performance metrics for evaluating trading algorithm performance.
 * Used to assess the quality of a trading strategy during backtesting.
 * Enhanced with fee and tax tracking.
 */
@Schema(
    description = "Performance metrics for evaluating trading algorithm effectiveness",
    name = "PerformanceMetrics"
)
public class PerformanceMetrics {
    private final double totalProfit;
    private final double totalReturnPercentage;
    private final double maxDrawdownPercentage;
    private final int numberOfTrades;
    private final double volatilityPercentage;
    private final double sharpeRatio;
    private final double totalFees;
    private final double totalTaxes;
    private final double profitAfterFeesAndTaxes;
    private final double returnAfterFeesAndTaxes;
    private final double feeImpactPercentage;
    private final double taxImpactPercentage;

    /**
     * Default constructor with zero values.
     */
    public PerformanceMetrics() {
        this(0, 0, 0, 0, 0, 0, 0, 0);
    }

    /**
     * Basic constructor with essential metrics.
     *
     * @param totalProfit the total profit in absolute terms
     * @param totalReturnPercentage the total return as a percentage
     * @param maxDrawdownPercentage the maximum drawdown as a percentage
     * @param numberOfTrades the total number of trades executed
     * @param volatilityPercentage the volatility as a percentage
     * @param sharpeRatio the Sharpe ratio (reward-to-risk)
     * @param totalFees the total fees paid
     * @param totalTaxes the total taxes paid
     */
    public PerformanceMetrics(double totalProfit, double totalReturnPercentage,
                             double maxDrawdownPercentage, int numberOfTrades,
                             double volatilityPercentage, double sharpeRatio,
                             double totalFees, double totalTaxes) {
        this.totalProfit = totalProfit;
        this.totalReturnPercentage = totalReturnPercentage;
        this.maxDrawdownPercentage = maxDrawdownPercentage;
        this.numberOfTrades = numberOfTrades;
        this.volatilityPercentage = volatilityPercentage;
        this.sharpeRatio = sharpeRatio;
        this.totalFees = totalFees;
        this.totalTaxes = totalTaxes;
        this.profitAfterFeesAndTaxes = totalProfit - totalFees - totalTaxes;
        this.returnAfterFeesAndTaxes = calculateReturnAfterFeesAndTaxes(totalProfit, totalReturnPercentage, totalFees, totalTaxes);
        this.feeImpactPercentage = totalProfit != 0 ? (totalFees / Math.abs(totalProfit)) * 100 : 0;
        this.taxImpactPercentage = totalProfit != 0 ? (totalTaxes / Math.abs(totalProfit)) * 100 : 0;
    }
    
    /**
     * Calculate the return percentage after fees and taxes.
     * 
     * @param totalProfit the total profit
     * @param returnPercentage the return percentage before fees and taxes
     * @param totalFees the total fees
     * @param totalTaxes the total taxes
     * @return the return percentage after fees and taxes
     */
    private double calculateReturnAfterFeesAndTaxes(double totalProfit, double returnPercentage, 
                                                  double totalFees, double totalTaxes) {
        if (totalProfit <= 0) {
            return returnPercentage;
        }
        
        double initialCapital = totalProfit / (returnPercentage / 100);
        double netProfit = totalProfit - totalFees - totalTaxes;
        return (netProfit / initialCapital) * 100;
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
    
    /**
     * Get the total fees paid.
     *
     * @return the total fees
     */
    public double getTotalFees() {
        return totalFees;
    }
    
    /**
     * Get the total taxes paid.
     *
     * @return the total taxes
     */
    public double getTotalTaxes() {
        return totalTaxes;
    }
    
    /**
     * Get the profit after fees and taxes.
     *
     * @return the profit after fees and taxes
     */
    public double getProfitAfterFeesAndTaxes() {
        return profitAfterFeesAndTaxes;
    }
    
    /**
     * Get the return percentage after fees and taxes.
     *
     * @return the return percentage after fees and taxes
     */
    public double getReturnAfterFeesAndTaxes() {
        return returnAfterFeesAndTaxes;
    }
    
    /**
     * Get the fee impact percentage (fees as a percentage of profit).
     *
     * @return the fee impact percentage
     */
    public double getFeeImpactPercentage() {
        return feeImpactPercentage;
    }
    
    /**
     * Get the tax impact percentage (taxes as a percentage of profit).
     *
     * @return the tax impact percentage
     */
    public double getTaxImpactPercentage() {
        return taxImpactPercentage;
    }
    
    /**
     * Calculate the average fee per trade.
     *
     * @return the average fee per trade
     */
    public double getAverageFeePerTrade() {
        if (numberOfTrades <= 0) {
            return 0;
        }
        return totalFees / numberOfTrades;
    }
    
    /**
     * Calculate the average tax per trade.
     *
     * @return the average tax per trade
     */
    public double getAverageTaxPerTrade() {
        if (numberOfTrades <= 0) {
            return 0;
        }
        return totalTaxes / numberOfTrades;
    }
    
    /**
     * Calculate the cost ratio (fees and taxes as a percentage of profit).
     *
     * @return the cost ratio
     */
    public double getCostRatio() {
        if (totalProfit <= 0) {
            return 0;
        }
        return ((totalFees + totalTaxes) / Math.abs(totalProfit)) * 100;
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
                ", totalFees=" + totalFees +
                ", totalTaxes=" + totalTaxes +
                ", profitAfterFeesAndTaxes=" + profitAfterFeesAndTaxes +
                ", returnAfterFeesAndTaxes=" + returnAfterFeesAndTaxes + "%" +
                ", feeImpactPercentage=" + feeImpactPercentage + "%" +
                ", taxImpactPercentage=" + taxImpactPercentage + "%" +
                '}';
    }
}
