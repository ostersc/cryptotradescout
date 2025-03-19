package com.crypto.trading.entity;

import com.crypto.trading.exchange.model.Order;
import com.crypto.trading.exchange.model.OrderType;

import java.time.LocalDateTime;

/**
 * Entity class for orders stored in the database.
 */
public class OrderEntity {
    private final long id;
    private final String orderId;
    private final String tradingPair;
    private final OrderType type;
    private final double amount;
    private final double price;
    private final LocalDateTime createdAt;
    private final String status;
    private final String exchange;
    
    /**
     * Constructor with all fields.
     * 
     * @param id the database ID
     * @param orderId the order ID (from the exchange)
     * @param tradingPair the trading pair
     * @param type the order type
     * @param amount the amount
     * @param price the price
     * @param createdAt the creation timestamp
     * @param status the order status
     * @param exchange the exchange name
     */
    public OrderEntity(long id, String orderId, String tradingPair, OrderType type,
                     double amount, double price, LocalDateTime createdAt,
                     String status, String exchange) {
        this.id = id;
        this.orderId = orderId;
        this.tradingPair = tradingPair;
        this.type = type;
        this.amount = amount;
        this.price = price;
        this.createdAt = createdAt;
        this.status = status;
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
     * Get the order ID (from the exchange).
     * 
     * @return the order ID
     */
    public String getOrderId() {
        return orderId;
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
     * Get the order type.
     * 
     * @return the order type
     */
    public OrderType getType() {
        return type;
    }
    
    /**
     * Get the amount.
     * 
     * @return the amount
     */
    public double getAmount() {
        return amount;
    }
    
    /**
     * Get the price.
     * 
     * @return the price
     */
    public double getPrice() {
        return price;
    }
    
    /**
     * Get the creation timestamp.
     * 
     * @return the creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Get the order status.
     * 
     * @return the status
     */
    public String getStatus() {
        return status;
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
     * Calculate the total value of this order (amount * price).
     * 
     * @return the total value
     */
    public double getTotalValue() {
        return amount * price;
    }
    
    /**
     * Convert this entity to a domain model object.
     * 
     * @return the Order domain object
     */
    public Order toDomainModel() {
        return new Order(
                orderId,
                tradingPair,
                type,
                amount,
                price,
                createdAt,
                status,
                exchange
        );
    }
    
    /**
     * Create an entity from a domain model object.
     * 
     * @param order the domain model object
     * @return the entity
     */
    public static OrderEntity fromDomainModel(Order order) {
        return new OrderEntity(
                0, // ID will be assigned by the database
                order.getId(),
                order.getTradingPair(),
                order.getType(),
                order.getAmount(),
                order.getPrice(),
                order.getCreatedAt(),
                order.getStatus(),
                order.getExchange()
        );
    }
    
    @Override
    public String toString() {
        return "OrderEntity{" +
                "id=" + id +
                ", orderId='" + orderId + '\'' +
                ", tradingPair='" + tradingPair + '\'' +
                ", type=" + type +
                ", amount=" + amount +
                ", price=" + price +
                ", createdAt=" + createdAt +
                ", status='" + status + '\'' +
                ", exchange='" + exchange + '\'' +
                '}';
    }
}
