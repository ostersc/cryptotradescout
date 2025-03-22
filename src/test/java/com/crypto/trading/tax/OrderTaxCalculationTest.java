package com.crypto.trading.tax;

import com.crypto.trading.exchange.model.Order;
import com.crypto.trading.exchange.model.OrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for order tax calculations.
 * Tests the behavior of setting tax fields on Order objects.
 */
public class OrderTaxCalculationTest {

    private TaxCalculator taxCalculator;
    private List<Order> orders;
    private double taxRate = 0.15; // 15% tax rate
    private double feeRate = 0.002; // 0.2% fee rate

    @BeforeEach
    public void setUp() {
        taxCalculator = new TaxCalculator();
        orders = new ArrayList<>();
    }

    @Test
    public void testSingleBuyAndSellWithProfit() {
        // Given
        LocalDateTime buyTime = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        LocalDateTime sellTime = LocalDateTime.now();
        
        // Create a buy order
        double buyAmount = 1.0;
        double buyPrice = 10000.0;
        Order buyOrder = createOrder(OrderType.BUY, buyAmount, buyPrice, buyTime);
        orders.add(buyOrder);
        
        // Record the purchase in the tax calculator
        taxCalculator.addPurchase(buyAmount, buyPrice, buyTime);
        
        // Create a sell order with profit
        double sellAmount = 1.0;
        double sellPrice = 15000.0;
        Order sellOrder = createOrder(OrderType.SELL, sellAmount, sellPrice, sellTime);
        orders.add(sellOrder);
        
        // When calculating tax
        TaxResult result = taxCalculator.calculateTax(sellAmount, sellPrice, sellTime);
        
        // Then update the sell order with tax information
        updateOrderWithTaxInfo(sellOrder, result, taxRate);
        
        // Verify calculations
        double expectedGain = (sellPrice - buyPrice) * sellAmount; // 5000
        double expectedTax = expectedGain * taxRate; // 750
        
        assertEquals(expectedGain, sellOrder.getTaxableGain(), 0.001);
        assertEquals(expectedTax, sellOrder.getTax(), 0.001);
        assertEquals(0.0, sellOrder.getShortTermGain() + sellOrder.getLongTermGain() - sellOrder.getTaxableGain(), 0.001);
        assertTrue(sellOrder.getShortTermGain() > 0, "Should have short-term gain");
        assertEquals(0.0, sellOrder.getLongTermGain(), 0.001, "Should have no long-term gain");
    }

    @Test
    public void testSingleBuyAndSellWithLoss() {
        // Given
        LocalDateTime buyTime = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        LocalDateTime sellTime = LocalDateTime.now();
        
        // Create a buy order
        double buyAmount = 1.0;
        double buyPrice = 15000.0;
        Order buyOrder = createOrder(OrderType.BUY, buyAmount, buyPrice, buyTime);
        orders.add(buyOrder);
        
        // Record the purchase in the tax calculator
        taxCalculator.addPurchase(buyAmount, buyPrice, buyTime);
        
        // Create a sell order with loss
        double sellAmount = 1.0;
        double sellPrice = 10000.0;
        Order sellOrder = createOrder(OrderType.SELL, sellAmount, sellPrice, sellTime);
        orders.add(sellOrder);
        
        // When calculating tax
        TaxResult result = taxCalculator.calculateTax(sellAmount, sellPrice, sellTime);
        
        // Then update the sell order with tax information
        updateOrderWithTaxInfo(sellOrder, result, taxRate);
        
        // Verify calculations
        double expectedGain = (sellPrice - buyPrice) * sellAmount; // -5000 (loss)
        
        assertEquals(expectedGain, sellOrder.getTaxableGain(), 0.001);
        assertEquals(0.0, sellOrder.getTax(), 0.001, "Tax should be 0 for a loss");
        assertEquals(expectedGain, sellOrder.getShortTermGain(), 0.001);
        assertEquals(0.0, sellOrder.getLongTermGain(), 0.001);
    }

    @Test
    public void testMultipleBuysAndSingleSellWithMixedGainLoss() {
        // Given
        LocalDateTime firstBuyTime = LocalDateTime.now().minus(400, ChronoUnit.DAYS); // Long-term
        LocalDateTime secondBuyTime = LocalDateTime.now().minus(30, ChronoUnit.DAYS); // Short-term
        LocalDateTime sellTime = LocalDateTime.now();
        
        // Create first buy order (long-term)
        double firstBuyAmount = 1.0;
        double firstBuyPrice = 8000.0;
        Order firstBuyOrder = createOrder(OrderType.BUY, firstBuyAmount, firstBuyPrice, firstBuyTime);
        orders.add(firstBuyOrder);
        
        // Create second buy order (short-term)
        double secondBuyAmount = 1.0;
        double secondBuyPrice = 18000.0;
        Order secondBuyOrder = createOrder(OrderType.BUY, secondBuyAmount, secondBuyPrice, secondBuyTime);
        orders.add(secondBuyOrder);
        
        // Record purchases in the tax calculator
        taxCalculator.addPurchase(firstBuyAmount, firstBuyPrice, firstBuyTime);
        taxCalculator.addPurchase(secondBuyAmount, secondBuyPrice, secondBuyTime);
        
        // Create a sell order selling all 2.0 BTC
        double sellAmount = 2.0;
        double sellPrice = 15000.0;
        Order sellOrder = createOrder(OrderType.SELL, sellAmount, sellPrice, sellTime);
        orders.add(sellOrder);
        
        // When calculating tax
        TaxResult result = taxCalculator.calculateTax(sellAmount, sellPrice, sellTime);
        
        // Then update the sell order with tax information
        updateOrderWithTaxInfo(sellOrder, result, taxRate);
        
        // Verify calculations
        double longTermGain = (sellPrice - firstBuyPrice) * firstBuyAmount; // (15000 - 8000) * 1 = 7000
        double shortTermGain = (sellPrice - secondBuyPrice) * secondBuyAmount; // (15000 - 18000) * 1 = -3000
        double totalGain = longTermGain + shortTermGain; // 7000 - 3000 = 4000
        double expectedTax = Math.max(0, (longTermGain * taxRate)); // 7000 * 0.15 = 1050 (only tax on the gain portion)
        
        assertEquals(totalGain, sellOrder.getTaxableGain(), 0.001);
        assertEquals(expectedTax, sellOrder.getTax(), 0.001);
        assertEquals(shortTermGain, sellOrder.getShortTermGain(), 0.001);
        assertEquals(longTermGain, sellOrder.getLongTermGain(), 0.001);
    }

    @Test
    public void testPartialSale() {
        // Given
        LocalDateTime buyTime = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        LocalDateTime sellTime = LocalDateTime.now();
        
        // Create a buy order for 2.0 BTC
        double buyAmount = 2.0;
        double buyPrice = 10000.0;
        Order buyOrder = createOrder(OrderType.BUY, buyAmount, buyPrice, buyTime);
        orders.add(buyOrder);
        
        // Record the purchase in the tax calculator
        taxCalculator.addPurchase(buyAmount, buyPrice, buyTime);
        
        // Create a sell order for 1.0 BTC (half)
        double sellAmount = 1.0;
        double sellPrice = 15000.0;
        Order sellOrder = createOrder(OrderType.SELL, sellAmount, sellPrice, sellTime);
        orders.add(sellOrder);
        
        // When calculating tax
        TaxResult result = taxCalculator.calculateTax(sellAmount, sellPrice, sellTime);
        
        // Then update the sell order with tax information
        updateOrderWithTaxInfo(sellOrder, result, taxRate);
        
        // Verify calculations
        double expectedGain = (sellPrice - buyPrice) * sellAmount; // (15000 - 10000) * 1 = 5000
        double expectedTax = expectedGain * taxRate; // 5000 * 0.15 = 750
        
        assertEquals(expectedGain, sellOrder.getTaxableGain(), 0.001);
        assertEquals(expectedTax, sellOrder.getTax(), 0.001);
        
        // Ensure remaining amount is correct in the tax calculator
        assertEquals(buyAmount - sellAmount, taxCalculator.getTotalRemainingAmount(), 0.001);
    }

    // Helper method to create an order
    private Order createOrder(OrderType type, double amount, double price, LocalDateTime createdAt) {
        Order order = new Order();
        order.setType(type);
        order.setAmount(amount);
        order.setPrice(price);
        order.setCreatedAt(createdAt);
        order.setFeeRate(feeRate);
        order.setTaxRate(taxRate);
        order.setFee(amount * price * feeRate); // Fee is calculated as a percentage of the order value
        return order;
    }
    
    // Helper method to update an order with tax information
    private void updateOrderWithTaxInfo(Order order, TaxResult taxResult, double taxRate) {
        order.setTaxableGain(taxResult.getTotalGain());
        order.setShortTermGain(taxResult.getShortTermGain());
        order.setLongTermGain(taxResult.getLongTermGain());
        
        // Only apply tax on positive gains
        double taxableAmount = Math.max(0, taxResult.getTotalGain());
        order.setTax(taxableAmount * taxRate);
    }
}