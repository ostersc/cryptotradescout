package com.crypto.trading.tax;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for the TaxCalculator class.
 * Tests various scenarios of crypto purchases and sales using FIFO accounting.
 */
public class TaxCalculatorTest {

    private TaxCalculator calculator;

    @BeforeEach
    public void setUp() {
        calculator = new TaxCalculator();
    }

    @Test
    public void testEmptyCalculator() {
        assertEquals(0.0, calculator.getTotalCostBasis(), 0.001);
        assertEquals(0.0, calculator.getTotalRemainingAmount(), 0.001);
        assertTrue(calculator.getLots().isEmpty());
    }

    @Test
    public void testSinglePurchaseNoSale() {
        // Given a single purchase with no sales
        LocalDateTime purchaseDate = LocalDateTime.now();
        double amount = 1.0;
        double price = 10000.0;
        
        // When we add the purchase
        calculator.addPurchase(amount, price, purchaseDate);
        
        // Then the cost basis and remaining amount should be correct
        assertEquals(price, calculator.getTotalCostBasis(), 0.001);
        assertEquals(amount, calculator.getTotalRemainingAmount(), 0.001);
        assertEquals(1, calculator.getLots().size());
    }

    @Test
    public void testSinglePurchaseFullSale_Profit() {
        // Given a purchase and a full sale at a higher price (profit scenario)
        LocalDateTime purchaseDate = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        double purchaseAmount = 1.0;
        double purchasePrice = 10000.0;
        
        LocalDateTime saleDate = LocalDateTime.now();
        double salePrice = 15000.0;
        
        // When we add the purchase and calculate tax for a full sale
        calculator.addPurchase(purchaseAmount, purchasePrice, purchaseDate);
        TaxResult result = calculator.calculateTax(purchaseAmount, salePrice, saleDate);
        
        // Then we should have the correct gain calculated
        double expectedGain = (salePrice - purchasePrice) * purchaseAmount; // (15000 - 10000) * 1 = 5000
        assertEquals(expectedGain, result.getTotalGain(), 0.001);
        assertEquals(expectedGain, result.getShortTermGain(), 0.001); // Should be short-term gain
        assertEquals(0.0, result.getLongTermGain(), 0.001); // No long-term gain
        
        // And the remaining amount should be zero
        assertEquals(0.0, calculator.getTotalRemainingAmount(), 0.001);
    }

    @Test
    public void testSinglePurchaseFullSale_Loss() {
        // Given a purchase and a full sale at a lower price (loss scenario)
        LocalDateTime purchaseDate = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        double purchaseAmount = 1.0;
        double purchasePrice = 15000.0;
        
        LocalDateTime saleDate = LocalDateTime.now();
        double salePrice = 10000.0;
        
        // When we add the purchase and calculate tax for a full sale
        calculator.addPurchase(purchaseAmount, purchasePrice, purchaseDate);
        TaxResult result = calculator.calculateTax(purchaseAmount, salePrice, saleDate);
        
        // Then we should have the correct loss calculated (negative gain)
        double expectedGain = (salePrice - purchasePrice) * purchaseAmount; // (10000 - 15000) * 1 = -5000
        assertEquals(expectedGain, result.getTotalGain(), 0.001);
        assertEquals(expectedGain, result.getShortTermGain(), 0.001); // Should be short-term loss
        assertEquals(0.0, result.getLongTermGain(), 0.001); // No long-term gain/loss
        
        // And the remaining amount should be zero
        assertEquals(0.0, calculator.getTotalRemainingAmount(), 0.001);
    }

    @Test
    public void testSinglePurchasePartialSale() {
        // Given a purchase and a partial sale
        LocalDateTime purchaseDate = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        double purchaseAmount = 2.0;
        double purchasePrice = 10000.0;
        
        LocalDateTime saleDate = LocalDateTime.now();
        double saleAmount = 1.0; // Selling half
        double salePrice = 15000.0;
        
        // When we add the purchase and calculate tax for a partial sale
        calculator.addPurchase(purchaseAmount, purchasePrice, purchaseDate);
        TaxResult result = calculator.calculateTax(saleAmount, salePrice, saleDate);
        
        // Then we should have the correct gain calculated for the portion sold
        double expectedGain = (salePrice - purchasePrice) * saleAmount; // (15000 - 10000) * 1 = 5000
        assertEquals(expectedGain, result.getTotalGain(), 0.001);
        
        // And the remaining amount should be correct
        assertEquals(purchaseAmount - saleAmount, calculator.getTotalRemainingAmount(), 0.001);
    }

    @Test
    public void testMultiplePurchasesFIFOSale() {
        // Given multiple purchases at different times and prices
        LocalDateTime firstPurchaseDate = LocalDateTime.now().minus(400, ChronoUnit.DAYS); // Over 1 year old (long-term)
        double firstPurchaseAmount = 1.0;
        double firstPurchasePrice = 8000.0;
        
        LocalDateTime secondPurchaseDate = LocalDateTime.now().minus(30, ChronoUnit.DAYS); // Less than 1 year old (short-term)
        double secondPurchaseAmount = 1.0;
        double secondPurchasePrice = 12000.0;
        
        LocalDateTime saleDate = LocalDateTime.now();
        double saleAmount = 1.5; // Selling 1.5 BTC (all of the first purchase + half of the second)
        double salePrice = 15000.0;
        
        // When we add the purchases and calculate tax for a FIFO sale
        calculator.addPurchase(firstPurchaseAmount, firstPurchasePrice, firstPurchaseDate);
        calculator.addPurchase(secondPurchaseAmount, secondPurchasePrice, secondPurchaseDate);
        TaxResult result = calculator.calculateTax(saleAmount, salePrice, saleDate);
        
        // Then we should have the correct gains calculated with proper long-term/short-term split
        double longTermGain = (salePrice - firstPurchasePrice) * firstPurchaseAmount; // (15000 - 8000) * 1 = 7000
        double shortTermGain = (salePrice - secondPurchasePrice) * (saleAmount - firstPurchaseAmount); // (15000 - 12000) * 0.5 = 1500
        double totalGain = longTermGain + shortTermGain; // 7000 + 1500 = 8500
        
        assertEquals(totalGain, result.getTotalGain(), 0.001);
        assertEquals(shortTermGain, result.getShortTermGain(), 0.001);
        assertEquals(longTermGain, result.getLongTermGain(), 0.001);
        
        // And the remaining amount should be correct
        assertEquals(0.5, calculator.getTotalRemainingAmount(), 0.001); // 1 + 1 - 1.5 = 0.5
    }

    @Test
    public void testLotConsumptionAndRemoval() {
        // Given multiple purchases
        calculator.addPurchase(1.0, 10000.0, LocalDateTime.now().minus(30, ChronoUnit.DAYS));
        calculator.addPurchase(1.0, 12000.0, LocalDateTime.now().minus(20, ChronoUnit.DAYS));
        calculator.addPurchase(1.0, 14000.0, LocalDateTime.now().minus(10, ChronoUnit.DAYS));
        
        // When we sell exactly the amount of the first lot
        calculator.calculateTax(1.0, 15000.0, LocalDateTime.now());
        
        // Then the first lot should be fully consumed
        List<TaxLot> remainingLots = calculator.getLots();
        assertEquals(3, remainingLots.size()); // All lots still exist, but first one is consumed
        assertTrue(remainingLots.get(0).isFullyConsumed());
        assertFalse(remainingLots.get(1).isFullyConsumed());
        assertFalse(remainingLots.get(2).isFullyConsumed());
        
        // When we clean up consumed lots
        calculator.cleanupConsumedLots();
        
        // Then the consumed lot should be removed
        remainingLots = calculator.getLots();
        assertEquals(2, remainingLots.size());
        assertEquals(12000.0, remainingLots.get(0).getCostBasis()); // Second lot is now first
    }

    @Test
    public void testNoLotsAvailableForSale() {
        // When we try to calculate tax with no lots available
        TaxResult result = calculator.calculateTax(1.0, 10000.0, LocalDateTime.now());
        
        // Then we should get zeros
        assertEquals(0.0, result.getTotalGain(), 0.001);
        assertEquals(0.0, result.getShortTermGain(), 0.001);
        assertEquals(0.0, result.getLongTermGain(), 0.001);
    }

    @Test
    public void testSellingMoreThanAvailable() {
        // Given a purchase of 1.0 BTC
        calculator.addPurchase(1.0, 10000.0, LocalDateTime.now().minus(30, ChronoUnit.DAYS));
        
        // When we try to sell 2.0 BTC (more than available)
        TaxResult result = calculator.calculateTax(2.0, 15000.0, LocalDateTime.now());
        
        // Then we should get gain only for the available amount
        double expectedGain = (15000.0 - 10000.0) * 1.0; // Only 1.0 BTC is available
        assertEquals(expectedGain, result.getTotalGain(), 0.001);
        
        // And the lot should be fully consumed
        assertEquals(0.0, calculator.getTotalRemainingAmount(), 0.001);
    }

    @Test
    public void testLongTermVsShortTermClassification() {
        // Given one long-term purchase and one short-term purchase
        LocalDateTime longTermDate = LocalDateTime.now().minus(366, ChronoUnit.DAYS);
        LocalDateTime shortTermDate = LocalDateTime.now().minus(364, ChronoUnit.DAYS);
        
        calculator.addPurchase(1.0, 10000.0, longTermDate);
        calculator.addPurchase(1.0, 10000.0, shortTermDate);
        
        // When we sell 1.0 BTC (the long-term one due to FIFO)
        LocalDateTime saleDate = LocalDateTime.now();
        TaxResult result = calculator.calculateTax(1.0, 15000.0, saleDate);
        
        // Then it should be classified as long-term gain
        assertEquals(5000.0, result.getTotalGain(), 0.001);
        assertEquals(0.0, result.getShortTermGain(), 0.001);
        assertEquals(5000.0, result.getLongTermGain(), 0.001);
        
        // When we sell another 1.0 BTC (the short-term one)
        result = calculator.calculateTax(1.0, 15000.0, saleDate);
        
        // Then it should be classified as short-term gain
        assertEquals(5000.0, result.getTotalGain(), 0.001);
        assertEquals(5000.0, result.getShortTermGain(), 0.001);
        assertEquals(0.0, result.getLongTermGain(), 0.001);
    }

    @Test
    public void testMixedGainsAndLosses() {
        // Given purchases at different prices
        calculator.addPurchase(1.0, 10000.0, LocalDateTime.now().minus(400, ChronoUnit.DAYS)); // Long-term
        calculator.addPurchase(1.0, 20000.0, LocalDateTime.now().minus(30, ChronoUnit.DAYS));  // Short-term
        
        // When we sell both at a price in between their purchase prices
        LocalDateTime saleDate = LocalDateTime.now();
        TaxResult result = calculator.calculateTax(2.0, 15000.0, saleDate);
        
        // Then we should have a long-term gain and a short-term loss
        double longTermGain = (15000.0 - 10000.0) * 1.0;  // +5000 (long-term gain)
        double shortTermGain = (15000.0 - 20000.0) * 1.0; // -5000 (short-term loss)
        double totalGain = longTermGain + shortTermGain;  // 0 (net)
        
        assertEquals(totalGain, result.getTotalGain(), 0.001);
        assertEquals(shortTermGain, result.getShortTermGain(), 0.001);
        assertEquals(longTermGain, result.getLongTermGain(), 0.001);
    }
}