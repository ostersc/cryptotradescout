package com.crypto.trading.algorithm;

import com.crypto.trading.exchange.model.MarketData;
import com.crypto.trading.exchange.model.Order;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Interface for trading algorithms.
 * This is the core interface that all trading algorithms must implement.
 */
public interface TradingAlgorithm {

    /**
     * Get the unique identifier for this algorithm.
     * 
     * @return the algorithm ID
     */
    String getId();
    
    /**
     * Get a human-readable name for this algorithm.
     * 
     * @return the algorithm name
     */
    String getName();
    
    /**
     * Get a description of how this algorithm works.
     * 
     * @return the algorithm description
     */
    String getDescription();
    
    /**
     * Initialize the algorithm with configuration parameters.
     * 
     * @param parameters the configuration parameters
     */
    void initialize(Map<String, Object> parameters);
    
    /**
     * Process new market data and decide whether to generate trading signals.
     * 
     * @param marketData the latest market data
     * @return a Mono that emits a trading order if a signal is generated, or empty if no action is needed
     */
    Mono<Order> processMarketData(MarketData marketData);
    
    /**
     * Backtest the algorithm using historical market data.
     * 
     * @param historicalData a list of historical market data points
     * @param initialCapital the initial capital to simulate with
     * @return a list of orders that would have been generated
     */
    List<Order> backtest(List<MarketData> historicalData, double initialCapital);
    
    /**
     * Get the required parameters for this algorithm.
     * 
     * @return a map of parameter names to their descriptions
     */
    Map<String, String> getRequiredParameters();
    
    /**
     * Get the default parameter values for this algorithm.
     * These values will be used when no specific values are provided.
     * 
     * @return a map of parameter names to their default values
     */
    Map<String, Object> getDefaultParameters();
    
    /**
     * Validate the configuration parameters.
     * 
     * @param parameters the configuration parameters to validate
     * @return true if the parameters are valid, false otherwise
     */
    boolean validateParameters(Map<String, Object> parameters);
}
