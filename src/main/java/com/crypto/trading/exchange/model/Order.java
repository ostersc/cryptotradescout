package com.crypto.trading.exchange.model;

import java.time.LocalDateTime;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a cryptocurrency order with details like trading pair, type, amount, and price.
 * Enhanced with fee and tax tracking capabilities.
 */
@Schema(
    description = "Cryptocurrency order with fee and tax tracking capability",
    name = "Order"
)
public class Order {
    @Schema(description = "Unique identifier for the order", example = "ord-123456")
    private String id;
    
    @Schema(description = "Trading pair for the order", example = "BTC-USD")
    private String tradingPair;
    
    @Schema(description = "Type of order (BUY, SELL)", example = "BUY")
    private OrderType type;
    
    @Schema(description = "Amount of cryptocurrency to buy or sell", example = "0.5")
    private double amount;
    
    @Schema(description = "Price per unit of cryptocurrency", example = "83991.50")
    private double price;
    
    @Schema(description = "Timestamp when the order was created", example = "2025-03-23T00:51:57")
    private LocalDateTime createdAt;
    
    @Schema(description = "Current status of the order", example = "FILLED")
    private String status;
    
    @Schema(description = "Exchange where the order was placed", example = "Kraken")
    private String exchange;
    
    @Schema(description = "Total portfolio value at the time of this order", example = "100000.00")
    private Double totalValue; // Portfolio value at the time of the order, used for backtesting
    
    @JsonProperty("fee")
    @Schema(description = "Exchange fee for this transaction", example = "83.99")
    private double fee; // Exchange fee for the transaction
    
    @JsonProperty("tax")
    @Schema(description = "Estimated tax liability for this transaction", example = "1259.87")
    private double tax; // Estimated tax liability for the transaction
    
    @JsonProperty("feeRate")
    @Schema(description = "Fee rate applied to this order (e.g., 0.002 for 0.2%)", example = "0.002")
    private double feeRate; // Fee rate applied to this order (e.g., 0.001 for 0.1%)
    
    @JsonProperty("taxRate")
    @Schema(description = "Tax rate applied to this order", example = "0.15")
    private double taxRate; // Tax rate applied to this order
    
    @JsonProperty("taxableGain")
    @Schema(description = "Total taxable gain for this transaction", example = "8399.15")
    private double taxableGain; // Total taxable gain for this transaction
    
    @JsonProperty("shortTermGain")
    @Schema(description = "Short-term capital gain (held < 1 year)", example = "8399.15")
    private double shortTermGain; // Short-term capital gain (held < 1 year)
    
    @JsonProperty("longTermGain")
    @Schema(description = "Long-term capital gain (held >= 1 year)", example = "0.00")
    private double longTermGain; // Long-term capital gain (held >= 1 year)

    /**
     * Default constructor.
     */
    public Order() {
        this.status = "FILLED"; // Default status for backtesting
        this.fee = 0.0;
        this.tax = 0.0;
        this.feeRate = 0.0;
        this.taxRate = 0.0;
        this.feeAsset = null;
        this.estimatedTaxLiability = 0.0;
        this.taxableGain = 0.0;
        this.shortTermGain = 0.0;
        this.longTermGain = 0.0;
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
        this.status = "FILLED"; // Default status for backtesting
        this.fee = 0.0;
        this.tax = 0.0;
        this.feeRate = 0.0;
        this.taxRate = 0.0;
        this.feeAsset = null;
        this.estimatedTaxLiability = 0.0;
        this.taxableGain = 0.0;
        this.shortTermGain = 0.0;
        this.longTermGain = 0.0;
    }

    /**
     * Constructor with fee rate included.
     * 
     * @param tradingPair the trading pair (e.g., "BTC-USD")
     * @param type the order type (MARKET or LIMIT)
     * @param amount the amount of cryptocurrency to buy or sell
     * @param price the price per unit (for LIMIT orders)
     * @param feeRate the fee rate to apply (e.g., 0.001 for 0.1%)
     * @param taxRate the tax rate to apply
     */
    public Order(String tradingPair, OrderType type, double amount, double price, double feeRate, double taxRate) {
        this.tradingPair = tradingPair;
        this.type = type;
        this.amount = amount;
        this.price = price;
        this.createdAt = LocalDateTime.now();
        this.status = "FILLED"; // Default status for backtesting
        this.feeRate = feeRate;
        this.taxRate = taxRate;
        this.fee = calculateFee();
        this.tax = calculateTax();
        this.feeAsset = null;
        this.estimatedTaxLiability = this.tax; // For consistency
        this.taxableGain = 0.0;
        this.shortTermGain = 0.0;
        this.longTermGain = 0.0;
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
        this.fee = 0.0;
        this.tax = 0.0;
        this.feeRate = 0.0;
        this.taxRate = 0.0;
        this.feeAsset = null;
        this.estimatedTaxLiability = 0.0;
        this.taxableGain = 0.0;
        this.shortTermGain = 0.0;
        this.longTermGain = 0.0;
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
     * Get the total portfolio value at the time of this order.
     * 
     * @return the total portfolio value
     */
    public Double getTotalValue() {
        if (totalValue != null) {
            return totalValue;
        }
        // Default to calculating the order value if no portfolio value is set
        return amount * price;
    }
    
    /**
     * Set the total portfolio value at the time of this order.
     * This is used primarily for backtesting to track equity changes.
     * 
     * @param totalValue the total portfolio value to set
     */
    public void setTotalValue(Double totalValue) {
        this.totalValue = totalValue;
    }
    
    /**
     * Get the fee for this order.
     * 
     * @return the fee amount
     */
    public double getFee() {
        return fee;
    }
    
    /**
     * Set the fee for this order.
     * 
     * @param fee the fee to set
     */
    public void setFee(double fee) {
        this.fee = fee;
    }
    
    /**
     * Set the fee amount for this order (alias for setFee for compatibility).
     * 
     * @param feeAmount the fee amount to set
     */
    public void setFeeAmount(double feeAmount) {
        this.fee = feeAmount;
    }
    
    /**
     * The currency/asset in which the fee is denominated.
     */
    @JsonProperty("feeAsset")
    @Schema(description = "Currency/asset in which the fee is denominated", example = "USD")
    private String feeAsset;
    
    /**
     * Get the fee asset (currency) for this order.
     * 
     * @return the fee asset
     */
    public String getFeeAsset() {
        return feeAsset;
    }
    
    /**
     * Set the fee asset (currency) for this order.
     * 
     * @param feeAsset the fee asset to set
     */
    public void setFeeAsset(String feeAsset) {
        this.feeAsset = feeAsset;
    }
    
    /**
     * Estimated tax liability for this transaction.
     */
    @JsonProperty("estimatedTaxLiability")
    @Schema(description = "Estimated tax liability for this transaction", example = "1259.87")
    private double estimatedTaxLiability;
    
    /**
     * Get the estimated tax liability for this order.
     * 
     * @return the estimated tax liability
     */
    public double getEstimatedTaxLiability() {
        return estimatedTaxLiability;
    }
    
    /**
     * Set the estimated tax liability for this order.
     * 
     * @param estimatedTaxLiability the estimated tax liability to set
     */
    public void setEstimatedTaxLiability(double estimatedTaxLiability) {
        this.estimatedTaxLiability = estimatedTaxLiability;
    }
    
    /**
     * Get the tax for this order.
     * 
     * @return the tax amount
     */
    public double getTax() {
        return tax;
    }
    
    /**
     * Set the tax for this order.
     * 
     * @param tax the tax to set
     */
    public void setTax(double tax) {
        this.tax = tax;
    }
    
    /**
     * Get the fee rate for this order.
     * 
     * @return the fee rate (e.g., 0.001 for 0.1%)
     */
    public double getFeeRate() {
        return feeRate;
    }
    
    /**
     * Set the fee rate for this order.
     * 
     * @param feeRate the fee rate to set
     */
    public void setFeeRate(double feeRate) {
        this.feeRate = feeRate;
        this.fee = calculateFee(); // Recalculate fee based on the new rate
    }
    
    /**
     * Get the tax rate for this order.
     * 
     * @return the tax rate
     */
    public double getTaxRate() {
        return taxRate;
    }
    
    /**
     * Set the tax rate for this order.
     * 
     * @param taxRate the tax rate to set
     */
    public void setTaxRate(double taxRate) {
        this.taxRate = taxRate;
        this.tax = calculateTax(); // Recalculate tax based on the new rate
    }
    
    /**
     * Calculate the fee for this order based on the fee rate.
     * 
     * @return the calculated fee
     */
    public double calculateFee() {
        return amount * price * feeRate;
    }
    
    /**
     * Calculate the tax for this order based on the tax rate.
     * This is a simplified calculation and may not reflect actual tax liabilities.
     * 
     * @return the calculated tax
     */
    public double calculateTax() {
        // Simplified tax calculation - in reality tax would depend on gain/loss
        // which requires knowing the cost basis
        return amount * price * taxRate;
    }
    
    /**
     * Get the total order value including fees.
     * 
     * @return the total order value
     */
    public double getTotalOrderValue() {
        double baseValue = amount * price;
        return type == OrderType.BUY ? baseValue + fee : baseValue - fee;
    }
    
    /**
     * Get the total cost including fees and taxes.
     * 
     * @return the total cost
     */
    public double getTotalCost() {
        return getTotalOrderValue() + tax;
    }
    
    /**
     * Get the total taxable gain for this transaction.
     * 
     * @return the taxable gain
     */
    public double getTaxableGain() {
        return taxableGain;
    }
    
    /**
     * Set the total taxable gain for this transaction.
     * 
     * @param taxableGain the taxable gain to set
     */
    public void setTaxableGain(double taxableGain) {
        this.taxableGain = taxableGain;
    }
    
    /**
     * Get the short-term capital gain (assets held < 1 year).
     * 
     * @return the short-term gain
     */
    public double getShortTermGain() {
        return shortTermGain;
    }
    
    /**
     * Set the short-term capital gain.
     * 
     * @param shortTermGain the short-term gain to set
     */
    public void setShortTermGain(double shortTermGain) {
        this.shortTermGain = shortTermGain;
    }
    
    /**
     * Get the long-term capital gain (assets held >= 1 year).
     * 
     * @return the long-term gain
     */
    public double getLongTermGain() {
        return longTermGain;
    }
    
    /**
     * Set the long-term capital gain.
     * 
     * @param longTermGain the long-term gain to set
     */
    public void setLongTermGain(double longTermGain) {
        this.longTermGain = longTermGain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Double.compare(order.amount, amount) == 0 &&
                Double.compare(order.price, price) == 0 &&
                Double.compare(order.fee, fee) == 0 &&
                Double.compare(order.tax, tax) == 0 &&
                Double.compare(order.feeRate, feeRate) == 0 &&
                Double.compare(order.taxRate, taxRate) == 0 &&
                Double.compare(order.estimatedTaxLiability, estimatedTaxLiability) == 0 &&
                Double.compare(order.taxableGain, taxableGain) == 0 &&
                Double.compare(order.shortTermGain, shortTermGain) == 0 &&
                Double.compare(order.longTermGain, longTermGain) == 0 &&
                Objects.equals(id, order.id) &&
                Objects.equals(tradingPair, order.tradingPair) &&
                type == order.type &&
                Objects.equals(createdAt, order.createdAt) &&
                Objects.equals(status, order.status) &&
                Objects.equals(exchange, order.exchange) &&
                Objects.equals(totalValue, order.totalValue) &&
                Objects.equals(feeAsset, order.feeAsset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tradingPair, type, amount, price, createdAt, status, exchange, totalValue, 
                           fee, tax, feeRate, taxRate, feeAsset, estimatedTaxLiability, 
                           taxableGain, shortTermGain, longTermGain);
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
                ", totalValue=" + totalValue +
                ", fee=" + fee +
                ", tax=" + tax +
                ", feeRate=" + feeRate +
                ", taxRate=" + taxRate +
                ", feeAsset='" + feeAsset + '\'' +
                ", estimatedTaxLiability=" + estimatedTaxLiability +
                ", taxableGain=" + taxableGain +
                ", shortTermGain=" + shortTermGain +
                ", longTermGain=" + longTermGain +
                '}';
    }
}
