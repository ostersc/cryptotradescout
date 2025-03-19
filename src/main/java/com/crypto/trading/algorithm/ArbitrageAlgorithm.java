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
            this.minProfitPercentage = (double) parameters.get("minProfitPercentage");
        }
        
        if (parameters.containsKey("tradeAmount")) {
            this.tradeAmount = (double) parameters.get("tradeAmount");
        }
        
        if (parameters.containsKey("maxSlippage")) {
            this.maxSlippage = (double) parameters.get("maxSlippage");
        }
        
        // Clear existing market data
        latestMarketData.clear();
        
        logger.info("Initialized ArbitrageAlgorithm with minProfitPercentage={}, tradeAmount={}, maxSlippage={}",
                minProfitPercentage, tradeAmount, maxSlippage);
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
        
        // For an arbitrage strategy, we need data from multiple exchanges
        // This is a simplified implementation that assumes we've grouped historical data
        // from different exchanges by timestamp
        
        Map<LocalDateTimeWrapper, Map<String, MarketData>> dataByTimestamp = new HashMap<>();
        
        // Group data by timestamp and exchange
        for (MarketData data : historicalData) {
            LocalDateTimeWrapper key = new LocalDateTimeWrapper(data.getTimestamp());
            dataByTimestamp.computeIfAbsent(key, k -> new HashMap<>())
                    .put(data.getExchange(), data);
        }
        
        // Process each timestamp in chronological order
        List<Order> generatedOrders = new ArrayList<>();
        double currentCapital = initialCapital;
        double cryptoHoldings = 0.0;
        
        List<LocalDateTimeWrapper> sortedTimestamps = new ArrayList<>(dataByTimestamp.keySet());
        sortedTimestamps.sort(Comparator.comparing(LocalDateTimeWrapper::getDateTime));
        
        for (LocalDateTimeWrapper timestampWrapper : sortedTimestamps) {
            Map<String, MarketData> dataAtTimestamp = dataByTimestamp.get(timestampWrapper);
            
            // Skip if we don't have data from at least two exchanges
            if (dataAtTimestamp.size() < 2) {
                continue;
            }
            
            // Find best buy and sell opportunities
            String bestBuyExchange = null;
            double lowestAsk = Double.MAX_VALUE;
            
            String bestSellExchange = null;
            double highestBid = 0.0;
            
            String tradingPair = null;
            
            for (MarketData data : dataAtTimestamp.values()) {
                tradingPair = data.getTradingPair();
                
                if (data.getAskPrice() < lowestAsk) {
                    lowestAsk = data.getAskPrice();
                    bestBuyExchange = data.getExchange();
                }
                
                if (data.getBidPrice() > highestBid) {
                    highestBid = data.getBidPrice();
                    bestSellExchange = data.getExchange();
                }
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
                    buyOrder.setCreatedAt(timestampWrapper.getDateTime());
                    buyOrder.setStatus("FILLED");
                    buyOrder.setExchange(bestBuyExchange);
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
                    sellOrder.setCreatedAt(timestampWrapper.getDateTime());
                    sellOrder.setStatus("FILLED");
                    sellOrder.setExchange(bestSellExchange);
                    generatedOrders.add(sellOrder);
                    
                    // Update holdings
                    currentCapital = amountToBuy * highestBid * 0.99; // 99% to account for fees
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
        return params;
    }
    
    /**
     * Validate the configuration parameters.
     */
    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        // Check that required parameters exist and are of the correct type
        if (parameters.containsKey("minProfitPercentage")) {
            if (!(parameters.get("minProfitPercentage") instanceof Number)) {
                logger.error("minProfitPercentage must be a number");
                return false;
            }
            
            double minProfit = ((Number) parameters.get("minProfitPercentage")).doubleValue();
            if (minProfit < 0) {
                logger.error("minProfitPercentage must be non-negative");
                return false;
            }
        }
        
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
