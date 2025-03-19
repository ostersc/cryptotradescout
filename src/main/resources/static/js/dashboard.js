/**
 * Dashboard JavaScript file for handling the dashboard functionality
 */

// Chart instance for price display
let priceChart;

// Global variables for data tracking
let marketDataHistory = [];
let lastUpdateTime = null;
let systemUptime = 0;
let uptimeInterval = null;

/**
 * Initializes the dashboard
 */
function initializeDashboard() {
    // Initialize price chart
    initializePriceChart();
    
    // Start polling for market data
    startMarketDataPolling();
    
    // Start polling for system status
    startSystemStatusPolling();
    
    // Set up refresh button
    document.getElementById('refresh-market-data').addEventListener('click', () => {
        updateMarketData();
        flashUpdateIndicator();
    });
    
    // Set up trading buttons
    document.getElementById('start-trading-btn').addEventListener('click', startTrading);
    document.getElementById('stop-trading-btn').addEventListener('click', stopTrading);
    
    // Initialize system uptime
    updateSystemUptime();
    uptimeInterval = setInterval(updateSystemUptime, 1000);
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
                label: 'Price (USD)',
                data: [],
                borderColor: 'rgb(75, 192, 192)',
                backgroundColor: 'rgba(75, 192, 192, 0.1)',
                fill: true,
                tension: 0.1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                title: {
                    display: true,
                    text: 'BTC/USD Price'
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            return formatCurrency(context.raw);
                        }
                    }
                }
            },
            scales: {
                x: {
                    title: {
                        display: true,
                        text: 'Time'
                    }
                },
                y: {
                    title: {
                        display: true,
                        text: 'Price (USD)'
                    },
                    ticks: {
                        callback: function(value) {
                            return formatCurrency(value);
                        }
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
    // Update immediately
    updateMarketData();
    
    // Then update every 5 seconds
    setInterval(updateMarketData, 5000);
}

/**
 * Starts polling for system status
 */
function startSystemStatusPolling() {
    // Update system status every 10 seconds
    setInterval(updateSystemStatus, 10000);
    
    // Update immediately
    updateSystemStatus();
}

/**
 * Updates the market data display
 */
function updateMarketData() {
    const exchangeSelector = document.getElementById('exchange-selector');
    const pairSelector = document.getElementById('trading-pair-selector');
    
    if (!exchangeSelector || !pairSelector) {
        console.error('Exchange or trading pair selector not found in the DOM');
        return;
    }
    
    const exchange = exchangeSelector.value;
    const tradingPair = pairSelector.value;
    
    console.log(`Fetching market data for ${exchange} ${tradingPair}`);
    
    fetch(`/api/trading/market-data?exchange=${exchange}&tradingPair=${tradingPair}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            updateMarketDataDisplay(data);
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
    if (!data) {
        console.error('No market data received');
        return;
    }
    
    // Store in history (limit to 100 points)
    marketDataHistory.push(data);
    if (marketDataHistory.length > 100) {
        marketDataHistory.shift();
    }
    
    // Update price display
    const lastPriceElement = document.getElementById('last-price');
    const bidPriceElement = document.getElementById('bid-price');
    const askPriceElement = document.getElementById('ask-price');
    const volumeElement = document.getElementById('volume');
    const timestampElement = document.getElementById('timestamp');
    const lastUpdateElement = document.getElementById('last-update');
    
    // Check if all required elements exist
    if (!lastPriceElement || !bidPriceElement || !askPriceElement || !volumeElement || !timestampElement) {
        console.error('One or more required DOM elements for market data display not found');
        return;
    }
    
    // Get previous values if available
    const previousLastPrice = lastPriceElement.getAttribute('data-value') ? 
        parseFloat(lastPriceElement.getAttribute('data-value')) : 0;
    const previousBidPrice = bidPriceElement.getAttribute('data-value') ? 
        parseFloat(bidPriceElement.getAttribute('data-value')) : 0;
    const previousAskPrice = askPriceElement.getAttribute('data-value') ? 
        parseFloat(askPriceElement.getAttribute('data-value')) : 0;
    
    // Update elements
    lastPriceElement.textContent = formatCurrency(data.lastPrice);
    lastPriceElement.setAttribute('data-value', data.lastPrice);
    updatePriceChangeIndicator(lastPriceElement, data.lastPrice, previousLastPrice);
    
    bidPriceElement.textContent = formatCurrency(data.bidPrice);
    bidPriceElement.setAttribute('data-value', data.bidPrice);
    updatePriceChangeIndicator(bidPriceElement, data.bidPrice, previousBidPrice);
    
    askPriceElement.textContent = formatCurrency(data.askPrice);
    askPriceElement.setAttribute('data-value', data.askPrice);
    updatePriceChangeIndicator(askPriceElement, data.askPrice, previousAskPrice);
    
    volumeElement.textContent = data.volume.toFixed(4);
    timestampElement.textContent = formatTimestamp(data.timestamp);
    
    // Update chart
    updatePriceChart(data);
    
    // Update last update time
    lastUpdateTime = new Date();
    if (lastUpdateElement) {
        lastUpdateElement.textContent = lastUpdateTime.toLocaleTimeString();
    }
    
    // Flash the update indicator
    flashUpdateIndicator();
}

/**
 * Updates the price chart with new data
 * 
 * @param {Object} data - The market data object
 */
function updatePriceChart(data) {
    if (!data || !priceChart) {
        console.error('Cannot update price chart: missing data or chart not initialized');
        return;
    }
    
    try {
        // Add new data point to chart
        const timestamp = new Date(data.timestamp).toLocaleTimeString();
        
        priceChart.data.labels.push(timestamp);
        priceChart.data.datasets[0].data.push(data.lastPrice);
        
        // Limit to 20 visible points
        if (priceChart.data.labels.length > 20) {
            priceChart.data.labels.shift();
            priceChart.data.datasets[0].data.shift();
        }
        
        // Update chart
        priceChart.update();
    } catch (error) {
        console.error('Error updating price chart:', error);
    }
}

/**
 * Updates the system status
 */
function updateSystemStatus() {
    fetch('/api/trading/system-status')
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(statusData => {
            // Update trading status
            const tradingStatus = document.getElementById('trading-status');
            const activeAlgorithm = document.getElementById('active-algorithm');
            const tradingStartTime = document.getElementById('trading-start-time');
            
            if (statusData.isActive) {
                tradingStatus.textContent = 'Active';
                tradingStatus.className = 'badge bg-success';
                activeAlgorithm.textContent = statusData.algorithm || 'Unknown';
                tradingStartTime.textContent = formatTimestamp(statusData.startTime) || 'Unknown';
                
                // Enable stop button, disable start button
                document.getElementById('stop-trading-btn').disabled = false;
                document.getElementById('start-trading-btn').disabled = true;
            } else {
                tradingStatus.textContent = 'Inactive';
                tradingStatus.className = 'badge bg-secondary';
                activeAlgorithm.textContent = 'None';
                tradingStartTime.textContent = '-';
                
                // Enable start button, disable stop button
                document.getElementById('stop-trading-btn').disabled = true;
                document.getElementById('start-trading-btn').disabled = false;
            }
            
            // Update system health
            const systemHealth = document.getElementById('system-health');
            const memoryUsage = document.getElementById('memory-usage');
            const cpuUsage = document.getElementById('cpu-usage');
            
            if (statusData.isHealthy) {
                systemHealth.textContent = 'Healthy';
                systemHealth.className = 'badge bg-success';
            } else {
                systemHealth.textContent = 'Issues Detected';
                systemHealth.className = 'badge bg-warning';
            }
            
            if (statusData.memoryUsage) {
                memoryUsage.textContent = `${statusData.memoryUsage.toFixed(1)}%`;
            }
            
            if (statusData.cpuUsage) {
                cpuUsage.textContent = `${statusData.cpuUsage.toFixed(1)}%`;
            }
        })
        .catch(error => {
            console.error('Error fetching system status:', error);
        });
}

/**
 * Starts the automated trading
 */
function startTrading() {
    // Redirect to trading tab
    document.getElementById('trading-tab').click();
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
    if (previousValue === 0) {
        return;
    }
    
    const priceChange = currentValue - previousValue;
    
    if (priceChange > 0) {
        element.classList.remove('negative-change');
        element.classList.add('positive-change');
    } else if (priceChange < 0) {
        element.classList.remove('positive-change');
        element.classList.add('negative-change');
    }
    
    // Remove classes after animation
    setTimeout(() => {
        element.classList.remove('positive-change');
        element.classList.remove('negative-change');
    }, 1000);
}

/**
 * Flashes the update indicator
 */
function flashUpdateIndicator() {
    const indicator = document.getElementById('price-update-indicator');
    
    if (indicator) {
        indicator.classList.add('flash');
        
        setTimeout(() => {
            indicator.classList.remove('flash');
        }, 1000);
    }
}

/**
 * Updates the system uptime display
 */
function updateSystemUptime() {
    systemUptime++;
    
    const hours = Math.floor(systemUptime / 3600);
    const minutes = Math.floor((systemUptime % 3600) / 60);
    const seconds = systemUptime % 60;
    
    const uptimeString = 
        `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
    
    document.getElementById('system-uptime').textContent = uptimeString;
}

/**
 * Formats a timestamp string for display
 * 
 * @param {string} timestamp - The timestamp string
 * @returns {string} The formatted timestamp
 */
function formatTimestamp(timestamp) {
    if (!timestamp) return '-';
    
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