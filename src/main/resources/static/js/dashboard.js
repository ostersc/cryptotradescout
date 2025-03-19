/**
 * Dashboard JavaScript file for handling the market data display and price chart
 */

// Chart instance
let priceChart;
// Price history for the chart
let priceHistory = [];
// Last price values to detect changes
let lastValues = {
    bid: null,
    ask: null,
    last: null
};
// Market data polling interval
let marketDataInterval;
// System status polling interval
let systemStatusInterval;

/**
 * Initializes the dashboard
 */
function initializeDashboard() {
    // Setup the price chart
    initializePriceChart();
    
    // Add event listeners for exchange and trading pair selectors
    document.getElementById('exchange-selector').addEventListener('change', updateMarketData);
    document.getElementById('trading-pair-selector').addEventListener('change', updateMarketData);
    
    // Add event listeners for system control buttons
    document.getElementById('start-trading-btn').addEventListener('click', startTrading);
    document.getElementById('stop-trading-btn').addEventListener('click', stopTrading);
    
    // Start polling for market data
    startMarketDataPolling();
    
    // Start polling for system status
    startSystemStatusPolling();
}

/**
 * Initializes the price chart
 */
function initializePriceChart() {
    const ctx = document.getElementById('price-chart').getContext('2d');
    
    priceChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: 'Price',
                data: [],
                borderColor: 'rgb(75, 192, 192)',
                tension: 0.1,
                pointRadius: 0
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    intersect: false,
                    mode: 'index'
                }
            },
            scales: {
                x: {
                    display: true,
                    title: {
                        display: false
                    }
                },
                y: {
                    display: true,
                    title: {
                        display: false
                    }
                }
            }
        }
    });
}

/**
 * Starts polling for market data
 */
function startMarketDataPolling() {
    // Clear any existing interval
    if (marketDataInterval) {
        clearInterval(marketDataInterval);
    }
    
    // Initial update
    updateMarketData();
    
    // Set up polling every 5 seconds
    marketDataInterval = setInterval(updateMarketData, 5000);
}

/**
 * Starts polling for system status
 */
function startSystemStatusPolling() {
    // Clear any existing interval
    if (systemStatusInterval) {
        clearInterval(systemStatusInterval);
    }
    
    // Initial update
    updateSystemStatus();
    
    // Set up polling every 10 seconds
    systemStatusInterval = setInterval(updateSystemStatus, 10000);
}

/**
 * Updates the market data display
 */
function updateMarketData() {
    const exchange = document.getElementById('exchange-selector').value;
    const tradingPair = document.getElementById('trading-pair-selector').value;
    
    fetch(`/api/trading/market-data?exchange=${exchange}&tradingPair=${tradingPair}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            updateMarketDataDisplay(data);
            updatePriceChart(data);
        })
        .catch(error => {
            console.error('Error fetching market data:', error);
        });
}

/**
 * Updates the market data display with the fetched data
 * 
 * @param {Object} data - The market data object
 */
function updateMarketDataDisplay(data) {
    // Update price values
    const bidPrice = document.getElementById('bid-price');
    const askPrice = document.getElementById('ask-price');
    const lastPrice = document.getElementById('last-price');
    
    // Format prices
    const formattedBid = formatCurrency(data.bidPrice);
    const formattedAsk = formatCurrency(data.askPrice);
    const formattedLast = formatCurrency(data.lastPrice);
    
    // Update DOM elements
    bidPrice.textContent = formattedBid;
    askPrice.textContent = formattedAsk;
    lastPrice.textContent = formattedLast;
    
    // Apply up/down classes for price change indication
    updatePriceChangeIndicator(bidPrice, data.bidPrice, lastValues.bid);
    updatePriceChangeIndicator(askPrice, data.askPrice, lastValues.ask);
    updatePriceChangeIndicator(lastPrice, data.lastPrice, lastValues.last);
    
    // Update last values
    lastValues.bid = data.bidPrice;
    lastValues.ask = data.askPrice;
    lastValues.last = data.lastPrice;
    
    // Update additional information
    document.getElementById('volume').textContent = data.volume.toFixed(4);
    document.getElementById('spread').textContent = `${formatCurrency(data.spread)} (${(data.spreadPercentage * 100).toFixed(4)}%)`;
    document.getElementById('timestamp').textContent = formatTimestamp(data.timestamp);
    
    // Flash the update indicator
    flashUpdateIndicator();
}

/**
 * Updates the price chart with new data
 * 
 * @param {Object} data - The market data object
 */
function updatePriceChart(data) {
    const timestamp = new Date(data.timestamp);
    const formattedTime = timestamp.toLocaleTimeString();
    
    // Add the new data point
    priceHistory.push({
        time: formattedTime,
        price: data.lastPrice
    });
    
    // Keep only the last 20 data points
    if (priceHistory.length > 20) {
        priceHistory.shift();
    }
    
    // Update chart data
    priceChart.data.labels = priceHistory.map(item => item.time);
    priceChart.data.datasets[0].data = priceHistory.map(item => item.price);
    
    // Update the chart
    priceChart.update();
}

/**
 * Updates the system status
 */
function updateSystemStatus() {
    fetch('/api/trading/system-control', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ command: 'status' })
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    })
    .then(data => {
        const tradingStatus = document.getElementById('trading-status');
        
        if (data.active) {
            tradingStatus.textContent = 'Active';
            tradingStatus.className = 'badge bg-success';
        } else {
            tradingStatus.textContent = 'Inactive';
            tradingStatus.className = 'badge bg-secondary';
        }
        
        // For demo purposes, update the system uptime
        updateSystemUptime();
    })
    .catch(error => {
        console.error('Error fetching system status:', error);
    });
}

/**
 * Starts the automated trading
 */
function startTrading() {
    fetch('/api/trading/system-control', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ command: 'start' })
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    })
    .then(data => {
        alert(data.message || 'Trading started successfully');
        updateSystemStatus();
    })
    .catch(error => {
        console.error('Error starting trading:', error);
        alert('Failed to start trading. See console for details.');
    });
}

/**
 * Stops the automated trading
 */
function stopTrading() {
    fetch('/api/trading/system-control', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ command: 'stop' })
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    })
    .then(data => {
        alert(data.message || 'Trading stopped successfully');
        updateSystemStatus();
    })
    .catch(error => {
        console.error('Error stopping trading:', error);
        alert('Failed to stop trading. See console for details.');
    });
}

/**
 * Updates the price change indicator
 * 
 * @param {HTMLElement} element - The element to update
 * @param {number} currentValue - The current price value
 * @param {number} previousValue - The previous price value
 */
function updatePriceChangeIndicator(element, currentValue, previousValue) {
    // Remove existing classes
    element.classList.remove('up', 'down');
    
    // If there's a previous value, compare and add appropriate class
    if (previousValue !== null) {
        if (currentValue > previousValue) {
            element.classList.add('up');
        } else if (currentValue < previousValue) {
            element.classList.add('down');
        }
    }
}

/**
 * Flashes the update indicator
 */
function flashUpdateIndicator() {
    const indicator = document.getElementById('price-update-indicator');
    
    // Add flash class
    indicator.classList.add('flash');
    
    // Remove flash class after animation
    setTimeout(() => {
        indicator.classList.remove('flash');
    }, 1000);
}

/**
 * Updates the system uptime display
 */
function updateSystemUptime() {
    // For demo purposes, this is just a placeholder
    // In a real application, this would come from the server
    const startTime = new Date();
    startTime.setHours(startTime.getHours() - Math.floor(Math.random() * 24));
    
    const now = new Date();
    const diff = now - startTime;
    
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    
    document.getElementById('system-uptime').textContent = `${days}d ${hours}h ${minutes}m`;
}

/**
 * Formats a timestamp string for display
 * 
 * @param {string} timestamp - The timestamp string
 * @returns {string} The formatted timestamp
 */
function formatTimestamp(timestamp) {
    const date = new Date(timestamp);
    return date.toLocaleString();
}

/**
 * Formats a number as currency
 * 
 * @param {number} value - The value to format
 * @returns {string} The formatted currency string
 */
function formatCurrency(value) {
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD'
    }).format(value);
}