package com.crypto.trading.exchange.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a cryptocurrency order with details like trading pair, type, amount, and price.
 */
public class Order {
    private String id;
    private String tradingPair;
    private OrderType type;
    private double amount;
    private double price;
    private LocalDateTime createdAt;
    private String status;
    private String exchange;

    /**
     * Default constructor.
     */
    public Order() {
    }

    /**
     * Constructor for creating a new order to be sent to an exchange.
     * 
     * @param tradingPair the trading pair (e.g., "BTC-USD")
     * @param type the order type (MARKET or LIMIT)
     * @param amount the amount of cryptocurrency to buy or sell
     * @param price the price per unit (for LIMIT orders)
     */
    public Order(String tradingPair, OrderType type, double amount, double price) {
        this.tradingPair = tradingPair;
        this.type = type;
        this.amount = amount;
        this.price = price;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Full constructor with all fields.
     * 
     * @param id the order ID (typically assigned by the exchange)
     * @param tradingPair the trading pair
     * @param type the order type
     * @param amount the amount
     * @param price the price
     * @param createdAt when the order was created
     * @param status the current status of the order
     * @param exchange the exchange where the order was placed
     */
    public Order(String id, String tradingPair, OrderType type, double amount, double price,
                LocalDateTime createdAt, String status, String exchange) {
        this.id = id;
        this.tradingPair = tradingPair;
        this.type = type;
        this.amount = amount;
        this.price = price;
        this.createdAt = createdAt;
        this.status = status;
        this.exchange = exchange;
    }

    /**
     * Get the order ID.
     * 
     * @return the order ID
     */
    public String getId() {
        return id;
    }

    /**
     * Set the order ID.
     * 
     * @param id the order ID to set
     */
    public void setId(String id) {
        this.id = id;
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
     * Get the order type.
     * 
     * @return the order type
     */
    public OrderType getType() {
        return type;
    }

    /**
     * Set the order type.
     * 
     * @param type the order type to set
     */
    public void setType(OrderType type) {
        this.type = type;
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
     * Set the amount.
     * 
     * @param amount the amount to set
     */
    public void setAmount(double amount) {
        this.amount = amount;
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
     * Set the price.
     * 
     * @param price the price to set
     */
    public void setPrice(double price) {
        this.price = price;
    }

    /**
     * Get the creation timestamp.
     * 
     * @return when the order was created
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Set the creation timestamp.
     * 
     * @param createdAt the timestamp to set
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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
     * Set the order status.
     * 
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Get the exchange name.
     * 
     * @return the exchange
     */
    public String getExchange() {
        return exchange;
    }

    /**
     * Set the exchange name.
     * 
     * @param exchange the exchange to set
     */
    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    /**
     * Calculate the total value of this order (amount * price).
     * 
     * @return the total value
     */
    public double getTotalValue() {
        return amount * price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Double.compare(order.amount, amount) == 0 &&
                Double.compare(order.price, price) == 0 &&
                Objects.equals(id, order.id) &&
                Objects.equals(tradingPair, order.tradingPair) &&
                type == order.type &&
                Objects.equals(createdAt, order.createdAt) &&
                Objects.equals(status, order.status) &&
                Objects.equals(exchange, order.exchange);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tradingPair, type, amount, price, createdAt, status, exchange);
    }

    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' +
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
