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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Implementation of the ExchangeService interface for the Coinbase Pro cryptocurrency exchange.
 */
@Service
public class CoinbaseExchangeService implements ExchangeService {
    private static final Logger logger = LoggerFactory.getLogger(CoinbaseExchangeService.class);
    
    private WebClient webClient;
    
    @Value("${exchange.coinbase.api.key}")
    private String apiKey;
    
    @Value("${exchange.coinbase.api.secret}")
    private String apiSecret;
    
    @Value("${exchange.coinbase.api.passphrase}")
    private String passphrase;
    
    @Value("${exchange.coinbase.api.url}")
    private String apiBaseUrl;

    /**
     * Constructor for CoinbaseExchangeService.
     * 
     * @param webClientBuilder WebClient.Builder for creating the WebClient
     */
    public CoinbaseExchangeService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl(apiBaseUrl)
                .build();
    }

    @Override
    public String getExchangeName() {
        return "Coinbase";
    }

    @Override
    public Mono<MarketData> getCurrentMarketData(String tradingPair) {
        logger.info("Fetching current market data for {}", tradingPair);
        
        String coinbasePair = formatCoinbasePair(tradingPair);
        
        return webClient.get()
                .uri("/products/{product_id}/ticker", coinbasePair)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> parseMarketData(response, tradingPair))
                .doOnError(e -> logger.error("Error fetching market data from Coinbase: {}", e.getMessage()));
    }

    @Override
    public Flux<MarketData> getMarketDataStream(String tradingPair) {
        // Coinbase doesn't have a direct WebSocket available in this implementation
        // We'll poll their REST API at regular intervals
        return Flux.interval(java.time.Duration.ofSeconds(5))
                .flatMap(i -> getCurrentMarketData(tradingPair))
                .doOnSubscribe(s -> logger.info("Starting market data stream for {}", tradingPair))
                .doOnCancel(() -> logger.info("Cancelling market data stream for {}", tradingPair));
    }

    @Override
    public Mono<List<MarketData>> getHistoricalMarketData(String tradingPair, LocalDateTime startTime, LocalDateTime endTime) {
        logger.info("Fetching historical market data for {} from {} to {}", tradingPair, startTime, endTime);
        
        String coinbasePair = formatCoinbasePair(tradingPair);
        
        // Convert to ISO 8601 string format
        String start = startTime.atZone(ZoneId.systemDefault()).toInstant().toString();
        String end = endTime.atZone(ZoneId.systemDefault()).toInstant().toString();
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/products/{product_id}/candles")
                        .queryParam("start", start)
                        .queryParam("end", end)
                        .queryParam("granularity", 3600) // 1 hour granularity
                        .build(coinbasePair))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> parseHistoricalMarketData(response, tradingPair))
                .doOnError(e -> logger.error("Error fetching historical market data from Coinbase: {}", e.getMessage()));
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
        
        String timestamp = generateTimestamp();
        String signature = generateSignature("/orders/" + orderId, "GET", timestamp, "");
        
        return webClient.get()
                .uri("/orders/{order_id}", orderId)
                .headers(headers -> {
                    headers.set("CB-ACCESS-KEY", apiKey);
                    headers.set("CB-ACCESS-SIGN", signature);
                    headers.set("CB-ACCESS-TIMESTAMP", timestamp);
                    headers.set("CB-ACCESS-PASSPHRASE", passphrase);
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(this::parseOrderStatus)
                .doOnError(e -> logger.error("Error checking order status on Coinbase: {}", e.getMessage()));
    }

    @Override
    public Mono<Double> getAvailableBalance(String cryptoCurrency) {
        logger.info("Checking available balance for: {}", cryptoCurrency);
        
        String timestamp = generateTimestamp();
        String signature = generateSignature("/accounts", "GET", timestamp, "");
        
        return webClient.get()
                .uri("/accounts")
                .headers(headers -> {
                    headers.set("CB-ACCESS-KEY", apiKey);
                    headers.set("CB-ACCESS-SIGN", signature);
                    headers.set("CB-ACCESS-TIMESTAMP", timestamp);
                    headers.set("CB-ACCESS-PASSPHRASE", passphrase);
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> parseBalance(response, cryptoCurrency))
                .doOnError(e -> logger.error("Error fetching balance from Coinbase: {}", e.getMessage()));
    }

    // Helper methods
    
    private String formatCoinbasePair(String pair) {
        // Convert standard pair format (e.g., BTC-USD) to Coinbase format (e.g., BTC-USD is the same)
        return pair;
    }
    
    private MarketData parseMarketData(JsonNode response, String originalPair) {
        double price = Double.parseDouble(response.get("price").asText());
        double bid = response.has("bid") ? Double.parseDouble(response.get("bid").asText()) : price;
        double ask = response.has("ask") ? Double.parseDouble(response.get("ask").asText()) : price;
        double volume = Double.parseDouble(response.get("volume").asText());
        
        // Parse timestamp if available, otherwise use current time
        LocalDateTime timestamp;
        if (response.has("time")) {
            String timeStr = response.get("time").asText();
            timestamp = LocalDateTime.ofInstant(Instant.parse(timeStr), ZoneId.systemDefault());
        } else {
            timestamp = LocalDateTime.now();
        }
        
        return new MarketData(
                originalPair,
                bid,
                ask,
                price,
                volume,
                timestamp,
                getExchangeName()
        );
    }
    
    private List<MarketData> parseHistoricalMarketData(JsonNode response, String originalPair) {
        List<MarketData> marketDataList = new ArrayList<>();
        
        if (response.isArray()) {
            for (JsonNode candle : response) {
                // Coinbase candle format: [timestamp, low, high, open, close, volume]
                long timestamp = candle.get(0).asLong();
                double low = candle.get(1).asDouble();
                double high = candle.get(2).asDouble();
                double open = candle.get(3).asDouble();
                double close = candle.get(4).asDouble();
                double volume = candle.get(5).asDouble();
                
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
        }
        
        // Sort by timestamp in ascending order
        marketDataList.sort(Comparator.comparing(MarketData::getTimestamp));
        
        return marketDataList;
    }
    
    private Mono<Order> executeOrder(Order order, String side) {
        logger.info("Executing {} order for {}", side, order.getTradingPair());
        
        String coinbasePair = formatCoinbasePair(order.getTradingPair());
        String orderType = order.getType() == OrderType.MARKET ? "market" : "limit";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("side", side);
        requestBody.put("product_id", coinbasePair);
        requestBody.put("type", orderType);
        
        if (order.getType() == OrderType.MARKET) {
            if (side.equals("buy")) {
                requestBody.put("funds", order.getAmount() * order.getPrice());
            } else {
                requestBody.put("size", order.getAmount());
            }
        } else {
            requestBody.put("price", order.getPrice());
            requestBody.put("size", order.getAmount());
        }
        
        String requestBodyJson = "{" +
                "\"side\":\"" + side + "\"," +
                "\"product_id\":\"" + coinbasePair + "\"," +
                "\"type\":\"" + orderType + "\"";
        
        if (order.getType() == OrderType.MARKET) {
            if (side.equals("buy")) {
                requestBodyJson += ",\"funds\":" + (order.getAmount() * order.getPrice());
            } else {
                requestBodyJson += ",\"size\":" + order.getAmount();
            }
        } else {
            requestBodyJson += ",\"price\":" + order.getPrice() + 
                               ",\"size\":" + order.getAmount();
        }
        
        requestBodyJson += "}";
        
        String timestamp = generateTimestamp();
        String signature = generateSignature("/orders", "POST", timestamp, requestBodyJson);
        
        return webClient.post()
                .uri("/orders")
                .headers(headers -> {
                    headers.set("CB-ACCESS-KEY", apiKey);
                    headers.set("CB-ACCESS-SIGN", signature);
                    headers.set("CB-ACCESS-TIMESTAMP", timestamp);
                    headers.set("CB-ACCESS-PASSPHRASE", passphrase);
                })
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBodyJson)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> parseOrderResponse(response, order, side))
                .doOnError(e -> logger.error("Error executing order on Coinbase: {}", e.getMessage()));
    }
    
    private Order parseOrderResponse(JsonNode response, Order originalOrder, String side) {
        String orderId = response.get("id").asText();
        String status = response.get("status").asText();
        String product = response.get("product_id").asText();
        
        double size = response.has("size") ? Double.parseDouble(response.get("size").asText()) : originalOrder.getAmount();
        double price = response.has("price") ? Double.parseDouble(response.get("price").asText()) : originalOrder.getPrice();
        
        // Convert Coinbase product to standard pair format
        String standardPair = convertCoinbaseProductToStandard(product);
        
        return new Order(
                orderId,
                standardPair,
                originalOrder.getType(),
                size,
                price,
                LocalDateTime.now(),
                convertCoinbaseStatus(status),
                getExchangeName()
        );
    }
    
    private Order parseOrderStatus(JsonNode orderData) {
        String orderId = orderData.get("id").asText();
        String status = orderData.get("status").asText();
        String product = orderData.get("product_id").asText();
        String type = orderData.get("type").asText();
        
        double size = orderData.has("size") ? Double.parseDouble(orderData.get("size").asText()) : 0.0;
        double price = orderData.has("price") ? Double.parseDouble(orderData.get("price").asText()) : 0.0;
        
        LocalDateTime createdAt = LocalDateTime.now();
        if (orderData.has("created_at")) {
            createdAt = LocalDateTime.ofInstant(
                    Instant.parse(orderData.get("created_at").asText()),
                    ZoneId.systemDefault());
        }
        
        // Convert Coinbase product to standard pair format
        String standardPair = convertCoinbaseProductToStandard(product);
        
        return new Order(
                orderId,
                standardPair,
                type.equals("limit") ? OrderType.LIMIT : OrderType.MARKET,
                size,
                price,
                createdAt,
                convertCoinbaseStatus(status),
                getExchangeName()
        );
    }
    
    private String convertCoinbaseProductToStandard(String product) {
        // Coinbase product IDs are already in the format we want (e.g., BTC-USD)
        return product;
    }
    
    private String convertCoinbaseStatus(String coinbaseStatus) {
        switch (coinbaseStatus) {
            case "pending": return "PENDING";
            case "open": return "OPEN";
            case "done": return "FILLED";
            case "active": return "OPEN";
            case "rejected": return "REJECTED";
            case "cancelled": return "CANCELLED";
            default: return "UNKNOWN";
        }
    }
    
    private double parseBalance(JsonNode response, String cryptoCurrency) {
        if (response.isArray()) {
            for (JsonNode account : response) {
                if (account.has("currency") && 
                    account.get("currency").asText().equals(cryptoCurrency)) {
                    return Double.parseDouble(account.get("available").asText());
                }
            }
        }
        
        logger.warn("Balance for {} not found on Coinbase", cryptoCurrency);
        return 0.0;
    }
    
    private String generateTimestamp() {
        return String.valueOf(Instant.now().getEpochSecond());
    }
    
    private String generateSignature(String requestPath, String method, String timestamp, String body) {
        try {
            String prehash = timestamp + method + requestPath + body;
            byte[] decodedSecret = Base64.getDecoder().decode(apiSecret);
            
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(decodedSecret, "HmacSHA256");
            sha256_HMAC.init(secret_key);
            
            byte[] hash = sha256_HMAC.doFinal(prehash.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            logger.error("Error generating signature: {}", e.getMessage());
            throw new RuntimeException("Failed to generate Coinbase API signature", e);
        }
    }
}
