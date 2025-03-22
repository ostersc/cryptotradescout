/**
 * Chart Manager - Handles all chart creation and updates
 * This is a separate module to improve maintenance and focus on the chart functionality
 * Using a namespace to avoid conflicts with other scripts
 */

// Define the ChartManager namespace to prevent naming conflicts
window.ChartManager = (function() {
    // Private variables
    let activePriceChart = null;
    
    // Private helper functions
    function formatTimeString(timestamp) {
        return new Date(timestamp).toLocaleTimeString(undefined, {
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
            hour12: false
        });
    }
    
    function formatCurrency(value) {
        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD'
        }).format(value);
    }
    
    // Public API
    return {
        /**
         * Creates or recreates the price chart with initial data
         * 
         * @param {string} containerId - The ID of the container element 
         * @param {Object} data - Initial market data for the chart
         * @param {string} pair - Trading pair (e.g., 'BTC-USD')
         * @param {string} exchange - Exchange name (e.g., 'Kraken')
         * @returns {Object} The Chart.js chart instance
         */
        createPriceChart: function(containerId, data, pair, exchange) {
            console.log(`ChartManager: Creating price chart for ${pair} (${exchange}) with initial price: ${data.lastPrice}`);
            
            // Get the chart container
            const container = document.getElementById(containerId);
            if (!container) {
                console.error(`ChartManager: Chart container #${containerId} not found`);
                return null;
            }
            
            // Destroy existing chart if any
            if (activePriceChart) {
                activePriceChart.destroy();
                activePriceChart = null;
            }
            
            // Format timestamp for display
            const timeString = formatTimeString(data.timestamp);
            
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
                                    return formatCurrency(context.raw);
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
                                    return formatCurrency(value);
                                }
                            }
                        }
                    }
                }
            });
            
            // Set initial scale with a default range of 1% of the price
            // and a buffer of 10% of that range
            const defaultRange = data.lastPrice * 0.01;
            const buffer = defaultRange * 0.1;
            activePriceChart.options.scales.y.min = Math.max(0, data.lastPrice - buffer);
            activePriceChart.options.scales.y.max = data.lastPrice + buffer;
            console.log(`Chart Y-axis initialized: min=${activePriceChart.options.scales.y.min}, max=${activePriceChart.options.scales.y.max}, defaultRange=${defaultRange}, buffer=${buffer}`);
            activePriceChart.update('none'); // Immediate update with no animation
            
            return activePriceChart;
        },
        
        /**
         * Updates an existing price chart with new data
         * 
         * @param {Object} data - The market data to add
         * @param {string} pair - Trading pair
         * @param {string} exchange - Exchange name
         * @returns {boolean} Whether the update was successful
         */
        updatePriceChart: function(data, pair, exchange) {
            // If chart doesn't exist or the pair/exchange changed, create a new chart
            if (!activePriceChart || activePriceChart.data.datasets[0].label !== `${pair} (${exchange})`) {
                return this.createPriceChart('price-chart', data, pair, exchange);
            }
            
            try {
                // Format timestamp for display
                const timeString = formatTimeString(data.timestamp);
                
                // Add the new data point
                activePriceChart.data.labels.push(timeString);
                activePriceChart.data.datasets[0].data.push(data.lastPrice);
                
                // Limit to 20 visible points
                if (activePriceChart.data.labels.length > 20) {
                    activePriceChart.data.labels.shift();
                    activePriceChart.data.datasets[0].data.shift();
                }
                
                // Calculate min and max values with 10% buffer based on the range
                const prices = activePriceChart.data.datasets[0].data;
                if (prices.length > 0) {
                    // Find min and max
                    const minPrice = Math.min(...prices);
                    const maxPrice = Math.max(...prices);
                    const range = maxPrice - minPrice;
                    
                    // Use 10% of the range as the buffer
                    // If all prices are identical, use 1% of the price as a default range
                    const effectiveRange = range || maxPrice * 0.01;
                    const buffer = effectiveRange * 0.1;
                    
                    // Set scale with buffer
                    activePriceChart.options.scales.y.min = Math.max(0, minPrice - buffer);
                    activePriceChart.options.scales.y.max = maxPrice + buffer;
                    
                    console.log(`Chart Y-axis adjusted: min=${activePriceChart.options.scales.y.min}, max=${activePriceChart.options.scales.y.max}, range=${effectiveRange}, buffer=${buffer}`);
                }
                
                // Update without animation
                activePriceChart.update('none');
                return true;
            } catch (error) {
                console.error('ChartManager: Error updating price chart:', error);
                
                // Try to recover by creating a new chart
                try {
                    return this.createPriceChart('price-chart', data, pair, exchange);
                } catch (e) {
                    console.error('ChartManager: Failed to recover chart:', e);
                    return false;
                }
            }
        },
        
        /**
         * Get the active chart instance
         * 
         * @returns {Object|null} The active Chart.js instance or null
         */
        getActiveChart: function() {
            return activePriceChart;
        },
        
        /**
         * Destroy the active chart
         */
        destroyChart: function() {
            if (activePriceChart) {
                activePriceChart.destroy();
                activePriceChart = null;
            }
        }
    };
})();