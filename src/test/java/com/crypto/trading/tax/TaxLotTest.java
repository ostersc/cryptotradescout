package com.crypto.trading.tax;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for the TaxLot class.
 */
public class TaxLotTest {

    @Test
    public void testTaxLotCreation() {
        // Given
        double amount = 1.0;
        double costBasis = 10000.0;
        LocalDateTime purchaseDate = LocalDateTime.now();
        
        // When
        TaxLot lot = new TaxLot(amount, costBasis, purchaseDate);
        
        // Then
        assertEquals(amount, lot.getAmount(), 0.001);
        assertEquals(costBasis, lot.getCostBasis(), 0.001);
        assertEquals(purchaseDate, lot.getPurchaseDate());
        assertEquals(amount, lot.getRemainingAmount(), 0.001);
        assertFalse(lot.isFullyConsumed());
    }
    
    @Test
    public void testSetRemainingAmount() {
        // Given
        TaxLot lot = new TaxLot(1.0, 10000.0, LocalDateTime.now());
        
        // When
        lot.setRemainingAmount(0.5);
        
        // Then
        assertEquals(0.5, lot.getRemainingAmount(), 0.001);
        assertFalse(lot.isFullyConsumed());
        
        // When
        lot.setRemainingAmount(0.0);
        
        // Then
        assertEquals(0.0, lot.getRemainingAmount(), 0.001);
        assertTrue(lot.isFullyConsumed());
    }
    
    @Test
    public void testIsFullyConsumed() {
        // Given
        TaxLot lot = new TaxLot(1.0, 10000.0, LocalDateTime.now());
        
        // When/Then
        assertFalse(lot.isFullyConsumed()); // Initially not consumed
        
        // When
        lot.setRemainingAmount(0.0);
        
        // Then
        assertTrue(lot.isFullyConsumed());
        
        // Test floating point precision handling
        lot.setRemainingAmount(0.0000001);
        assertTrue(lot.isFullyConsumed()); // Should still be considered consumed due to threshold
    }
    
    @Test
    public void testIsLongTerm() {
        // Given
        LocalDateTime purchaseDate = LocalDateTime.now().minus(366, ChronoUnit.DAYS);
        TaxLot lot = new TaxLot(1.0, 10000.0, purchaseDate);
        
        // When/Then
        assertTrue(lot.isLongTerm(LocalDateTime.now())); // Over 1 year should be long-term
        
        // Given
        LocalDateTime shortTermPurchaseDate = LocalDateTime.now().minus(364, ChronoUnit.DAYS);
        TaxLot shortTermLot = new TaxLot(1.0, 10000.0, shortTermPurchaseDate);
        
        // When/Then
        assertFalse(shortTermLot.isLongTerm(LocalDateTime.now())); // Less than 1 year should be short-term
        
        // Test exactly 365 days
        LocalDateTime exactlyOneYearPurchaseDate = LocalDateTime.now().minus(365, ChronoUnit.DAYS);
        TaxLot exactlyOneYearLot = new TaxLot(1.0, 10000.0, exactlyOneYearPurchaseDate);
        
        // Should be long-term at exactly 365 days
        assertTrue(exactlyOneYearLot.isLongTerm(LocalDateTime.now()));
    }
    
    @Test
    public void testToString() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        TaxLot lot = new TaxLot(1.0, 10000.0, now);
        
        // When
        String stringRepresentation = lot.toString();
        
        // Then
        assertTrue(stringRepresentation.contains("amount=1.0"));
        assertTrue(stringRepresentation.contains("costBasis=10000.0"));
        assertTrue(stringRepresentation.contains("remainingAmount=1.0"));
        assertTrue(stringRepresentation.contains("purchaseDate=" + now.toString()));
    }
}