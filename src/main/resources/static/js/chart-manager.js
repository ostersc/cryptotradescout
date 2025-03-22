/**
 * Chart Manager - Handles all chart creation and updates
 * This is a separate module to improve maintenance and focus on the chart functionality
 */

// The current chart instance
let activePriceChart = null;

/**
 * Creates or recreates the price chart with initial data
 * 
 * @param {string} containerId - The ID of the container element 
 * @param {Object} data - Initial market data for the chart
 * @param {string} pair - Trading pair (e.g., 'BTC-USD')
 * @param {string} exchange - Exchange name (e.g., 'Kraken')
 * @returns {Object} The Chart.js chart instance
 */
function createPriceChart(containerId, data, pair, exchange) {
    console.log(`Creating price chart for ${pair} (${exchange}) with initial price: ${data.lastPrice}`);
    
    // Get the chart container
    const container = document.getElementById(containerId);
    if (!container) {
        console.error(`Chart container #${containerId} not found`);
        return null;
    }
    
    // Destroy existing chart if any
    if (activePriceChart) {
        activePriceChart.destroy();
        activePriceChart = null;
    }
    
    // Format timestamp for display
    const timeString = new Date(data.timestamp).toLocaleTimeString(undefined, {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
    });
    
    // Create chart label
    const chartLabel = `${pair} (${exchange})`;
    
    // Create initial chart
    activePriceChart = new Chart(container, {
        type: 'line',
        data: {
            labels: [timeString],
            datasets: [{
                label: chartLabel,
                data: [data.lastPrice],
                borderColor: 'rgb(75, 192, 192)',
                backgroundColor: 'rgba(75, 192, 192, 0.1)',
                fill: true,
                tension: 0.1,
                pointRadius: 3,
                pointHoverRadius: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            animation: {
                duration: 0 // No animations for better performance
            },
            plugins: {
                title: {
                    display: true,
                    text: `${pair} Price (${exchange})`,
                    font: {
                        size: 16,
                        weight: 'bold'
                    }
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            return new Intl.NumberFormat('en-US', {
                                style: 'currency',
                                currency: 'USD'
                            }).format(context.raw);
                        }
                    }
                },
                legend: {
                    display: false
                }
            },
            scales: {
                x: {
                    title: {
                        display: true,
                        text: 'Time'
                    },
                    ticks: {
                        maxRotation: 0,
                        autoSkip: true,
                        maxTicksLimit: 8
                    }
                },
                y: {
                    title: {
                        display: true,
                        text: 'Price (USD)'
                    },
                    beginAtZero: false,
                    ticks: {
                        callback: function(value) {
                            return new Intl.NumberFormat('en-US', {
                                style: 'currency',
                                currency: 'USD'
                            }).format(value);
                        }
                    }
                }
            }
        }
    });
    
    // Set initial scale with 2% buffer
    const buffer = data.lastPrice * 0.02;
    activePriceChart.options.scales.y.min = Math.max(0, data.lastPrice - buffer);
    activePriceChart.options.scales.y.max = data.lastPrice + buffer;
    activePriceChart.update('none'); // Immediate update with no animation
    
    return activePriceChart;
}

/**
 * Updates an existing price chart with new data
 * 
 * @param {Object} data - The market data to add
 * @param {string} pair - Trading pair
 * @param {string} exchange - Exchange name
 * @returns {boolean} Whether the update was successful
 */
function updatePriceChart(data, pair, exchange) {
    // If chart doesn't exist or the pair/exchange changed, create a new chart
    if (!activePriceChart || activePriceChart.data.datasets[0].label !== `${pair} (${exchange})`) {
        return createPriceChart('price-chart', data, pair, exchange);
    }
    
    try {
        // Format timestamp for display
        const timeString = new Date(data.timestamp).toLocaleTimeString(undefined, {
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
            hour12: false
        });
        
        // Add the new data point
        activePriceChart.data.labels.push(timeString);
        activePriceChart.data.datasets[0].data.push(data.lastPrice);
        
        // Limit to 20 visible points
        if (activePriceChart.data.labels.length > 20) {
            activePriceChart.data.labels.shift();
            activePriceChart.data.datasets[0].data.shift();
        }
        
        // Calculate min and max values with 10% buffer
        const prices = activePriceChart.data.datasets[0].data;
        if (prices.length > 0) {
            // Find min and max
            const minPrice = Math.min(...prices);
            const maxPrice = Math.max(...prices);
            const range = maxPrice - minPrice || maxPrice * 0.1; // Handle case of all identical prices
            
            // Add buffer (10% of range or 0.5% of value, whichever is larger)
            const buffer = Math.max(range * 0.1, maxPrice * 0.005);
            
            // Set scale with buffer
            activePriceChart.options.scales.y.min = Math.max(0, minPrice - buffer);
            activePriceChart.options.scales.y.max = maxPrice + buffer;
        }
        
        // Update without animation
        activePriceChart.update('none');
        return true;
    } catch (error) {
        console.error('Error updating price chart:', error);
        
        // Try to recover by creating a new chart
        try {
            return createPriceChart('price-chart', data, pair, exchange);
        } catch (e) {
            console.error('Failed to recover chart:', e);
            return false;
        }
    }
}

/**
 * Get the active chart instance
 * 
 * @returns {Object|null} The active Chart.js instance or null
 */
function getActiveChart() {
    return activePriceChart;
}

/**
 * Destroy the active chart
 */
function destroyChart() {
    if (activePriceChart) {
        activePriceChart.destroy();
        activePriceChart = null;
    }
}