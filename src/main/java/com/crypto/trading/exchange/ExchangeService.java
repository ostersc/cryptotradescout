package com.crypto.trading.exchange;

import com.crypto.trading.exchange.model.MarketData;
import com.crypto.trading.exchange.model.Order;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Interface for cryptocurrency exchange services.
 * Defines operations for interacting with exchange APIs.
 */
public interface ExchangeService {

    /**
     * Get the name of the exchange.
     * 
     * @return the exchange name
     */
    String getExchangeName();
    
    /**
     * Retrieve current market data for a specific trading pair.
     * 
     * @param tradingPair the trading pair (e.g., "BTC-USD")
     * @return a Mono containing the latest market data
     */
    Mono<MarketData> getCurrentMarketData(String tradingPair);
    
    /**
     * Retrieve a stream of real-time market data for a trading pair.
     * 
     * @param tradingPair the trading pair to monitor
     * @return a Flux of market data updates
     */
    Flux<MarketData> getMarketDataStream(String tradingPair);
    
    /**
     * Retrieve historical market data for a trading pair in a specified time range.
     * 
     * @param tradingPair the trading pair
     * @param startTime the start of the time range
     * @param endTime the end of the time range
     * @return a list of historical market data points
     */
    Mono<List<MarketData>> getHistoricalMarketData(String tradingPair, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Execute a buy order.
     * 
     * @param order the order details
     * @return a Mono containing the executed order with updated information
     */
    Mono<Order> executeBuyOrder(Order order);
    
    /**
     * Execute a sell order.
     * 
     * @param order the order details
     * @return a Mono containing the executed order with updated information
     */
    Mono<Order> executeSellOrder(Order order);
    
    /**
     * Check the status of an existing order.
     * 
     * @param orderId the ID of the order to check
     * @return a Mono containing the current state of the order
     */
    Mono<Order> checkOrderStatus(String orderId);
    
    /**
     * Get the available balance for a specific cryptocurrency.
     * 
     * @param cryptoCurrency the cryptocurrency code (e.g., "BTC")
     * @return a Mono containing the available balance
     */
    Mono<Double> getAvailableBalance(String cryptoCurrency);
}
