package com.crypto.trading.algorithm;

import com.crypto.trading.exchange.model.MarketData;
import com.crypto.trading.exchange.model.Order;
import com.crypto.trading.exchange.model.OrderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A simple moving average crossover trading algorithm.
 * Generates buy signals when the short-term moving average crosses above the long-term moving average,
 * and sell signals when it crosses below.
 */
@Component
public class SimpleMovingAverageAlgorithm implements TradingAlgorithm {
    private static final Logger logger = LoggerFactory.getLogger(SimpleMovingAverageAlgorithm.class);
    
    private int shortPeriod = 10;
    private int longPeriod = 30;
    private double tradeAmount = 0.01; // Default amount to trade
    private double maxSlippage = 0.5; // Default max slippage in percentage
    private double feeRate = 0.002; // 0.2% fee rate by default
    private double taxRate = 0.15; // 15% tax rate by default 
    private double positionSize = 0.1; // 10% position size by default
    private final Queue<MarketData> dataWindow = new LinkedList<>();
    private boolean lastCrossover = false; // false = below, true = above
    private double lastBuyPrice = 0; // Track the average price at which we bought
    
    // Track cumulative gain/loss for proper tax treatment
    private double liveCumulativeCapitalGainLoss = 0.0;
    
    /**
     * Get the unique identifier for this algorithm.
     */
    @Override
    public String getId() {
        return "simple-moving-average";
    }
    
    /**
     * Get a human-readable name for this algorithm.
     */
    @Override
    public String getName() {
        return "Simple Moving Average Crossover";
    }
    
    /**
     * Get a description of how this algorithm works.
     */
    @Override
    public String getDescription() {
        return "Generates buy signals when the short-term moving average crosses above the long-term " +
               "moving average, and sell signals when it crosses below. The algorithm uses the last " +
               "traded price for calculations.";
    }
    
    /**
     * Initialize the algorithm with configuration parameters.
     */
    @Override
    public void initialize(Map<String, Object> parameters) {
        if (parameters.containsKey("shortPeriod")) {
            this.shortPeriod = (int) parameters.get("shortPeriod");
        }
        
        if (parameters.containsKey("longPeriod")) {
            this.longPeriod = (int) parameters.get("longPeriod");
        }
        
        if (parameters.containsKey("tradeAmount")) {
            this.tradeAmount = (double) parameters.get("tradeAmount");
        }
        
        if (parameters.containsKey("maxSlippage")) {
            this.maxSlippage = (double) parameters.get("maxSlippage");
        }
        
        if (parameters.containsKey("feeRate")) {
            this.feeRate = (double) parameters.get("feeRate");
        }
        
        if (parameters.containsKey("taxRate")) {
            this.taxRate = (double) parameters.get("taxRate");
        }
        
        if (parameters.containsKey("positionSize")) {
            this.positionSize = (double) parameters.get("positionSize");
        }
        
        // Validate that short period is less than long period
        if (shortPeriod >= longPeriod) {
            throw new IllegalArgumentException("Short period must be less than long period");
        }
        
        // Clear the data window
        dataWindow.clear();
        
        logger.info("Initialized SimpleMovingAverageAlgorithm with shortPeriod={}, longPeriod={}, tradeAmount={}, " +
                "maxSlippage={}, feeRate={}, taxRate={}, positionSize={}",
                shortPeriod, longPeriod, tradeAmount, maxSlippage, feeRate, taxRate, positionSize);
    }
    
    /**
     * Process new market data and decide whether to generate trading signals.
     */
    @Override
    public Mono<Order> processMarketData(MarketData marketData) {
        // Add the new data point to our window
        dataWindow.add(marketData);
        
        // Keep only the most recent data points needed for calculation
        while (dataWindow.size() > longPeriod) {
            dataWindow.poll();
        }
        
        // If we don't have enough data yet, return empty
        if (dataWindow.size() < longPeriod) {
            return Mono.empty();
        }
        
        // Calculate moving averages
        double shortTermMA = calculateMA(shortPeriod);
        double longTermMA = calculateMA(longPeriod);
        
        // Determine if there's a crossover
        boolean currentCrossover = shortTermMA > longTermMA;
        
        // Check for a signal (crossover)
        if (currentCrossover != lastCrossover) {
            Order order = null;
            
            if (currentCrossover) {
                // Short-term MA crossed above long-term MA - BUY signal
                logger.info("BUY signal generated at price: {}", marketData.getLastPrice());
                order = new Order(
                        marketData.getTradingPair(),
                        OrderType.MARKET,
                        tradeAmount,
                        marketData.getLastPrice()
                );
            } else {
                // Short-term MA crossed below long-term MA - SELL signal
                logger.info("SELL signal generated at price: {}", marketData.getLastPrice());
                order = new Order(
                        marketData.getTradingPair(),
                        OrderType.MARKET,
                        tradeAmount,
                        marketData.getLastPrice()
                );
            }
            
            // Update the last crossover state
            lastCrossover = currentCrossover;
            
            // Return the order if one was created
            if (order != null) {
                return Mono.just(order);
            }
        }
        
        // No signals, return empty
        return Mono.empty();
    }
    
    /**
     * Backtest the algorithm using historical market data.
     */
    @Override
    public List<Order> backtest(List<MarketData> historicalData, double initialCapital) {
        logger.info("Starting backtest with {} data points and {} initial capital", 
                historicalData.size(), initialCapital);
        
        List<Order> generatedOrders = new ArrayList<>();
        dataWindow.clear();
        lastCrossover = false;
        lastBuyPrice = 0;
        
        double currentCapital = initialCapital;
        double cryptoHoldings = 0.0;
        
        // Process each historical data point
        for (MarketData data : historicalData) {
            // Add the data point to our window
            dataWindow.add(data);
            
            // Keep only the most recent data points needed for calculation
            while (dataWindow.size() > longPeriod) {
                dataWindow.poll();
            }
            
            // If we don't have enough data yet, continue
            if (dataWindow.size() < longPeriod) {
                continue;
            }
            
            // Calculate moving averages
            double shortTermMA = calculateMA(shortPeriod);
            double longTermMA = calculateMA(longPeriod);
            
            // Determine if there's a crossover
            boolean currentCrossover = shortTermMA > longTermMA;
            
            // Check for a signal (crossover)
            if (currentCrossover != lastCrossover) {
                if (currentCrossover) {
                    // BUY signal
                    if (currentCapital > 0) {
                        // Calculate position size based on specified parameter
                        double investmentAmount = currentCapital * positionSize;
                        
                        // Calculate the fee
                        double fee = investmentAmount * feeRate;
                        
                        // Amount to buy after fees
                        double amountToBuy = (investmentAmount - fee) / data.getLastPrice();
                        
                        Order order = new Order(
                                data.getTradingPair(),
                                OrderType.MARKET,
                                amountToBuy,
                                data.getLastPrice()
                        );
                        order.setCreatedAt(data.getTimestamp());
                        order.setStatus("FILLED");
                        order.setExchange(data.getExchange());
                        
                        // Set fee information in the order - No tax on buys, only establish cost basis
                        order.setFeeAmount(fee);
                        order.setFeeAsset(data.getTradingPair().split("-")[1]); // Fee in USD for BTC-USD
                        order.setFeeRate(feeRate);
                        order.setTaxRate(0); // No tax on buy orders
                        order.setEstimatedTaxLiability(0);
                        
                        // Add the order to our list
                        generatedOrders.add(order);
                        
                        // Update holdings
                        cryptoHoldings += amountToBuy;
                        currentCapital -= (investmentAmount); // Deduct the total investment including fees
                        // Save the buy price for tax calculations
                        lastBuyPrice = data.getLastPrice();
                        
                        logger.debug("Backtest BUY: {} {} at price {} - Fee: {}, Capital: {}, Holdings: {}", 
                                amountToBuy, data.getTradingPair().split("-")[0], 
                                data.getLastPrice(), fee, currentCapital, cryptoHoldings);
                    }
                } else {
                    // SELL signal
                    if (cryptoHoldings > 0) {
                        // Calculate the gross proceeds
                        double grossProceeds = cryptoHoldings * data.getLastPrice();
                        
                        // Calculate the fee
                        double fee = grossProceeds * feeRate;
                        
                        // Calculate the net proceeds after fees
                        double netProceeds = grossProceeds - fee;
                        
                        Order order = new Order(
                                data.getTradingPair(),
                                OrderType.MARKET,
                                cryptoHoldings,
                                data.getLastPrice()
                        );
                        order.setCreatedAt(data.getTimestamp());
                        order.setStatus("FILLED");
                        order.setExchange(data.getExchange());
                        
                        // Set fee information in the order
                        order.setFeeAmount(fee);
                        order.setFeeAsset(data.getTradingPair().split("-")[1]); // Fee in USD for BTC-USD
                        order.setFeeRate(feeRate);
                        
                        // Calculate and set tax information
                        // We need to track the cost basis and only apply tax to actual profits
                        // For simplicity, we'll estimate the cost basis from when we bought
                        double costBasis = order.getAmount() * lastBuyPrice;
                        double profit = grossProceeds - fee - costBasis;
                        double estimatedTaxLiability = Math.max(0, profit * taxRate); // Only tax positive profits
                        order.setTaxRate(taxRate);
                        order.setEstimatedTaxLiability(estimatedTaxLiability);
                        
                        // Add the order to our list
                        generatedOrders.add(order);
                        
                        // Update holdings - we include the tax liability in our model to be realistic
                        currentCapital += netProceeds - estimatedTaxLiability;
                        cryptoHoldings = 0;
                        
                        logger.debug("Backtest SELL: {} {} at price {} - Fee: {}, Tax: {}, Net: {}, Capital: {}", 
                                order.getAmount(), data.getTradingPair().split("-")[0], 
                                data.getLastPrice(), fee, estimatedTaxLiability, netProceeds - estimatedTaxLiability, 
                                currentCapital);
                    }
                }
                
                // Update the last crossover state
                lastCrossover = currentCrossover;
            }
        }
        
        // Calculate final value
        MarketData lastData = historicalData.get(historicalData.size() - 1);
        double finalValue = currentCapital + (cryptoHoldings * lastData.getLastPrice());
        
        logger.info("Backtest complete. Initial capital: {}, Final value: {}, Return: {}%", 
                initialCapital, finalValue, ((finalValue / initialCapital) - 1) * 100);
        
        return generatedOrders;
    }
    
    /**
     * Get the required parameters for this algorithm.
     */
    @Override
    public Map<String, String> getRequiredParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("shortPeriod", "Short-term moving average period (number of data points)");
        params.put("longPeriod", "Long-term moving average period (number of data points)");
        params.put("tradeAmount", "Amount of cryptocurrency to trade on each signal");
        params.put("maxSlippage", "Maximum allowed slippage in percentage");
        params.put("positionSize", "The size of each position as a fraction of available capital (0.0-1.0)");
        params.put("feeRate", "The trading fee rate as a decimal (e.g., 0.002 for 0.2%)");
        params.put("taxRate", "The tax rate for calculating estimated tax liability");
        return params;
    }
    
    /**
     * Get the default parameter values for this algorithm.
     * 
     * @return a map of parameter names to their default values
     */
    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("shortPeriod", shortPeriod);
        defaults.put("longPeriod", longPeriod);
        defaults.put("tradeAmount", tradeAmount);
        defaults.put("maxSlippage", maxSlippage);
        defaults.put("positionSize", 0.1);
        defaults.put("feeRate", 0.002);
        defaults.put("taxRate", 0.15);
        return defaults;
    }
    
    /**
     * Validate the configuration parameters.
     */
    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        // Check that required parameters exist and are of the correct type
        if (!parameters.containsKey("shortPeriod") || !(parameters.get("shortPeriod") instanceof Integer)) {
            logger.error("Missing or invalid shortPeriod parameter");
            return false;
        }
        
        if (!parameters.containsKey("longPeriod") || !(parameters.get("longPeriod") instanceof Integer)) {
            logger.error("Missing or invalid longPeriod parameter");
            return false;
        }
        
        int shortPeriod = (int) parameters.get("shortPeriod");
        int longPeriod = (int) parameters.get("longPeriod");
        
        // Validate values
        if (shortPeriod <= 0) {
            logger.error("shortPeriod must be positive");
            return false;
        }
        
        if (longPeriod <= 0) {
            logger.error("longPeriod must be positive");
            return false;
        }
        
        if (shortPeriod >= longPeriod) {
            logger.error("shortPeriod must be less than longPeriod");
            return false;
        }
        
        // tradeAmount is optional, but if present, validate it
        if (parameters.containsKey("tradeAmount")) {
            if (!(parameters.get("tradeAmount") instanceof Number)) {
                logger.error("tradeAmount must be a number");
                return false;
            }
            
            double tradeAmount = ((Number) parameters.get("tradeAmount")).doubleValue();
            if (tradeAmount <= 0) {
                logger.error("tradeAmount must be positive");
                return false;
            }
        }
        
        // maxSlippage is optional, but if present, validate it
        if (parameters.containsKey("maxSlippage")) {
            if (!(parameters.get("maxSlippage") instanceof Number)) {
                logger.error("maxSlippage must be a number");
                return false;
            }
            
            double maxSlippage = ((Number) parameters.get("maxSlippage")).doubleValue();
            if (maxSlippage < 0) {
                logger.error("maxSlippage must be non-negative");
                return false;
            }
        }
        
        // feeRate is optional, but if present, validate it
        if (parameters.containsKey("feeRate")) {
            if (!(parameters.get("feeRate") instanceof Number)) {
                logger.error("feeRate must be a number");
                return false;
            }
            
            double feeRate = ((Number) parameters.get("feeRate")).doubleValue();
            if (feeRate < 0 || feeRate > 1) {
                logger.error("feeRate must be between 0 and 1");
                return false;
            }
        }
        
        // taxRate is optional, but if present, validate it
        if (parameters.containsKey("taxRate")) {
            if (!(parameters.get("taxRate") instanceof Number)) {
                logger.error("taxRate must be a number");
                return false;
            }
            
            double taxRate = ((Number) parameters.get("taxRate")).doubleValue();
            if (taxRate < 0 || taxRate > 1) {
                logger.error("taxRate must be between 0 and 1");
                return false;
            }
        }
        
        // positionSize is optional, but if present, validate it
        if (parameters.containsKey("positionSize")) {
            if (!(parameters.get("positionSize") instanceof Number)) {
                logger.error("positionSize must be a number");
                return false;
            }
            
            double positionSize = ((Number) parameters.get("positionSize")).doubleValue();
            if (positionSize <= 0 || positionSize > 1) {
                logger.error("positionSize must be greater than 0 and less than or equal to 1");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Calculate the moving average for a given period.
     * 
     * @param period the number of data points to include in the average
     * @return the calculated moving average
     */
    private double calculateMA(int period) {
        List<MarketData> recentData = new ArrayList<>(dataWindow);
        
        // Take only the most recent 'period' data points
        if (recentData.size() > period) {
            recentData = recentData.subList(recentData.size() - period, recentData.size());
        }
        
        // Calculate the average of last prices
        return recentData.stream()
                .mapToDouble(MarketData::getLastPrice)
                .average()
                .orElse(0.0);
    }
}
