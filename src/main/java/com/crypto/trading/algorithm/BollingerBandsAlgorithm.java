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
 * Bollinger Bands Algorithm Implementation.
 * Bollinger Bands consist of a middle band (simple moving average),
 * an upper band (middle band + standard deviation), and
 * a lower band (middle band - standard deviation).
 * The bands expand and contract based on market volatility.
 */
@Component
public class BollingerBandsAlgorithm implements TradingAlgorithm {
    private static final String ALGORITHM_ID = "bollinger-bands";
    private static final String ALGORITHM_NAME = "Bollinger Bands";
    private static final String ALGORITHM_DESCRIPTION = 
            "A volatility-based trading algorithm that uses Bollinger Bands to identify potential market reversals " +
            "and price targets. Bollinger Bands consist of a middle band (the simple moving average), " +
            "and an upper and lower band (the middle band plus/minus a standard deviation multiple). " +
            "When price reaches the upper band, it's considered overbought. " +
            "When price reaches the lower band, it's considered oversold.";
    
    // Bollinger Bands specific parameters
    private int period = 20; // Default period
    private double deviationMultiple = 2.0; // Default standard deviation multiple
    
    // General algorithm parameters
    private double positionSize = 0.1; // Default 10% of available capital
    private double feeRate = 0.002; // Default 0.2% fee rate
    private double taxRate = 0.15; // Default 15% tax rate
    
    // Data tracking for Bollinger Bands calculation
    private final ConcurrentLinkedDeque<Double> prices = new ConcurrentLinkedDeque<>();
    private Double middleBand = null;
    private Double upperBand = null;
    private Double lowerBand = null;
    private Double lastPrice = null;
    private boolean priceAboveBands = false;
    private boolean priceBelowBands = false;
    
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
            if (paramPeriod >= 10 && paramPeriod <= 50) {
                this.period = paramPeriod;
            }
        }
        
        // Process deviation multiple parameter
        if (parameters.containsKey("deviationMultiple")) {
            double paramDeviation = ((Number) parameters.get("deviationMultiple")).doubleValue();
            if (paramDeviation >= 1.0 && paramDeviation <= 3.0) {
                this.deviationMultiple = paramDeviation;
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
        middleBand = null;
        upperBand = null;
        lowerBand = null;
        lastPrice = null;
        priceAboveBands = false;
        priceBelowBands = false;
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
        
        // Process the price and update Bollinger Bands
        updateBollingerBands(currentPrice);
        
        // Skip if we don't have enough data yet
        if (middleBand == null || upperBand == null || lowerBand == null || lastPrice == null) {
            lastPrice = currentPrice;
            return Mono.empty();
        }
        
        // Check for trading signals
        
        // Buy signal: Price crossing above the lower band from below
        if (lastPrice <= lowerBand && currentPrice > lowerBand && !priceBelowBands) {
            priceBelowBands = true;
            Order buyOrder = new Order(
                    marketData.getTradingPair(),
                    OrderType.BUY,
                    calculatePositionSize(currentPrice),
                    currentPrice,
                    feeRate,
                    0.0  // No tax on buy orders, only establish cost basis
            );
            buyOrder.setEstimatedTaxLiability(0); // Explicitly set tax to 0 for buys
            lastPrice = currentPrice;
            return Mono.just(buyOrder);
        }
        
        // Sell signal: Price crossing below the upper band from above
        if (lastPrice >= upperBand && currentPrice < upperBand && !priceAboveBands) {
            priceAboveBands = true;
            // For live trading, we'd need to look up the cost basis from a service
            // that tracks all buy orders. This is simplified for the demo.
            double costBasisPrice = lastPrice * 0.95; // Simplified - assume 5% price movement
            double amount = calculatePositionSize(currentPrice);
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
            lastPrice = currentPrice;
            return Mono.just(sellOrder);
        }
        
        // Reset flags when price moves back inside the bands
        if (currentPrice < upperBand) {
            priceAboveBands = false;
        }
        
        if (currentPrice > lowerBand) {
            priceBelowBands = false;
        }
        
        lastPrice = currentPrice;
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
        middleBand = null;
        upperBand = null;
        lowerBand = null;
        lastPrice = null;
        priceAboveBands = false;
        priceBelowBands = false;
        
        List<Order> orders = new ArrayList<>();
        double availableCapital = initialCapital;
        double cryptoHoldings = 0.0;
        this.cumulativeCapitalGainLoss = 0.0; // Reset the class variable for this backtest run
        
        // Sort historical data by timestamp (oldest first)
        List<MarketData> sortedData = historicalData.stream()
                .sorted(Comparator.comparing(MarketData::getTimestamp))
                .collect(Collectors.toList());
        
        for (MarketData data : sortedData) {
            double price = data.getLastPrice();
            
            // Update Bollinger Bands with new price
            updateBollingerBands(price);
            
            // Skip until we have enough data for Bollinger Bands calculation
            if (middleBand == null || upperBand == null || lowerBand == null || lastPrice == null) {
                lastPrice = price;
                continue;
            }
            
            // Buy signal: Price crossing above the lower band from below
            if (lastPrice <= lowerBand && price > lowerBand && !priceBelowBands) {
                priceBelowBands = true;
                
                // Only buy if we have available capital
                if (availableCapital > 0) {
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
                        
                        // Track total portfolio value at order time
                        double portfolioValue = availableCapital + (cryptoHoldings * price);
                        buyOrder.setTotalValue(portfolioValue);
                        
                        orders.add(buyOrder);
                    }
                }
            }
            
            // Sell signal: Price crossing below the upper band from above
            else if (lastPrice >= upperBand && price < upperBand && !priceAboveBands) {
                priceAboveBands = true;
                
                // Only sell if we have holdings
                if (cryptoHoldings > 0) {
                    double amount = cryptoHoldings;
                    double revenue = amount * price;
                    double sellFee = revenue * feeRate;
                    
                    // Calculate the cost basis from the previous buy order
                    // For simplicity, we're using the last known buy price as the cost basis
                    // In a real implementation, you'd use a proper FIFO queue of buy prices
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
                    
                    // Tax is only applied to the cumulative gain, not individual trades
                    // If cumulative is negative, there's no tax liability (it's a capital loss)
                    double tax = 0.0;
                    if (this.cumulativeCapitalGainLoss > 0) {
                        // Only apply tax to positive cumulative gains
                        tax = this.cumulativeCapitalGainLoss * taxRate;
                    }
                    
                    Order sellOrder = new Order(
                            data.getTradingPair(),
                            OrderType.SELL,
                            amount,
                            price,
                            feeRate,
                            taxRate
                    );
                    sellOrder.setTaxableGain(netGain); // Set the taxable gain
                    sellOrder.setEstimatedTaxLiability(tax); // Set the estimated tax liability
                    sellOrder.setCreatedAt(data.getTimestamp());
                    sellOrder.setStatus("FILLED"); // Important: Set status to avoid NPE
                    sellOrder.setExchange(data.getExchange());
                    
                    // Update portfolio
                    availableCapital += (revenue - sellFee - tax);
                    cryptoHoldings = 0;
                    
                    // Track total portfolio value at order time
                    double portfolioValue = availableCapital + (cryptoHoldings * price);
                    sellOrder.setTotalValue(portfolioValue);
                    
                    orders.add(sellOrder);
                }
            }
            
            // Reset flags when price moves back inside the bands
            if (price < upperBand) {
                priceAboveBands = false;
            }
            
            if (price > lowerBand) {
                priceBelowBands = false;
            }
            
            lastPrice = price;
        }
        
        return orders;
    }
    
    /**
     * Updates the Bollinger Bands calculation with a new price.
     * 
     * @param price the new price to process
     */
    private void updateBollingerBands(double price) {
        // Add price to the list
        prices.add(price);
        
        // Maintain window size
        if (prices.size() > period) {
            prices.removeFirst();
        }
        
        // Need enough prices to calculate bands
        if (prices.size() < period) {
            return;
        }
        
        // Calculate middle band (SMA)
        middleBand = prices.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        
        // Calculate standard deviation
        double sumSquaredDiff = prices.stream()
                .mapToDouble(p -> Math.pow(p - middleBand, 2))
                .sum();
        double standardDeviation = Math.sqrt(sumSquaredDiff / period);
        
        // Calculate bands
        upperBand = middleBand + (deviationMultiple * standardDeviation);
        lowerBand = middleBand - (deviationMultiple * standardDeviation);
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
        parameters.put("period", "The period for calculating the moving average and standard deviation (typically 20 days). Default: 20");
        parameters.put("deviationMultiple", "The multiple of standard deviation to use for the bands (typically 2.0). Default: 2.0");
        parameters.put("positionSize", "The size of each position as a fraction of available capital (0.0-1.0). Default: 0.1");
        parameters.put("feeRate", "The trading fee rate as a decimal (e.g., 0.002 for 0.2%). Default: 0.002");
        parameters.put("taxRate", "The tax rate for calculating estimated tax liability. Default: 0.15");
        return parameters;
    }
    
    @Override
    public Map<String, Object> getDefaultParameters() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("period", period);
        defaults.put("deviationMultiple", deviationMultiple);
        defaults.put("positionSize", positionSize);
        defaults.put("feeRate", feeRate);
        defaults.put("taxRate", taxRate);
        return defaults;
    }
    
    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        if (parameters.containsKey("period")) {
            int paramPeriod = ((Number) parameters.get("period")).intValue();
            if (paramPeriod < 10 || paramPeriod > 50) {
                return false;
            }
        }
        
        if (parameters.containsKey("deviationMultiple")) {
            double paramDeviation = ((Number) parameters.get("deviationMultiple")).doubleValue();
            if (paramDeviation < 1.0 || paramDeviation > 3.0) {
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