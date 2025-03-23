package com.crypto.trading.tax;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a tax lot for a cryptocurrency purchase.
 * Used to track cost basis and holding period for tax purposes.
 */
@Schema(description = "Tax lot for tracking cryptocurrency purchases and cost basis", 
        name = "TaxLot")
public class TaxLot {
    @Schema(description = "Original amount of cryptocurrency in this lot", example = "0.5")
    private final double amount;
    
    @Schema(description = "Cost basis per unit in USD", example = "42500.00")
    private final double costBasis;
    
    @Schema(description = "Date and time when the cryptocurrency was purchased", 
            example = "2025-01-15T14:30:00")
    private final LocalDateTime purchaseDate;
    
    @Schema(description = "Remaining amount of cryptocurrency in this lot", example = "0.35")
    private double remainingAmount;
    
    /**
     * Constructor for TaxLot.
     * 
     * @param amount the amount of cryptocurrency purchased
     * @param costBasis the cost basis per unit
     * @param purchaseDate the date the cryptocurrency was purchased
     */
    public TaxLot(double amount, double costBasis, LocalDateTime purchaseDate) {
        this.amount = amount;
        this.costBasis = costBasis;
        this.purchaseDate = purchaseDate;
        this.remainingAmount = amount;
    }
    
    /**
     * Get the amount of cryptocurrency in this lot.
     * 
     * @return the amount
     */
    public double getAmount() {
        return amount;
    }
    
    /**
     * Get the cost basis per unit.
     * 
     * @return the cost basis
     */
    public double getCostBasis() {
        return costBasis;
    }
    
    /**
     * Get the purchase date.
     * 
     * @return the purchase date
     */
    public LocalDateTime getPurchaseDate() {
        return purchaseDate;
    }
    
    /**
     * Get the remaining amount in this lot.
     * 
     * @return the remaining amount
     */
    public double getRemainingAmount() {
        return remainingAmount;
    }
    
    /**
     * Set the remaining amount in this lot.
     * 
     * @param remainingAmount the remaining amount
     */
    public void setRemainingAmount(double remainingAmount) {
        this.remainingAmount = remainingAmount;
    }
    
    /**
     * Check if this lot is fully consumed.
     * 
     * @return true if the lot is fully consumed, false otherwise
     */
    public boolean isFullyConsumed() {
        return remainingAmount <= 0.000001; // Account for floating point precision
    }
    
    /**
     * Check if this lot represents a long-term holding (held for >= 1 year).
     * 
     * @param sellDate the date the cryptocurrency was sold
     * @return true if long-term, false if short-term
     */
    public boolean isLongTerm(LocalDateTime sellDate) {
        // Calculate holding period in days
        // 365 days is used as a simple approximation for a year
        long daysBetween = java.time.Duration.between(purchaseDate, sellDate).toDays();
        return daysBetween >= 365;
    }
    
    @Override
    public String toString() {
        return "TaxLot{" +
                "amount=" + amount +
                ", costBasis=" + costBasis +
                ", purchaseDate=" + purchaseDate +
                ", remainingAmount=" + remainingAmount +
                '}';
    }
}