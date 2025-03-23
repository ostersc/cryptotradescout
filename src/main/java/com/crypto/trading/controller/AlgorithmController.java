package com.crypto.trading.controller;

import com.crypto.trading.algorithm.AlgorithmRegistry;
import com.crypto.trading.algorithm.TradingAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for managing trading algorithms.
 * Provides endpoints for listing available algorithms and their parameters.
 */
@RestController
@RequestMapping("/api/algorithms")
@Tag(name = "Algorithms", description = "API for managing trading algorithms and their parameters")
public class AlgorithmController {
    private static final Logger logger = LoggerFactory.getLogger(AlgorithmController.class);
    
    private final AlgorithmRegistry algorithmRegistry;
    
    /**
     * Constructor with AlgorithmRegistry.
     * 
     * @param algorithmRegistry the algorithm registry
     */
    public AlgorithmController(AlgorithmRegistry algorithmRegistry) {
        this.algorithmRegistry = algorithmRegistry;
    }
    
    /**
     * Get a list of all available trading algorithms.
     * 
     * @return a ResponseEntity containing the list of algorithms
     */
    @Operation(
        summary = "Get list of trading algorithms",
        description = "Retrieves a list of all available trading algorithms with basic information",
        tags = { "Algorithms" }
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Successfully retrieved list of algorithms",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping
    public ResponseEntity<List<Map<String, String>>> getAlgorithms() {
        logger.info("Request for list of algorithms");
        
        List<Map<String, String>> algorithmList = algorithmRegistry.getAllAlgorithms().stream()
                .map(algorithm -> {
                    Map<String, String> info = new HashMap<>();
                    info.put("id", algorithm.getId());
                    info.put("name", algorithm.getName());
                    info.put("description", algorithm.getDescription());
                    return info;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(algorithmList);
    }
    
    /**
     * Get detailed information about a specific algorithm.
     * 
     * @param algorithmId the algorithm ID
     * @return a ResponseEntity containing the algorithm details
     */
    @Operation(
        summary = "Get algorithm details",
        description = "Retrieves detailed information about a specific trading algorithm including parameters",
        tags = { "Algorithms" }
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Successfully retrieved algorithm details",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Algorithm not found"
        )
    })
    @GetMapping("/{algorithmId}")
    public Mono<ResponseEntity<Map<String, Object>>> getAlgorithmDetails(
            @Parameter(description = "Algorithm ID (e.g., simple-moving-average, bollinger-bands)", required = true)
            @PathVariable String algorithmId) {
        
        logger.info("Request for algorithm details: {}", algorithmId);
        
        if (!algorithmRegistry.hasAlgorithm(algorithmId)) {
            return Mono.just(ResponseEntity.notFound().build());
        }
        
        TradingAlgorithm algorithm = algorithmRegistry.getAlgorithm(algorithmId);
        
        Map<String, Object> details = new HashMap<>();
        details.put("id", algorithm.getId());
        details.put("name", algorithm.getName());
        details.put("description", algorithm.getDescription());
        details.put("parameters", algorithm.getRequiredParameters());
        details.put("defaultParameters", algorithm.getDefaultParameters());
        
        return Mono.just(ResponseEntity.ok(details));
    }
    
    /**
     * Validate parameters for a specific algorithm.
     * 
     * @param algorithmId the algorithm ID
     * @param parameters the parameters to validate
     * @return a ResponseEntity with the validation result
     */
    @Operation(
        summary = "Validate algorithm parameters",
        description = "Checks if the provided parameters are valid for the specified algorithm",
        tags = { "Algorithms", "Parameters" }
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Parameters validated successfully (check 'valid' field in response)",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Algorithm not found"
        )
    })
    @PostMapping("/{algorithmId}/validate-parameters")
    public Mono<ResponseEntity<Map<String, Object>>> validateParameters(
            @Parameter(description = "Algorithm ID (e.g., simple-moving-average, bollinger-bands)", required = true)
            @PathVariable String algorithmId,
            @Parameter(description = "Algorithm parameters to validate", required = true)
            @RequestBody Map<String, Object> parameters) {
        
        logger.info("Request to validate parameters for algorithm {}: {}", 
                algorithmId, parameters);
        
        if (!algorithmRegistry.hasAlgorithm(algorithmId)) {
            return Mono.just(ResponseEntity.notFound().build());
        }
        
        TradingAlgorithm algorithm = algorithmRegistry.getAlgorithm(algorithmId);
        boolean isValid = algorithm.validateParameters(parameters);
        
        Map<String, Object> result = new HashMap<>();
        result.put("valid", isValid);
        
        if (!isValid) {
            result.put("message", "Invalid parameters for algorithm " + algorithmId);
            result.put("requiredParameters", algorithm.getRequiredParameters());
        }
        
        return Mono.just(ResponseEntity.ok(result));
    }
    
    /**
     * Get the required parameters for an algorithm.
     * 
     * @param algorithmId the algorithm ID
     * @return a ResponseEntity containing the required parameters
     */
    @GetMapping("/{algorithmId}/parameters")
    public Mono<ResponseEntity<Map<String, String>>> getAlgorithmParameters(
            @PathVariable String algorithmId) {
        
        logger.info("Request for algorithm parameters: {}", algorithmId);
        
        if (!algorithmRegistry.hasAlgorithm(algorithmId)) {
            return Mono.just(ResponseEntity.notFound().build());
        }
        
        Map<String, String> parameters = algorithmRegistry.getAlgorithm(algorithmId)
                .getRequiredParameters();
        
        return Mono.just(ResponseEntity.ok(parameters));
    }
    
    /**
     * Get the default parameter values for an algorithm.
     * 
     * @param algorithmId the algorithm ID
     * @return a ResponseEntity containing the default parameter values
     */
    @GetMapping("/{algorithmId}/default-parameters")
    public Mono<ResponseEntity<Map<String, Object>>> getAlgorithmDefaultParameters(
            @PathVariable String algorithmId) {
        
        logger.info("Request for algorithm default parameters: {}", algorithmId);
        
        if (!algorithmRegistry.hasAlgorithm(algorithmId)) {
            return Mono.just(ResponseEntity.notFound().build());
        }
        
        Map<String, Object> defaultParameters = algorithmRegistry.getAlgorithm(algorithmId)
                .getDefaultParameters();
        
        return Mono.just(ResponseEntity.ok(defaultParameters));
    }
}
