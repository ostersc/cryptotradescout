package com.crypto.trading.datasource;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Interface for data sources that can provide data to trading algorithms.
 * This allows algorithms to use external data beyond just market data.
 * 
 * @param <T> the type of data provided by this source
 */
public interface DataSource<T> {
    
    /**
     * Get the unique identifier for this data source.
     * 
     * @return the data source ID
     */
    String getId();
    
    /**
     * Get a human-readable name for this data source.
     * 
     * @return the data source name
     */
    String getName();
    
    /**
     * Get a description of this data source.
     * 
     * @return the data source description
     */
    String getDescription();
    
    /**
     * Initialize the data source with configuration parameters.
     * 
     * @param parameters the configuration parameters
     */
    void initialize(Map<String, Object> parameters);
    
    /**
     * Get the current value from this data source.
     * 
     * @return a Mono containing the current data value
     */
    Mono<T> getCurrentValue();
    
    /**
     * Subscribe to a stream of data updates from this source.
     * 
     * @return a Flux emitting data values as they become available
     */
    Flux<T> getDataStream();
    
    /**
     * Get the required parameters for this data source.
     * 
     * @return a map of parameter names to their descriptions
     */
    Map<String, String> getRequiredParameters();
    
    /**
     * Validate the configuration parameters.
     * 
     * @param parameters the configuration parameters to validate
     * @return true if the parameters are valid, false otherwise
     */
    boolean validateParameters(Map<String, Object> parameters);
}
