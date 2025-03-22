
package com.crypto.trading.tax;

import java.time.LocalDateTime;

public class TaxLot {
    private final double amount;
    private final double price;
    private final LocalDateTime purchaseDate;
    private double remainingAmount;

    public TaxLot(double amount, double price, LocalDateTime purchaseDate) {
        this.amount = amount;
        this.price = price;
        this.purchaseDate = purchaseDate;
        this.remainingAmount = amount;
    }

    public double getCostBasis() {
        return price * amount;
    }

    public boolean isLongTerm(LocalDateTime sellDate) {
        return sellDate.isAfter(purchaseDate.plusYears(1));
    }

    // Getters and setters
    public double getAmount() { return amount; }
    public double getPrice() { return price; }
    public LocalDateTime getPurchaseDate() { return purchaseDate; }
    public double getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(double remainingAmount) { this.remainingAmount = remainingAmount; }
}
