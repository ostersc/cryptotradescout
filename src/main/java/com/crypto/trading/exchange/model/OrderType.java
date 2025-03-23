package com.crypto.trading.exchange.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enum representing different types of cryptocurrency orders.
 */
@Schema(description = "Type of cryptocurrency order", enumAsRef = true)
public enum OrderType {
    /**
     * Market order - executed immediately at the current market price.
     */
    @Schema(description = "Order executed immediately at the current market price")
    MARKET,
    
    /**
     * Limit order - executed only at the specified price or better.
     */
    @Schema(description = "Order executed only when the price reaches a specified limit or better")
    LIMIT,
    
    /**
     * Buy order - purchase of cryptocurrency.
     */
    @Schema(description = "Purchase of cryptocurrency assets")
    BUY,
    
    /**
     * Sell order - sale of cryptocurrency.
     */
    @Schema(description = "Sale of cryptocurrency assets")
    SELL
    
    // Additional order types can be added here as the system evolves
    // For example: STOP_LOSS, STOP_LIMIT, etc.
}
