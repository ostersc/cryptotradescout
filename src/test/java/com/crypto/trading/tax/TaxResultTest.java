package com.crypto.trading.tax;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for the TaxResult class.
 */
public class TaxResultTest {

    @Test
    public void testTaxResultCreation() {
        // Given
        double totalGain = 5000.0;
        double shortTermGain = 2000.0;
        double longTermGain = 3000.0;
        
        // When
        TaxResult result = new TaxResult(totalGain, shortTermGain, longTermGain);
        
        // Then
        assertEquals(totalGain, result.getTotalGain(), 0.001);
        assertEquals(shortTermGain, result.getShortTermGain(), 0.001);
        assertEquals(longTermGain, result.getLongTermGain(), 0.001);
    }
    
    @Test
    public void testTaxResultWithZeroValues() {
        // When
        TaxResult result = new TaxResult(0.0, 0.0, 0.0);
        
        // Then
        assertEquals(0.0, result.getTotalGain(), 0.001);
        assertEquals(0.0, result.getShortTermGain(), 0.001);
        assertEquals(0.0, result.getLongTermGain(), 0.001);
    }
    
    @Test
    public void testTaxResultWithNegativeValues() {
        // When
        TaxResult result = new TaxResult(-5000.0, -2000.0, -3000.0);
        
        // Then
        assertEquals(-5000.0, result.getTotalGain(), 0.001);
        assertEquals(-2000.0, result.getShortTermGain(), 0.001);
        assertEquals(-3000.0, result.getLongTermGain(), 0.001);
    }
    
    @Test
    public void testTaxResultWithMixedValues() {
        // When
        TaxResult result = new TaxResult(0.0, -2000.0, 2000.0);
        
        // Then
        assertEquals(0.0, result.getTotalGain(), 0.001);
        assertEquals(-2000.0, result.getShortTermGain(), 0.001);
        assertEquals(2000.0, result.getLongTermGain(), 0.001);
    }
    
    @Test
    public void testToString() {
        // Given
        TaxResult result = new TaxResult(5000.0, 2000.0, 3000.0);
        
        // When
        String stringRepresentation = result.toString();
        
        // Then
        assertTrue(stringRepresentation.contains("totalGain=5000.0"));
        assertTrue(stringRepresentation.contains("shortTermGain=2000.0"));
        assertTrue(stringRepresentation.contains("longTermGain=3000.0"));
    }
}