package com.crypto.trading.ai.service;

import com.crypto.trading.ai.model.AlgorithmSuggestion;
import com.crypto.trading.ai.model.MarketAnalysis;
import com.crypto.trading.algorithm.AlgorithmRegistry;
import com.crypto.trading.exchange.model.MarketData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for providing AI-powered trading strategy recommendations
 */
@Service
public class AIAdvisorService {
    private static final Logger logger = LoggerFactory.getLogger(AIAdvisorService.class);
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    
    @Value("${openai.api.key}")
    private String openaiApiKey;
    
    @Autowired
    private AlgorithmRegistry algorithmRegistry;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public AIAdvisorService() {
        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Generate market analysis and algorithm recommendations based on current market data
     *
     * @param marketData The current market data
     * @return A market analysis with AI recommendations
     */
    public Mono<MarketAnalysis> generateMarketAnalysis(MarketData marketData) {
        logger.info("Generating market analysis for {}-{}", marketData.getExchange(), marketData.getTradingPair());
        
        // Create market context for the AI
        Map<String, Object> marketContext = createMarketContext(marketData);
        
        // Create the prompt for the AI model
        String prompt = createAIPrompt(marketContext);
        
        // Call OpenAI API
        return callOpenAI(prompt)
                .flatMap(responseJson -> {
                    try {
                        return Mono.just(parseAIResponse(responseJson, marketData));
                    } catch (Exception e) {
                        logger.error("Error parsing AI response", e);
                        return Mono.error(e);
                    }
                })
                .onErrorResume(e -> {
                    logger.error("Error generating market analysis", e);
                    return Mono.empty();
                });
    }
    
    /**
     * Create a market context object for the AI
     *
     * @param marketData The current market data
     * @return A map containing market context information
     */
    private Map<String, Object> createMarketContext(MarketData marketData) {
        Map<String, Object> context = new HashMap<>();
        context.put("tradingPair", marketData.getTradingPair());
        context.put("exchange", marketData.getExchange());
        context.put("lastPrice", marketData.getLastPrice());
        context.put("bidPrice", marketData.getBidPrice());
        context.put("askPrice", marketData.getAskPrice());
        context.put("volume", marketData.getVolume());
        context.put("timestamp", marketData.getTimestamp().toString());
        context.put("availableAlgorithms", getAvailableAlgorithms());
        
        return context;
    }
    
    /**
     * Get the list of available trading algorithms
     *
     * @return A list of maps containing algorithm information
     */
    private List<Map<String, Object>> getAvailableAlgorithms() {
        List<Map<String, Object>> algorithms = new ArrayList<>();
        
        // Add Simple Moving Average algorithm
        Map<String, Object> sma = new HashMap<>();
        sma.put("id", "simple-moving-average");
        sma.put("name", "Simple Moving Average Crossover");
        sma.put("description", "Uses crossovers of short-term and long-term moving averages to generate buy/sell signals");
        sma.put("bestFor", "Trending markets with clear directional movement");
        Map<String, Object> smaParams = new HashMap<>();
        smaParams.put("shortPeriod", "Integer (default: 5) - The period for the short-term moving average");
        smaParams.put("longPeriod", "Integer (default: 20) - The period for the long-term moving average");
        smaParams.put("positionSize", "Double (default: 10) - The percentage of available capital to use per trade");
        smaParams.put("feeRate", "Double (default: 0.2) - The exchange fee rate in percentage");
        smaParams.put("taxRate", "Double (default: 15) - The tax rate for capital gains in percentage");
        sma.put("parameters", smaParams);
        algorithms.add(sma);
        
        // Add RSI algorithm
        Map<String, Object> rsi = new HashMap<>();
        rsi.put("id", "relative-strength-index");
        rsi.put("name", "Relative Strength Index");
        rsi.put("description", "Uses the Relative Strength Index (RSI) to identify overbought and oversold conditions");
        rsi.put("bestFor", "Ranging markets with price oscillations between support and resistance levels");
        Map<String, Object> rsiParams = new HashMap<>();
        rsiParams.put("period", "Integer (default: 14) - The period for RSI calculation");
        rsiParams.put("overbought", "Double (default: 70) - The threshold for overbought condition");
        rsiParams.put("oversold", "Double (default: 30) - The threshold for oversold condition");
        rsiParams.put("positionSize", "Double (default: 10) - The percentage of available capital to use per trade");
        rsiParams.put("feeRate", "Double (default: 0.2) - The exchange fee rate in percentage");
        rsiParams.put("taxRate", "Double (default: 15) - The tax rate for capital gains in percentage");
        rsi.put("parameters", rsiParams);
        algorithms.add(rsi);
        
        // Add Bollinger Bands algorithm
        Map<String, Object> bb = new HashMap<>();
        bb.put("id", "bollinger-bands");
        bb.put("name", "Bollinger Bands");
        bb.put("description", "Uses Bollinger Bands to identify volatility and potential price reversals");
        bb.put("bestFor", "Volatile markets with expanding and contracting price ranges");
        Map<String, Object> bbParams = new HashMap<>();
        bbParams.put("period", "Integer (default: 20) - The period for moving average calculation");
        bbParams.put("deviation", "Double (default: 2.0) - The standard deviation multiplier for band width");
        bbParams.put("positionSize", "Double (default: 10) - The percentage of available capital to use per trade");
        bbParams.put("feeRate", "Double (default: 0.2) - The exchange fee rate in percentage");
        bbParams.put("taxRate", "Double (default: 15) - The tax rate for capital gains in percentage");
        bb.put("parameters", bbParams);
        algorithms.add(bb);
        
        // Add Arbitrage algorithm
        Map<String, Object> arb = new HashMap<>();
        arb.put("id", "arbitrage");
        arb.put("name", "Exchange Arbitrage");
        arb.put("description", "Exploits price differences between exchanges for the same asset");
        arb.put("bestFor", "Markets with pricing inefficiencies across different exchanges");
        Map<String, Object> arbParams = new HashMap<>();
        arbParams.put("minProfitPercent", "Double (default: 1.0) - The minimum profit percentage to execute a trade");
        arbParams.put("maxSlippagePercent", "Double (default: 0.5) - The maximum allowed slippage percentage");
        arbParams.put("positionSize", "Double (default: 10) - The percentage of available capital to use per trade");
        arbParams.put("feeRate", "Double (default: 0.2) - The exchange fee rate in percentage");
        arbParams.put("taxRate", "Double (default: 15) - The tax rate for capital gains in percentage");
        arb.put("parameters", arbParams);
        algorithms.add(arb);
        
        return algorithms;
    }
    
    /**
     * Create a prompt for the OpenAI model
     *
     * @param marketContext The market context information
     * @return A formatted prompt string
     */
    private String createAIPrompt(Map<String, Object> marketContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("As a cryptocurrency trading advisor, analyze the current market data and recommend the most suitable trading algorithm(s) based on the current market conditions. ");
        prompt.append("Provide a comprehensive market analysis including trend, sentiment, and volatility score. ");
        prompt.append("Then recommend the best algorithm(s) to use with confidence scores and parameter optimizations.\n\n");
        
        prompt.append("Current Market Data:\n");
        prompt.append("Trading Pair: ").append(marketContext.get("tradingPair")).append("\n");
        prompt.append("Exchange: ").append(marketContext.get("exchange")).append("\n");
        prompt.append("Last Price: ").append(marketContext.get("lastPrice")).append("\n");
        prompt.append("Bid Price: ").append(marketContext.get("bidPrice")).append("\n");
        prompt.append("Ask Price: ").append(marketContext.get("askPrice")).append("\n");
        prompt.append("Volume: ").append(marketContext.get("volume")).append("\n");
        prompt.append("Timestamp: ").append(marketContext.get("timestamp")).append("\n\n");
        
        prompt.append("Available Trading Algorithms:\n");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> algorithms = (List<Map<String, Object>>) marketContext.get("availableAlgorithms");
        for (Map<String, Object> algorithm : algorithms) {
            prompt.append("ID: ").append(algorithm.get("id")).append("\n");
            prompt.append("Name: ").append(algorithm.get("name")).append("\n");
            prompt.append("Description: ").append(algorithm.get("description")).append("\n");
            prompt.append("Best For: ").append(algorithm.get("bestFor")).append("\n");
            prompt.append("Parameters:\n");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) algorithm.get("parameters");
            for (Map.Entry<String, Object> param : params.entrySet()) {
                prompt.append("  - ").append(param.getKey()).append(": ").append(param.getValue()).append("\n");
            }
            prompt.append("\n");
        }
        
        prompt.append("Please provide your analysis and recommendations in the following JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"marketTrend\": \"[Bullish/Bearish/Neutral]\",\n");
        prompt.append("  \"marketSentiment\": \"[Description of market sentiment]\",\n");
        prompt.append("  \"volatilityScore\": [Score from 1-10],\n");
        prompt.append("  \"analysisExplanation\": \"[Detailed explanation of your market analysis]\",\n");
        prompt.append("  \"algorithmSuggestions\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"algorithmId\": \"[algorithm-id]\",\n");
        prompt.append("      \"algorithmName\": \"[Algorithm Name]\",\n");
        prompt.append("      \"confidenceScore\": [Score from 1-10],\n");
        prompt.append("      \"reasoning\": \"[Explanation of why this algorithm is recommended]\",\n");
        prompt.append("      \"recommendedParameters\": {\n");
        prompt.append("        \"[paramName]\": [value],\n");
        prompt.append("        ...\n");
        prompt.append("      },\n");
        prompt.append("      \"expectedReturnPercent\": [Estimated return percentage]\n");
        prompt.append("    },\n");
        prompt.append("    ...\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }
    
    /**
     * Call the OpenAI API with the given prompt
     *
     * @param prompt The prompt to send to the AI model
     * @return A Mono containing the response JSON
     */
    private Mono<JsonNode> callOpenAI(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4");
        
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);
        
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2000);
        
        return webClient.post()
                .uri(OPENAI_API_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openaiApiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(error -> logger.error("Error calling OpenAI API", error));
    }
    
    /**
     * Parse the AI response and convert it to a MarketAnalysis object
     *
     * @param responseJson The response JSON from the AI
     * @param marketData The original market data
     * @return A MarketAnalysis object
     */
    private MarketAnalysis parseAIResponse(JsonNode responseJson, MarketData marketData) {
        try {
            // Extract the content from the response
            String content = responseJson.path("choices").get(0).path("message").path("content").asText();
            
            // Extract the JSON portion from the content
            int jsonStart = content.indexOf('{');
            int jsonEnd = content.lastIndexOf('}') + 1;
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonString = content.substring(jsonStart, jsonEnd);
                JsonNode aiAnalysis = objectMapper.readTree(jsonString);
                
                MarketAnalysis analysis = new MarketAnalysis();
                analysis.setTradingPair(marketData.getTradingPair());
                analysis.setExchange(marketData.getExchange());
                analysis.setTimestamp(LocalDateTime.now());
                analysis.setMarketTrend(aiAnalysis.path("marketTrend").asText());
                analysis.setMarketSentiment(aiAnalysis.path("marketSentiment").asText());
                analysis.setVolatilityScore(aiAnalysis.path("volatilityScore").asDouble());
                analysis.setAnalysisExplanation(aiAnalysis.path("analysisExplanation").asText());
                
                List<AlgorithmSuggestion> suggestions = new ArrayList<>();
                JsonNode suggestionsNode = aiAnalysis.path("algorithmSuggestions");
                if (suggestionsNode.isArray()) {
                    for (JsonNode suggestionNode : suggestionsNode) {
                        AlgorithmSuggestion suggestion = new AlgorithmSuggestion();
                        suggestion.setAlgorithmId(suggestionNode.path("algorithmId").asText());
                        suggestion.setAlgorithmName(suggestionNode.path("algorithmName").asText());
                        suggestion.setConfidenceScore(suggestionNode.path("confidenceScore").asDouble());
                        suggestion.setReasoning(suggestionNode.path("reasoning").asText());
                        suggestion.setExpectedReturnPercent(suggestionNode.path("expectedReturnPercent").asDouble());
                        
                        // Parse recommended parameters
                        Map<String, Object> params = new HashMap<>();
                        JsonNode paramsNode = suggestionNode.path("recommendedParameters");
                        Iterator<Map.Entry<String, JsonNode>> fields = paramsNode.fields();
                        while (fields.hasNext()) {
                            Map.Entry<String, JsonNode> field = fields.next();
                            JsonNode valueNode = field.getValue();
                            if (valueNode.isInt()) {
                                params.put(field.getKey(), valueNode.asInt());
                            } else if (valueNode.isDouble()) {
                                params.put(field.getKey(), valueNode.asDouble());
                            } else if (valueNode.isBoolean()) {
                                params.put(field.getKey(), valueNode.asBoolean());
                            } else {
                                params.put(field.getKey(), valueNode.asText());
                            }
                        }
                        suggestion.setRecommendedParameters(params);
                        
                        suggestions.add(suggestion);
                    }
                }
                
                analysis.setAlgorithmSuggestions(suggestions);
                return analysis;
            } else {
                throw new RuntimeException("Could not find valid JSON in AI response");
            }
        } catch (Exception e) {
            logger.error("Error parsing AI response", e);
            throw new RuntimeException("Error parsing AI response", e);
        }
    }
}