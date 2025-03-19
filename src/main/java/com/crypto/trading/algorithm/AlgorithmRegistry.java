package com.crypto.trading.algorithm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Registry for all available trading algorithms.
 * This service manages the collection of algorithms and provides methods to access them.
 */
@Service
public class AlgorithmRegistry {
    private static final Logger logger = LoggerFactory.getLogger(AlgorithmRegistry.class);
    
    private final Map<String, TradingAlgorithm> algorithms = new HashMap<>();
    
    /**
     * Constructor that autowires all available algorithm implementations.
     * 
     * @param algorithms list of all trading algorithm implementations
     */
    public AlgorithmRegistry(List<TradingAlgorithm> algorithms) {
        for (TradingAlgorithm algorithm : algorithms) {
            register(algorithm);
        }
        logger.info("Registered {} trading algorithms", this.algorithms.size());
    }
    
    /**
     * Register a new trading algorithm.
     * 
     * @param algorithm the algorithm to register
     */
    public void register(TradingAlgorithm algorithm) {
        algorithms.put(algorithm.getId(), algorithm);
        logger.debug("Registered algorithm: {}", algorithm.getName());
    }
    
    /**
     * Get an algorithm by its ID.
     * 
     * @param id the algorithm ID
     * @return the algorithm, or null if not found
     */
    public TradingAlgorithm getAlgorithm(String id) {
        return algorithms.get(id);
    }
    
    /**
     * Get all registered algorithms.
     * 
     * @return a list of all registered algorithms
     */
    public List<TradingAlgorithm> getAllAlgorithms() {
        return algorithms.values().stream().collect(Collectors.toList());
    }
    
    /**
     * Get information about all registered algorithms without returning the actual implementation.
     * 
     * @return a map of algorithm IDs to their metadata
     */
    public Map<String, Map<String, String>> getAlgorithmInfo() {
        Map<String, Map<String, String>> infoMap = new HashMap<>();
        
        for (TradingAlgorithm algorithm : algorithms.values()) {
            Map<String, String> info = new HashMap<>();
            info.put("name", algorithm.getName());
            info.put("description", algorithm.getDescription());
            
            infoMap.put(algorithm.getId(), info);
        }
        
        return infoMap;
    }
    
    /**
     * Get the parameter descriptions for a specific algorithm.
     * 
     * @param algorithmId the ID of the algorithm
     * @return a map of parameter names to their descriptions, or null if algorithm not found
     */
    public Map<String, String> getAlgorithmParameters(String algorithmId) {
        TradingAlgorithm algorithm = getAlgorithm(algorithmId);
        if (algorithm == null) {
            return null;
        }
        
        return algorithm.getRequiredParameters();
    }
    
    /**
     * Check if an algorithm with the given ID exists.
     * 
     * @param algorithmId the ID to check
     * @return true if the algorithm exists, false otherwise
     */
    public boolean hasAlgorithm(String algorithmId) {
        return algorithms.containsKey(algorithmId);
    }
}
