package com.crypto.trading.exchange.model;

import java.time.LocalDateTime;
import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents market data for a cryptocurrency trading pair.
 * This includes price information, volume, and timestamp.
 */
@Schema(
    description = "Market data for a cryptocurrency trading pair including price information and volume",
    name = "MarketData"
)
public class MarketData {
    @Schema(description = "Trading pair identifier (e.g., BTC-USD)", example = "BTC-USD")
    private String tradingPair;
    
    @Schema(description = "Highest buy price currently available", example = "83991.40")
    private double bidPrice;
    
    @Schema(description = "Lowest sell price currently available", example = "83991.50")
    private double askPrice;
    
    @Schema(description = "Price of the most recent trade", example = "83991.50")
    private double lastPrice;
    
    @Schema(description = "Trading volume in the base currency over the last 24 hours", example = "433.43")
    private double volume;
    
    @Schema(description = "Timestamp when this data was recorded", example = "2025-03-23T00:50:47")
    private LocalDateTime timestamp;
    
    @Schema(description = "Name of the exchange providing this data", example = "Kraken")
    private String exchange;

    /**
     * Default constructor.
     */
    public MarketData() {
    }

    /**
     * Full constructor with all fields.
     *
     * @param tradingPair the trading pair (e.g., "BTC-USD")
     * @param bidPrice the highest buy price
     * @param askPrice the lowest sell price
     * @param lastPrice the last traded price
     * @param volume the trading volume
     * @param timestamp the time this data was recorded
     * @param exchange the name of the exchange
     */
    public MarketData(String tradingPair, double bidPrice, double askPrice, double lastPrice,
                     double volume, LocalDateTime timestamp, String exchange) {
        this.tradingPair = tradingPair;
        this.bidPrice = bidPrice;
        this.askPrice = askPrice;
        this.lastPrice = lastPrice;
        this.volume = volume;
        this.timestamp = timestamp;
        this.exchange = exchange;
    }

    /**
     * Get the trading pair.
     *
     * @return the trading pair
     */
    public String getTradingPair() {
        return tradingPair;
    }

    /**
     * Set the trading pair.
     *
     * @param tradingPair the trading pair to set
     */
    public void setTradingPair(String tradingPair) {
        this.tradingPair = tradingPair;
    }

    /**
     * Get the bid price.
     *
     * @return the highest buy price
     */
    public double getBidPrice() {
        return bidPrice;
    }

    /**
     * Set the bid price.
     *
     * @param bidPrice the bid price to set
     */
    public void setBidPrice(double bidPrice) {
        this.bidPrice = bidPrice;
    }

    /**
     * Get the ask price.
     *
     * @return the lowest sell price
     */
    public double getAskPrice() {
        return askPrice;
    }

    /**
     * Set the ask price.
     *
     * @param askPrice the ask price to set
     */
    public void setAskPrice(double askPrice) {
        this.askPrice = askPrice;
    }

    /**
     * Get the last traded price.
     *
     * @return the last price
     */
    public double getLastPrice() {
        return lastPrice;
    }

    /**
     * Set the last traded price.
     *
     * @param lastPrice the last price to set
     */
    public void setLastPrice(double lastPrice) {
        this.lastPrice = lastPrice;
    }

    /**
     * Get the trading volume.
     *
     * @return the volume
     */
    public double getVolume() {
        return volume;
    }

    /**
     * Set the trading volume.
     *
     * @param volume the volume to set
     */
    public void setVolume(double volume) {
        this.volume = volume;
    }

    /**
     * Get the timestamp.
     *
     * @return the timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Set the timestamp.
     *
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Get the exchange name.
     *
     * @return the exchange name
     */
    public String getExchange() {
        return exchange;
    }

    /**
     * Set the exchange name.
     *
     * @param exchange the exchange name to set
     */
    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    /**
     * Calculate the spread between ask and bid prices.
     *
     * @return the spread
     */
    public double getSpread() {
        return askPrice - bidPrice;
    }

    /**
     * Calculate the spread percentage relative to the ask price.
     *
     * @return the spread percentage
     */
    public double getSpreadPercentage() {
        return (askPrice - bidPrice) / askPrice * 100;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MarketData that = (MarketData) o;
        return Double.compare(that.bidPrice, bidPrice) == 0 &&
                Double.compare(that.askPrice, askPrice) == 0 &&
                Double.compare(that.lastPrice, lastPrice) == 0 &&
                Double.compare(that.volume, volume) == 0 &&
                Objects.equals(tradingPair, that.tradingPair) &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(exchange, that.exchange);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tradingPair, bidPrice, askPrice, lastPrice, volume, timestamp, exchange);
    }

    @Override
    public String toString() {
        return "MarketData{" +
                "tradingPair='" + tradingPair + '\'' +
                ", bidPrice=" + bidPrice +
                ", askPrice=" + askPrice +
                ", lastPrice=" + lastPrice +
                ", volume=" + volume +
                ", timestamp=" + timestamp +
                ", exchange='" + exchange + '\'' +
                '}';
    }
}
