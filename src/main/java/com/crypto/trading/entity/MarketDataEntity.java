package com.crypto.trading.entity;

import com.crypto.trading.exchange.model.MarketData;

import java.time.LocalDateTime;

/**
 * Entity class for market data stored in the database.
 */
public class MarketDataEntity {
    private final long id;
    private final String tradingPair;
    private final double bidPrice;
    private final double askPrice;
    private final double lastPrice;
    private final double volume;
    private final LocalDateTime timestamp;
    private final String exchange;
    
    /**
     * Constructor with all fields.
     * 
     * @param id the database ID
     * @param tradingPair the trading pair
     * @param bidPrice the bid price
     * @param askPrice the ask price
     * @param lastPrice the last traded price
     * @param volume the trading volume
     * @param timestamp the timestamp
     * @param exchange the exchange name
     */
    public MarketDataEntity(long id, String tradingPair, double bidPrice, double askPrice,
                          double lastPrice, double volume, LocalDateTime timestamp, String exchange) {
        this.id = id;
        this.tradingPair = tradingPair;
        this.bidPrice = bidPrice;
        this.askPrice = askPrice;
        this.lastPrice = lastPrice;
        this.volume = volume;
        this.timestamp = timestamp;
        this.exchange = exchange;
    }
    
    /**
     * Get the database ID.
     * 
     * @return the ID
     */
    public long getId() {
        return id;
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
     * Get the bid price.
     * 
     * @return the bid price
     */
    public double getBidPrice() {
        return bidPrice;
    }
    
    /**
     * Get the ask price.
     * 
     * @return the ask price
     */
    public double getAskPrice() {
        return askPrice;
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
     * Get the trading volume.
     * 
     * @return the volume
     */
    public double getVolume() {
        return volume;
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
     * Get the exchange name.
     * 
     * @return the exchange name
     */
    public String getExchange() {
        return exchange;
    }
    
    /**
     * Convert this entity to a domain model object.
     * 
     * @return the MarketData domain object
     */
    public MarketData toDomainModel() {
        return new MarketData(
                tradingPair,
                bidPrice,
                askPrice,
                lastPrice,
                volume,
                timestamp,
                exchange
        );
    }
    
    /**
     * Create an entity from a domain model object.
     * 
     * @param marketData the domain model object
     * @return the entity
     */
    public static MarketDataEntity fromDomainModel(MarketData marketData) {
        return new MarketDataEntity(
                0, // ID will be assigned by the database
                marketData.getTradingPair(),
                marketData.getBidPrice(),
                marketData.getAskPrice(),
                marketData.getLastPrice(),
                marketData.getVolume(),
                marketData.getTimestamp(),
                marketData.getExchange()
        );
    }
    
    @Override
    public String toString() {
        return "MarketDataEntity{" +
                "id=" + id +
                ", tradingPair='" + tradingPair + '\'' +
                ", bidPrice=" + bidPrice +
                ", askPrice=" + askPrice +
                ", lastPrice=" + lastPrice +
                ", volume=" + volume +
                ", timestamp=" + timestamp +
                ", exchange='" + exchange + '\'' +
                '}';
    }
}
