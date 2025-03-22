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
    private double cumulativeCapitalGainLoss = 0.0; // For backtest method
    
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
                        OrderType.BUY, // Explicitly use BUY instead of MARKET
                        tradeAmount,
                        marketData.getLastPrice()
                );
            } else {
                // Short-term MA crossed below long-term MA - SELL signal
                logger.info("SELL signal generated at price: {}", marketData.getLastPrice());
                
                double amount = tradeAmount;
                double currentPrice = marketData.getLastPrice();
                
                // For live trading, we'd need to look up the cost basis from a service
                // that tracks all buy orders. This is simplified for the demo.
                double costBasisPrice = lastBuyPrice > 0 ? lastBuyPrice : currentPrice * 0.95; 
                
                double revenue = amount * currentPrice;
                double sellFee = revenue * feeRate;
                
                // Calculate total cost basis (what you paid for the crypto originally)
                double totalCostBasis = amount * costBasisPrice;
                double buyFee = totalCostBasis * feeRate;
                double totalBuyCost = totalCostBasis + buyFee;
                
                // Calculate the gain (or loss) - this is revenue minus what you paid
                double gain = revenue - totalCostBasis;
                
                // Calculate the net gain after fees
                double netGain = gain - sellFee - buyFee;
                
                // Add this gain/loss to our cumulative running total for live trading
                liveCumulativeCapitalGainLoss += netGain;
                
                // Tax is only applied to the cumulative gain, not individual trades
                // If cumulative is negative, there's no tax liability (it's a capital loss credit)
                double tax = 0.0;
                if (liveCumulativeCapitalGainLoss > 0) {
                    // Only apply tax to positive cumulative gains
                    tax = liveCumulativeCapitalGainLoss * taxRate;
                }
                
                order = new Order(
                        marketData.getTradingPair(),
                        OrderType.SELL, // Changed to SELL specifically
                        amount,
                        currentPrice,
                        feeRate,
                        taxRate
                );
                order.setTaxableGain(netGain);
                order.setEstimatedTaxLiability(tax);
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
        cumulativeCapitalGainLoss = 0.0; // Reset cumulative gain/loss tracker for this backtest run
        
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
                                OrderType.BUY, // Explicitly use BUY instead of MARKET
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
                                OrderType.SELL, // Explicitly use SELL instead of MARKET
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
                        double buyFee = costBasis * feeRate; // Fee paid during purchase
                        double profit = grossProceeds - fee - costBasis - buyFee;
                        
                        // Add this gain/loss to our cumulative running total
                        this.cumulativeCapitalGainLoss += profit;
                        
                        // Calculate tax both at the individual trade level and cumulative level
                        // This ensures we have reasonable values for both individual trades and overall tax calculation
                        
                        // Individual trade tax - use for display purposes
                        double individualTax = 0.0;
                        if (profit > 0) {
                            // Only apply tax on positive gains for individual trades
                            individualTax = profit * taxRate;
                        }
                        
                        // Cumulative tax - more accurate for overall tax calculation
                        // If cumulative is negative, there's no tax liability (it's a capital loss)
                        double cumulativeTax = 0.0;
                        if (this.cumulativeCapitalGainLoss > 0) {
                            // Only apply tax to positive cumulative gains
                            cumulativeTax = this.cumulativeCapitalGainLoss * taxRate;
                        }
                        // Set both tax fields - one for individual trade display, one for cumulative
                        order.setTaxableGain(profit); // Record the individual trade profit/loss
                        order.setTaxRate(taxRate);
                        order.setTax(individualTax);
                        order.setEstimatedTaxLiability(individualTax); // Use individual tax for per-trade display
                        
                        // Add the order to our list
                        generatedOrders.add(order);
                        
                        // Update holdings - we include the tax liability in our model to be realistic
                        // For portfolio purposes, use the individual trade tax (to match display)
                        // Using the cumulative tax was causing discrepancies in returns calculation
                        currentCapital += netProceeds - individualTax;
                        cryptoHoldings = 0;
                        
                        logger.debug("Backtest SELL: {} {} at price {} - Fee: {}, indivTax: {}, cumTax: {}, Net: {}, Capital: {}", 
                                order.getAmount(), data.getTradingPair().split("-")[0], 
                                data.getLastPrice(), fee, individualTax, cumulativeTax, netProceeds - individualTax, 
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
