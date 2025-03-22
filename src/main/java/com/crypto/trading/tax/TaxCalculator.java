package com.crypto.trading.tax;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Calculator for tax implications of cryptocurrency trades.
 * Implements FIFO (First-In, First-Out) accounting method for cost basis.
 * Tracks long-term vs. short-term capital gains.
 */
public class TaxCalculator {
    private final List<TaxLot> lots;
    
    /**
     * Constructor for TaxCalculator.
     */
    public TaxCalculator() {
        this.lots = new ArrayList<>();
    }
    
    /**
     * Add a purchase to the calculator.
     * 
     * @param amount the amount of cryptocurrency purchased
     * @param price the price per unit
     * @param purchaseDate the date of purchase
     */
    public void addPurchase(double amount, double price, LocalDateTime purchaseDate) {
        TaxLot lot = new TaxLot(amount, price, purchaseDate);
        lots.add(lot);
    }
    
    /**
     * Calculate tax for a sale using FIFO accounting method.
     * 
     * @param sellAmount the amount of cryptocurrency sold
     * @param sellPrice the price per unit
     * @param sellDate the date of the sale
     * @return the tax result containing gain information
     */
    public TaxResult calculateTax(double sellAmount, double sellPrice, LocalDateTime sellDate) {
        double remainingToSell = sellAmount;
        double totalGain = 0.0;
        double shortTermGain = 0.0;
        double longTermGain = 0.0;
        
        // Nothing to sell
        if (remainingToSell <= 0 || lots.isEmpty()) {
            return new TaxResult(0, 0, 0);
        }
        
        // FIFO - First-In, First-Out approach
        for (TaxLot lot : new ArrayList<>(lots)) {
            // Skip fully consumed lots
            if (lot.isFullyConsumed()) {
                continue;
            }
            
            // Calculate how much we can sell from this lot
            double amountFromLot = Math.min(remainingToSell, lot.getRemainingAmount());
            
            // Calculate the gain from this portion
            double costBasisForPortion = amountFromLot * lot.getCostBasis();
            double proceedsForPortion = amountFromLot * sellPrice;
            double gainForPortion = proceedsForPortion - costBasisForPortion;
            
            // Add to total gain
            totalGain += gainForPortion;
            
            // Determine if short-term or long-term gain
            if (lot.isLongTerm(sellDate)) {
                longTermGain += gainForPortion;
            } else {
                shortTermGain += gainForPortion;
            }
            
            // Update remaining amount in the lot
            lot.setRemainingAmount(lot.getRemainingAmount() - amountFromLot);
            
            // Update remaining to sell
            remainingToSell -= amountFromLot;
            
            // If we've sold all we need to, break
            if (remainingToSell <= 0.000001) { // Account for floating point precision
                break;
            }
        }
        
        // If there's still more to sell but no lots left, we're selling more than we have
        // This could be an error, but for the purpose of this implementation, we'll just return what we have so far
        
        return new TaxResult(totalGain, shortTermGain, longTermGain);
    }
    
    /**
     * Get the current tax lots.
     * 
     * @return the list of tax lots
     */
    public List<TaxLot> getLots() {
        return new ArrayList<>(lots);
    }
    
    /**
     * Get the current cost basis for all holdings.
     * 
     * @return the total cost basis
     */
    public double getTotalCostBasis() {
        double totalCostBasis = 0.0;
        for (TaxLot lot : lots) {
            totalCostBasis += lot.getRemainingAmount() * lot.getCostBasis();
        }
        return totalCostBasis;
    }
    
    /**
     * Get the total remaining cryptocurrency amount.
     * 
     * @return the total remaining amount
     */
    public double getTotalRemainingAmount() {
        double totalRemaining = 0.0;
        for (TaxLot lot : lots) {
            totalRemaining += lot.getRemainingAmount();
        }
        return totalRemaining;
    }
    
    /**
     * Remove fully consumed lots from the list.
     * This is an optimization to reduce the number of lots to process.
     */
    public void cleanupConsumedLots() {
        lots.removeIf(TaxLot::isFullyConsumed);
    }
}