package com.crypto.trading.algorithm;

import com.crypto.trading.exchange.model.MarketData;
import com.crypto.trading.exchange.model.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SimpleMovingAverageAlgorithmTest {

    private SimpleMovingAverageAlgorithm algorithm;

    @BeforeEach
    void setUp() {
        algorithm = new SimpleMovingAverageAlgorithm();
        // Initialize with default test parameters
        algorithm.initialize(Map.of(
                "shortPeriod", 5,
                "longPeriod", 10,
                "tradeAmount", 0.1
        ));
    }

    @Test
    void validateParameters_withValidParameters_shouldReturnTrue() {
        // Arrange
        Map<String, Object> params = Map.of(
                "shortPeriod", 5,
                "longPeriod", 10,
                "tradeAmount", 0.1
        );
        
        // Act
        boolean result = algorithm.validateParameters(params);
        
        // Assert
        assertTrue(result);
    }
    
    @Test
    void validateParameters_withInvalidShortPeriod_shouldReturnFalse() {
        // Arrange
        Map<String, Object> params = Map.of(
                "shortPeriod", -1,
                "longPeriod", 10,
                "tradeAmount", 0.1
        );
        
        // Act
        boolean result = algorithm.validateParameters(params);
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void validateParameters_withShortPeriodGreaterThanLongPeriod_shouldReturnFalse() {
        // Arrange
        Map<String, Object> params = Map.of(
                "shortPeriod", 15,
                "longPeriod", 10,
                "tradeAmount", 0.1
        );
        
        // Act
        boolean result = algorithm.validateParameters(params);
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void processMarketData_withNotEnoughData_shouldReturnEmpty() {
        // Arrange
        MarketData marketData = createMarketData(40000.0);
        
        // Act
        Mono<Order> result = algorithm.processMarketData(marketData);
        
        // Assert
        StepVerifier.create(result)
                .expectComplete()
                .verify();
    }
    
    @Test
    void processMarketData_withCrossoverEvent_shouldGenerateOrder() {
        // Arrange - Add enough data points to trigger a calculation
        List<MarketData> initialData = createRisingMarketData(11);
        
        // Feed initial data to build history
        for (int i = 0; i < 10; i++) {
            algorithm.processMarketData(initialData.get(i)).subscribe();
        }
        
        // Act - Process the data point that triggers a crossover
        Mono<Order> result = algorithm.processMarketData(initialData.get(10));
        
        // Assert - Should generate a buy order
        StepVerifier.create(result)
                .expectNextMatches(order -> 
                    order.getTradingPair().equals("BTC-USD") &&
                    order.getAmount() == 0.1 &&
                    order.getPrice() == 40100.0
                )
                .verifyComplete();
    }
    
    @Test
    void backtest_shouldGenerateOrders() {
        // Arrange
        List<MarketData> historicalData = createAlternatingMarketData(100);
        double initialCapital = 10000.0;
        
        // Act
        List<Order> orders = algorithm.backtest(historicalData, initialCapital);
        
        // Assert
        assertFalse(orders.isEmpty(), "Should generate at least one order");
        
        // We expect orders to be generated at crossover points
        // The specific number depends on the test data pattern
    }
    
    // Helper methods to create test data
    
    private MarketData createMarketData(double price) {
        return new MarketData(
                "BTC-USD",
                price - 50,
                price + 50,
                price,
                1.0,
                LocalDateTime.now(),
                "TestExchange"
        );
    }
    
    private List<MarketData> createRisingMarketData(int count) {
        List<MarketData> data = new ArrayList<>();
        LocalDateTime time = LocalDateTime.now().minusHours(count);
        
        for (int i = 0; i < count; i++) {
            // Create a rising price trend
            double basePrice = 40000.0 + (i * 100.0);
            
            data.add(new MarketData(
                    "BTC-USD",
                    basePrice - 50,
                    basePrice + 50,
                    basePrice,
                    1.0,
                    time.plusHours(i),
                    "TestExchange"
            ));
        }
        
        return data;
    }
    
    private List<MarketData> createAlternatingMarketData(int count) {
        List<MarketData> data = new ArrayList<>();
        LocalDateTime time = LocalDateTime.now().minusHours(count);
        boolean rising = true;
        double basePrice = 40000.0;
        
        for (int i = 0; i < count; i++) {
            // Switch direction every 15 data points
            if (i % 15 == 0) {
                rising = !rising;
            }
            
            // Calculate price movement
            basePrice += rising ? 100 : -100;
            
            data.add(new MarketData(
                    "BTC-USD",
                    basePrice - 50,
                    basePrice + 50,
                    basePrice,
                    1.0,
                    time.plusHours(i),
                    "TestExchange"
            ));
        }
        
        return data;
    }
}
