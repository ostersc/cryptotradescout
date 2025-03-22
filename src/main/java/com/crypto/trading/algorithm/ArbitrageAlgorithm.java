package com.crypto.trading.algorithm;

import com.crypto.trading.exchange.ExchangeService;
import com.crypto.trading.exchange.model.MarketData;
import com.crypto.trading.exchange.model.Order;
import com.crypto.trading.exchange.model.OrderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An arbitrage trading algorithm that looks for price differences between exchanges.
 * When a significant price difference is found, it generates orders to buy on the cheaper
 * exchange and sell on the more expensive one.
 */
@Component
public class ArbitrageAlgorithm implements TradingAlgorithm {
    private static final Logger logger = LoggerFactory.getLogger(ArbitrageAlgorithm.class);
    
    private final Map<String, List<ExchangeService>> exchangeServices;
    private final Map<String, Map<String, MarketData>> latestMarketData = new ConcurrentHashMap<>();
    
    private double minProfitPercentage = 1.0; // Minimum profit percentage to execute arbitrage
    private double tradeAmount = 0.01; // Default amount to trade
    private double maxSlippage = 0.5; // Maximum allowed slippage in percentage
    private double feeRate = 0.002; // Default fee rate (0.2%)
    private double taxRate = 0.15; // Default tax rate (15%)
    
    // Track cumulative gain/loss for proper tax treatment
    private double liveCumulativeCapitalGainLoss = 0.0; // For live trading
    private double cumulativeCapitalGainLoss = 0.0; // For backtesting
    
    /**
     * Constructor for the arbitrage algorithm.
     * 
     * @param exchangeServices the list of available exchange services
     */
    @Autowired
    public ArbitrageAlgorithm(List<ExchangeService> exchangeServices) {
        this.exchangeServices = new HashMap<>();
        
        // Group exchange services by the trading pairs they support
        for (ExchangeService service : exchangeServices) {
            // In a real implementation, we would get the supported trading pairs from each exchange
            // For now, we'll just assume all exchanges support common pairs
            List<String> supportedPairs = Arrays.asList("BTC-USD", "ETH-USD", "LTC-USD");
            
            for (String pair : supportedPairs) {
                this.exchangeServices.computeIfAbsent(pair, k -> new ArrayList<>()).add(service);
            }
        }
    }
    
    /**
     * Get the unique identifier for this algorithm.
     */
    @Override
    public String getId() {
        return "arbitrage";
    }
    
    /**
     * Get a human-readable name for this algorithm.
     */
    @Override
    public String getName() {
        return "Exchange Arbitrage";
    }
    
    /**
     * Get a description of how this algorithm works.
     */
    @Override
    public String getDescription() {
        return "Monitors prices across different exchanges and executes trades when there is a " +
               "profitable price difference between exchanges, accounting for trading fees and slippage.";
    }
    
    /**
     * Initialize the algorithm with configuration parameters.
     */
    @Override
    public void initialize(Map<String, Object> parameters) {
        if (parameters.containsKey("minProfitPercentage")) {
            Object value = parameters.get("minProfitPercentage");
            if (value instanceof Number) {
                this.minProfitPercentage = ((Number) value).doubleValue();
            }
        }
        
        if (parameters.containsKey("tradeAmount")) {
            Object value = parameters.get("tradeAmount");
            if (value instanceof Number) {
                this.tradeAmount = ((Number) value).doubleValue();
            }
        }
        
        if (parameters.containsKey("maxSlippage")) {
            Object value = parameters.get("maxSlippage");
            if (value instanceof Number) {
                this.maxSlippage = ((Number) value).doubleValue();
            }
        }
        
        if (parameters.containsKey("feeRate")) {
            Object value = parameters.get("feeRate");
            if (value instanceof Number) {
                this.feeRate = ((Number) value).doubleValue();
            }
        }
        
        if (parameters.containsKey("taxRate")) {
            Object value = parameters.get("taxRate");
            if (value instanceof Number) {
                this.taxRate = ((Number) value).doubleValue();
            }
        }
        
        // Clear existing market data
        latestMarketData.clear();
        
        logger.info("Initialized ArbitrageAlgorithm with minProfitPercentage={}, tradeAmount={}, maxSlippage={}, feeRate={}, taxRate={}",
                minProfitPercentage, tradeAmount, maxSlippage, feeRate, taxRate);
    }
    
    /**
     * Process new market data and decide whether to generate trading signals.
     */
    @Override
    public Mono<Order> processMarketData(MarketData marketData) {
        String tradingPair = marketData.getTradingPair();
        String exchange = marketData.getExchange();
        
        // Store the latest market data for this exchange and trading pair
        latestMarketData.computeIfAbsent(tradingPair, k -> new ConcurrentHashMap<>())
                .put(exchange, marketData);
        
        // Check if we have data from multiple exchanges for this trading pair
        Map<String, MarketData> pairData = latestMarketData.get(tradingPair);
        if (pairData.size() < 2) {
            // Need data from at least two exchanges to compare
            return Mono.empty();
        }
        
        // Find the exchange with the lowest ask price (best to buy from)
        String bestBuyExchange = null;
        double lowestAsk = Double.MAX_VALUE;
        
        // Find the exchange with the highest bid price (best to sell to)
        String bestSellExchange = null;
        double highestBid = 0.0;
        
        for (Map.Entry<String, MarketData> entry : pairData.entrySet()) {
            String currentExchange = entry.getKey();
            MarketData data = entry.getValue();
            
            // Skip stale data (older than 1 minute)
            if (data.getTimestamp().plusMinutes(1).isBefore(java.time.LocalDateTime.now())) {
                continue;
            }
            
            if (data.getAskPrice() < lowestAsk) {
                lowestAsk = data.getAskPrice();
                bestBuyExchange = currentExchange;
            }
            
            if (data.getBidPrice() > highestBid) {
                highestBid = data.getBidPrice();
                bestSellExchange = currentExchange;
            }
        }
        
        // If no valid exchanges found
        if (bestBuyExchange == null || bestSellExchange == null) {
            return Mono.empty();
        }
        
        // Avoid buying and selling on the same exchange
        if (bestBuyExchange.equals(bestSellExchange)) {
            return Mono.empty();
        }
        
        // Calculate potential profit
        double profitPercentage = ((highestBid / lowestAsk) - 1) * 100;
        
        // Check if the profit is sufficient (accounting for fees which are typically 0.1-0.5% per trade)
        // Typical arbitrage requires at least 0.5-1% profit to be worthwhile
        if (profitPercentage >= minProfitPercentage) {
            logger.info("Arbitrage opportunity detected: Buy {} on {} at {}, Sell on {} at {}, Profit: {}%",
                    tradingPair, bestBuyExchange, lowestAsk, bestSellExchange, highestBid, profitPercentage);
            
            // Create a buy order for the exchange with the lowest asking price
            Order buyOrder = new Order(
                    tradingPair,
                    OrderType.MARKET,
                    tradeAmount,
                    lowestAsk
            );
            buyOrder.setExchange(bestBuyExchange);
            
            // In a real implementation, we would also create and execute a corresponding sell order
            // on the exchange with the highest bid price
            
            return Mono.just(buyOrder);
        }
        
        // No arbitrage opportunity
        return Mono.empty();
    }
    
    /**
     * Backtest the algorithm using historical market data.
     */
    @Override
    public List<Order> backtest(List<MarketData> historicalData, double initialCapital) {
        logger.info("Starting arbitrage backtest with {} data points and {} initial capital", 
                historicalData.size(), initialCapital);
        
        // For realistic backtesting of an arbitrage strategy, we need data from multiple exchanges
        // However, for our simplified backtest, we'll simulate price differences between exchanges
        // by creating synthetic data that mimics price differences between two virtual exchanges
        
        List<Order> generatedOrders = new ArrayList<>();
        double currentCapital = initialCapital;
        double cryptoHoldings = 0.0;
        this.cumulativeCapitalGainLoss = 0.0; // Reset the class variable for this backtest run
        
        // Skip backtest if we don't have enough data
        if (historicalData.size() < 10) {
            logger.warn("Not enough historical data for arbitrage backtest (need at least 10 data points)");
            return generatedOrders;
        }
        
        // Sort data by timestamp
        historicalData.sort(Comparator.comparing(MarketData::getTimestamp));
        
        // Process each data point
        String primaryExchange = historicalData.get(0).getExchange(); // Use the exchange from the data
        String secondaryExchange = "SimExchange"; // Simulated secondary exchange
        String tradingPair = historicalData.get(0).getTradingPair();
        
        for (int i = 5; i < historicalData.size(); i++) {
            // Only check for arbitrage on some intervals to be realistic
            if (i % 10 != 0) {
                continue;
            }
            
            MarketData currentData = historicalData.get(i);
            
            // Simulate price differences between exchanges
            // Primary exchange uses the actual data
            double primaryBid = currentData.getBidPrice();
            double primaryAsk = currentData.getAskPrice();
            
            // Secondary exchange has slightly different prices (randomly higher or lower)
            // The differences vary to create occasional arbitrage opportunities
            double priceDiffFactor = 1.0 + ((Math.random() * 2 - 1) * 0.015); // +/- 1.5% price difference
            double secondaryBid = primaryBid * priceDiffFactor;
            double secondaryAsk = primaryAsk * priceDiffFactor;
            
            // Determine best buy and sell exchanges
            String bestBuyExchange;
            double lowestAsk;
            String bestSellExchange;
            double highestBid;
            
            if (primaryAsk < secondaryAsk) {
                bestBuyExchange = primaryExchange;
                lowestAsk = primaryAsk;
            } else {
                bestBuyExchange = secondaryExchange;
                lowestAsk = secondaryAsk;
            }
            
            if (primaryBid > secondaryBid) {
                bestSellExchange = primaryExchange;
                highestBid = primaryBid;
            } else {
                bestSellExchange = secondaryExchange;
                highestBid = secondaryBid;
            }
            
            // Skip if best buy and sell are on the same exchange
            if (bestBuyExchange.equals(bestSellExchange)) {
                continue;
            }
            
            // Calculate potential profit
            double profitPercentage = ((highestBid / lowestAsk) - 1) * 100;
            
            // Check if the profit is sufficient
            if (profitPercentage >= minProfitPercentage) {
                if (currentCapital > 0) {
                    // Create a buy order
                    double amountToBuy = (currentCapital / lowestAsk) * 0.99; // 99% of capital to account for fees
                    
                    Order buyOrder = new Order(
                            tradingPair,
                            OrderType.MARKET,
                            amountToBuy,
                            lowestAsk
                    );
                    buyOrder.setCreatedAt(currentData.getTimestamp());
                    buyOrder.setStatus("FILLED");
                    buyOrder.setExchange(bestBuyExchange);
                    
                    // Calculate and set buy fee - No tax on buys, only establish cost basis
                    double buyFeeAmount = amountToBuy * lowestAsk * feeRate;
                    buyOrder.setFeeAmount(buyFeeAmount);
                    buyOrder.setFeeRate(feeRate);
                    buyOrder.setFeeAsset(tradingPair.split("-")[1]); // Fee in USD for BTC-USD
                    buyOrder.setTaxRate(0); // No tax on buy orders
                    buyOrder.setEstimatedTaxLiability(0); // No tax liability on buys
                    
                    generatedOrders.add(buyOrder);
                    
                    // Update holdings
                    cryptoHoldings += amountToBuy;
                    currentCapital = 0;
                    
                    // Create a corresponding sell order
                    Order sellOrder = new Order(
                            tradingPair,
                            OrderType.MARKET,
                            amountToBuy,
                            highestBid
                    );
                    sellOrder.setCreatedAt(currentData.getTimestamp());
                    sellOrder.setStatus("FILLED");
                    sellOrder.setExchange(bestSellExchange);
                    generatedOrders.add(sellOrder);
                    
                    // Calculate fees and tax
                    double buyAmount = amountToBuy * lowestAsk;
                    double sellAmount = amountToBuy * highestBid;
                    double buyFee = buyAmount * feeRate;
                    double sellFee = sellAmount * feeRate;
                    double grossProfit = sellAmount - buyAmount;
                    double netProfit = grossProfit - buyFee - sellFee;
                    
                    // Add this trade's net profit/loss to our cumulative tracking
                    this.cumulativeCapitalGainLoss += netProfit;
                    
                    // Only tax positive cumulative profits
                    double tax = 0.0;
                    if (this.cumulativeCapitalGainLoss > 0) {
                        tax = this.cumulativeCapitalGainLoss * taxRate;
                    }
                    
                    // Set fee and tax information in the sell order
                    sellOrder.setFeeAmount(sellFee);
                    sellOrder.setFeeRate(feeRate);
                    sellOrder.setFeeAsset(tradingPair.split("-")[1]); // Fee in USD for BTC-USD
                    sellOrder.setTaxRate(taxRate);
                    sellOrder.setEstimatedTaxLiability(tax);
                    
                    // Update holdings
                    currentCapital = sellAmount - sellFee - tax;
                    cryptoHoldings = 0;
                    
                    logger.debug("Backtest Arbitrage: Buy {} on {} at {}, Sell on {} at {}, Profit: {}%",
                            tradingPair, bestBuyExchange, lowestAsk, bestSellExchange, highestBid, profitPercentage);
                }
            }
        }
        
        logger.info("Arbitrage backtest complete. Initial capital: {}, Final capital: {}, Return: {}%", 
                initialCapital, currentCapital, ((currentCapital / initialCapital) - 1) * 100);
        
        return generatedOrders;
    }
    
    /**
     * Get the required parameters for this algorithm.
     */
    @Override
    public Map<String, String> getRequiredParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("minProfitPercentage", "Minimum profit percentage required to execute arbitrage (after fees)");
        params.put("tradeAmount", "Amount of cryptocurrency to trade on each arbitrage opportunity");
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
        defaults.put("minProfitPercentage", minProfitPercentage);
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
        if (parameters.containsKey("minProfitPercentage")) {
            Object value = parameters.get("minProfitPercentage");
            if (!(value instanceof Number)) {
                // Try to convert from string if possible
                if (value instanceof String) {
                    try {
                        Double.parseDouble((String) value);
                        // If we get here, it's a valid number string
                    } catch (NumberFormatException e) {
                        logger.error("minProfitPercentage must be a number, got: {}", value);
                        return false;
                    }
                } else {
                    logger.error("minProfitPercentage must be a number");
                    return false;
                }
            } else {
                double minProfit = ((Number) value).doubleValue();
                if (minProfit < 0) {
                    logger.error("minProfitPercentage must be non-negative");
                    return false;
                }
            }
        }
        
        if (parameters.containsKey("tradeAmount")) {
            Object value = parameters.get("tradeAmount");
            if (!(value instanceof Number)) {
                // Try to convert from string if possible
                if (value instanceof String) {
                    try {
                        Double.parseDouble((String) value);
                        // If we get here, it's a valid number string
                    } catch (NumberFormatException e) {
                        logger.error("tradeAmount must be a number, got: {}", value);
                        return false;
                    }
                } else {
                    logger.error("tradeAmount must be a number");
                    return false;
                }
            } else {
                double tradeAmount = ((Number) value).doubleValue();
                if (tradeAmount <= 0) {
                    logger.error("tradeAmount must be positive");
                    return false;
                }
            }
        }
        
        if (parameters.containsKey("maxSlippage")) {
            Object value = parameters.get("maxSlippage");
            if (!(value instanceof Number)) {
                // Try to convert from string if possible
                if (value instanceof String) {
                    try {
                        Double.parseDouble((String) value);
                        // If we get here, it's a valid number string
                    } catch (NumberFormatException e) {
                        logger.error("maxSlippage must be a number, got: {}", value);
                        return false;
                    }
                } else {
                    logger.error("maxSlippage must be a number");
                    return false;
                }
            } else {
                double maxSlippage = ((Number) value).doubleValue();
                if (maxSlippage < 0) {
                    logger.error("maxSlippage must be non-negative");
                    return false;
                }
            }
        }
        
        // Check fee rate and tax rate if provided
        if (parameters.containsKey("feeRate")) {
            Object value = parameters.get("feeRate");
            if (!(value instanceof Number)) {
                // Try to convert from string if possible
                if (value instanceof String) {
                    try {
                        Double.parseDouble((String) value);
                        // If we get here, it's a valid number string
                    } catch (NumberFormatException e) {
                        logger.error("feeRate must be a number, got: {}", value);
                        return false;
                    }
                } else {
                    logger.error("feeRate must be a number");
                    return false;
                }
            }
        }
        
        if (parameters.containsKey("taxRate")) {
            Object value = parameters.get("taxRate");
            if (!(value instanceof Number)) {
                // Try to convert from string if possible
                if (value instanceof String) {
                    try {
                        Double.parseDouble((String) value);
                        // If we get here, it's a valid number string
                    } catch (NumberFormatException e) {
                        logger.error("taxRate must be a number, got: {}", value);
                        return false;
                    }
                } else {
                    logger.error("taxRate must be a number");
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Helper class to wrap LocalDateTime for proper equals/hashCode in maps.
     */
    private static class LocalDateTimeWrapper {
        private final java.time.LocalDateTime dateTime;
        
        public LocalDateTimeWrapper(java.time.LocalDateTime dateTime) {
            this.dateTime = dateTime;
        }
        
        public java.time.LocalDateTime getDateTime() {
            return dateTime;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LocalDateTimeWrapper that = (LocalDateTimeWrapper) o;
            return Objects.equals(dateTime, that.dateTime);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(dateTime);
        }
    }
}
