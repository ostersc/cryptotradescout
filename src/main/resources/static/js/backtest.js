/**
 * Backtest JavaScript file for handling the backtesting functionality
 */

// Chart instance for equity curve
let equityChart;

/**
 * Initializes the backtest form
 */
function initializeBacktestForm() {
    try {
        console.log('Initializing backtest form...');
        const backtestForm = document.getElementById('backtest-form');
        const backtestAlgorithm = document.getElementById('backtest-algorithm');
        
        if (!backtestForm || !backtestAlgorithm) {
            console.error('Backtest form elements not found');
            return;
        }
        
        // Set default dates for the backtest period
        setDefaultDates();
        
        // Add event listener for algorithm selection
        backtestAlgorithm.addEventListener('change', loadAlgorithmParameters);
        
        // Add event listener for form submission
        backtestForm.addEventListener('submit', runBacktest);
        
        // Load initial algorithm parameters
        if (backtestAlgorithm.value) {
            loadAlgorithmParameters();
        }
    } catch (error) {
        console.error('Error initializing backtest form:', error);
    }
}

/**
 * Sets default dates for the backtest period
 */
function setDefaultDates() {
    try {
        const endDate = new Date();
        const startDate = new Date();
        startDate.setMonth(startDate.getMonth() - 1); // One month ago
        
        // Format the dates for datetime-local input
        const formattedEndDate = formatDateForInput(endDate);
        const formattedStartDate = formatDateForInput(startDate);
        
        const startTimeInput = document.getElementById('backtest-start-time');
        const endTimeInput = document.getElementById('backtest-end-time');
        
        if (startTimeInput && endTimeInput) {
            startTimeInput.value = formattedStartDate;
            endTimeInput.value = formattedEndDate;
        } else {
            console.error('Date input elements not found');
        }
    } catch (error) {
        console.error('Error setting default dates:', error);
    }
}

/**
 * Formats a date for datetime-local input
 * 
 * @param {Date} date - The date to format
 * @returns {string} The formatted date string
 */
function formatDateForInput(date) {
    return date.toISOString().slice(0, 16);
}

/**
 * Loads the parameters for the selected algorithm
 */
function loadAlgorithmParameters() {
    const algorithmId = document.getElementById('backtest-algorithm').value;
    const paramsContainer = document.getElementById('algorithm-params-container');
    
    if (!paramsContainer) {
        console.error('Algorithm parameters container not found');
        return;
    }
    
    // Clear existing parameters
    paramsContainer.innerHTML = '';
    
    if (!algorithmId) {
        return;
    }
    
    console.log('Loading parameters for algorithm:', algorithmId);
    
    // Try to fetch from API first, but fall back to sample data if it fails
    fetch(`/api/algorithms/${algorithmId}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(algorithm => {
            populateAlgorithmParameters(algorithm, paramsContainer);
        })
        .catch(error => {
            console.error('Error fetching algorithm parameters, using defaults:', error);
            
            // Use default parameters based on algorithm ID
            const defaultParams = getDefaultParameters(algorithmId);
            populateAlgorithmParameters({
                id: algorithmId,
                parameters: defaultParams
            }, paramsContainer);
        });
}

/**
 * Gets default parameters for a given algorithm ID
 * 
 * @param {string} algorithmId - The algorithm ID
 * @returns {Object} Object with parameter defaults
 */
function getDefaultParameters(algorithmId) {
    switch (algorithmId) {
        case 'simple-moving-average':
            return {
                shortPeriod: "Period for short moving average (typically 5-15)",
                longPeriod: "Period for long moving average (typically 20-50)",
                orderAmount: "Amount of asset to trade per order (e.g., 0.1 BTC)"
            };
        case 'arbitrage':
            return {
                minProfitPercentage: "Minimum profit percentage to execute arbitrage (e.g., 1.5)",
                maxTransactionFee: "Maximum transaction fee percentage (e.g., 0.5)",
                orderAmount: "Amount of asset to trade per order (e.g., 0.1 BTC)"
            };
        default:
            return {
                period: "Time period parameter",
                threshold: "Threshold value for decisions",
                amount: "Trading amount per order"
            };
    }
}

/**
 * Populates algorithm parameters in the container
 * 
 * @param {Object} algorithm - The algorithm object
 * @param {HTMLElement} container - The container element
 */
function populateAlgorithmParameters(algorithm, container) {
    if (algorithm.parameters) {
        // Add a title for the parameters section
        const title = document.createElement('h6');
        title.className = 'mt-3 mb-2';
        title.textContent = 'Algorithm Parameters';
        container.appendChild(title);
        
        // Create input fields for each parameter
        for (const [param, description] of Object.entries(algorithm.parameters)) {
            const paramDiv = document.createElement('div');
            paramDiv.className = 'mb-3';
            
            const label = document.createElement('label');
            label.setAttribute('for', `param-${param}`);
            label.className = 'form-label';
            label.textContent = param;
            
            const input = document.createElement('input');
            input.type = 'text';
            input.className = 'form-control';
            input.id = `param-${param}`;
            input.name = `param-${param}`;
            input.setAttribute('placeholder', description);
            input.required = true;
            
            // Set default values based on parameter names
            if (param.toLowerCase().includes('period')) {
                input.type = 'number';
                if (param.toLowerCase().includes('short')) {
                    input.value = '5';
                } else if (param.toLowerCase().includes('long')) {
                    input.value = '20';
                } else {
                    input.value = '10';
                }
            } else if (param.toLowerCase().includes('amount')) {
                input.type = 'number';
                input.value = '0.1';
                input.step = '0.01';
            } else if (param.toLowerCase().includes('percentage') || 
                       param.toLowerCase().includes('fee')) {
                input.type = 'number';
                input.value = '1.5';
                input.step = '0.1';
            }
            
            paramDiv.appendChild(label);
            paramDiv.appendChild(input);
            container.appendChild(paramDiv);
        }
    }
}

/**
 * Runs a backtest with the specified parameters
 * 
 * @param {Event} event - The form submission event
 */
function runBacktest(event) {
    event.preventDefault();
    
    try {
        // Show loading indicator
        const loadingEl = document.getElementById('backtest-loading');
        const resultsEl = document.getElementById('backtest-results-container');
        const noResultsEl = document.getElementById('backtest-no-results');
        
        if (loadingEl) loadingEl.classList.remove('d-none');
        if (resultsEl) resultsEl.classList.add('d-none');
        if (noResultsEl) noResultsEl.classList.add('d-none');
        
        // Get form values
        const algorithmId = document.getElementById('backtest-algorithm').value;
        const exchange = document.getElementById('backtest-exchange').value;
        const tradingPair = document.getElementById('backtest-pair').value;
        
        // Format dates for LocalDateTime.parse() in backend (ISO-8601 format)
        let startTime = document.getElementById('backtest-start-time').value;
        let endTime = document.getElementById('backtest-end-time').value;
        
        // Ensure the time part includes seconds for ISO format
        if (startTime && !startTime.endsWith('Z') && startTime.length === 16) {
            startTime = startTime + ':00';
        }
        
        if (endTime && !endTime.endsWith('Z') && endTime.length === 16) {
            endTime = endTime + ':00';
        }
        
        const initialCapital = parseFloat(document.getElementById('backtest-initial-capital').value);
        
        console.log('Running backtest with:', {
            algorithmId,
            exchange,
            tradingPair,
            startTime,
            endTime,
            initialCapital
        });
        
        // Collect algorithm parameters
        const algorithmParams = {};
        const paramInputs = document.querySelectorAll('#algorithm-params-container input');
        
        paramInputs.forEach(input => {
            const paramName = input.id.replace('param-', '');
            let paramValue = input.value;
            
            // Convert to number if possible
            if (!isNaN(paramValue)) {
                paramValue = parseFloat(paramValue);
            }
            
            algorithmParams[paramName] = paramValue;
        });
        
        // Create request body
        const requestBody = {
            algorithmId,
            exchange,
            tradingPair,
            startTime,
            endTime,
            initialCapital,
            algorithmParams
        };
        
        // Try both API endpoints
        fetch('/api/backtest/run', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        })
        .then(response => {
            if (!response.ok) {
                // Try the alternate endpoint
                return fetch('/api/backtest', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(requestBody)
                });
            }
            return response;
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Both backtest endpoints failed. Check server logs.');
            }
            return response.json();
        })
        .then(data => {
            displayBacktestResults(data);
        })
        .catch(error => {
            console.error('Error running backtest:', error);
            if (noResultsEl) {
                noResultsEl.innerHTML = `
                    <div class="alert alert-danger">
                        <h5>Backtest Error</h5>
                        <p>${error.message || 'Failed to run backtest'}</p>
                        <p>Please check that your parameters are valid and the server is running.</p>
                    </div>
                `;
                noResultsEl.classList.remove('d-none');
            }
            if (loadingEl) {
                loadingEl.classList.add('d-none');
            }
        });
    } catch (error) {
        console.error('Critical error running backtest:', error);
    }
}

/**
 * Displays the backtest results
 * 
 * @param {Object} results - The backtest results object
 */
function displayBacktestResults(results) {
    try {
        // Hide loading indicator and show results container
        const loadingEl = document.getElementById('backtest-loading');
        const resultsEl = document.getElementById('backtest-results-container');
        const noResultsEl = document.getElementById('backtest-no-results');
        
        if (loadingEl) loadingEl.classList.add('d-none');
        if (resultsEl) resultsEl.classList.remove('d-none');
        if (noResultsEl) noResultsEl.classList.add('d-none');
        
        // Update summary information
        const initialCapital = results.initialCapital || 10000;
        let finalCapital = initialCapital;
        
        if (results.generatedOrders && results.generatedOrders.length > 0) {
            const lastOrder = results.generatedOrders[results.generatedOrders.length - 1];
            finalCapital = lastOrder.totalValue || finalCapital;
        }
        
        const profit = finalCapital - initialCapital;
        const returnPercent = (profit / initialCapital) * 100;
        
        const initialCapitalEl = document.getElementById('result-initial-capital');
        const finalCapitalEl = document.getElementById('result-final-capital');
        const profitLossEl = document.getElementById('result-profit-loss');
        const returnEl = document.getElementById('result-return');
        
        if (initialCapitalEl) initialCapitalEl.textContent = formatCurrency(initialCapital);
        if (finalCapitalEl) finalCapitalEl.textContent = formatCurrency(finalCapital);
        
        if (profitLossEl) {
            profitLossEl.textContent = formatCurrency(profit);
            profitLossEl.className = profit >= 0 ? 'positive-value' : 'negative-value';
        }
        
        if (returnEl) {
            returnEl.textContent = `${returnPercent >= 0 ? '+' : ''}${returnPercent.toFixed(2)}%`;
            returnEl.className = returnPercent >= 0 ? 'positive-value' : 'negative-value';
        }
        
        // Draw equity chart
        drawEquityChart(results);
        
        // Display performance metrics
        displayPerformanceMetrics(results, initialCapital, finalCapital, profit, returnPercent);
        
        // Display trade statistics
        displayTradeStatistics(results.generatedOrders);
        
        // Display trades
        displayTrades(results.generatedOrders);
    } catch (error) {
        console.error('Error displaying backtest results:', error);
    }
}

/**
 * Draws the equity chart
 * 
 * @param {Object} results - The backtest results object
 */
function drawEquityChart(results) {
    try {
        const chartCanvas = document.getElementById('equity-chart');
        
        if (!chartCanvas) {
            console.error('Equity chart canvas not found');
            return;
        }
        
        // Destroy existing chart if it exists
        if (window.equityChart) {
            window.equityChart.destroy();
        }
        
        // Prepare data for the chart
        const orders = results.generatedOrders || [];
        const equityData = [];
        let runningCapital = results.initialCapital || 10000;
        
        // Add initial point
        equityData.push({
            date: new Date(results.startTime || new Date().toISOString()),
            equity: runningCapital
        });
        
        // Add a point for each order
        orders.forEach(order => {
            if (order.totalValue) {
                runningCapital = order.totalValue;
                equityData.push({
                    date: new Date(order.createdAt || new Date().toISOString()),
                    equity: runningCapital
                });
            }
        });
        
        // Sort by date
        equityData.sort((a, b) => a.date - b.date);
        
        // Create the chart
        const ctx = chartCanvas.getContext('2d');
        
        window.equityChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: equityData.map(data => data.date.toLocaleDateString()),
                datasets: [{
                    label: 'Portfolio Value',
                    data: equityData.map(data => data.equity),
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
                        text: 'Portfolio Equity Curve'
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
                            text: 'Date'
                        }
                    },
                    y: {
                        title: {
                            display: true,
                            text: 'Portfolio Value'
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
    } catch (error) {
        console.error('Error drawing equity chart:', error);
    }
}

/**
 * Displays performance metrics
 * 
 * @param {Object} results - The backtest results object
 * @param {number} initialCapital - The initial capital
 * @param {number} finalCapital - The final capital
 * @param {number} profit - The profit/loss
 * @param {number} returnPercent - The return percentage
 */
function displayPerformanceMetrics(results, initialCapital, finalCapital, profit, returnPercent) {
    try {
        const metricsTable = document.getElementById('performance-metrics');
        
        if (!metricsTable) {
            console.error('Performance metrics table not found');
            return;
        }
        
        metricsTable.innerHTML = '';
        
        // Calculate additional metrics
        const orders = results.generatedOrders || [];
        const startDate = new Date(results.startTime || new Date().setMonth(new Date().getMonth() - 1));
        const endDate = new Date(results.endTime || new Date());
        const tradeDays = Math.max(1, Math.ceil((endDate - startDate) / (1000 * 60 * 60 * 24)));
        const annualizedReturn = (Math.pow(1 + returnPercent / 100, 365 / tradeDays) - 1) * 100;
        
        // Calculate drawdown
        let maxDrawdown = 0;
        let maxEquity = initialCapital;
        let currentDrawdown = 0;
        
        if (orders.length > 0) {
            let runningEquity = initialCapital;
            
            orders.forEach(order => {
                if (order.totalValue) {
                    runningEquity = order.totalValue;
                    
                    if (runningEquity > maxEquity) {
                        maxEquity = runningEquity;
                        currentDrawdown = 0;
                    } else {
                        currentDrawdown = (maxEquity - runningEquity) / maxEquity * 100;
                        if (currentDrawdown > maxDrawdown) {
                            maxDrawdown = currentDrawdown;
                        }
                    }
                }
            });
        }
        
        // Add metrics to the table
        const metrics = [
            { name: 'Total Return', value: `${returnPercent >= 0 ? '+' : ''}${returnPercent.toFixed(2)}%`, positive: returnPercent >= 0 },
            { name: 'Annualized Return', value: `${annualizedReturn >= 0 ? '+' : ''}${annualizedReturn.toFixed(2)}%`, positive: annualizedReturn >= 0 },
            { name: 'Max Drawdown', value: `-${maxDrawdown.toFixed(2)}%`, positive: false },
            { name: 'Profit/Loss', value: formatCurrency(profit), positive: profit >= 0 },
            { name: 'Test Period', value: `${tradeDays} days` }
        ];
        
        metrics.forEach(metric => {
            const row = document.createElement('tr');
            
            const nameCell = document.createElement('td');
            nameCell.textContent = metric.name;
            
            const valueCell = document.createElement('td');
            valueCell.textContent = metric.value;
            if (metric.hasOwnProperty('positive')) {
                valueCell.className = metric.positive ? 'positive-value' : 'negative-value';
            }
            
            row.appendChild(nameCell);
            row.appendChild(valueCell);
            metricsTable.appendChild(row);
        });
    } catch (error) {
        console.error('Error displaying performance metrics:', error);
    }
}

/**
 * Displays trade statistics
 * 
 * @param {Array} orders - Array of order objects
 */
function displayTradeStatistics(orders) {
    try {
        const statsTable = document.getElementById('trade-statistics');
        
        if (!statsTable) {
            console.error('Trade statistics table not found');
            return;
        }
        
        statsTable.innerHTML = '';
        
        if (!orders || orders.length === 0) {
            const row = document.createElement('tr');
            row.innerHTML = '<td colspan="2">No trades executed</td>';
            statsTable.appendChild(row);
            return;
        }
        
        // Calculate trade statistics
        const totalTrades = orders.length;
        const averageTradeSize = orders.reduce((sum, order) => {
            return sum + ((order.price || 0) * (order.amount || 0));
        }, 0) / totalTrades;
        
        // Add statistics to the table
        const statistics = [
            { name: 'Total Trades', value: totalTrades },
            { name: 'Average Trade Size', value: formatCurrency(averageTradeSize) },
            { name: 'First Trade', value: new Date(orders[0].createdAt || new Date()).toLocaleDateString() },
            { name: 'Last Trade', value: new Date(orders[orders.length - 1].createdAt || new Date()).toLocaleDateString() },
            { name: 'Trading Pair', value: orders[0].tradingPair || 'Unknown' },
            { name: 'Exchange', value: orders[0].exchange || 'Unknown' }
        ];
        
        statistics.forEach(stat => {
            const row = document.createElement('tr');
            
            const nameCell = document.createElement('td');
            nameCell.textContent = stat.name;
            
            const valueCell = document.createElement('td');
            valueCell.textContent = stat.value;
            
            row.appendChild(nameCell);
            row.appendChild(valueCell);
            statsTable.appendChild(row);
        });
    } catch (error) {
        console.error('Error displaying trade statistics:', error);
    }
}

/**
 * Displays the list of trades
 * 
 * @param {Array} orders - Array of order objects
 */
function displayTrades(orders) {
    try {
        const tradesTable = document.getElementById('backtest-trades');
        
        if (!tradesTable) {
            console.error('Backtest trades table not found');
            return;
        }
        
        tradesTable.innerHTML = '';
        
        if (!orders || orders.length === 0) {
            const row = document.createElement('tr');
            row.innerHTML = '<td colspan="5" class="text-center">No trades executed</td>';
            tradesTable.appendChild(row);
            return;
        }
        
        // Add each trade to the table
        orders.forEach(order => {
            const row = document.createElement('tr');
            const createdAt = order.createdAt ? new Date(order.createdAt).toLocaleString() : 'Unknown';
            const type = order.type || 'Unknown';
            const price = order.price || 0;
            const amount = order.amount || 0;
            
            row.innerHTML = `
                <td>${createdAt}</td>
                <td>${type}</td>
                <td>${formatCurrency(price)}</td>
                <td>${amount.toFixed(8)}</td>
                <td>${formatCurrency(price * amount)}</td>
            `;
            
            tradesTable.appendChild(row);
        });
    } catch (error) {
        console.error('Error displaying trades:', error);
    }
}

/**
 * Formats a number as currency
 * 
 * @param {number} value - The value to format
 * @returns {string} The formatted currency string
 */
function formatCurrency(value) {
    try {
        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD'
        }).format(value);
    } catch (error) {
        console.error('Error formatting currency:', error);
        return '$' + (value || 0).toFixed(2);
    }
}
