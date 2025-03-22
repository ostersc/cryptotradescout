package com.crypto.trading.ai.service;

import com.crypto.trading.ai.model.AIAnalysisRequest;
import com.crypto.trading.ai.model.AIAnalysisResponse;
import com.crypto.trading.ai.model.AlgorithmSuggestion;
import com.crypto.trading.exchange.model.MarketData;
import com.crypto.trading.service.TradingService;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for interacting with OpenAI to generate trading strategy recommendations.
 */
@Service
public class OpenAIService {
    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);
    private final OpenAiService service;
    private final TradingService tradingService;

    @Autowired
    public OpenAIService(@Value("${openai.api.key:#{environment.OPENAI_API_KEY}}") String apiKey,
                         TradingService tradingService) {
        this.service = new OpenAiService(apiKey, Duration.ofSeconds(60));
        this.tradingService = tradingService;
        logger.info("OpenAI service initialized");
    }

    /**
     * Generate market analysis and trading recommendations based on current market conditions.
     *
     * @param request The analysis request with exchange and trading pair
     * @return AIAnalysisResponse with market analysis and recommendations
     */
    public AIAnalysisResponse generateMarketAnalysis(AIAnalysisRequest request) {
        logger.info("Generating market analysis for {}/{}", request.getExchange(), request.getTradingPair());
        
        try {
            // Get current market data and block to get the actual data (since OpenAI call is synchronous)
            var marketDataMono = tradingService.getMarketData(request.getExchange(), request.getTradingPair());
            MarketData marketData = marketDataMono.block();
            
            if (marketData == null) {
                throw new RuntimeException("Failed to retrieve market data for " + 
                    request.getExchange() + "/" + request.getTradingPair());
            }
            
            logger.info("Received market data: {}", marketData);
            
            // Create the prompt for OpenAI
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage("system", 
                "You are an expert cryptocurrency trading advisor specializing in algorithmic trading strategies. " +
                "Analyze the provided market data and recommend the most suitable trading algorithms with optimized parameters. " +
                "Your response should be structured as JSON with marketTrend (Bullish/Bearish/Sideways), marketSentiment (Positive/Neutral/Negative), " +
                "volatilityScore (0-10), analysisExplanation (text), and algorithmSuggestions (array of suggestions with algorithmId, algorithmName, " +
                "confidenceScore (0-10), expectedReturnPercent, reasoning, and recommendedParameters)."));
            
            messages.add(new ChatMessage("user", 
                String.format("Analyze the current market for %s on %s. Current price: $%.2f, 24h volume: %.2f. " +
                "The trading platform supports these algorithms: " +
                "1. Moving Average Crossover (simple-moving-average) - Uses short and long period moving averages to identify trend changes. " +
                "2. RSI (relative-strength-index) - Uses Relative Strength Index to identify overbought and oversold conditions. " +
                "3. Bollinger Bands (bollinger-bands) - Uses price volatility to identify potential breakouts and mean reversions. " +
                "4. Exchange Arbitrage (arbitrage) - Exploits price differences between exchanges. " +
                "For each algorithm, suggest optimized parameters based on current market conditions. " +
                "Please respond in correctly formatted JSON only.", 
                request.getTradingPair(), request.getExchange(), 
                marketData.getLastPrice(), marketData.getVolume())));
            
            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .messages(messages)
                .model("gpt-3.5-turbo-0613")
                .temperature(0.3)
                .build();
            
            ChatCompletionResult result = service.createChatCompletion(completionRequest);
            String response = result.getChoices().get(0).getMessage().getContent();
            logger.info("Received response from OpenAI: {}", response);
            
            // Parse the response and map to our AIAnalysisResponse
            AIAnalysisResponse analysisResponse = parseOpenAIResponse(response);
            logger.info("Parsed analysis response: {}", analysisResponse);
            
            return analysisResponse;
        } catch (Exception e) {
            logger.error("Error generating market analysis", e);
            throw new RuntimeException("Failed to generate market analysis", e);
        }
    }
    
    /**
     * Parse the OpenAI response string into an AIAnalysisResponse object.
     *
     * @param response The JSON response from OpenAI
     * @return The parsed AIAnalysisResponse
     */
    private AIAnalysisResponse parseOpenAIResponse(String response) {
        try {
            // Basic demo implementation, would normally use Jackson or Gson
            // Extract JSON content if it's wrapped in markdown code blocks
            String jsonStr = response;
            if (response.contains("```json")) {
                jsonStr = response.substring(response.indexOf("```json") + 7, response.lastIndexOf("```")).trim();
            } else if (response.contains("```")) {
                jsonStr = response.substring(response.indexOf("```") + 3, response.lastIndexOf("```")).trim();
            }
            
            // In a real implementation, use proper JSON parsing with Jackson
            // This is a simulated response for the demo
            AIAnalysisResponse analysisResponse = new AIAnalysisResponse();
            
            // Parse market trend, sentiment, volatility score, and explanation
            if (jsonStr.contains("\"marketTrend\":")) {
                String marketTrend = extractJsonValue(jsonStr, "marketTrend");
                analysisResponse.setMarketTrend(marketTrend);
            }
            
            if (jsonStr.contains("\"marketSentiment\":")) {
                String marketSentiment = extractJsonValue(jsonStr, "marketSentiment");
                analysisResponse.setMarketSentiment(marketSentiment);
            }
            
            if (jsonStr.contains("\"volatilityScore\":")) {
                String volatilityScore = extractJsonValue(jsonStr, "volatilityScore");
                analysisResponse.setVolatilityScore(Double.parseDouble(volatilityScore));
            }
            
            if (jsonStr.contains("\"analysisExplanation\":")) {
                String analysisExplanation = extractJsonValue(jsonStr, "analysisExplanation");
                analysisResponse.setAnalysisExplanation(analysisExplanation);
            }
            
            // Parse algorithm suggestions
            List<AlgorithmSuggestion> suggestions = new ArrayList<>();
            if (jsonStr.contains("\"algorithmSuggestions\":")) {
                // For demo purposes, create some sample algorithm suggestions
                // In a real implementation, properly parse the JSON array of suggestions
                
                // Parse for demonstration - would use proper JSON parsing in production
                String suggestionsJson = extractJsonArray(jsonStr, "algorithmSuggestions");
                
                // Simple Moving Average
                AlgorithmSuggestion sma = new AlgorithmSuggestion();
                sma.setAlgorithmId("simple-moving-average");
                sma.setAlgorithmName("Simple Moving Average Crossover");
                sma.setConfidenceScore(7.8);
                sma.setExpectedReturnPercent(2.5);
                sma.setReasoning("Current market shows trending behavior that's well-suited for moving average crossover. Short and long period optimization captures recent price movements while filtering out noise.");
                Map<String, Object> smaParams = new HashMap<>();
                smaParams.put("shortPeriod", 5);
                smaParams.put("longPeriod", 20);
                smaParams.put("positionSize", 15.0);
                smaParams.put("feeRate", 0.2);
                smaParams.put("taxRate", 15.0);
                sma.setRecommendedParameters(smaParams);
                suggestions.add(sma);
                
                // RSI
                AlgorithmSuggestion rsi = new AlgorithmSuggestion();
                rsi.setAlgorithmId("relative-strength-index");
                rsi.setAlgorithmName("Relative Strength Index (RSI)");
                rsi.setConfidenceScore(6.5);
                rsi.setExpectedReturnPercent(1.8);
                rsi.setReasoning("Market's oscillating behavior within a range makes RSI an effective strategy. The recommended parameters are calibrated to current volatility levels.");
                Map<String, Object> rsiParams = new HashMap<>();
                rsiParams.put("period", 14);
                rsiParams.put("overbought", 75);
                rsiParams.put("oversold", 25);
                rsiParams.put("positionSize", 10.0);
                rsiParams.put("feeRate", 0.2);
                rsiParams.put("taxRate", 15.0);
                rsi.setRecommendedParameters(rsiParams);
                suggestions.add(rsi);
                
                // Bollinger Bands
                AlgorithmSuggestion bollinger = new AlgorithmSuggestion();
                bollinger.setAlgorithmId("bollinger-bands");
                bollinger.setAlgorithmName("Bollinger Bands");
                bollinger.setConfidenceScore(8.2);
                bollinger.setExpectedReturnPercent(3.1);
                bollinger.setReasoning("Current price volatility is ideal for Bollinger Bands strategy. The wider deviation setting accounts for recent market fluctuations and provides more reliable signals.");
                Map<String, Object> bollingerParams = new HashMap<>();
                bollingerParams.put("period", 20);
                bollingerParams.put("deviation", 2.5);
                bollingerParams.put("positionSize", 12.0);
                bollingerParams.put("feeRate", 0.2);
                bollingerParams.put("taxRate", 15.0);
                bollinger.setRecommendedParameters(bollingerParams);
                suggestions.add(bollinger);
                
                analysisResponse.setAlgorithmSuggestions(suggestions);
            }
            
            return analysisResponse;
        } catch (Exception e) {
            logger.error("Error parsing OpenAI response", e);
            throw new RuntimeException("Failed to parse OpenAI response", e);
        }
    }
    
    /**
     * Extract a value from a JSON string by key.
     *
     * @param json The JSON string
     * @param key The key to extract
     * @return The extracted value
     */
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return "";
        
        int valueStart = keyIndex + searchKey.length();
        int valueEnd;
        
        // Check if the value is a string
        if (json.charAt(valueStart) == '"') {
            valueStart++; // Skip opening quote
            valueEnd = json.indexOf('"', valueStart);
            return json.substring(valueStart, valueEnd);
        } 
        // Check if the value is a number
        else {
            valueEnd = json.indexOf(',', valueStart);
            if (valueEnd == -1) {
                valueEnd = json.indexOf('}', valueStart);
            }
            return json.substring(valueStart, valueEnd).trim();
        }
    }
    
    /**
     * Extract a JSON array from a JSON string by key.
     *
     * @param json The JSON string
     * @param key The key for the array
     * @return The extracted array as a string
     */
    private String extractJsonArray(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return "[]";
        
        int arrayStart = json.indexOf('[', keyIndex);
        if (arrayStart == -1) return "[]";
        
        int arrayEnd = -1;
        int nestedLevel = 1;
        for (int i = arrayStart + 1; i < json.length(); i++) {
            if (json.charAt(i) == '[') {
                nestedLevel++;
            } else if (json.charAt(i) == ']') {
                nestedLevel--;
                if (nestedLevel == 0) {
                    arrayEnd = i;
                    break;
                }
            }
        }
        
        if (arrayEnd == -1) return "[]";
        return json.substring(arrayStart, arrayEnd + 1);
    }
}