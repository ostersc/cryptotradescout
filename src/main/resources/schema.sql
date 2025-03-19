-- Schema for the trading system database

-- Market data table
CREATE TABLE IF NOT EXISTS market_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trading_pair VARCHAR(20) NOT NULL,
    bid_price DECIMAL(20, 8) NOT NULL,
    ask_price DECIMAL(20, 8) NOT NULL,
    last_price DECIMAL(20, 8) NOT NULL,
    volume DECIMAL(20, 8) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    exchange VARCHAR(50) NOT NULL
);

-- Create indexes for market data table
CREATE INDEX IF NOT EXISTS idx_market_data_pair_exchange ON market_data(trading_pair, exchange);
CREATE INDEX IF NOT EXISTS idx_market_data_timestamp ON market_data(timestamp);

-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(100) NOT NULL,
    trading_pair VARCHAR(20) NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(20, 8) NOT NULL,
    price DECIMAL(20, 8) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    exchange VARCHAR(50) NOT NULL,
    
    -- Unique constraint on order_id
    CONSTRAINT uk_order_id UNIQUE (order_id)
);

-- Create indexes for orders table
CREATE INDEX IF NOT EXISTS idx_orders_pair_exchange ON orders(trading_pair, exchange);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);

-- Algorithm configuration table
CREATE TABLE IF NOT EXISTS algorithm_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    algorithm_id VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    trading_pair VARCHAR(20) NOT NULL,
    exchange VARCHAR(50) NOT NULL,
    parameters CLOB,
    is_active BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    
    -- Unique constraint on algorithm_id, trading_pair, exchange
    CONSTRAINT uk_algorithm_config UNIQUE (algorithm_id, trading_pair, exchange)
);

-- Backtest results table
CREATE TABLE IF NOT EXISTS backtest_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    algorithm_id VARCHAR(100) NOT NULL,
    trading_pair VARCHAR(20) NOT NULL,
    exchange VARCHAR(50) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    initial_capital DECIMAL(20, 8) NOT NULL,
    final_capital DECIMAL(20, 8) NOT NULL,
    total_return_percentage DECIMAL(10, 4) NOT NULL,
    number_of_trades INT NOT NULL,
    max_drawdown_percentage DECIMAL(10, 4) NOT NULL,
    sharpe_ratio DECIMAL(10, 4),
    execution_time_ms BIGINT NOT NULL,
    parameters CLOB,
    created_at TIMESTAMP NOT NULL
);
