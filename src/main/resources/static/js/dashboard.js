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
 * Checks if API endpoints are available with improved error handling
 * @returns {Promise<{available: boolean, message: string}>} Object with availability status and message
 */
async function checkApiAvailability() {
    try {
        console.log('Checking API availability...');
        
        // Set a timeout for the fetch request
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 5000);
        
        const response = await fetch('/api/trading/system-status', {
            signal: controller.signal
        });
        
        // Clear the timeout
        clearTimeout(timeoutId);
        
        if (!response.ok) {
            const errorMsg = `API status check failed: ${response.status} ${response.statusText}`;
            console.error(errorMsg);
            
            let userMessage = 'Server error. ';
            
            // Provide more specific messages based on status code
            if (response.status === 401 || response.status === 403) {
                userMessage += 'Authentication required.';
            } else if (response.status === 404) {
                userMessage += 'Service endpoint not found.';
            } else if (response.status >= 500) {
                userMessage += 'Trading server unavailable.';
            }
            
            return { 
                available: false, 
                message: userMessage 
            };
        }
        
        console.log('API is available');
        return { 
            available: true, 
            message: 'All systems operational'
        };
    } catch (error) {
        let errorMessage = 'API availability check failed';
        
        // Provide clearer error messages based on error type
        if (error.name === 'AbortError') {
            errorMessage = 'API request timed out';
        } else if (error.message.includes('NetworkError')) {
            errorMessage = 'Network connection issue';
        }
        
        console.error(`${errorMessage}:`, error);
        
        return { 
            available: false, 
            message: errorMessage
        };
    }
}

/**
 * Initializes the dashboard
 */
function initializeDashboard() {
    try {
        console.log('Initializing dashboard...');
        
        // Check API availability with the improved implementation
        checkApiAvailability().then(result => {
            // Display API status to the user
            const statusElement = document.getElementById('trading-status');
            
            if (!result.available) {
                console.warn(`API is not available: ${result.message}`);
                
                // Update the status display with the specific error message
                if (statusElement) {
                    statusElement.textContent = 'API: ' + result.message;
                    statusElement.className = 'badge bg-danger';
                }
                
                // Also update system health indicator if it exists
                const healthElement = document.getElementById('system-health');
                if (healthElement) {
                    healthElement.textContent = 'Connection Issue';
                    healthElement.className = 'badge bg-danger';
                }
                
                // Show a user-friendly message in the dashboard area
                const marketDataContent = document.getElementById('market-data-content');
                if (marketDataContent) {
                    marketDataContent.innerHTML = `
                        <div class="alert alert-warning">
                            <h5>Trading API Connection Issue</h5>
                            <p>${result.message}</p>
                            <button id="retry-api-connection" class="btn btn-sm btn-primary">Retry Connection</button>
                        </div>
                    `;
                    
                    // Add retry functionality
                    const retryButton = document.getElementById('retry-api-connection');
                    if (retryButton) {
                        retryButton.addEventListener('click', () => {
                            updateMarketData();
                            flashUpdateIndicator();
                        });
                    }
                }
            } else {
                // Update with success message
                if (statusElement) {
                    statusElement.textContent = 'API Connected';
                    statusElement.className = 'badge bg-success';
                }
            }
        }).catch(error => {
            console.error('Error checking API availability:', error);
            
            // Handle the error by updating UI
            const statusElement = document.getElementById('trading-status');
            if (statusElement) {
                statusElement.textContent = 'API Error';
                statusElement.className = 'badge bg-danger';
            }
        });
        
        // Initialize event listeners for exchange and trading pair selectors
        const exchangeSelector = document.getElementById('exchange-selector');
        const tradingPairSelector = document.getElementById('trading-pair-selector');
        
        if (exchangeSelector && tradingPairSelector) {
            // Ensure we have options in the selectors
            if (exchangeSelector.options.length === 0) {
                const exchanges = ['Coinbase', 'Kraken'];
                exchanges.forEach(exchange => {
                    const option = document.createElement('option');
                    option.value = exchange.toLowerCase();
                    option.textContent = exchange;
                    exchangeSelector.appendChild(option);
                });
            }
            
            if (tradingPairSelector.options.length === 0) {
                const pairs = ['BTC/USD', 'ETH/USD', 'SOL/USD', 'AVAX/USD'];
                pairs.forEach(pair => {
                    const option = document.createElement('option');
                    option.value = pair;
                    option.textContent = pair;
                    tradingPairSelector.appendChild(option);
                });
            }
            
            // Set up event listeners
            exchangeSelector.addEventListener('change', updateMarketData);
            tradingPairSelector.addEventListener('change', updateMarketData);
        }
        
        // Initialize price chart
        initializePriceChart();
        
        // Start polling for market data
        startMarketDataPolling();
        
        // Start polling for system status
        startSystemStatusPolling();
        
        // Set up refresh button
        const refreshButton = document.getElementById('refresh-market-data');
        if (refreshButton) {
            refreshButton.addEventListener('click', () => {
                updateMarketData();
                flashUpdateIndicator();
            });
            console.log('Refresh button initialized');
        } else {
            console.warn('Refresh market data button not found');
        }
        
        // Set up trading buttons
        const startTradingBtn = document.getElementById('start-trading-btn');
        const stopTradingBtn = document.getElementById('stop-trading-btn');
        
        if (startTradingBtn) {
            startTradingBtn.addEventListener('click', startTrading);
        } else {
            console.warn('Start trading button not found');
        }
        
        if (stopTradingBtn) {
            stopTradingBtn.addEventListener('click', stopTrading);
        } else {
            console.warn('Stop trading button not found');
        }
        
        // Initialize system uptime
        updateSystemUptime();
        uptimeInterval = setInterval(updateSystemUptime, 1000);
        
        console.log('Dashboard initialization complete');
    } catch (error) {
        console.error('Error initializing dashboard:', error);
    }
}

/**
 * Initializes the price chart
 */
function initializePriceChart() {
    try {
        const chartCanvas = document.getElementById('price-chart');
        
        if (!chartCanvas) {
            console.error('Price chart canvas element not found');
            return;
        }
        
        const ctx = chartCanvas.getContext('2d');
        
        if (!ctx) {
            console.error('Failed to get 2D context from canvas');
            return;
        }
        
        // Get trading pair from UI
        const pairSelector = document.getElementById('trading-pair-selector');
        const exchangeSelector = document.getElementById('exchange-selector');
        const currentPair = pairSelector ? pairSelector.value : 'BTC-USD';
        const currentExchange = exchangeSelector ? exchangeSelector.value : 'Kraken';
        
        // Create chart with dynamic scaling
        priceChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: [],
                datasets: [{
                    label: `${currentPair} (${currentExchange})`,
                    data: [],
                    borderColor: 'rgb(75, 192, 192)',
                    backgroundColor: 'rgba(75, 192, 192, 0.1)',
                    fill: true,
                    tension: 0.1,
                    pointRadius: 2 // Smaller points for better visuals
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animation: false, // Disable animations
                transitions: {
                    active: {
                        animation: {
                            duration: 0 // No animation during interactions
                        }
                    }
                },
                elements: {
                    line: {
                        tension: 0.1 // Slight curve for better visuals
                    }
                },
                interaction: {
                    mode: 'nearest',
                    intersect: false // Easier tooltips
                },
                plugins: {
                    title: {
                        display: true,
                        text: `${currentPair} Price (${currentExchange})`
                    },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                return formatCurrency(context.raw);
                            }
                        }
                    },
                    legend: {
                        display: false // Hide legend to save space
                    }
                },
                scales: {
                    x: {
                        title: {
                            display: true,
                            text: 'Time (Local)'
                        },
                        ticks: {
                            autoSkip: true,
                            maxRotation: 0 // Horizontal labels
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
                            },
                            precision: 0, // Whole dollar values
                            autoSkip: false
                        },
                        // Start with an empty dataset - scale will be set when data comes in
                        beginAtZero: false,
                        grace: 0
                    }
                }
            }
        });
        
        console.log('Price chart initialized successfully');
    } catch (error) {
        console.error('Error initializing price chart:', error);
    }
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
    
    // Update last update time - create Date object in local time zone
    lastUpdateTime = new Date();
    if (lastUpdateElement) {
        // Format in local time zone (already in local time)
        lastUpdateElement.textContent = lastUpdateTime.toLocaleTimeString(undefined, {
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
            hour12: false
            // No need to specify timeZone as the Date is already in local time
        });
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
        // Use the server timestamp directly to create a display string in local time
        const serverTime = new Date(data.timestamp);
        
        // Format time with hours/minutes/seconds - NO timezone specifier needed
        // JavaScript Date objects automatically use the local timezone for display
        const localTimeString = serverTime.toLocaleTimeString(undefined, {
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
            hour12: false  // Use 24-hour format
        });
        
        // Clear chart data if trading pair or exchange has changed
        const pairSelector = document.getElementById('trading-pair-selector');
        const exchangeSelector = document.getElementById('exchange-selector');
        const currentPair = pairSelector ? pairSelector.value : 'BTC-USD';
        const currentExchange = exchangeSelector ? exchangeSelector.value : 'Kraken';
        
        // If trading pair changed, reset chart and add initial point
        const currentChartLabel = `${currentPair} (${currentExchange})`;
        if (priceChart.data.datasets[0].label !== currentChartLabel) {
            console.log(`Switching chart from ${priceChart.data.datasets[0].label} to ${currentChartLabel}`);
            
            // Save the incoming data point
            const currentPrice = data.lastPrice;
            
            // Format time for display (simple format for chart points)
            const now = new Date();
            const timeString = now.toLocaleTimeString(undefined, {
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit',
                hour12: false
            });
            
            // Create new dataset rather than clearing the old one
            priceChart.data = {
                labels: [timeString],
                datasets: [{
                    label: currentChartLabel,
                    data: [currentPrice],
                    borderColor: 'rgb(75, 192, 192)',
                    backgroundColor: 'rgba(75, 192, 192, 0.1)',
                    fill: true,
                    tension: 0.1,
                    pointRadius: 3
                }]
            };
            
            // Update chart title
            priceChart.options.plugins.title.text = `${currentPair} Price (${currentExchange})`;
            
            // Set initial scale with buffer
            const buffer = currentPrice * 0.02; // 2% buffer 
            priceChart.options.scales.y.min = Math.max(0, currentPrice - buffer);
            priceChart.options.scales.y.max = currentPrice + buffer;
            
            // Force immediate update to show the first point
            priceChart.update('none'); // Use 'none' mode for instant update with no animation
            
            console.log(`Chart reset for ${currentChartLabel} with initial price: ${currentPrice}`);
            
            // Skip adding the point twice
            return;
        }
        
        // Add the formatted local time to the chart
        priceChart.data.labels.push(localTimeString);
        priceChart.data.datasets[0].data.push(data.lastPrice);
        
        // Limit to 20 visible points
        if (priceChart.data.labels.length > 20) {
            priceChart.data.labels.shift();
            priceChart.data.datasets[0].data.shift();
        }
        
        // DYNAMIC SCALING: Calculate the appropriate scale based on the min/max of actual data
        // with +/- 10% buffer as requested
        if (priceChart.data.datasets[0].data.length > 0) {
            const prices = priceChart.data.datasets[0].data;
            
            // Find min and max prices in the current dataset
            let minPrice = Number.MAX_VALUE;
            let maxPrice = Number.MIN_VALUE;
            
            for (const price of prices) {
                if (price < minPrice) minPrice = price;
                if (price > maxPrice) maxPrice = price;
            }
            
            // Calculate the range (difference between max and min)
            const priceRange = maxPrice - minPrice;
            
            // Add 10% buffer on both sides
            const buffer = priceRange * 0.1;
            
            // Set min and max with buffer (ensuring min never goes below 0)
            const newMin = Math.max(0, minPrice - buffer);
            const newMax = maxPrice + buffer;
            
            // Apply the new scale to the chart
            priceChart.options.scales.y.min = newMin;
            priceChart.options.scales.y.max = newMax;
            
            // Update chart title to reflect current pair
            priceChart.options.plugins.title.text = `${currentPair} Price (${currentExchange})`;
        }
        
        // Force Chart.js to respect our scale by disabling auto-scaling
        priceChart.options.scales.y.beginAtZero = false;
        priceChart.options.scales.y.grace = 0;
        
        // Update the chart with animation disabled to prevent jumping
        priceChart.update({
            duration: 0,  // No animation
            easing: 'linear' // Linear easing (no easing curve)
        });
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
            try {
                // Update trading status
                const tradingStatus = document.getElementById('trading-status');
                const activeAlgorithm = document.getElementById('active-algorithm');
                const tradingStartTime = document.getElementById('trading-start-time');
                const stopTradingBtn = document.getElementById('stop-trading-btn');
                const startTradingBtn = document.getElementById('start-trading-btn');
                
                // Check if required elements exist
                if (!tradingStatus || !activeAlgorithm || !tradingStartTime) {
                    console.error('One or more required trading status elements not found');
                    return;
                }
                
                if (statusData.isActive) {
                    tradingStatus.textContent = 'Active';
                    tradingStatus.className = 'badge bg-success';
                    activeAlgorithm.textContent = statusData.algorithm || 'Unknown';
                    tradingStartTime.textContent = formatTimestamp(statusData.startTime) || 'Unknown';
                    
                    // Enable stop button, disable start button
                    if (stopTradingBtn) stopTradingBtn.disabled = false;
                    if (startTradingBtn) startTradingBtn.disabled = true;
                } else {
                    tradingStatus.textContent = 'Inactive';
                    tradingStatus.className = 'badge bg-secondary';
                    activeAlgorithm.textContent = 'None';
                    tradingStartTime.textContent = '-';
                    
                    // Enable start button, disable stop button
                    if (stopTradingBtn) stopTradingBtn.disabled = true;
                    if (startTradingBtn) startTradingBtn.disabled = false;
                }
                
                // Update system health
                const systemHealth = document.getElementById('system-health');
                const memoryUsage = document.getElementById('memory-usage');
                const cpuUsage = document.getElementById('cpu-usage');
                
                if (systemHealth && statusData.isHealthy !== undefined) {
                    systemHealth.textContent = statusData.isHealthy ? 'Healthy' : 'Issues Detected';
                    systemHealth.className = statusData.isHealthy ? 'badge bg-success' : 'badge bg-warning';
                }
                
                if (memoryUsage && statusData.memoryUsage) {
                    memoryUsage.textContent = `${statusData.memoryUsage.toFixed(1)}%`;
                }
                
                if (cpuUsage && statusData.cpuUsage) {
                    cpuUsage.textContent = `${statusData.cpuUsage.toFixed(1)}%`;
                }
            } catch (error) {
                console.error('Error updating system status display:', error);
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
    try {
        // Redirect to trading tab
        const tradingTab = document.getElementById('trading-tab');
        if (tradingTab) {
            tradingTab.click();
        } else {
            console.error('Trading tab element not found');
        }
    } catch (error) {
        console.error('Error starting trading:', error);
    }
}

/**
 * Stops the automated trading
 */
function stopTrading() {
    try {
        console.log('Stopping trading...');
        
        fetch('/api/trading/system-control', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ command: 'stop' })
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Network response was not ok: ${response.status} ${response.statusText}`);
            }
            return response.json();
        })
        .then(data => {
            console.log('Trading stop command successful:', data);
            alert(data.message || 'Trading stopped successfully');
            updateSystemStatus();
        })
        .catch(error => {
            console.error('Error stopping trading:', error);
            alert('Failed to stop trading. See console for details.');
        });
    } catch (error) {
        console.error('Critical error in stopTrading function:', error);
    }
}

/**
 * Updates the price change indicator
 * 
 * @param {HTMLElement} element - The element to update
 * @param {number} currentValue - The current price value
 * @param {number} previousValue - The previous price value
 */
function updatePriceChangeIndicator(element, currentValue, previousValue) {
    // Check if element exists and previousValue is valid
    if (!element || previousValue === 0) {
        console.debug('Price change indicator update skipped: element null or no previous value');
        return;
    }
    
    try {
        const priceChange = currentValue - previousValue;
        
        // Apply direct styling instead of using classList
        if (priceChange > 0) {
            element.style.backgroundColor = 'rgba(40, 167, 69, 0.3)';
        } else if (priceChange < 0) {
            element.style.backgroundColor = 'rgba(220, 53, 69, 0.3)';
        }
        
        // Remove background after animation
        setTimeout(() => {
            // Check if element still exists when timeout executes
            if (element) {
                element.style.backgroundColor = 'transparent';
            }
        }, 1000);
    } catch (error) {
        console.error('Error updating price change indicator:', error);
    }
}

/**
 * Flashes the update indicator
 */
function flashUpdateIndicator() {
    try {
        const indicator = document.getElementById('price-update-indicator');
        
        if (!indicator) {
            console.warn('Price update indicator element not found');
            return;
        }
        
        // Use direct style manipulation instead of classList
        indicator.style.opacity = '1';
        
        setTimeout(() => {
            if (indicator) {
                indicator.style.opacity = '0';
            }
        }, 1000);
    } catch (error) {
        console.error('Error flashing update indicator:', error);
    }
}

/**
 * Updates the system uptime display
 */
function updateSystemUptime() {
    try {
        systemUptime++;
        
        const hours = Math.floor(systemUptime / 3600);
        const minutes = Math.floor((systemUptime % 3600) / 60);
        const seconds = systemUptime % 60;
        
        const uptimeString = 
            `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
        
        const uptimeElement = document.getElementById('system-uptime');
        if (uptimeElement) {
            uptimeElement.textContent = uptimeString;
        }
    } catch (error) {
        console.error('Error updating system uptime:', error);
    }
}

/**
 * Formats a timestamp string for display
 * 
 * @param {string} timestamp - The timestamp string
 * @returns {string} The formatted timestamp
 */
function formatTimestamp(timestamp) {
    if (!timestamp) return '-';
    
    // Create a Date object which automatically converts the ISO timestamp to local time
    const date = new Date(timestamp);
    
    // Get the browser's timezone for display purposes
    const browserTimeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
    
    // Format the date (already in local time) with timezone name
    return date.toLocaleString(undefined, {
        year: 'numeric',
        month: 'numeric',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false, // Use 24-hour format
        // No need to explicitly set timeZone here as the Date is already in local time
        timeZoneName: 'short' // Show timezone abbreviation
    });
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