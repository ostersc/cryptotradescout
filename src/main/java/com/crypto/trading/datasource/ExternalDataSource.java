package com.crypto.trading.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Data source implementation that fetches data from an external API.
 * This can be used to incorporate economic indicators, social media sentiment,
 * or other external factors into trading algorithms.
 */
@Component
public class ExternalDataSource implements DataSource<Double> {
    private static final Logger logger = LoggerFactory.getLogger(ExternalDataSource.class);
    
    private final WebClient.Builder webClientBuilder;
    
    private String apiUrl;
    private String apiKey;
    private String dataPath;
    private int pollingInterval = 60; // Default 60 seconds
    private WebClient webClient;
    
    /**
     * Constructor that accepts a WebClient builder.
     * 
     * @param webClientBuilder the WebClient builder for making HTTP requests
     */
    public ExternalDataSource(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }
    
    /**
     * Get the unique identifier for this data source.
     */
    @Override
    public String getId() {
        return "external-api";
    }
    
    /**
     * Get a human-readable name for this data source.
     */
    @Override
    public String getName() {
        return "External API Data";
    }
    
    /**
     * Get a description of this data source.
     */
    @Override
    public String getDescription() {
        return "Fetches numerical data from external APIs for use in trading algorithms, " +
               "such as economic indicators, social media sentiment, or other market factors.";
    }
    
    /**
     * Initialize the data source with configuration parameters.
     */
    @Override
    public void initialize(Map<String, Object> parameters) {
        if (!parameters.containsKey("apiUrl")) {
            throw new IllegalArgumentException("API URL parameter is required");
        }
        
        this.apiUrl = (String) parameters.get("apiUrl");
        
        if (parameters.containsKey("apiKey")) {
            this.apiKey = (String) parameters.get("apiKey");
        }
        
        if (parameters.containsKey("dataPath")) {
            this.dataPath = (String) parameters.get("dataPath");
        } else {
            this.dataPath = "value"; // Default JSON path to extract the value
        }
        
        if (parameters.containsKey("pollingInterval")) {
            this.pollingInterval = (int) parameters.get("pollingInterval");
        }
        
        // Create a WebClient instance for this data source
        WebClient.Builder builder = webClientBuilder.baseUrl(apiUrl);
        
        // Add API key if provided
        if (apiKey != null && !apiKey.isEmpty()) {
            builder.defaultHeader("Authorization", "Bearer " + apiKey);
        }
        
        this.webClient = builder.build();
        
        logger.info("Initialized ExternalDataSource for {} with polling interval {}s", 
                apiUrl, pollingInterval);
    }
    
    /**
     * Get the current value from the external API.
     */
    @Override
    public Mono<Double> getCurrentValue() {
        if (webClient == null) {
            return Mono.error(new IllegalStateException("Data source not initialized"));
        }
        
        return webClient.get()
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> extractValue(response))
                .doOnError(e -> logger.error("Error fetching data from external API: {}", e.getMessage()));
    }
    
    /**
     * Subscribe to a stream of data updates from the external API.
     * This implementation polls the API at regular intervals.
     */
    @Override
    public Flux<Double> getDataStream() {
        if (webClient == null) {
            return Flux.error(new IllegalStateException("Data source not initialized"));
        }
        
        return Flux.interval(Duration.ofSeconds(pollingInterval))
                .flatMap(i -> getCurrentValue())
                .doOnSubscribe(s -> logger.info("Starting data stream from {}", apiUrl))
                .doOnCancel(() -> logger.info("Cancelling data stream from {}", apiUrl));
    }
    
    /**
     * Get the required parameters for this data source.
     */
    @Override
    public Map<String, String> getRequiredParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("apiUrl", "URL of the external API");
        params.put("apiKey", "API key for authentication (optional)");
        params.put("dataPath", "JSON path to extract the numerical value (default: 'value')");
        params.put("pollingInterval", "Interval in seconds to poll the API (default: 60)");
        return params;
    }
    
    /**
     * Validate the configuration parameters.
     */
    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        if (!parameters.containsKey("apiUrl") || !(parameters.get("apiUrl") instanceof String)) {
            logger.error("Missing or invalid apiUrl parameter");
            return false;
        }
        
        // apiKey is optional, but if provided, it should be a string
        if (parameters.containsKey("apiKey") && !(parameters.get("apiKey") instanceof String)) {
            logger.error("Invalid apiKey parameter (should be a string)");
            return false;
        }
        
        // dataPath is optional, but if provided, it should be a string
        if (parameters.containsKey("dataPath") && !(parameters.get("dataPath") instanceof String)) {
            logger.error("Invalid dataPath parameter (should be a string)");
            return false;
        }
        
        // pollingInterval is optional, but if provided, it should be a positive integer
        if (parameters.containsKey("pollingInterval")) {
            if (!(parameters.get("pollingInterval") instanceof Integer)) {
                logger.error("Invalid pollingInterval parameter (should be an integer)");
                return false;
            }
            
            int interval = (int) parameters.get("pollingInterval");
            if (interval <= 0) {
                logger.error("pollingInterval must be positive");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Extract a numerical value from the API response using the configured data path.
     * 
     * @param response the API response as a Map
     * @return the extracted numerical value
     */
    private Double extractValue(Map<String, Object> response) {
        if (dataPath == null || dataPath.isEmpty()) {
            if (response.containsKey("value")) {
                return parseValue(response.get("value"));
            } else {
                throw new IllegalStateException("Response does not contain a 'value' field");
            }
        }
        
        // Handle nested paths (e.g., "data.main.value")
        String[] pathSegments = dataPath.split("\\.");
        Object current = response;
        
        for (String segment : pathSegments) {
            if (current instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) current;
                if (map.containsKey(segment)) {
                    current = map.get(segment);
                } else {
                    throw new IllegalStateException("Path segment '" + segment + "' not found in response");
                }
            } else {
                throw new IllegalStateException("Cannot navigate path segment '" + segment + 
                        "' because current value is not a map");
            }
        }
        
        return parseValue(current);
    }
    
    /**
     * Parse a value from the API response as a Double.
     * 
     * @param value the value to parse
     * @return the parsed Double value
     */
    private Double parseValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Value '" + value + "' is not a valid number");
            }
        } else {
            throw new IllegalStateException("Value is not a number or string: " + value);
        }
    }
}
