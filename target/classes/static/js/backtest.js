/**
 * Backtest JavaScript file for handling the backtesting functionality
 */

// Chart instance for equity curve
let equityChart;

/**
 * Initializes the backtest form
 */
function initializeBacktestForm() {
    const backtestForm = document.getElementById('backtest-form');
    const backtestAlgorithm = document.getElementById('backtest-algorithm');
    
    // Set default dates for the backtest period
    setDefaultDates();
    
    // Add event listener for algorithm selection
    backtestAlgorithm.addEventListener('change', loadAlgorithmParameters);
    
    // Add event listener for form submission
    backtestForm.addEventListener('submit', runBacktest);
}

/**
 * Sets default dates for the backtest period
 */
function setDefaultDates() {
    const endDate = new Date();
    const startDate = new Date();
    startDate.setMonth(startDate.getMonth() - 1); // One month ago
    
    // Format the dates for datetime-local input
    const formattedEndDate = formatDateForInput(endDate);
    const formattedStartDate = formatDateForInput(startDate);
    
    document.getElementById('backtest-start-time').value = formattedStartDate;
    document.getElementById('backtest-end-time').value = formattedEndDate;
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
    
    // Clear existing parameters
    paramsContainer.innerHTML = '';
    
    if (!algorithmId) {
        return;
    }
    
    fetch(`/api/algorithms/${algorithmId}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(algorithm => {
            if (algorithm.parameters) {
                // Add a title for the parameters section
                const title = document.createElement('h6');
                title.className = 'mt-3 mb-2';
                title.textContent = 'Algorithm Parameters';
                paramsContainer.appendChild(title);
                
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
                    }
                    
                    paramDiv.appendChild(label);
                    paramDiv.appendChild(input);
                    paramsContainer.appendChild(paramDiv);
                }
            }
        })
        .catch(error => {
            console.error('Error fetching algorithm parameters:', error);
        });
}

/**
 * Runs a backtest with the specified parameters
 * 
 * @param {Event} event - The form submission event
 */
function runBacktest(event) {
    event.preventDefault();
    
    // Show loading indicator
    document.getElementById('backtest-loading').classList.remove('d-none');
    document.getElementById('backtest-results-container').classList.add('d-none');
    document.getElementById('backtest-no-results').classList.add('d-none');
    
    // Get form values
    const algorithmId = document.getElementById('backtest-algorithm').value;
    const exchange = document.getElementById('backtest-exchange').value;
    const tradingPair = document.getElementById('backtest-pair').value;
    // Format dates for LocalDateTime.parse() in backend (ISO-8601 format)
    const startTime = document.getElementById('backtest-start-time').value + ':00';  // Add seconds
    const endTime = document.getElementById('backtest-end-time').value + ':00';      // Add seconds
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
    
    // Run the backtest
    fetch('/api/backtest/run', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestBody)
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    })
    .then(data => {
        displayBacktestResults(data);
    })
    .catch(error => {
        console.error('Error running backtest:', error);
        alert('Failed to run backtest. See console for details.');
        document.getElementById('backtest-loading').classList.add('d-none');
        document.getElementById('backtest-no-results').classList.remove('d-none');
    });
}

/**
 * Displays the backtest results
 * 
 * @param {Object} results - The backtest results object
 */
function displayBacktestResults(results) {
    // Hide loading indicator and show results container
    document.getElementById('backtest-loading').classList.add('d-none');
    document.getElementById('backtest-results-container').classList.remove('d-none');
    document.getElementById('backtest-no-results').classList.add('d-none');
    
    // Update summary information
    const initialCapital = results.initialCapital;
    let finalCapital = initialCapital;
    
    if (results.generatedOrders && results.generatedOrders.length > 0) {
        const lastOrder = results.generatedOrders[results.generatedOrders.length - 1];
        finalCapital = lastOrder.totalValue;
    }
    
    const profit = finalCapital - initialCapital;
    const returnPercent = (profit / initialCapital) * 100;
    
    document.getElementById('result-initial-capital').textContent = formatCurrency(initialCapital);
    document.getElementById('result-final-capital').textContent = formatCurrency(finalCapital);
    
    const profitLossElement = document.getElementById('result-profit-loss');
    profitLossElement.textContent = formatCurrency(profit);
    profitLossElement.className = profit >= 0 ? 'positive-value' : 'negative-value';
    
    const returnElement = document.getElementById('result-return');
    returnElement.textContent = `${returnPercent >= 0 ? '+' : ''}${returnPercent.toFixed(2)}%`;
    returnElement.className = returnPercent >= 0 ? 'positive-value' : 'negative-value';
    
    // Draw equity chart
    drawEquityChart(results);
    
    // Display performance metrics
    displayPerformanceMetrics(results, initialCapital, finalCapital, profit, returnPercent);
    
    // Display trade statistics
    displayTradeStatistics(results.generatedOrders);
    
    // Display trades
    displayTrades(results.generatedOrders);
}

/**
 * Draws the equity chart
 * 
 * @param {Object} results - The backtest results object
 */
function drawEquityChart(results) {
    // Destroy existing chart if it exists
    if (equityChart) {
        equityChart.destroy();
    }
    
    // Prepare data for the chart
    const orders = results.generatedOrders || [];
    const equityData = [];
    let runningCapital = results.initialCapital;
    
    // Add initial point
    equityData.push({
        date: new Date(results.startTime),
        equity: runningCapital
    });
    
    // Add a point for each order
    orders.forEach(order => {
        runningCapital = order.totalValue;
        equityData.push({
            date: new Date(order.createdAt),
            equity: runningCapital
        });
    });
    
    // Sort by date
    equityData.sort((a, b) => a.date - b.date);
    
    // Create the chart
    const ctx = document.getElementById('equity-chart').getContext('2d');
    
    equityChart = new Chart(ctx, {
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
    const metricsTable = document.getElementById('performance-metrics');
    metricsTable.innerHTML = '';
    
    // Calculate additional metrics
    const orders = results.generatedOrders || [];
    const tradeDays = Math.ceil((new Date(results.endTime) - new Date(results.startTime)) / (1000 * 60 * 60 * 24));
    const annualizedReturn = (Math.pow(1 + returnPercent / 100, 365 / tradeDays) - 1) * 100;
    
    // Calculate drawdown
    let maxDrawdown = 0;
    let maxEquity = initialCapital;
    let currentDrawdown = 0;
    
    if (orders.length > 0) {
        let runningEquity = initialCapital;
        
        orders.forEach(order => {
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
}

/**
 * Displays trade statistics
 * 
 * @param {Array} orders - Array of order objects
 */
function displayTradeStatistics(orders) {
    const statsTable = document.getElementById('trade-statistics');
    statsTable.innerHTML = '';
    
    if (!orders || orders.length === 0) {
        const row = document.createElement('tr');
        row.innerHTML = '<td colspan="2">No trades executed</td>';
        statsTable.appendChild(row);
        return;
    }
    
    // Calculate trade statistics
    const totalTrades = orders.length;
    
    // Add statistics to the table
    const statistics = [
        { name: 'Total Trades', value: totalTrades },
        { name: 'Average Trade Size', value: formatCurrency(orders.reduce((sum, order) => sum + (order.price * order.amount), 0) / totalTrades) },
        { name: 'First Trade', value: new Date(orders[0].createdAt).toLocaleDateString() },
        { name: 'Last Trade', value: new Date(orders[orders.length - 1].createdAt).toLocaleDateString() },
        { name: 'Trading Pair', value: orders[0].tradingPair },
        { name: 'Exchange', value: orders[0].exchange }
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
}

/**
 * Displays the list of trades
 * 
 * @param {Array} orders - Array of order objects
 */
function displayTrades(orders) {
    const tradesTable = document.getElementById('backtest-trades');
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
        
        row.innerHTML = `
            <td>${new Date(order.createdAt).toLocaleString()}</td>
            <td>${order.type}</td>
            <td>${formatCurrency(order.price)}</td>
            <td>${order.amount.toFixed(8)}</td>
            <td>${formatCurrency(order.price * order.amount)}</td>
        `;
        
        tradesTable.appendChild(row);
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
document.addEventListener('DOMContentLoaded', function() {
    function initializeBacktestForm() {
        const form = document.getElementById('backtest-form');
        const loadingEl = document.getElementById('backtest-loading');
        const resultsEl = document.getElementById('backtest-results-container');
        const noResultsEl = document.getElementById('backtest-no-results');

        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            loadingEl.classList.remove('d-none');
            resultsEl.classList.add('d-none');
            noResultsEl.classList.add('d-none');

            try {
                const formData = {
                    algorithm: document.getElementById('backtest-algorithm').value,
                    exchange: document.getElementById('backtest-exchange').value,
                    pair: document.getElementById('backtest-pair').value,
                    startTime: document.getElementById('backtest-start-time').value,
                    endTime: document.getElementById('backtest-end-time').value,
                    initialCapital: document.getElementById('backtest-initial-capital').value
                };

                const response = await fetch('/api/backtest', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(formData)
                });

                if (!response.ok) throw new Error('Backtest request failed');
                
                const results = await response.json();
                displayResults(results);
                resultsEl.classList.remove('d-none');
            } catch (error) {
                console.error('Backtest error:', error);
                noResultsEl.innerHTML = `<p class="text-danger">Error: ${error.message}</p>`;
                noResultsEl.classList.remove('d-none');
            } finally {
                loadingEl.classList.add('d-none');
            }
        });
    }

    window.initializeBacktestForm = initializeBacktestForm;
});
