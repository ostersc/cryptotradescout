package com.crypto.trading.datasource;

import com.crypto.trading.exchange.ExchangeService;
import com.crypto.trading.exchange.model.MarketData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Data source implementation that provides market data from a cryptocurrency exchange.
 */
@Component
public class MarketDataSource implements DataSource<MarketData> {
    private static final Logger logger = LoggerFactory.getLogger(MarketDataSource.class);
    
    private final Map<String, ExchangeService> exchangeServices;
    
    private ExchangeService selectedExchange;
    private String tradingPair;
    
    /**
     * Constructor that accepts all available exchange services.
     * 
     * @param exchangeServices the list of available exchange services
     */
    public MarketDataSource(Map<String, ExchangeService> exchangeServices) {
        this.exchangeServices = exchangeServices;
    }
    
    /**
     * Get the unique identifier for this data source.
     */
    @Override
    public String getId() {
        return "market-data";
    }
    
    /**
     * Get a human-readable name for this data source.
     */
    @Override
    public String getName() {
        return "Cryptocurrency Market Data";
    }
    
    /**
     * Get a description of this data source.
     */
    @Override
    public String getDescription() {
        return "Provides real-time market data (prices, volumes) from cryptocurrency exchanges.";
    }
    
    /**
     * Initialize the data source with configuration parameters.
     */
    @Override
    public void initialize(Map<String, Object> parameters) {
        if (!parameters.containsKey("exchange")) {
            throw new IllegalArgumentException("Exchange parameter is required");
        }
        
        if (!parameters.containsKey("tradingPair")) {
            throw new IllegalArgumentException("Trading pair parameter is required");
        }
        
        String exchangeName = (String) parameters.get("exchange");
        this.tradingPair = (String) parameters.get("tradingPair");
        
        // Find the requested exchange service
        this.selectedExchange = null;
        for (ExchangeService exchange : exchangeServices.values()) {
            if (exchange.getExchangeName().equalsIgnoreCase(exchangeName)) {
                this.selectedExchange = exchange;
                break;
            }
        }
        
        if (this.selectedExchange == null) {
            throw new IllegalArgumentException("Exchange not found: " + exchangeName);
        }
        
        logger.info("Initialized MarketDataSource for {} on {}", tradingPair, exchangeName);
    }
    
    /**
     * Get the current market data value.
     */
    @Override
    public Mono<MarketData> getCurrentValue() {
        if (selectedExchange == null || tradingPair == null) {
            return Mono.error(new IllegalStateException("Data source not initialized"));
        }
        
        return selectedExchange.getCurrentMarketData(tradingPair);
    }
    
    /**
     * Subscribe to a stream of market data updates.
     */
    @Override
    public Flux<MarketData> getDataStream() {
        if (selectedExchange == null || tradingPair == null) {
            return Flux.error(new IllegalStateException("Data source not initialized"));
        }
        
        return selectedExchange.getMarketDataStream(tradingPair);
    }
    
    /**
     * Get the required parameters for this data source.
     */
    @Override
    public Map<String, String> getRequiredParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("exchange", "Name of the exchange to get data from");
        params.put("tradingPair", "Trading pair to monitor (e.g., BTC-USD)");
        return params;
    }
    
    /**
     * Validate the configuration parameters.
     */
    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        if (!parameters.containsKey("exchange") || !(parameters.get("exchange") instanceof String)) {
            logger.error("Missing or invalid exchange parameter");
            return false;
        }
        
        if (!parameters.containsKey("tradingPair") || !(parameters.get("tradingPair") instanceof String)) {
            logger.error("Missing or invalid tradingPair parameter");
            return false;
        }
        
        String exchangeName = (String) parameters.get("exchange");
        boolean exchangeExists = false;
        
        for (ExchangeService exchange : exchangeServices.values()) {
            if (exchange.getExchangeName().equalsIgnoreCase(exchangeName)) {
                exchangeExists = true;
                break;
            }
        }
        
        if (!exchangeExists) {
            logger.error("Exchange not found: {}", exchangeName);
            return false;
        }
        
        return true;
    }
}
