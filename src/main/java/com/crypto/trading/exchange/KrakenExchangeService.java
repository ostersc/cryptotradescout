package com.crypto.trading.exchange;

import com.crypto.trading.exchange.model.MarketData;
import com.crypto.trading.exchange.model.Order;
import com.crypto.trading.exchange.model.OrderType;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of the ExchangeService interface for the Kraken cryptocurrency exchange.
 */
@Service
public class KrakenExchangeService implements ExchangeService {
    private static final Logger logger = LoggerFactory.getLogger(KrakenExchangeService.class);
    
    private WebClient webClient;
    
    @Value("${exchange.kraken.api.key}")
    private String apiKey;
    
    @Value("${exchange.kraken.api.secret}")
    private String apiSecret;
    
    @Value("${exchange.kraken.api.url}")
    private String apiBaseUrl;

    private final WebClient.Builder webClientBuilder;

    /**
     * Constructor for KrakenExchangeService.
     * 
     * @param webClientBuilder WebClient.Builder for creating the WebClient
     */
    public KrakenExchangeService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
        this.webClient = null; // Will be initialized in initWebClient
    }
    
    /**
     * Initialize the WebClient after properties are loaded.
     */
    @jakarta.annotation.PostConstruct
    public void initWebClient() {
        logger.info("Initializing Kraken WebClient with base URL: {}", apiBaseUrl);
        this.webClient = webClientBuilder
                .baseUrl(apiBaseUrl)
                .build();
        logger.info("Kraken WebClient initialized successfully");
    }

    @Override
    public String getExchangeName() {
        return "Kraken";
    }

    @Override
    public Mono<MarketData> getCurrentMarketData(String tradingPair) {
        logger.info("Fetching current market data for {} - WebClient: {}", tradingPair, webClient);
        
        if (webClient == null) {
            logger.error("WebClient is null. Initializing now...");
            initWebClient();
            if (webClient == null) {
                logger.error("Failed to initialize WebClient!");
                return Mono.error(new RuntimeException("WebClient is not initialized"));
            }
        }
        
        String krakenPair = formatKrakenPair(tradingPair);
        logger.info("Formatted Kraken pair: {} for original pair: {}", krakenPair, tradingPair);
        
        return webClient.get()
                .uri(uriBuilder -> {
                    logger.info("Building URI for Kraken API call with base URL: {}", apiBaseUrl);
                    return uriBuilder
                            .path("/0/public/Ticker")
                            .queryParam("pair", krakenPair)
                            .build();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnNext(response -> logger.info("Received response from Kraken API: {}", response))
                .map(response -> parseMarketData(response, tradingPair))
                .doOnSuccess(marketData -> logger.info("Successfully parsed market data: {}", marketData))
                .doOnError(e -> {
                    logger.error("Error fetching market data from Kraken: {}", e.getMessage(), e);
                    if (e instanceof WebClientResponseException) {
                        WebClientResponseException wcre = (WebClientResponseException) e;
                        logger.error("HTTP Status: {}, Response body: {}", wcre.getStatusCode(), wcre.getResponseBodyAsString());
                    }
                });
    }

    @Override
    public Flux<MarketData> getMarketDataStream(String tradingPair) {
        // Kraken doesn't have a direct WebSocket available in this implementation
        // We'll poll their REST API at regular intervals
        return Flux.interval(java.time.Duration.ofSeconds(5))
                .flatMap(i -> getCurrentMarketData(tradingPair))
                .doOnSubscribe(s -> logger.info("Starting market data stream for {}", tradingPair))
                .doOnCancel(() -> logger.info("Cancelling market data stream for {}", tradingPair));
    }

    @Override
    public Mono<List<MarketData>> getHistoricalMarketData(String tradingPair, LocalDateTime startTime, LocalDateTime endTime) {
        logger.info("Fetching historical market data for {} from {} to {}", tradingPair, startTime, endTime);
        
        String krakenPair = formatKrakenPair(tradingPair);
        long startUnix = startTime.atZone(ZoneId.systemDefault()).toEpochSecond();
        long endUnix = endTime.atZone(ZoneId.systemDefault()).toEpochSecond();
        
        String uri = "/0/public/OHLC?pair=" + krakenPair + "&interval=60&since=" + startUnix;
        logger.info("Making Kraken API request for historical data: {}", uri);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/0/public/OHLC")
                        .queryParam("pair", krakenPair)
                        .queryParam("interval", 60) // 1 hour intervals
                        .queryParam("since", startUnix)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnNext(response -> logger.info("Received Kraken historical response with: {} nodes", 
                                                 response != null ? response.size() : 0))
                .map(response -> parseHistoricalMarketData(response, tradingPair, startUnix, endUnix))
                .doOnError(e -> logger.error("Error fetching historical market data from Kraken: {}", e.getMessage()));
    }

    @Override
    public Mono<Order> executeBuyOrder(Order order) {
        logger.info("Executing buy order: {}", order);
        return executeOrder(order, "buy");
    }

    @Override
    public Mono<Order> executeSellOrder(Order order) {
        logger.info("Executing sell order: {}", order);
        return executeOrder(order, "sell");
    }

    @Override
    public Mono<Order> checkOrderStatus(String orderId) {
        logger.info("Checking status of order: {}", orderId);
        
        return webClient.post()
                .uri("/0/private/QueryOrders")
                .headers(headers -> {
                    headers.set("API-Key", apiKey);
                    // Add signature and nonce for authentication
                    addAuthHeaders(headers, "/0/private/QueryOrders");
                })
                .bodyValue(UriComponentsBuilder.newInstance()
                        .queryParam("txid", orderId)
                        .build()
                        .getQueryParams())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(this::parseOrderStatus)
                .doOnError(e -> logger.error("Error checking order status on Kraken: {}", e.getMessage()));
    }

    @Override
    public Mono<Double> getAvailableBalance(String cryptoCurrency) {
        logger.info("Checking available balance for: {}", cryptoCurrency);
        
        return webClient.post()
                .uri("/0/private/Balance")
                .headers(headers -> {
                    headers.set("API-Key", apiKey);
                    // Add signature and nonce for authentication
                    addAuthHeaders(headers, "/0/private/Balance");
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> parseBalance(response, cryptoCurrency))
                .doOnError(e -> logger.error("Error fetching balance from Kraken: {}", e.getMessage()));
    }

    // Helper methods
    
    private String formatKrakenPair(String pair) {
        // Convert standard pair format (e.g., BTC-USD) to Kraken format (e.g., XXBTZUSD)
        logger.debug("Formatting pair: {}", pair);
        
        String[] parts = pair.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid trading pair format: " + pair);
        }
        
        // Handle special cases (BTC is XBT on Kraken)
        String base = parts[0].equals("BTC") ? "XBT" : parts[0];
        String quote = parts[1];
        
        // Kraken uses 'X' prefix for most crypto assets and 'Z' prefix for fiat currencies
        String formattedBase = (base.equals("XBT") || base.equals("ETH") || base.equals("XDG")) ? "X" + base : base;
        String formattedQuote = (quote.equals("USD") || quote.equals("EUR") || quote.equals("GBP") || quote.equals("JPY")) ? "Z" + quote : quote;
        
        String result = formattedBase + formattedQuote;
        logger.debug("Formatted Kraken pair: {} -> {}", pair, result);
        return result;
    }
    
    private MarketData parseMarketData(JsonNode response, String originalPair) {
        // Check for errors in the response
        if (response.has("error") && response.get("error").size() > 0 && !response.get("error").isEmpty()) {
            throw new RuntimeException("Kraken API error: " + response.get("error").toString());
        }
        
        String krakenPair = formatKrakenPair(originalPair);
        JsonNode result = response.get("result");
        logger.debug("Full result from Kraken API: {}", result);
        
        // Get first key if krakenPair is not found directly
        if (!result.has(krakenPair)) {
            logger.debug("Exact key {} not found in result, checking available keys", krakenPair);
            // Iterate through keys to find possible match
            if (result.size() > 0) {
                // Get first key (should be the pair we requested)
                String firstKey = result.fieldNames().next();
                logger.debug("Using first available key in response: {}", firstKey);
                krakenPair = firstKey;
            } else {
                throw new RuntimeException("No data found for pair: " + originalPair);
            }
        }
        
        JsonNode pairData = result.get(krakenPair);
        if (pairData == null) {
            throw new RuntimeException("No data found for pair: " + originalPair + " using key: " + krakenPair);
        }
        
        logger.debug("Found pair data: {}", pairData);
        
        try {
            double ask = Double.parseDouble(pairData.get("a").get(0).asText());
            double bid = Double.parseDouble(pairData.get("b").get(0).asText());
            double last = Double.parseDouble(pairData.get("c").get(0).asText());
            double volume = Double.parseDouble(pairData.get("v").get(1).asText());
            
            MarketData data = new MarketData(
                    originalPair,
                    bid,
                    ask,
                    last,
                    volume,
                    LocalDateTime.now(),
                    getExchangeName()
            );
            
            logger.info("Successfully parsed market data: {}", data);
            return data;
        } catch (Exception e) {
            logger.error("Error parsing market data: {}", e.getMessage(), e);
            throw new RuntimeException("Error parsing market data: " + e.getMessage(), e);
        }
    }
    
    private List<MarketData> parseHistoricalMarketData(JsonNode response, String originalPair, long startTime, long endTime) {
        // Check for errors in the response
        if (response.has("error") && response.get("error").size() > 0 && !response.get("error").isEmpty()) {
            throw new RuntimeException("Kraken API error: " + response.get("error").toString());
        }
        
        String krakenPair = formatKrakenPair(originalPair);
        JsonNode result = response.get("result");
        logger.debug("Full historical result from Kraken API: {}", result);
        
        // Get first key if krakenPair is not found directly
        if (!result.has(krakenPair)) {
            logger.debug("Exact key {} not found in historical result, checking available keys", krakenPair);
            // Iterate through keys to find possible match
            if (result.size() > 0) {
                // Skip the 'last' field which is not the OHLC data
                Iterator<String> fieldNames = result.fieldNames();
                String firstKey = null;
                while (fieldNames.hasNext()) {
                    String key = fieldNames.next();
                    if (!key.equals("last")) {
                        firstKey = key;
                        break;
                    }
                }
                
                if (firstKey != null) {
                    logger.debug("Using key in historical response: {}", firstKey);
                    krakenPair = firstKey;
                } else {
                    throw new RuntimeException("No valid OHLC data found for pair: " + originalPair);
                }
            } else {
                throw new RuntimeException("No historical data found for pair: " + originalPair);
            }
        }
        
        JsonNode ohlcData = result.get(krakenPair);
        List<MarketData> marketDataList = new ArrayList<>();
        
        if (ohlcData != null && ohlcData.isArray()) {
            for (JsonNode candle : ohlcData) {
                try {
                    long timestamp = candle.get(0).asLong();
                    
                    // Only include data within the requested time range
                    if (timestamp >= startTime && timestamp <= endTime) {
                        double open = Double.parseDouble(candle.get(1).asText());
                        double high = Double.parseDouble(candle.get(2).asText());
                        double low = Double.parseDouble(candle.get(3).asText());
                        double close = Double.parseDouble(candle.get(4).asText());
                        double volume = Double.parseDouble(candle.get(6).asText());
                        
                        LocalDateTime dateTime = LocalDateTime.ofInstant(
                                Instant.ofEpochSecond(timestamp), 
                                ZoneId.systemDefault());
                        
                        marketDataList.add(new MarketData(
                                originalPair,
                                low,  // Using low as bid (approximation)
                                high, // Using high as ask (approximation)
                                close,
                                volume,
                                dateTime,
                                getExchangeName()
                        ));
                    }
                } catch (Exception e) {
                    logger.error("Error parsing historical candle data: {}", e.getMessage());
                    // Continue with next candle instead of failing completely
                }
            }
        } else {
            logger.warn("No OHLC array data found for key: {}", krakenPair);
        }
        
        logger.info("Retrieved {} historical data points for {}", marketDataList.size(), originalPair);
        return marketDataList;
    }
    
    private Mono<Order> executeOrder(Order order, String type) {
        // Generate a new order with a unique ID
        String orderId = UUID.randomUUID().toString();
        
        // Create the request parameters
        String krakenPair = formatKrakenPair(order.getTradingPair());
        String orderType = order.getType() == OrderType.MARKET ? "market" : "limit";
        
        return webClient.post()
                .uri("/0/private/AddOrder")
                .headers(headers -> {
                    headers.set("API-Key", apiKey);
                    // Add signature and nonce for authentication
                    addAuthHeaders(headers, "/0/private/AddOrder");
                })
                .bodyValue(UriComponentsBuilder.newInstance()
                        .queryParam("pair", krakenPair)
                        .queryParam("type", type)
                        .queryParam("ordertype", orderType)
                        .queryParam("volume", order.getAmount())
                        .queryParam("price", order.getType() == OrderType.LIMIT ? order.getPrice() : "")
                        .build()
                        .getQueryParams())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> parseOrderResponse(response, order, orderId))
                .doOnError(e -> logger.error("Error executing order on Kraken: {}", e.getMessage()));
    }
    
    private Order parseOrderResponse(JsonNode response, Order originalOrder, String orderId) {
        // Check for errors in the response
        if (response.has("error") && response.get("error").size() > 0) {
            throw new RuntimeException("Kraken API error: " + response.get("error").toString());
        }
        
        JsonNode result = response.get("result");
        
        // If Kraken provides its own transaction IDs, use those
        if (result.has("txid") && result.get("txid").size() > 0) {
            orderId = result.get("txid").get(0).asText();
        }
        
        return new Order(
                orderId,
                originalOrder.getTradingPair(),
                originalOrder.getType(),
                originalOrder.getAmount(),
                originalOrder.getPrice(),
                LocalDateTime.now(),
                "PENDING",
                getExchangeName()
        );
    }
    
    private Order parseOrderStatus(JsonNode response) {
        // Check for errors in the response
        if (response.has("error") && response.get("error").size() > 0) {
            throw new RuntimeException("Kraken API error: " + response.get("error").toString());
        }
        
        JsonNode result = response.get("result");
        if (result.size() == 0) {
            throw new RuntimeException("Order not found");
        }
        
        // Get the first order (should only be one)
        String orderId = result.fieldNames().next();
        JsonNode orderData = result.get(orderId);
        
        String status = orderData.get("status").asText();
        String pair = orderData.get("descr").get("pair").asText();
        String type = orderData.get("descr").get("type").asText();
        double volume = Double.parseDouble(orderData.get("vol").asText());
        double price = orderData.has("price") ? 
                Double.parseDouble(orderData.get("price").asText()) : 0.0;
        
        // Convert Kraken pair back to standard format
        String standardPair = convertKrakenPairToStandard(pair);
        
        return new Order(
                orderId,
                standardPair,
                type.equals("limit") ? OrderType.LIMIT : OrderType.MARKET,
                volume,
                price,
                LocalDateTime.now(), // Using current time as we don't parse the time from the response
                convertKrakenStatus(status),
                getExchangeName()
        );
    }
    
    private String convertKrakenPairToStandard(String krakenPair) {
        logger.debug("Converting Kraken pair to standard format: {}", krakenPair);
        
        // Handle special cases with prefixes
        if (krakenPair.startsWith("XXBT")) {
            // XXBTZUSD -> BTC-USD
            String quote = krakenPair.substring(4);
            if (quote.startsWith("Z")) {
                quote = quote.substring(1);
            }
            return "BTC-" + quote;
        }
        
        // Handle X (crypto) prefix and Z (fiat) prefix
        String base, quote;
        
        if (krakenPair.startsWith("X")) {
            // XETHZUSD -> ETH-USD
            base = krakenPair.substring(1, 4);
        } else {
            // First 3 or 4 characters depending on format
            base = krakenPair.substring(0, Math.min(3, krakenPair.length()));
        }
        
        // Handle special cases
        if (base.equals("XBT")) {
            base = "BTC";
        }
        
        // Extract quote currency
        int splitIndex = krakenPair.startsWith("X") ? 4 : 3;
        if (splitIndex < krakenPair.length()) {
            quote = krakenPair.substring(splitIndex);
            if (quote.startsWith("Z")) {
                quote = quote.substring(1);
            }
        } else {
            quote = ""; // Fallback, should not happen with valid pairs
        }
        
        String standardPair = base + "-" + quote;
        logger.debug("Converted pair: {} -> {}", krakenPair, standardPair);
        return standardPair;
    }
    
    private String convertKrakenStatus(String krakenStatus) {
        switch (krakenStatus) {
            case "pending": return "PENDING";
            case "open": return "OPEN";
            case "closed": return "FILLED";
            case "canceled": return "CANCELLED";
            case "expired": return "EXPIRED";
            default: return "UNKNOWN";
        }
    }
    
    private double parseBalance(JsonNode response, String cryptoCurrency) {
        // Check for errors in the response
        if (response.has("error") && response.get("error").size() > 0) {
            throw new RuntimeException("Kraken API error: " + response.get("error").toString());
        }
        
        JsonNode result = response.get("result");
        
        // Handle special cases (BTC is XBT on Kraken)
        String krakenCurrency = cryptoCurrency.equals("BTC") ? "XXBT" : cryptoCurrency;
        
        // Try different formats of the currency code
        if (result.has(krakenCurrency)) {
            return Double.parseDouble(result.get(krakenCurrency).asText());
        }
        
        // If no match, try with "X" prefix for crypto
        if (result.has("X" + cryptoCurrency)) {
            return Double.parseDouble(result.get("X" + cryptoCurrency).asText());
        }
        
        // If no match, try with "Z" prefix for fiat
        if (result.has("Z" + cryptoCurrency)) {
            return Double.parseDouble(result.get("Z" + cryptoCurrency).asText());
        }
        
        // If still no match, default to 0
        logger.warn("Balance for {} not found on Kraken", cryptoCurrency);
        return 0.0;
    }
    
    private void addAuthHeaders(org.springframework.http.HttpHeaders headers, String endpoint) {
        long nonce = System.currentTimeMillis();
        headers.set("nonce", String.valueOf(nonce));
        
        // In a real implementation, you would create a proper signature based on 
        // Kraken's authentication requirements using the nonce, endpoint, and apiSecret
        // This is a placeholder for the actual signature generation
        String signature = "placeholder_signature";
        headers.set("API-Sign", signature);
    }
}
