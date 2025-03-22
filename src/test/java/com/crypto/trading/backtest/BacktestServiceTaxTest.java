package com.crypto.trading.backtest;

import com.crypto.trading.algorithm.TradingAlgorithm;
import com.crypto.trading.exchange.ExchangeService;
import com.crypto.trading.exchange.model.MarketData;
import com.crypto.trading.exchange.model.Order;
import com.crypto.trading.exchange.model.OrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests tax calculation in the BacktestService.
 */
public class BacktestServiceTaxTest {

    @Mock
    private ExchangeService exchangeService;

    @Mock
    private TradingAlgorithm tradingAlgorithm;

    private BacktestService backtestService;
    private Map<String, ExchangeService> exchangeServices;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        exchangeServices = new HashMap<>();
        exchangeServices.put("kraken", exchangeService);
        backtestService = new BacktestService(exchangeServices);

        // Configure mocks
        when(exchangeService.getExchangeName()).thenReturn("Kraken");
    }

    @Test
    void calculatePerformanceMetrics_withProfit_shouldCalculateTaxesCorrectly() {
        // Create test data
        List<MarketData> historicalData = createSampleMarketData("BTC-USD", 10);
        List<Order> orders = createProfitableTradeOrders();

        // Use reflection to access private method
        PerformanceMetrics metrics = null;
        try {
            java.lang.reflect.Method method = BacktestService.class.getDeclaredMethod(
                    "calculatePerformanceMetrics", List.class, List.class, double.class);
            method.setAccessible(true);
            metrics = (PerformanceMetrics) method.invoke(backtestService, historicalData, orders, 10000.0);
        } catch (Exception e) {
            fail("Failed to invoke calculatePerformanceMetrics via reflection: " + e.getMessage());
        }

        assertNotNull(metrics);
        
        // Verify tax calculations
        double expectedTotalFees = orders.stream().mapToDouble(Order::getFee).sum();
        double expectedTotalTaxes = orders.stream().mapToDouble(Order::getTax).sum();
        
        // Check that tax is calculated only on gains, not on losses
        double totalGain = orders.stream()
                .filter(o -> o.getType() == OrderType.SELL)
                .mapToDouble(Order::getTaxableGain)
                .sum();
                
        assertTrue(expectedTotalTaxes > 0, "Total taxes should be positive when there's profit");
        assertEquals(expectedTotalFees, metrics.getTotalFees(), 0.01, "Total fees should match sum of individual order fees");
        assertEquals(expectedTotalTaxes, metrics.getTotalTaxes(), 0.01, "Total taxes should match sum of individual order taxes");
        
        // Verify that tax is only applied to the gain, not the full amount
        for (Order order : orders) {
            if (order.getType() == OrderType.SELL && order.getTaxableGain() > 0) {
                assertEquals(order.getTaxableGain() * order.getTaxRate(), order.getTax(), 0.01,
                    "Tax should be calculated as taxableGain * taxRate for profitable sells");
            }
        }
    }

    @Test
    void calculatePerformanceMetrics_withLoss_shouldNotApplyTax() {
        // Create test data
        List<MarketData> historicalData = createSampleMarketData("BTC-USD", 10);
        List<Order> orders = createLossTradeOrders();

        // Use reflection to access private method
        PerformanceMetrics metrics = null;
        try {
            java.lang.reflect.Method method = BacktestService.class.getDeclaredMethod(
                    "calculatePerformanceMetrics", List.class, List.class, double.class);
            method.setAccessible(true);
            metrics = (PerformanceMetrics) method.invoke(backtestService, historicalData, orders, 10000.0);
        } catch (Exception e) {
            fail("Failed to invoke calculatePerformanceMetrics via reflection: " + e.getMessage());
        }

        assertNotNull(metrics);
        
        // Verify tax and fee calculations
        double expectedTotalFees = orders.stream().mapToDouble(Order::getFee).sum();
        
        // For losing trades, tax should be zero
        for (Order order : orders) {
            if (order.getType() == OrderType.SELL && order.getTaxableGain() < 0) {
                assertEquals(0.0, order.getTax(), 0.01, 
                    "Tax should be zero for losing sells");
            }
        }
        
        assertEquals(expectedTotalFees, metrics.getTotalFees(), 0.01, 
            "Total fees should match sum of individual order fees");
    }

    @Test
    void calculatePerformanceMetrics_withMixedResults_shouldApplyTaxOnlyToNetPositiveGains() {
        // Create test data
        List<MarketData> historicalData = createSampleMarketData("BTC-USD", 10);
        List<Order> orders = createMixedResultTradeOrders();

        // Use reflection to access private method
        PerformanceMetrics metrics = null;
        try {
            java.lang.reflect.Method method = BacktestService.class.getDeclaredMethod(
                    "calculatePerformanceMetrics", List.class, List.class, double.class);
            method.setAccessible(true);
            metrics = (PerformanceMetrics) method.invoke(backtestService, historicalData, orders, 10000.0);
        } catch (Exception e) {
            fail("Failed to invoke calculatePerformanceMetrics via reflection: " + e.getMessage());
        }

        assertNotNull(metrics);
        
        // Calculate net taxable gain
        double netGain = orders.stream()
                .filter(o -> o.getType() == OrderType.SELL)
                .mapToDouble(Order::getTaxableGain)
                .sum();
                
        double expectedTotalTax = Math.max(0, netGain) * 0.15; // Using 15% tax rate
        
        assertEquals(expectedTotalTax, metrics.getTotalTaxes(), 0.01, 
            "Total taxes should be calculated on net positive gains only");
    }

    // Helper methods to create test data
    
    private List<MarketData> createSampleMarketData(String tradingPair, int dataPoints) {
        List<MarketData> data = new ArrayList<>();
        LocalDateTime current = LocalDateTime.now().minusDays(dataPoints);
        
        for (int i = 0; i < dataPoints; i++) {
            // Create some price variation
            double price = 40000.0 + (i * 100); 
            
            data.add(new MarketData(
                    tradingPair,
                    price - 50, // bid
                    price + 50, // ask
                    price,      // last
                    10.0,       // volume
                    current.plusDays(i),
                    "Kraken"
            ));
        }
        
        return data;
    }
    
    private List<Order> createProfitableTradeOrders() {
        List<Order> orders = new ArrayList<>();
        String tradingPair = "BTC-USD";
        double taxRate = 0.15; // 15% tax rate
        double feeRate = 0.002; // 0.2% fee rate
        
        // Buy order - day 0
        Order buyOrder = new Order(
                "buy1",
                tradingPair,
                OrderType.BUY,
                1.0,
                40000.0,
                LocalDateTime.now().minusDays(10),
                "FILLED",
                "Kraken"
        );
        buyOrder.setFeeRate(feeRate);
        buyOrder.setTaxRate(taxRate);
        orders.add(buyOrder);
        
        // Sell order with profit - day 5
        Order sellOrder = new Order(
                "sell1",
                tradingPair,
                OrderType.SELL,
                1.0,
                45000.0,
                LocalDateTime.now().minusDays(5),
                "FILLED",
                "Kraken"
        );
        sellOrder.setFeeRate(feeRate);
        sellOrder.setTaxRate(taxRate);
        sellOrder.setTaxableGain(5000.0); // Manually set gain for test
        orders.add(sellOrder);
        
        return orders;
    }
    
    private List<Order> createLossTradeOrders() {
        List<Order> orders = new ArrayList<>();
        String tradingPair = "BTC-USD";
        double taxRate = 0.15; // 15% tax rate
        double feeRate = 0.002; // 0.2% fee rate
        
        // Buy order - day 0
        Order buyOrder = new Order(
                "buy1",
                tradingPair,
                OrderType.BUY,
                1.0,
                45000.0,
                LocalDateTime.now().minusDays(10),
                "FILLED",
                "Kraken"
        );
        buyOrder.setFeeRate(feeRate);
        buyOrder.setTaxRate(taxRate);
        orders.add(buyOrder);
        
        // Sell order with loss - day 5
        Order sellOrder = new Order(
                "sell1",
                tradingPair,
                OrderType.SELL,
                1.0,
                40000.0,
                LocalDateTime.now().minusDays(5),
                "FILLED",
                "Kraken"
        );
        sellOrder.setFeeRate(feeRate);
        sellOrder.setTaxRate(taxRate);
        sellOrder.setTaxableGain(-5000.0); // Manually set loss for test
        orders.add(sellOrder);
        
        return orders;
    }
    
    private List<Order> createMixedResultTradeOrders() {
        List<Order> orders = new ArrayList<>();
        String tradingPair = "BTC-USD";
        double taxRate = 0.15; // 15% tax rate
        double feeRate = 0.002; // 0.2% fee rate
        
        // First buy - day 0
        Order buyOrder1 = new Order(
                "buy1",
                tradingPair,
                OrderType.BUY,
                1.0,
                40000.0,
                LocalDateTime.now().minusDays(30),
                "FILLED",
                "Kraken"
        );
        buyOrder1.setFeeRate(feeRate);
        buyOrder1.setTaxRate(taxRate);
        orders.add(buyOrder1);
        
        // First sell with profit - day 10
        Order sellOrder1 = new Order(
                "sell1",
                tradingPair,
                OrderType.SELL,
                1.0,
                50000.0,
                LocalDateTime.now().minusDays(20),
                "FILLED",
                "Kraken"
        );
        sellOrder1.setFeeRate(feeRate);
        sellOrder1.setTaxRate(taxRate);
        sellOrder1.setTaxableGain(10000.0); // Profit of $10,000
        orders.add(sellOrder1);
        
        // Second buy - day 15
        Order buyOrder2 = new Order(
                "buy2",
                tradingPair,
                OrderType.BUY,
                1.0,
                52000.0,
                LocalDateTime.now().minusDays(15),
                "FILLED",
                "Kraken"
        );
        buyOrder2.setFeeRate(feeRate);
        buyOrder2.setTaxRate(taxRate);
        orders.add(buyOrder2);
        
        // Second sell with loss - day 20
        Order sellOrder2 = new Order(
                "sell2",
                tradingPair,
                OrderType.SELL,
                1.0,
                48000.0,
                LocalDateTime.now().minusDays(10),
                "FILLED",
                "Kraken"
        );
        sellOrder2.setFeeRate(feeRate);
        sellOrder2.setTaxRate(taxRate);
        sellOrder2.setTaxableGain(-4000.0); // Loss of $4,000
        orders.add(sellOrder2);
        
        return orders;
    }
}