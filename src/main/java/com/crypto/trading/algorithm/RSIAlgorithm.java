package com.crypto.trading.algorithm;

import com.crypto.trading.exchange.model.MarketData;
import com.crypto.trading.exchange.model.Order;
import com.crypto.trading.exchange.model.OrderType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * Relative Strength Index (RSI) Algorithm Implementation.
 * The RSI is a momentum oscillator that measures the speed and change of price movements.
 * It oscillates between 0 and 100, with:
 * - Values above 70 generally considered overbought (indicating potential price reversal or correction)
 * - Values below 30 generally considered oversold (indicating potential price reversal or correction)
 */
@Component
public class RSIAlgorithm implements TradingAlgorithm {
    private static final String ALGORITHM_ID = "relative-strength-index";
    private static final String ALGORITHM_NAME = "Relative Strength Index (RSI)";
    private static final String ALGORITHM_DESCRIPTION = 
            "A momentum-based trading algorithm that uses the Relative Strength Index (RSI) " +
            "to identify potential overbought and oversold conditions. " +
            "The RSI compares the magnitude of recent gains to recent losses and ranges from 0 to 100. " +
            "Typically, RSI values above 70 indicate overbought conditions (sell signal), " +
            "while values below 30 indicate oversold conditions (buy signal).";
    
    // RSI specific parameters
    private int period = 14; // Default RSI period
    private double overboughtThreshold = 70.0; // Default overbought threshold
    private double oversoldThreshold = 30.0; // Default oversold threshold
    
    // General algorithm parameters
    private double positionSize = 0.1; // Default 10% of available capital
    private double feeRate = 0.002; // Default 0.2% fee rate
    private double taxRate = 0.15; // Default 15% tax rate
    
    // Data tracking for RSI calculation
    private final ConcurrentLinkedDeque<Double> prices = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Double> gains = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Double> losses = new ConcurrentLinkedDeque<>();
    private Double previousRSI = null;
    private Double currentRSI = null;
    private double lastPrice = 0.0;
    
    // Track cumulative gain/loss for proper tax treatment
    private double liveCumulativeCapitalGainLoss = 0.0; // For live trading
    private double cumulativeCapitalGainLoss = 0.0; // For backtesting
    
    /**
     * Initialize the algorithm with configuration parameters.
     * 
     * @param parameters the configuration parameters
     */
    @Override
    public void initialize(Map<String, Object> parameters) {
        // Process period parameter
        if (parameters.containsKey("period")) {
            int paramPeriod = ((Number) parameters.get("period")).intValue();
            if (paramPeriod >= 2 && paramPeriod <= 50) {
                this.period = paramPeriod;
            }
        }
        
        // Process threshold parameters
        if (parameters.containsKey("overboughtThreshold")) {
            double paramThreshold = ((Number) parameters.get("overboughtThreshold")).doubleValue();
            if (paramThreshold > 50 && paramThreshold < 100) {
                this.overboughtThreshold = paramThreshold;
            }
        }
        
        if (parameters.containsKey("oversoldThreshold")) {
            double paramThreshold = ((Number) parameters.get("oversoldThreshold")).doubleValue();
            if (paramThreshold > 0 && paramThreshold < 50) {
                this.oversoldThreshold = paramThreshold;
            }
        }
        
        // Process position size parameter
        if (parameters.containsKey("positionSize")) {
            double paramSize = ((Number) parameters.get("positionSize")).doubleValue();
            if (paramSize > 0 && paramSize <= 1.0) {
                this.positionSize = paramSize;
            }
        }
        
        // Process fee and tax rates
        if (parameters.containsKey("feeRate")) {
            double paramFeeRate = ((Number) parameters.get("feeRate")).doubleValue();
            if (paramFeeRate >= 0 && paramFeeRate <= 0.01) { // Max 1% fee
                this.feeRate = paramFeeRate;
            }
        }
        
        if (parameters.containsKey("taxRate")) {
            double paramTaxRate = ((Number) parameters.get("taxRate")).doubleValue();
            if (paramTaxRate >= 0 && paramTaxRate <= 0.5) { // Max 50% tax
                this.taxRate = paramTaxRate;
            }
        }
        
        // Reset data collections
        prices.clear();
        gains.clear();
        losses.clear();
        previousRSI = null;
        currentRSI = null;
        lastPrice = 0.0;
    }
    
    /**
     * Process new market data and decide whether to generate trading signals.
     * 
     * @param marketData the latest market data
     * @return a Mono that emits a trading order if a signal is generated, or empty if no action is needed
     */
    @Override
    public Mono<Order> processMarketData(MarketData marketData) {
        double currentPrice = marketData.getLastPrice();
        
        // Process the price and update RSI
        updateRSI(currentPrice);
        
        // Check for trading signals
        if (previousRSI != null && currentRSI != null) {
            // Buy signal: RSI crosses above oversold threshold from below
            if (previousRSI < oversoldThreshold && currentRSI >= oversoldThreshold) {
                Order buyOrder = new Order(
                        marketData.getTradingPair(),
                        OrderType.BUY,
                        calculatePositionSize(currentPrice),
                        currentPrice,
                        feeRate,
                        taxRate
                );
                return Mono.just(buyOrder);
            }
            
            // Sell signal: RSI crosses below overbought threshold from above
            if (previousRSI > overboughtThreshold && currentRSI <= overboughtThreshold) {
                double amount = calculatePositionSize(currentPrice);
                
                // For live trading, we'd need to look up the cost basis from a service
                // that tracks all buy orders. This is simplified for the demo.
                double costBasisPrice = currentPrice * 0.95; // Simplified - assume 5% price movement
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
                this.liveCumulativeCapitalGainLoss += netGain;
                
                // Tax is only applied to the cumulative gain, not individual trades
                // If cumulative is negative, there's no tax liability (it's a capital loss credit)
                double tax = 0.0;
                if (this.liveCumulativeCapitalGainLoss > 0) {
                    // Only apply tax to positive cumulative gains
                    tax = this.liveCumulativeCapitalGainLoss * taxRate;
                }
                
                Order sellOrder = new Order(
                        marketData.getTradingPair(),
                        OrderType.SELL,
                        amount,
                        currentPrice,
                        feeRate,
                        taxRate
                );
                sellOrder.setTaxableGain(netGain);
                sellOrder.setEstimatedTaxLiability(tax);
                return Mono.just(sellOrder);
            }
        }
        
        return Mono.empty(); // No trading signal
    }
    
    /**
     * Backtests the algorithm using historical market data.
     * 
     * @param historicalData a list of historical market data points
     * @param initialCapital the initial capital to simulate with
     * @return a list of orders that would have been generated
     */
    @Override
    public List<Order> backtest(List<MarketData> historicalData, double initialCapital) {
        // Reset algorithm state
        prices.clear();
        gains.clear();
        losses.clear();
        previousRSI = null;
        currentRSI = null;
        lastPrice = 0.0;
        
        List<Order> orders = new ArrayList<>();
        double availableCapital = initialCapital;
        double cryptoHoldings = 0.0;
        boolean inPosition = false;
        this.cumulativeCapitalGainLoss = 0.0; // Reset the class variable for this backtest run
        
        // Sort historical data by timestamp (oldest first)
        List<MarketData> sortedData = historicalData.stream()
                .sorted(Comparator.comparing(MarketData::getTimestamp))
                .collect(Collectors.toList());
        
        for (MarketData data : sortedData) {
            double price = data.getLastPrice();
            updateRSI(price);
            
            // Skip until we have enough data for RSI calculation
            if (previousRSI == null || currentRSI == null) {
                continue;
            }
            
            // Buy signal: RSI crosses above oversold threshold from below
            if (previousRSI < oversoldThreshold && currentRSI >= oversoldThreshold && !inPosition) {
                double amount = (availableCapital * positionSize) / price;
                double cost = amount * price;
                double fee = cost * feeRate;
                
                if (availableCapital >= (cost + fee)) {
                    Order buyOrder = new Order(
                            data.getTradingPair(),
                            OrderType.BUY,
                            amount,
                            price,
                            feeRate,
                            0.0  // No tax on buy orders, only establish cost basis
                    );
                    buyOrder.setCreatedAt(data.getTimestamp());
                    buyOrder.setStatus("FILLED"); // Important: Set status to avoid NPE
                    buyOrder.setExchange(data.getExchange());
                    buyOrder.setEstimatedTaxLiability(0); // Explicitly set tax to 0 for buys
                    
                    // Update portfolio
                    availableCapital -= (cost + fee);
                    cryptoHoldings += amount;
                    inPosition = true;
                    
                    // Track total portfolio value at order time
                    double portfolioValue = availableCapital + (cryptoHoldings * price);
                    buyOrder.setTotalValue(portfolioValue);
                    
                    orders.add(buyOrder);
                }
            }
            
            // Sell signal: RSI crosses below overbought threshold from above
            else if (previousRSI > overboughtThreshold && currentRSI <= overboughtThreshold && inPosition) {
                if (cryptoHoldings > 0) {
                    double amount = cryptoHoldings;
                    double revenue = amount * price;
                    double sellFee = revenue * feeRate;
                    
                    // Get the cost basis price from the last buy order
                    double costBasisPrice = 0.0;
                    for (Order previousOrder : orders) {
                        if (previousOrder.getType() == OrderType.BUY) {
                            costBasisPrice = previousOrder.getPrice();
                        }
                    }
                    
                    // If we can't find a previous buy order, fallback to the last price
                    if (costBasisPrice == 0.0) {
                        costBasisPrice = lastPrice;
                    }
                    
                    // Calculate total cost basis (what you paid for the crypto originally)
                    double totalCostBasis = amount * costBasisPrice;
                    double buyFee = totalCostBasis * feeRate;
                    double totalBuyCost = totalCostBasis + buyFee;
                    
                    // Calculate the gain (or loss) - this is revenue minus what you paid
                    double gain = revenue - totalCostBasis;
                    
                    // Calculate the net gain after fees
                    double netGain = gain - sellFee - buyFee;
                    
                    // Add this gain/loss to our cumulative running total
                    this.cumulativeCapitalGainLoss += netGain;
                    
                    // Calculate tax both at the individual trade level and cumulative level
                    // This ensures we have reasonable values for both individual trades and overall tax calculation
                    
                    // Individual trade tax - use for display purposes
                    double individualTax = 0.0;
                    if (netGain > 0) {
                        // Only apply tax on positive gains for individual trades
                        individualTax = netGain * taxRate;
                    }
                    
                    // Cumulative tax - more accurate for overall tax calculation
                    // If cumulative is negative, there's no tax liability (it's a capital loss)
                    double cumulativeTax = 0.0;
                    if (this.cumulativeCapitalGainLoss > 0) {
                        // Only apply tax to positive cumulative gains
                        cumulativeTax = this.cumulativeCapitalGainLoss * taxRate;
                    }
                    
                    Order sellOrder = new Order(
                            data.getTradingPair(),
                            OrderType.SELL,
                            amount,
                            price,
                            feeRate,
                            taxRate
                    );
                    sellOrder.setTaxableGain(netGain); // Set the gain/loss amount for this trade
                    // Set both tax fields - one for individual trade display, one for cumulative
                    sellOrder.setTax(individualTax);
                    sellOrder.setEstimatedTaxLiability(individualTax); // Use individual tax for per-trade display
                    sellOrder.setCreatedAt(data.getTimestamp());
                    sellOrder.setStatus("FILLED"); // Important: Set status to avoid NPE
                    sellOrder.setExchange(data.getExchange());
                    
                    // Update portfolio - for portfolio purposes use the cumulative tax (more accurate)
                    availableCapital += (revenue - sellFee - cumulativeTax);
                    cryptoHoldings = 0;
                    inPosition = false;
                    
                    // Track total portfolio value at order time
                    double portfolioValue = availableCapital + (cryptoHoldings * price);
                    sellOrder.setTotalValue(portfolioValue);
                    
                    orders.add(sellOrder);
                }
            }
        }
        
        return orders;
    }
    
    /**
     * Updates the RSI calculation with a new price.
     * 
     * @param price the new price to process
     */
    private void updateRSI(double price) {
        // Add price to the list
        prices.add(price);
        
        // Maintain window size
        if (prices.size() > period) {
            prices.removeFirst();
        }
        
        // Need at least 2 prices to calculate gains/losses
        if (prices.size() < 2) {
            lastPrice = price;
            return;
        }
        
        // Calculate price changes and update gains/losses lists
        if (lastPrice > 0) {
            double change = price - lastPrice;
            if (change > 0) {
                gains.add(change);
                losses.add(0.0);
            } else {
                gains.add(0.0);
                losses.add(Math.abs(change));
            }
            
            // Trim to period
            if (gains.size() > period) {
                gains.removeFirst();
                losses.removeFirst();
            }
            
            // Calculate RSI if we have enough data
            if (gains.size() >= period) {
                calculateRSI();
            }
        }
        
        lastPrice = price;
    }
    
    /**
     * Calculates the RSI value from the collected price data.
     */
    private void calculateRSI() {
        // Calculate average gains and losses
        double avgGain = gains.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double avgLoss = losses.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        
        // Calculate RS (Relative Strength)
        double rs = (avgLoss == 0) ? 100 : avgGain / avgLoss;
        
        // Calculate RSI
        previousRSI = currentRSI;
        currentRSI = 100 - (100 / (1 + rs));
    }
    
    /**
     * Calculates the position size based on the current price.
     * 
     * @param price the current asset price
     * @return the amount of the asset to trade
     */
    private double calculatePositionSize(double price) {
        // This should be replaced with actual capital calculation in real implementation
        double assumedCapital = 10000.0; // Placeholder value
        return (assumedCapital * positionSize) / price;
    }
    
    @Override
    public String getId() {
        return ALGORITHM_ID;
    }

    @Override
    public String getName() {
        return ALGORITHM_NAME;
    }

    @Override
    public String getDescription() {
        return ALGORITHM_DESCRIPTION;
    }
    
    @Override
    public Map<String, String> getRequiredParameters() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("period", "The lookback period for RSI calculation (typically 14 days). Default: 14");
        parameters.put("overboughtThreshold", "The threshold above which RSI is considered overbought (sell signal). Default: 70");
        parameters.put("oversoldThreshold", "The threshold below which RSI is considered oversold (buy signal). Default: 30");
        parameters.put("positionSize", "The size of each position as a fraction of available capital (0.0-1.0). Default: 0.1");
        parameters.put("feeRate", "The trading fee rate as a decimal (e.g., 0.002 for 0.2%). Default: 0.002");
        parameters.put("taxRate", "The tax rate for calculating estimated tax liability. Default: 0.15");
        return parameters;
    }
    
    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("period", period);
        defaults.put("overboughtThreshold", overboughtThreshold);
        defaults.put("oversoldThreshold", oversoldThreshold);
        defaults.put("positionSize", positionSize);
        defaults.put("feeRate", feeRate);
        defaults.put("taxRate", taxRate);
        return defaults;
    }
    
    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        if (parameters.containsKey("period")) {
            int paramPeriod = ((Number) parameters.get("period")).intValue();
            if (paramPeriod < 2 || paramPeriod > 50) {
                return false;
            }
        }
        
        if (parameters.containsKey("overboughtThreshold")) {
            double paramThreshold = ((Number) parameters.get("overboughtThreshold")).doubleValue();
            if (paramThreshold <= 50 || paramThreshold >= 100) {
                return false;
            }
        }
        
        if (parameters.containsKey("oversoldThreshold")) {
            double paramThreshold = ((Number) parameters.get("oversoldThreshold")).doubleValue();
            if (paramThreshold <= 0 || paramThreshold >= 50) {
                return false;
            }
        }
        
        if (parameters.containsKey("positionSize")) {
            double paramSize = ((Number) parameters.get("positionSize")).doubleValue();
            if (paramSize <= 0 || paramSize > 1.0) {
                return false;
            }
        }
        
        return true;
    }
}