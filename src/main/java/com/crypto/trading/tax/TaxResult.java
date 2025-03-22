package com.crypto.trading.tax;

/**
 * Represents the result of a tax calculation for a cryptocurrency sale.
 * Contains information about total gains, short-term gains, and long-term gains.
 */
public class TaxResult {
    private final double totalGain;
    private final double shortTermGain;
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