package com.crypto.trading.repository;

import com.crypto.trading.entity.OrderEntity;
import com.crypto.trading.exchange.model.Order;
import com.crypto.trading.exchange.model.OrderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for storing and retrieving orders.
 */
@Repository
public class OrderRepository {
    private static final Logger logger = LoggerFactory.getLogger(OrderRepository.class);
    
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * Constructor with JdbcTemplate.
     * 
     * @param jdbcTemplate the JDBC template
     */
    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * Save an order to the database.
     * 
     * @param order the order to save
     * @return the saved entity
     */
    public OrderEntity save(Order order) {
        String sql = "INSERT INTO orders (order_id, trading_pair, type, amount, price, " +
                "created_at, status, exchange) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        jdbcTemplate.update(sql,
                order.getId(),
                order.getTradingPair(),
                order.getType().toString(),
                order.getAmount(),
                order.getPrice(),
                Timestamp.valueOf(order.getCreatedAt()),
                order.getStatus(),
                order.getExchange());
        
        return new OrderEntity(
                0, // ID will be generated
                order.getId(),
                order.getTradingPair(),
                order.getType(),
                order.getAmount(),
                order.getPrice(),
                order.getCreatedAt(),
                order.getStatus(),
                order.getExchange());
    }
    
    /**
     * Update the status of an order.
     * 
     * @param orderId the order ID
     * @param status the new status
     * @return true if updated successfully
     */
    public boolean updateStatus(String orderId, String status) {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        
        try {
            int updated = jdbcTemplate.update(sql, status, orderId);
            return updated > 0;
        } catch (Exception e) {
            logger.error("Error updating order status for order ID {}", orderId, e);
            return false;
        }
    }
    
    /**
     * Find an order by its ID.
     * 
     * @param orderId the order ID
     * @return the order entity, or null if not found
     */
    public OrderEntity findByOrderId(String orderId) {
        String sql = "SELECT id, order_id, trading_pair, type, amount, price, created_at, status, exchange " +
                "FROM orders WHERE order_id = ?";
        
        try {
            return jdbcTemplate.queryForObject(sql, new Object[]{orderId}, this::mapRowToOrderEntity);
        } catch (Exception e) {
            logger.error("Error finding order by ID {}", orderId, e);
            return null;
        }
    }
    
    /**
     * Find orders by trading pair and exchange.
     * 
     * @param tradingPair the trading pair
     * @param exchange the exchange name
     * @return a list of order entities
     */
    public List<OrderEntity> findByTradingPairAndExchange(String tradingPair, String exchange) {
        String sql = "SELECT id, order_id, trading_pair, type, amount, price, created_at, status, exchange " +
                "FROM orders WHERE trading_pair = ? AND exchange = ? ORDER BY created_at DESC";
        
        try {
            return jdbcTemplate.query(sql, new Object[]{tradingPair, exchange}, this::mapRowToOrderEntity);
        } catch (Exception e) {
            logger.error("Error finding orders for {} on {}", tradingPair, exchange, e);
            return List.of();
        }
    }
    
    /**
     * Find orders created within a time range.
     * 
     * @param startTime the start time
     * @param endTime the end time
     * @return a list of order entities
     */
    public List<OrderEntity> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "SELECT id, order_id, trading_pair, type, amount, price, created_at, status, exchange " +
                "FROM orders WHERE created_at BETWEEN ? AND ? ORDER BY created_at";
        
        try {
            return jdbcTemplate.query(sql, 
                    new Object[]{Timestamp.valueOf(startTime), Timestamp.valueOf(endTime)},
                    this::mapRowToOrderEntity);
        } catch (Exception e) {
            logger.error("Error finding orders between {} and {}", startTime, endTime, e);
            return List.of();
        }
    }
    
    /**
     * Map a database row to an OrderEntity.
     * 
     * @param rs the result set
     * @param rowNum the row number
     * @return the mapped entity
     * @throws SQLException if an error occurs
     */
    private OrderEntity mapRowToOrderEntity(ResultSet rs, int rowNum) throws SQLException {
        return new OrderEntity(
                rs.getLong("id"),
                rs.getString("order_id"),
                rs.getString("trading_pair"),
                OrderType.valueOf(rs.getString("type")),
                rs.getDouble("amount"),
                rs.getDouble("price"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getString("status"),
                rs.getString("exchange"));
    }
    
    /**
     * Count the number of orders for a trading pair.
     * 
     * @param tradingPair the trading pair
     * @return the number of orders
     */
    public int countByTradingPair(String tradingPair) {
        String sql = "SELECT COUNT(*) FROM orders WHERE trading_pair = ?";
        
        try {
            return jdbcTemplate.queryForObject(sql, Integer.class, tradingPair);
        } catch (Exception e) {
            logger.error("Error counting orders for {}", tradingPair, e);
            return 0;
        }
    }
    
    /**
     * Delete old orders to manage database size.
     * 
     * @param daysToKeep the number of days of data to keep
     * @return the number of records deleted
     */
    public int deleteOldOrders(int daysToKeep) {
        String sql = "DELETE FROM orders WHERE created_at < ?";
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        
        try {
            int deleted = jdbcTemplate.update(sql, Timestamp.valueOf(cutoffDate));
            logger.info("Deleted {} old orders", deleted);
            return deleted;
        } catch (Exception e) {
            logger.error("Error deleting old orders", e);
            return 0;
        }
    }
}
