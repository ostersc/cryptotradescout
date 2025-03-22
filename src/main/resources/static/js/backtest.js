/**
 * Backtest JavaScript file for handling the backtesting functionality
 */

// Chart instance for equity curve - using window object to avoid redeclaration
// This fixes "Uncaught SyntaxError: Identifier 'equityChart' has already been declared"
if (typeof window.equityChart === 'undefined') {
    window.equityChart = null;
}

/**
 * Initializes the backtest form
 * 
 * @param {string} selectedAlgorithmId - Optional ID of a pre-selected algorithm
 */
function initializeBacktestForm(selectedAlgorithmId) {
    try {
        console.log('Initializing backtest form...');
        const backtestForm = document.getElementById('backtest-form');
        const backtestAlgorithm = document.getElementById('backtest-algorithm');
        
        // Store the selected algorithm ID if provided
        if (selectedAlgorithmId) {
            console.log(`Pre-selected algorithm ID for backtest: ${selectedAlgorithmId}`);
            window.preSelectedAlgorithmId = selectedAlgorithmId;
        }
        
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
        
        // Load algorithms to populate the dropdown
        loadAlgorithmsForBacktest();
        
        // Load initial algorithm parameters
        if (backtestAlgorithm.value) {
            loadAlgorithmParameters();
        }
    } catch (error) {
        console.error('Error initializing backtest form:', error);
    }
}

/**
 * Loads the algorithms for the backtest dropdown
 */
function loadAlgorithmsForBacktest() {
    try {
        fetch('/api/algorithms')
            .then(response => {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                return response.json();
            })
            .then(algorithms => {
                if (Array.isArray(algorithms)) {
                    // Use the implementation from algorithms.js instead of our own
                    // Use the global implementation, not the local one
                    if (typeof window.populateAlgorithmDropdowns === 'function') {
                        window.populateAlgorithmDropdowns(algorithms);
                        
                        // After populating the dropdown, check if we have a pre-selected algorithm
                        if (window.preSelectedAlgorithmId) {
                            // Select the algorithm in the dropdown
                            const backtestAlgorithm = document.getElementById('backtest-algorithm');
                            if (backtestAlgorithm) {
                                backtestAlgorithm.value = window.preSelectedAlgorithmId;
                                console.log(`Setting backtest algorithm dropdown to: ${window.preSelectedAlgorithmId}`);
                                
                                // Trigger the change event to load algorithm parameters
                                const changeEvent = new Event('change');
                                backtestAlgorithm.dispatchEvent(changeEvent);
                                
                                // Clear the stored value after using it
                                window.preSelectedAlgorithmId = null;
                            }
                        }
                    } else if (typeof populateAlgorithmDropdowns === 'function') {
                        // Fall back to global scope function if not defined in window
                        populateAlgorithmDropdowns(algorithms);
                        
                        // After populating the dropdown, check if we have a pre-selected algorithm
                        if (window.preSelectedAlgorithmId) {
                            // Select the algorithm in the dropdown
                            const backtestAlgorithm = document.getElementById('backtest-algorithm');
                            if (backtestAlgorithm) {
                                backtestAlgorithm.value = window.preSelectedAlgorithmId;
                                console.log(`Setting backtest algorithm dropdown to: ${window.preSelectedAlgorithmId}`);
                                
                                // Trigger the change event to load algorithm parameters
                                const changeEvent = new Event('change');
                                backtestAlgorithm.dispatchEvent(changeEvent);
                                
                                // Clear the stored value after using it
                                window.preSelectedAlgorithmId = null;
                            }
                        }
                    } else {
                        console.error('populateAlgorithmDropdowns function not found');
                    }
                } else {
                    console.error('Invalid algorithms data format:', algorithms);
                }
            })
            .catch(error => {
                console.error('Error fetching algorithms:', error);
                // Fallback to default algorithms
                const defaultAlgorithms = [
                    { id: 'simple-moving-average', name: 'Simple Moving Average Crossover' },
                    { id: 'arbitrage', name: 'Exchange Arbitrage' }
                ];
                // Use the implementation from algorithms.js
                if (typeof window.populateAlgorithmDropdowns === 'function') {
                    window.populateAlgorithmDropdowns(defaultAlgorithms);
                } else if (typeof populateAlgorithmDropdowns === 'function') {
                    // Fall back to global scope function if not defined in window
                    populateAlgorithmDropdowns(defaultAlgorithms);
                } else {
                    console.error('populateAlgorithmDropdowns function not found');
                }
            });
    } catch (error) {
        console.error('Error loading algorithms for backtest:', error);
    }
}

/**
 * Populates the algorithm dropdown selectors - using the shared implementation from algorithms.js
 * This is a local wrapper to ensure compatibility with existing code.
 * 
 * @param {Array} algorithms - Array of algorithm objects
 */
// Using the populateAlgorithmDropdowns function from algorithms.js
// This is intentionally commented out to avoid duplicates
/*
function populateAlgorithmDropdowns(algorithms) {
    // This function is now defined in algorithms.js
    // We're using that implementation instead to avoid duplicate entries
    console.warn('Local implementation of populateAlgorithmDropdowns should not be called');
}
*/

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
    
    // Always clear existing parameters first
    paramsContainer.innerHTML = '';
    
    if (!algorithmId) {
        return;
    }
    
    console.log('Loading parameters for algorithm:', algorithmId);
    
    // Fetch algorithm details from API which includes both parameters and their default values
    fetch(`/api/algorithms/${algorithmId}`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error ${response.status}`);
            }
            return response.json();
        })
        .then(algorithm => {
            // Make sure container is empty before populating
            paramsContainer.innerHTML = '';
            populateAlgorithmParameters(algorithm, paramsContainer);
        })
        .catch(error => {
            console.error('Error fetching algorithm details:', error);
            // Fallback to parameters-only endpoint if the details endpoint fails
            fetchParametersOnly(algorithmId, paramsContainer);
        });
}

/**
 * Fallback method to fetch only parameters if the full details endpoint fails
 * 
 * @param {string} algorithmId - The algorithm ID
 * @param {HTMLElement} paramsContainer - The container element
 */
function fetchParametersOnly(algorithmId, paramsContainer) {
    // Fetch algorithm parameters from API
    fetch(`/api/algorithms/${algorithmId}/parameters`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error ${response.status}`);
            }
            return response.json();
        })
        .then(parameters => {
            // Fetch default parameters separately
            fetch(`/api/algorithms/${algorithmId}/default-parameters`)
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`HTTP error ${response.status}`);
                    }
                    return response.json();
                })
                .then(defaultParameters => {
                    populateAlgorithmParameters({
                        id: algorithmId,
                        parameters: parameters,
                        defaultParameters: defaultParameters
                    }, paramsContainer);
                })
                .catch(error => {
                    console.error('Error fetching default parameters:', error);
                    // Use hardcoded fallback defaults if API fails
                    populateAlgorithmParameters({
                        id: algorithmId,
                        parameters: parameters,
                        defaultParameters: getDefaultParameters(algorithmId)
                    }, paramsContainer);
                });
        })
        .catch(error => {
            console.error('Error fetching algorithm parameters:', error);
            // Last resort fallback using hardcoded values for both parameters and defaults
            populateAlgorithmParameters({
                id: algorithmId,
                parameters: getDefaultParameters(algorithmId),
                defaultParameters: getDefaultParameters(algorithmId)
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
                positionSize: "0.1",
                feeRate: "0.002",
                taxRate: "0.15"
            };
        case 'arbitrage':
            return {
                minProfitPercentage: "Minimum profit percentage to execute arbitrage (e.g., 1.5)",
                maxTransactionFee: "Maximum transaction fee percentage (e.g., 0.5)",
                positionSize: "0.1",
                feeRate: "0.002",
                taxRate: "0.15"
            };
        case 'relative-strength-index':
            return {
                period: "14",
                overboughtThreshold: "70",
                oversoldThreshold: "30",
                positionSize: "0.1",
                feeRate: "0.002",
                taxRate: "0.15"
            };
        case 'bollinger-bands':
            return {
                period: "20",
                deviationMultiple: "2.0",
                positionSize: "0.1",
                feeRate: "0.002",
                taxRate: "0.15"
            };
        default:
            return {
                period: "Time period parameter",
                threshold: "Threshold value for decisions",
                positionSize: "0.1",
                feeRate: "0.002",
                taxRate: "0.15"
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
        // First, completely empty the container to prevent parameter overlap
        container.innerHTML = '';
        
        // Add a title for the parameters section
        const title = document.createElement('h6');
        title.className = 'mt-3 mb-2';
        title.textContent = 'Algorithm Parameters';
        container.appendChild(title);
        
        // Create input fields for each parameter
        for (const [param, description] of Object.entries(algorithm.parameters)) {
            const paramDiv = document.createElement('div');
            paramDiv.className = 'mb-3 algorithm-param-field';
            paramDiv.dataset.paramName = param; // Add data attribute to track the parameter name
            
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
            
            // Determine the default value from the defaultParameters if available
            let defaultValue = '';
            
            // Check if default parameters are available from the API
            if (algorithm.defaultParameters && algorithm.defaultParameters[param] !== undefined) {
                defaultValue = algorithm.defaultParameters[param];
                console.log(`Using default value from API for ${param}: ${defaultValue}`);
            } else {
                // Set default values based on parameter names as fallback
                if (param.toLowerCase().includes('period')) {
                    if (param.toLowerCase().includes('short')) {
                        defaultValue = 5;
                    } else if (param.toLowerCase().includes('long')) {
                        defaultValue = 20;
                    } else {
                        defaultValue = 10;
                    }
                } else if (param.toLowerCase().includes('amount')) {
                    defaultValue = 0.1;
                } else if (param.toLowerCase() === 'maxslippage') {
                    defaultValue = 0.5;
                } else if (param.toLowerCase().includes('percentage')) {
                    defaultValue = 1.5;
                } else if (param.toLowerCase() === 'feerate') {
                    defaultValue = 0.002;
                } else if (param.toLowerCase() === 'taxrate') {
                    defaultValue = 0.15;
                } else if (param.toLowerCase().includes('threshold')) {
                    if (param.toLowerCase().includes('overbought')) {
                        defaultValue = 70;
                    } else if (param.toLowerCase().includes('oversold')) {
                        defaultValue = 30;
                    }
                } else if (param.toLowerCase().includes('deviation')) {
                    defaultValue = 2.0;
                }
            }
            
            // Set the input type based on the parameter name
            if (param.toLowerCase().includes('period') ||
                param.toLowerCase().includes('threshold') ||
                param.toLowerCase().includes('deviation')) {
                input.type = 'number';
                input.step = '0.1';
            } else if (param.toLowerCase().includes('amount') ||
                     param.toLowerCase().includes('size') ||
                     param.toLowerCase().includes('tax') ||
                     param.toLowerCase().includes('percentage')) {
                input.type = 'number';
                input.step = '0.01';
            } else if (param.toLowerCase() === 'feerate') {
                input.type = 'number';
                input.step = 'any';
                input.min = '0';
            } else if (param.toLowerCase().includes('fee')) {
                input.type = 'number';
                input.step = '0.01';
            }
            
            // Set the value from the default
            input.value = defaultValue;
            
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
            let paramValue = input.value.trim();
            
            // Convert to number if possible
            if (!isNaN(paramValue) && paramValue !== '') {
                paramValue = parseFloat(paramValue);
                
                // Ensure we don't send NaN or undefined values
                if (isNaN(paramValue)) {
                    console.warn(`Parameter ${paramName} value could not be parsed as a number: "${input.value}"`);
                    return; // Skip this parameter
                }
                
                // Provide special handling for arbitrage algorithm
                if (algorithmId === 'arbitrage') {
                    // Ensure these parameters are treated as numbers
                    if (['minProfitPercentage', 'maxSlippage', 'tradeAmount', 'positionSize', 'feeRate', 'taxRate'].includes(paramName)) {
                        // Explicitly convert to double to avoid integer conversion issues
                        algorithmParams[paramName] = paramValue;
                        console.log(`Setting ${paramName} to ${paramValue} (number)`);
                    } else {
                        algorithmParams[paramName] = paramValue;
                        console.log(`Setting ${paramName} to ${paramValue}`);
                    }
                } else {
                    algorithmParams[paramName] = paramValue;
                    console.log(`Setting ${paramName} to ${paramValue}`);
                }
            } else {
                // String value or empty
                algorithmParams[paramName] = paramValue;
                console.log(`Setting ${paramName} to "${paramValue}" (string)`);
            }
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
        
        // Debug output the full request body
        console.log('Backtest request payload:', JSON.stringify(requestBody, null, 2));
        
        // Use a single API endpoint for consistency
        fetch('/api/backtest', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        })
        .then(response => {
            if (!response.ok) {
                // Try to get more detailed error information from the response
                return response.text().then(text => {
                    try {
                        // Try to parse as JSON if possible
                        const errorData = JSON.parse(text);
                        throw new Error(errorData.message || errorData.error || `Server error: ${response.status}`);
                    } catch (parseError) {
                        // If not JSON, use the text or a generic message
                        throw new Error(text || `Backtest endpoint failed with status: ${response.status}. Check server logs.`);
                    }
                });
            }
            return response.json();
        })
        .then(data => {
            console.log('Backtest response:', data);
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
                        <p>Algorithm: ${algorithmId}, Parameters: ${JSON.stringify(algorithmParams)}</p>
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
        
        // Check if the data is simulated and show a warning
        if (results.isSimulatedData === true) {
            // Create or update simulation warning
            let simulationWarningEl = document.getElementById('simulation-warning');
            if (!simulationWarningEl) {
                simulationWarningEl = document.createElement('div');
                simulationWarningEl.id = 'simulation-warning';
                simulationWarningEl.className = 'alert alert-warning mb-4';
                simulationWarningEl.innerHTML = '<strong>Note:</strong> These results are based on simulated data since historical market data was not available for the selected period.';
                
                // Insert at the top of the results container
                if (resultsEl && resultsEl.firstChild) {
                    resultsEl.insertBefore(simulationWarningEl, resultsEl.firstChild);
                } else if (resultsEl) {
                    resultsEl.appendChild(simulationWarningEl);
                }
            } else {
                simulationWarningEl.classList.remove('d-none');
            }
        } else {
            // Hide simulation warning if it exists
            const simulationWarningEl = document.getElementById('simulation-warning');
            if (simulationWarningEl) {
                simulationWarningEl.classList.add('d-none');
            }
        }
        
        // Update summary information
        const initialCapital = results.initialCapital || 10000;
        // For proper portfolio calculation, we need to track:
        // 1. Available cash capital 
        // 2. Crypto holdings and their current market value
        
        // Initialize tracking variables
        let finalCapital = initialCapital;
        let availableCash = initialCapital;
        let cryptoHoldings = 0;
        let lastPrice = 0;
        
        // Process all trades to accurately track cash and crypto positions
        if (results.generatedOrders && results.generatedOrders.length > 0) {
            console.log(`Starting portfolio tracking: Initial capital = $${initialCapital}`);
            
            // Process each order to update cash and crypto positions
            for (const order of results.generatedOrders) {
                lastPrice = order.price; // Update price with each order
                
                if (order.type === 'BUY') {
                    // When buying, subtract the full purchase cost (including fees) from cash
                    const purchaseCost = (order.amount * order.price) + (order.feeAmount || order.fee || 0);
                    availableCash -= purchaseCost;
                    cryptoHoldings += order.amount;
                    
                    console.log(`BUY: ${order.amount} @ $${order.price} = $${order.amount * order.price} + $${order.feeAmount || order.fee || 0} fee`);
                    console.log(`Portfolio after BUY: Cash $${availableCash}, Crypto ${cryptoHoldings} units worth $${cryptoHoldings * order.price}`);
                    
                } else if (order.type === 'SELL') {
                    // When selling, add proceeds (minus fees and tax) to cash
                    const saleProceeds = (order.amount * order.price) - (order.feeAmount || order.fee || 0);
                    // Tax doesn't impact available cash directly, it's just a future liability
                    availableCash += saleProceeds;
                    cryptoHoldings -= order.amount;
                    
                    console.log(`SELL: ${order.amount} @ $${order.price} = $${order.amount * order.price} - $${order.feeAmount || order.fee || 0} fee`);
                    console.log(`Portfolio after SELL: Cash $${availableCash}, Crypto ${cryptoHoldings} units worth $${cryptoHoldings * order.price}`);
                }
            }
            
            // Calculate final portfolio value: cash + crypto holdings at last known price
            let cryptoValue = cryptoHoldings * lastPrice;
            finalCapital = availableCash + cryptoValue;
            
            // Detailed logging of final portfolio value calculation
            console.log('--------- FINAL PORTFOLIO CALCULATION ---------');
            console.log(`Cash: $${availableCash.toFixed(2)}`);
            console.log(`Crypto: ${cryptoHoldings} units @ $${lastPrice} = $${cryptoValue.toFixed(2)}`);
            console.log(`Total portfolio value: $${finalCapital.toFixed(2)}`);
            console.log(`Initial capital: $${initialCapital.toFixed(2)}`);
            console.log(`Return: ${((finalCapital / initialCapital - 1) * 100).toFixed(2)}%`);
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
        displayTradeStatistics(results.generatedOrders, results);
        
        // Display trades
        displayTrades(results.generatedOrders, results);
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
        const initialCapital = results.initialCapital || 10000;
        
        // Initialize tracking variables
        let availableCash = initialCapital;
        let cryptoHoldings = 0;
        let lastPrice = 0;
        
        // Add initial point
        equityData.push({
            date: new Date(results.startTime || new Date().toISOString()),
            equity: initialCapital
        });
        
        // Process each order and calculate portfolio value at each point
        orders.forEach(order => {
            lastPrice = order.price;
            
            if (order.type === 'BUY') {
                // When buying, subtract purchase cost from cash and add crypto
                const purchaseCost = (order.amount * order.price) + (order.feeAmount || order.fee || 0);
                availableCash -= purchaseCost;
                cryptoHoldings += order.amount;
            } else if (order.type === 'SELL') {
                // When selling, add proceeds to cash and reduce crypto
                const saleProceeds = (order.amount * order.price) - (order.feeAmount || order.fee || 0);
                availableCash += saleProceeds;
                cryptoHoldings -= order.amount;
            }
            
            // Calculate total portfolio value (cash + crypto holdings)
            const cryptoValue = cryptoHoldings * lastPrice;
            const totalEquity = availableCash + cryptoValue;
            
            console.log(`Equity chart - After ${order.type}: Cash $${availableCash.toFixed(2)}, Crypto ${cryptoHoldings} ($${cryptoValue.toFixed(2)}), Total $${totalEquity.toFixed(2)}`);
            
            equityData.push({
                date: new Date(order.createdAt || new Date().toISOString()),
                equity: totalEquity
            });
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
        
        // Get total fees and taxes from the backend metrics if available, or calculate from orders
        let totalFees = 0;
        let totalTaxes = 0;
        
        // Use backend-calculated metrics if available
        if (results.metrics && typeof results.metrics.totalFees !== 'undefined') {
            totalFees = results.metrics.totalFees;
            totalTaxes = results.metrics.totalTaxes || 0;
            console.log("Using backend metrics - Total fees:", totalFees, "Total taxes:", totalTaxes);
        } else if (orders.length > 0) {
            // Fallback to manual calculation only if backend metrics aren't available
            console.log("Backend metrics not available, calculating manually");
            orders.forEach(order => {
                totalFees += (order.feeAmount || order.fee || 0);
                
                // Only count positive tax values
                let taxAmount = 0;
                if (typeof order.estimatedTaxLiability !== 'undefined' && order.estimatedTaxLiability > 0) {
                    taxAmount = order.estimatedTaxLiability;
                } else if (typeof order.tax !== 'undefined' && order.tax > 0) {
                    taxAmount = order.tax;
                }
                totalTaxes += taxAmount;
            });
        }
        
        // Calculate drawdown using proper portfolio accounting
        let maxDrawdown = 0;
        let maxEquity = initialCapital;
        let currentDrawdown = 0;
        
        if (orders.length > 0) {
            // Initialize portfolio tracking
            let availableCash = initialCapital;
            let cryptoHoldings = 0;
            let lastPrice = 0;
            
            orders.forEach(order => {
                lastPrice = order.price;
                
                if (order.type === 'BUY') {
                    // Update cash and crypto holdings
                    const purchaseCost = (order.amount * order.price) + (order.feeAmount || order.fee || 0);
                    availableCash -= purchaseCost;
                    cryptoHoldings += order.amount;
                } else if (order.type === 'SELL') {
                    // Update cash and crypto holdings
                    const saleProceeds = (order.amount * order.price) - (order.feeAmount || order.fee || 0);
                    availableCash += saleProceeds;
                    cryptoHoldings -= order.amount;
                }
                
                // Calculate total portfolio value
                const cryptoValue = cryptoHoldings * lastPrice;
                const portfolioValue = availableCash + cryptoValue;
                
                // Update maximum drawdown
                if (portfolioValue > maxEquity) {
                    maxEquity = portfolioValue;
                    currentDrawdown = 0;
                } else {
                    currentDrawdown = (maxEquity - portfolioValue) / maxEquity * 100;
                    if (currentDrawdown > maxDrawdown) {
                        maxDrawdown = currentDrawdown;
                    }
                }
                
                console.log(`Drawdown calculation - Portfolio: $${portfolioValue.toFixed(2)}, Max: $${maxEquity.toFixed(2)}, Current drawdown: ${currentDrawdown.toFixed(2)}%, Max drawdown: ${maxDrawdown.toFixed(2)}%`);
            });
        }
        
        // Calculate net profit (after fees and taxes)
        const netProfit = profit - totalFees - totalTaxes;
        const netReturnPercent = (netProfit / initialCapital) * 100;
        
        // Add metrics to the table
        const metrics = [
            { name: 'Total Return (gross)', value: `${returnPercent >= 0 ? '+' : ''}${returnPercent.toFixed(2)}%`, positive: returnPercent >= 0 },
            { name: 'Total Return (net)', value: `${netReturnPercent >= 0 ? '+' : ''}${netReturnPercent.toFixed(2)}%`, positive: netReturnPercent >= 0 },
            { name: 'Annualized Return', value: `${annualizedReturn >= 0 ? '+' : ''}${annualizedReturn.toFixed(2)}%`, positive: annualizedReturn >= 0 },
            { name: 'Max Drawdown', value: `-${maxDrawdown.toFixed(2)}%`, positive: false },
            { name: 'Total Trading Fees', value: formatCurrency(totalFees), positive: false },
            { name: 'Total Taxes', value: formatCurrency(totalTaxes), positive: false },
            { name: 'Profit/Loss (gross)', value: formatCurrency(profit), positive: profit >= 0 },
            { name: 'Profit/Loss (net)', value: formatCurrency(netProfit), positive: netProfit >= 0 },
            { name: 'Test Period', value: `${tradeDays} days` }
        ];
        
        metrics.forEach(metric => {
            const row = document.createElement('tr');
            
            const nameCell = document.createElement('td');
            nameCell.textContent = metric.name;
            
            const valueCell = document.createElement('td');
            valueCell.textContent = metric.value;
            
            // Color coding: green for positive values, red for negative, neutral for informational fields
            if (metric.name === 'Test Period') {
                // No special coloring for test period
            } else if (metric.name === 'Total Trading Fees' || metric.name === 'Total Taxes' || metric.name === 'Max Drawdown') {
                // These are always costs, but don't use negative coloring if zero
                valueCell.className = (parseFloat(metric.value) === 0) ? '' : 'text-danger';
            } else if (metric.hasOwnProperty('positive')) {
                valueCell.className = metric.positive ? 'text-success' : 'text-danger';
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
 * @param {Object} results - Optional backtest results with backend metrics
 */
function displayTradeStatistics(orders, results) {
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
        
        // Prepare statistics - use backend metrics when available
        let totalTrades, buyCount, sellCount, totalFees, totalTaxes, averageTradeSize, 
            averageFeePerTrade, averageTaxPerTrade;
        
        // Check if we have metrics from the backend
        if (results && results.metrics) {
            console.log("Using backend statistics from metrics:", results.metrics);
            
            // Use values from backend metrics when available
            totalTrades = results.metrics.totalTrades || orders.length;
            buyCount = results.metrics.buyCount || 0;
            sellCount = results.metrics.sellCount || 0;
            totalFees = results.metrics.totalFees || 0;
            totalTaxes = results.metrics.totalTaxes || 0;
            averageTradeSize = results.metrics.averageTradeSize || 0;
            averageFeePerTrade = results.metrics.averageFeePerTrade || (totalFees / Math.max(1, totalTrades));
            averageTaxPerTrade = results.metrics.averageTaxPerTrade || (totalTaxes / Math.max(1, totalTrades));
        } else {
            console.log("Calculating statistics manually from orders");
            
            // Manual calculation as before
            totalTrades = orders.length;
            averageTradeSize = orders.reduce((sum, order) => {
                return sum + ((order.price || 0) * (order.amount || 0));
            }, 0) / totalTrades;
            
            // Calculate buy/sell counts
            buyCount = 0;
            sellCount = 0;
            orders.forEach(order => {
                if (order.type && order.type.toUpperCase() === 'BUY') {
                    buyCount++;
                } else if (order.type && order.type.toUpperCase() === 'SELL') {
                    sellCount++;
                }
            });
            
            // Calculate average fee and tax per trade
            totalFees = orders.reduce((sum, order) => sum + (order.feeAmount || order.fee || 0), 0);
            
            // Calculate total taxes properly from tax field
            totalTaxes = orders.reduce((sum, order) => {
                let taxAmount = 0;
                
                // Always use the tax field if it's available, as it's calculated by the backend
                if (typeof order.tax !== 'undefined') {
                    taxAmount = order.tax;
                }
                // Fallback to estimatedTaxLiability if tax is not available
                else if (typeof order.estimatedTaxLiability !== 'undefined') {
                    taxAmount = order.estimatedTaxLiability;
                }
                
                return sum + taxAmount;
            }, 0);
            
            averageFeePerTrade = totalFees / totalTrades;
            averageTaxPerTrade = totalTaxes / totalTrades;
        }
        
        // Get fee and tax rates if available from the first order
        const feeRate = (orders[0].feeRate || 0) * 100; // Convert to percentage
        const taxRate = (orders[0].taxRate || 0) * 100; // Convert to percentage
        
        // Add statistics to the table
        const statistics = [
            { name: 'Total Trades', value: totalTrades },
            { name: 'Buy Orders', value: buyCount },
            { name: 'Sell Orders', value: sellCount },
            { name: 'Average Trade Size', value: formatCurrency(averageTradeSize) },
            { name: 'Fee Rate', value: `${feeRate.toFixed(2)}%` },
            { name: 'Tax Rate', value: `${taxRate.toFixed(2)}%` },
            { name: 'Average Fee Per Trade', value: formatCurrency(averageFeePerTrade) },
            { name: 'Average Tax Per Trade', value: formatCurrency(averageTaxPerTrade) },
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
 * @param {Object} results - Optional backtest results with backend metrics
 */
function displayTrades(orders, results) {
    try {
        const tradesTable = document.getElementById('backtest-trades');
        
        if (!tradesTable) {
            console.error('Backtest trades table not found');
            return;
        }
        
        tradesTable.innerHTML = '';
        
        if (!orders || orders.length === 0) {
            const row = document.createElement('tr');
            row.innerHTML = '<td colspan="7" class="text-center">No trades executed</td>';
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
            const fee = order.feeAmount || order.fee || 0;
            
            // Get the tax value for this order, prioritizing certain fields
            let tax = 0;
            
            // Log tax-related fields for debugging
            console.log(`Order tax info - tax: ${order.tax}, taxableGain: ${order.taxableGain}, taxRate: ${order.taxRate}, estimatedTaxLiability: ${order.estimatedTaxLiability}`);
            
            // First try to get estimatedTaxLiability (from the backend)
            if (typeof order.estimatedTaxLiability !== 'undefined') {
                console.log(`Using estimatedTaxLiability: ${order.estimatedTaxLiability}`);
                tax = order.estimatedTaxLiability;
            } 
            // Directly use the tax field if present - this is set by the backend
            else if (typeof order.tax !== 'undefined') {
                console.log(`Using tax field: ${order.tax}`);
                tax = order.tax;
            }
            // Then try to get taxableGain (if present) - should be a fallback
            else if (typeof order.taxableGain !== 'undefined' && typeof order.taxRate !== 'undefined') {
                console.log(`Calculating tax from taxableGain: ${order.taxableGain} * ${order.taxRate}`);
                // Only apply tax on positive gains
                if (order.taxableGain > 0) {
                    tax = order.taxableGain * order.taxRate;
                } else {
                    tax = 0; // No tax on losses
                }
            }
            
            // Highlight buy/sell with different colors
            const typeClass = type.toUpperCase() === 'BUY' ? 'text-success' : 'text-danger';
            
            // Only display tax if positive (gains) or explicitly negative (losses/credits)
            // Negative values represent tax credits from capital losses
            let taxDisplay;
            if (tax > 0) {
                taxDisplay = formatCurrency(tax);
            } else if (tax < 0) {
                // Show tax credits with a different color and "(credit)" label
                taxDisplay = `<span class="text-info">${formatCurrency(Math.abs(tax))} (credit)</span>`;
            } else {
                taxDisplay = '$0.00';
            }
            
            row.innerHTML = `
                <td>${createdAt}</td>
                <td class="${typeClass}">${type}</td>
                <td>${formatCurrency(price)}</td>
                <td>${amount.toFixed(8)}</td>
                <td>${formatCurrency(price * amount)}</td>
                <td>${formatCurrency(fee)}</td>
                <td>${taxDisplay}</td>
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
