
package com.crypto.trading.tax;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Queue;

public class TaxCalculator {
    private final Queue<TaxLot> taxLots = new LinkedList<>();
    
    public void addPurchase(double amount, double price, LocalDateTime purchaseDate) {
        taxLots.add(new TaxLot(amount, price, purchaseDate));
    }
    
    public TaxResult calculateTax(double sellAmount, double sellPrice, LocalDateTime sellDate) {
        double remainingToSell = sellAmount;
        double totalProceeds = 0;
        double totalCostBasis = 0;
        double shortTermGain = 0;
        double longTermGain = 0;
        
        while (remainingToSell > 0 && !taxLots.isEmpty()) {
            TaxLot lot = taxLots.peek();
            double amountFromLot = Math.min(remainingToSell, lot.getRemainingAmount());
            
            double proceeds = amountFromLot * sellPrice;
            double costBasis = amountFromLot * lot.getPrice();
            
            totalProceeds += proceeds;
            totalCostBasis += costBasis;
            
            if (lot.isLongTerm(sellDate)) {
                longTermGain += (proceeds - costBasis);
            } else {
                shortTermGain += (proceeds - costBasis);
            }
            
            remainingToSell -= amountFromLot;
            lot.setRemainingAmount(lot.getRemainingAmount() - amountFromLot);
            
            if (lot.getRemainingAmount() <= 0) {
                taxLots.poll();
            }
        }
        
        return new TaxResult(totalProceeds, totalCostBasis, shortTermGain, longTermGain);
    }
}
