package com.crypto.trading.backtest;

import com.crypto.trading.algorithm.TradingAlgorithm;
import com.crypto.trading.exchange.ExchangeService;
import com.crypto.trading.exchange.model.MarketData;
import com.crypto.trading.exchange.model.Order;
import com.crypto.trading.exchange.model.OrderType;
import com.crypto.trading.tax.TaxCalculator;
import com.crypto.trading.tax.TaxResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for backtesting trading algorithms against historical market data.
 */
@Service
public class BacktestService {
    private static final Logger logger = LoggerFactory.getLogger(BacktestService.class);

    private final Map<String, ExchangeService> exchangeServices;

    /**
     * Constructor that accepts all available exchange services.
     * 
     * @param exchangeServices the map of available exchange services
     */
    public BacktestService(Map<String, ExchangeService> exchangeServices) {
        this.exchangeServices = exchangeServices;
    }
    
    /**
     * Get the exchange services map.
     * 
     * @return the map of available exchange services
     */
    public Map<String, ExchangeService> getExchangeServices() {
        return exchangeServices;
    }

    /**
     * Run a backtest for a trading algorithm against historical data.
     * 
     * @param algorithm the algorithm to test
     * @param exchange the name of the exchange to get data from
     * @param tradingPair the trading pair to test with
     * @param startTime the start time for historical data
     * @param endTime the end time for historical data
     * @param initialCapital the initial capital to simulate with
     * @param algorithmParams the parameters for the algorithm
     * @return a Mono containing the backtest results
     */
    public Mono<BacktestResult> runBacktest(
            TradingAlgorithm algorithm,
            String exchange,
            String tradingPair,
            LocalDateTime startTime,
            LocalDateTime endTime,
            double initialCapital,
            Map<String, Object> algorithmParams) {

        logger.info("Starting backtest for algorithm {} on {} {}, from {} to {} with initial capital {}",
                algorithm.getId(), exchange, tradingPair, startTime, endTime, initialCapital);

        // Find the exchange service
        ExchangeService exchangeService = null;
        for (ExchangeService service : exchangeServices.values()) {
            if (service.getExchangeName().equalsIgnoreCase(exchange)) {
                exchangeService = service;
                break;
            }
        }

        if (exchangeService == null) {
            return Mono.error(new IllegalArgumentException("Exchange not found: " + exchange));
        }

        // Initialize the algorithm with the provided parameters
        algorithm.initialize(algorithmParams);

        // Fetch historical market data
        return exchangeService.getHistoricalMarketData(tradingPair, startTime, endTime)
                .doOnNext(data -> logger.info("Received historical data from exchange with {} data points", 
                                            data != null ? data.size() : 0))
                .flatMap(historicalData -> {
                    if (historicalData == null || historicalData.isEmpty()) {
                        logger.error("No historical data available for the specified period. Cannot proceed with backtest.");
                        return Mono.error(new RuntimeException("No historical data available for " + 
                                          tradingPair + " on " + exchange + " between " + 
                                          startTime + " and " + endTime + ". Please try a different time range."));
                    }

                    long startTimeMillis = System.currentTimeMillis();

                    // Run the backtest
                    List<Order> generatedOrders = algorithm.backtest(historicalData, initialCapital);

                    long endTimeMillis = System.currentTimeMillis();
                    long executionTimeMs = endTimeMillis - startTimeMillis;

                    // Calculate performance metrics
                    PerformanceMetrics metrics = calculatePerformanceMetrics(
                            historicalData, generatedOrders, initialCapital);

                    BacktestResult result = new BacktestResult(
                            algorithm.getId(),
                            exchange,
                            tradingPair,
                            startTime,
                            endTime,
                            initialCapital,
                            generatedOrders,
                            executionTimeMs,
                            metrics
                    );

                    logger.info("Backtest completed in {}ms. Return: {}%, Profit: {}, Max Drawdown: {}%",
                            executionTimeMs, 
                            String.format("%.2f", metrics.getTotalReturnPercentage()),
                            String.format("%.2f", metrics.getTotalProfit()),
                            String.format("%.2f", metrics.getMaxDrawdownPercentage()));

                    return Mono.just(result);
                })
                .onErrorResume(e -> {
                    logger.error("Error during backtest: {}", e.getMessage());
                    return Mono.error(new RuntimeException("Failed to complete backtest: " + e.getMessage()));
                });
    }

    /**
     * Calculate performance metrics for a backtest.
     * 
     * @param historicalData the historical market data used in the backtest
     * @param orders the orders generated during the backtest
     * @param initialCapital the initial capital used in the backtest
     * @return the calculated performance metrics
     */
    private PerformanceMetrics calculatePerformanceMetrics(
            List<MarketData> historicalData, 
            List<Order> orders, 
            double initialCapital) {

        if (historicalData.isEmpty() || orders.isEmpty()) {
            return new PerformanceMetrics();
        }

        // Sort orders by timestamp
        orders.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));

        // Initialize tracking variables
        double currentCapital = initialCapital;
        double currentCryptoHoldings = 0.0;
        double highWaterMark = initialCapital;
        double maxDrawdown = 0.0;

        List<Double> portfolioValues = new ArrayList<>();
        List<Double> returns = new ArrayList<>();

        TaxCalculator taxCalculator = new TaxCalculator();

        // Calculate values at each trade
        for (Order order : orders) {
            double orderValue = order.getAmount() * order.getPrice();
            
            // Ensure the order has a status set to avoid NPE - default to "FILLED" if null 
            String orderStatus = order.getStatus() != null ? order.getStatus() : "FILLED";

            if (orderStatus.equals("FILLED")) {
                if (isMatchingPair(order.getTradingPair(), historicalData.get(0).getTradingPair())) {
                    // Adjust for transaction fees (approximately 0.1-0.25% per trade)
                    double fee = orderValue * 0.002; // 0.2% fee

                    TaxCalculator tc = new TaxCalculator();
                    if (order.getType().toString().contains("BUY")) {
                        // Buying crypto
                        currentCapital -= (orderValue + fee);
                        currentCryptoHoldings += order.getAmount();
                        tc.addPurchase(order.getAmount(), order.getPrice(), order.getCreatedAt());
                    } else {
                        // Selling crypto
                        TaxResult taxResult = tc.calculateTax(order.getAmount(), order.getPrice(), order.getCreatedAt());
                        currentCapital += (orderValue - fee);
                        currentCryptoHoldings -= order.getAmount();
                        order.setTaxableGain(taxResult.getTotalGain());
                        order.setShortTermGain(taxResult.getShortTermGain());
                        order.setLongTermGain(taxResult.getLongTermGain());
                    }

                    // Calculate current portfolio value
                    double portfolioValue = currentCapital;
                    if (currentCryptoHoldings > 0) {
                        // Add value of crypto holdings at current price
                        portfolioValue += currentCryptoHoldings * order.getPrice();
                    }

                    // Track portfolio value history
                    portfolioValues.add(portfolioValue);

                    // Calculate return since last value
                    if (portfolioValues.size() > 1) {
                        double previousValue = portfolioValues.get(portfolioValues.size() - 2);
                        double returnRate = (portfolioValue / previousValue) - 1;
                        returns.add(returnRate);
                    }

                    // Update high water mark and drawdown
                    if (portfolioValue > highWaterMark) {
                        highWaterMark = portfolioValue;
                    } else {
                        double currentDrawdown = (highWaterMark - portfolioValue) / highWaterMark * 100;
                        maxDrawdown = Math.max(maxDrawdown, currentDrawdown);
                    }
                }
            }
        }

        // Calculate final metrics
        double finalValue = currentCapital;
        if (currentCryptoHoldings > 0) {
            // Add value of remaining crypto holdings at the last price in the historical data
            double lastPrice = historicalData.get(historicalData.size() - 1).getLastPrice();
            finalValue += currentCryptoHoldings * lastPrice;
        }

        double totalProfit = finalValue - initialCapital;
        double totalReturnPercentage = (finalValue / initialCapital - 1) * 100;

        // Calculate average return and volatility
        double averageReturn = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        double volatility = 0;
        if (returns.size() > 1) {
            double sumSquaredDiffs = returns.stream()
                    .mapToDouble(r -> Math.pow(r - averageReturn, 2))
                    .sum();
            volatility = Math.sqrt(sumSquaredDiffs / (returns.size() - 1));
        }

        // Calculate Sharpe ratio (using 0% as risk-free rate for simplicity)
        double sharpeRatio = 0;
        if (volatility > 0) {
            sharpeRatio = averageReturn / volatility;
        }

        // Calculate total fees and taxes
        double totalFees = 0.0;
        double totalTaxes = 0.0;

        for (Order order : orders) {
            totalFees += order.getFee();
            totalTaxes += order.getTax();
        }

        return new PerformanceMetrics(
                totalProfit,
                totalReturnPercentage,
                maxDrawdown,
                orders.size(),
                volatility * 100, // Convert to percentage
                sharpeRatio,
                totalFees,
                totalTaxes
        );
    }

    /**
     * Check if two trading pairs match, ignoring case and allowing for different formats.
     * 
     * @param pair1 the first trading pair
     * @param pair2 the second trading pair
     * @return true if the pairs match, false otherwise
     */
    private boolean isMatchingPair(String pair1, String pair2) {
        // Simple equality check
        if (pair1.equalsIgnoreCase(pair2)) {
            return true;
        }

        // Handle different separators (BTC-USD vs BTC/USD vs BTCUSD)
        String normalized1 = pair1.replaceAll("[-/]", "").toUpperCase();
        String normalized2 = pair2.replaceAll("[-/]", "").toUpperCase();

        return normalized1.equals(normalized2);
    }


}