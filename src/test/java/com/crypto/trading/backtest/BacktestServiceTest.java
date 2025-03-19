package com.crypto.trading.backtest;

import com.crypto.trading.algorithm.TradingAlgorithm;
import com.crypto.trading.exchange.ExchangeService;
import com.crypto.trading.exchange.model.MarketData;
import com.crypto.trading.exchange.model.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BacktestServiceTest {

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
    void runBacktest_shouldReturnBacktestResult() {
        // Arrange
        LocalDateTime startTime = LocalDateTime.now().minusDays(30);
        LocalDateTime endTime = LocalDateTime.now();
        String tradingPair = "BTC-USD";
        double initialCapital = 10000.0;
        Map<String, Object> algorithmParams = Map.of("param1", "value1");
        
        List<MarketData> historicalData = createSampleMarketData(tradingPair, startTime, endTime);
        List<Order> generatedOrders = createSampleOrders(tradingPair);
        
        when(exchangeService.getHistoricalMarketData(eq(tradingPair), any(), any()))
                .thenReturn(Mono.just(historicalData));
        
        when(tradingAlgorithm.backtest(eq(historicalData), eq(initialCapital)))
                .thenReturn(generatedOrders);
        
        when(tradingAlgorithm.getId()).thenReturn("test-algorithm");
        
        // Act
        Mono<BacktestResult> result = backtestService.runBacktest(
                tradingAlgorithm,
                "Kraken",
                tradingPair,
                startTime,
                endTime,
                initialCapital,
                algorithmParams
        );
        
        // Assert
        StepVerifier.create(result)
                .expectNextMatches(backtestResult -> {
                    // Verify the backtestResult has the expected values
                    return backtestResult.getAlgorithmId().equals("test-algorithm")
                            && backtestResult.getExchange().equals("Kraken")
                            && backtestResult.getTradingPair().equals(tradingPair)
                            && backtestResult.getStartTime().equals(startTime)
                            && backtestResult.getEndTime().equals(endTime)
                            && backtestResult.getInitialCapital() == initialCapital
                            && backtestResult.getGeneratedOrders().size() == generatedOrders.size();
                })
                .verifyComplete();
        
        // Verify interactions
        verify(tradingAlgorithm).initialize(algorithmParams);
        verify(exchangeService).getHistoricalMarketData(tradingPair, startTime, endTime);
        verify(tradingAlgorithm).backtest(historicalData, initialCapital);
    }
    
    @Test
    void runBacktest_withNoHistoricalData_shouldReturnError() {
        // Arrange
        LocalDateTime startTime = LocalDateTime.now().minusDays(30);
        LocalDateTime endTime = LocalDateTime.now();
        String tradingPair = "BTC-USD";
        double initialCapital = 10000.0;
        
        when(exchangeService.getHistoricalMarketData(eq(tradingPair), any(), any()))
                .thenReturn(Mono.just(new ArrayList<>()));
        
        // Act
        Mono<BacktestResult> result = backtestService.runBacktest(
                tradingAlgorithm,
                "Kraken",
                tradingPair,
                startTime,
                endTime,
                initialCapital,
                Map.of()
        );
        
        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof IllegalStateException &&
                        e.getMessage().contains("No historical data available"))
                .verify();
    }
    
    @Test
    void runBacktest_withInvalidExchange_shouldReturnError() {
        // Arrange
        LocalDateTime startTime = LocalDateTime.now().minusDays(30);
        LocalDateTime endTime = LocalDateTime.now();
        String tradingPair = "BTC-USD";
        double initialCapital = 10000.0;
        
        // Act
        Mono<BacktestResult> result = backtestService.runBacktest(
                tradingAlgorithm,
                "NonExistentExchange",
                tradingPair,
                startTime,
                endTime,
                initialCapital,
                Map.of()
        );
        
        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().contains("Exchange not found"))
                .verify();
    }
    
    // Helper methods to create test data
    
    private List<MarketData> createSampleMarketData(String tradingPair, LocalDateTime startTime, LocalDateTime endTime) {
        List<MarketData> data = new ArrayList<>();
        LocalDateTime current = startTime;
        
        while (current.isBefore(endTime)) {
            data.add(new MarketData(
                    tradingPair,
                    40000.0, // bid
                    40100.0, // ask
                    40050.0, // last
                    10.0,    // volume
                    current,
                    "Kraken"
            ));
            
            current = current.plusHours(1);
        }
        
        return data;
    }
    
    private List<Order> createSampleOrders(String tradingPair) {
        List<Order> orders = new ArrayList<>();
        
        orders.add(new Order(
                "order1",
                tradingPair,
                com.crypto.trading.exchange.model.OrderType.MARKET,
                0.1,
                40000.0,
                LocalDateTime.now().minusDays(25),
                "FILLED",
                "Kraken"
        ));
        
        orders.add(new Order(
                "order2",
                tradingPair,
                com.crypto.trading.exchange.model.OrderType.MARKET,
                0.1,
                41000.0,
                LocalDateTime.now().minusDays(20),
                "FILLED",
                "Kraken"
        ));
        
        return orders;
    }
}
