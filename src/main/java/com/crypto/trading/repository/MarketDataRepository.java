package com.crypto.trading.repository;

import com.crypto.trading.entity.MarketDataEntity;
import com.crypto.trading.exchange.model.MarketData;
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
 * Repository for storing and retrieving market data.
 */
@Repository
public class MarketDataRepository {
    private static final Logger logger = LoggerFactory.getLogger(MarketDataRepository.class);
    
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * Constructor with JdbcTemplate.
     * 
     * @param jdbcTemplate the JDBC template
     */
    public MarketDataRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * Save market data to the database.
     * 
     * @param marketData the market data to save
     * @return the saved entity
     */
    public MarketDataEntity save(MarketData marketData) {
        String sql = "INSERT INTO market_data (trading_pair, bid_price, ask_price, last_price, " +
                "volume, timestamp, exchange) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        jdbcTemplate.update(sql,
                marketData.getTradingPair(),
                marketData.getBidPrice(),
                marketData.getAskPrice(),
                marketData.getLastPrice(),
                marketData.getVolume(),
                Timestamp.valueOf(marketData.getTimestamp()),
                marketData.getExchange());
        
        return new MarketDataEntity(
                0, // ID will be generated
                marketData.getTradingPair(),
                marketData.getBidPrice(),
                marketData.getAskPrice(),
                marketData.getLastPrice(),
                marketData.getVolume(),
                marketData.getTimestamp(),
                marketData.getExchange());
    }
    
    /**
     * Find market data by its ID.
     * 
     * @param id the market data ID
     * @return the market data entity, or null if not found
     */
    public MarketDataEntity findById(long id) {
        String sql = "SELECT id, trading_pair, bid_price, ask_price, last_price, volume, timestamp, exchange " +
                "FROM market_data WHERE id = ?";
        
        try {
            return jdbcTemplate.queryForObject(sql, new Object[]{id}, this::mapRowToMarketDataEntity);
        } catch (Exception e) {
            logger.error("Error finding market data by ID {}", id, e);
            return null;
        }
    }
    
    /**
     * Find the latest market data for a trading pair.
     * 
     * @param tradingPair the trading pair
     * @param exchange the exchange name
     * @return the latest market data entity, or null if not found
     */
    public MarketDataEntity findLatestByTradingPairAndExchange(String tradingPair, String exchange) {
        String sql = "SELECT id, trading_pair, bid_price, ask_price, last_price, volume, timestamp, exchange " +
                "FROM market_data WHERE trading_pair = ? AND exchange = ? " +
                "ORDER BY timestamp DESC LIMIT 1";
        
        try {
            return jdbcTemplate.queryForObject(sql, new Object[]{tradingPair, exchange}, this::mapRowToMarketDataEntity);
        } catch (Exception e) {
            logger.error("Error finding latest market data for {} on {}", tradingPair, exchange, e);
            return null;
        }
    }
    
    /**
     * Find historical market data for a trading pair in a time range.
     * 
     * @param tradingPair the trading pair
     * @param exchange the exchange name
     * @param startTime the start time
     * @param endTime the end time
     * @return a list of market data entities
     */
    public List<MarketDataEntity> findByTradingPairAndExchangeBetween(
            String tradingPair, String exchange, LocalDateTime startTime, LocalDateTime endTime) {
        
        String sql = "SELECT id, trading_pair, bid_price, ask_price, last_price, volume, timestamp, exchange " +
                "FROM market_data WHERE trading_pair = ? AND exchange = ? " +
                "AND timestamp BETWEEN ? AND ? ORDER BY timestamp";
        
        try {
            return jdbcTemplate.query(sql, 
                    new Object[]{tradingPair, exchange, Timestamp.valueOf(startTime), Timestamp.valueOf(endTime)},
                    this::mapRowToMarketDataEntity);
        } catch (Exception e) {
            logger.error("Error finding historical market data for {} on {} between {} and {}", 
                    tradingPair, exchange, startTime, endTime, e);
            return List.of();
        }
    }
    
    /**
     * Map a database row to a MarketDataEntity.
     * 
     * @param rs the result set
     * @param rowNum the row number
     * @return the mapped entity
     * @throws SQLException if an error occurs
     */
    private MarketDataEntity mapRowToMarketDataEntity(ResultSet rs, int rowNum) throws SQLException {
        return new MarketDataEntity(
                rs.getLong("id"),
                rs.getString("trading_pair"),
                rs.getDouble("bid_price"),
                rs.getDouble("ask_price"),
                rs.getDouble("last_price"),
                rs.getDouble("volume"),
                rs.getTimestamp("timestamp").toLocalDateTime(),
                rs.getString("exchange"));
    }
    
    /**
     * Delete old market data to manage database size.
     * 
     * @param daysToKeep the number of days of data to keep
     * @return the number of records deleted
     */
    public int deleteOldData(int daysToKeep) {
        String sql = "DELETE FROM market_data WHERE timestamp < ?";
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        
        try {
            int deleted = jdbcTemplate.update(sql, Timestamp.valueOf(cutoffDate));
            logger.info("Deleted {} old market data records", deleted);
            return deleted;
        } catch (Exception e) {
            logger.error("Error deleting old market data", e);
            return 0;
        }
    }
}
