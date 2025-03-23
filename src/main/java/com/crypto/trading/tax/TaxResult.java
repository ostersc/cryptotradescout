package com.crypto.trading.tax;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents the result of a tax calculation for a cryptocurrency sale.
 * Contains information about total gains, short-term gains, and long-term gains.
 */
@Schema(description = "Tax calculation result for cryptocurrency transactions", 
        name = "TaxResult")
public class TaxResult {
    @Schema(description = "Total capital gain for the transaction", example = "2500.00")
    private final double totalGain;
    
    @Schema(description = "Short-term capital gain (assets held < 1 year)", example = "1500.00")
    private final double shortTermGain;
    
    @Schema(description = "Long-term capital gain (assets held >= 1 year)", example = "1000.00")
    private final double longTermGain;
    
    /**
     * Constructor for TaxResult.
     * 
     * @param totalGain the total capital gain amount
     * @param shortTermGain the short-term capital gain amount (held < 1 year)
     * @param longTermGain the long-term capital gain amount (held >= 1 year)
     */
    public TaxResult(double totalGain, double shortTermGain, double longTermGain) {
        this.totalGain = totalGain;
        this.shortTermGain = shortTermGain;
        this.longTermGain = longTermGain;
    }
    
    /**
     * Get the total gain from the transaction.
     * 
     * @return the total gain
     */
    public double getTotalGain() {
        return totalGain;
    }
    
    /**
     * Get the short-term gain from the transaction (assets held < 1 year).
     * 
     * @return the short-term gain
     */
    public double getShortTermGain() {
        return shortTermGain;
    }
    
    /**
     * Get the long-term gain from the transaction (assets held >= 1 year).
     * 
     * @return the long-term gain
     */
    public double getLongTermGain() {
        return longTermGain;
    }
    
    @Override
    public String toString() {
        return "TaxResult{" +
                "totalGain=" + totalGain +
                ", shortTermGain=" + shortTermGain +
                ", longTermGain=" + longTermGain +
                '}';
    }
}