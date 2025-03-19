package com.crypto.trading.exchange.model;

/**
 * Enum representing different types of cryptocurrency orders.
 */
public enum OrderType {
    /**
     * Market order - executed immediately at the current market price.
     */
    MARKET,
    
    /**
     * Limit order - executed only at the specified price or better.
     */
    LIMIT
    
    // Additional order types can be added here as the system evolves
    // For example: STOP_LOSS, STOP_LIMIT, etc.
}
