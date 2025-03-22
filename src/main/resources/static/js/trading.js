/**
 * Trading JavaScript file for handling the live trading functionality
 */

/**
 * Initializes the trading form
 */
function initializeTradingForm() {
    const tradingForm = document.getElementById('trading-form');
    const tradingAlgorithm = document.getElementById('trading-algorithm');
    
    // Add event listener for algorithm selection
    tradingAlgorithm.addEventListener('change', loadTradingAlgorithmParameters);
    
    // Add event listener for form submission
    tradingForm.addEventListener('submit', startLiveTrading);
    
    // Add event listener for stop trading button
    document.getElementById('stop-live-trading-btn').addEventListener('click', stopLiveTrading);
}

/**
 * Loads the parameters for the selected trading algorithm
 */
function loadTradingAlgorithmParameters() {
    const algorithmId = document.getElementById('trading-algorithm').value;
    const paramsContainer = document.getElementById('trading-algorithm-params-container');
    
    // Clear existing parameters
    paramsContainer.innerHTML = '';
    
    if (!algorithmId) {
        return;
    }
    
    console.log('Loading parameters for algorithm:', algorithmId);
    
    // Fetch algorithm details from API
    fetch(`/api/algorithms/${algorithmId}`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error ${response.status}`);
            }
            return response.json();
        })
        .then(algorithm => {
            // Get default parameters
            fetch(`/api/algorithms/${algorithmId}/default-parameters`)
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`HTTP error ${response.status}`);
                    }
                    return response.json();
                })
                .then(defaultParameters => {
                    populateTradingAlgorithmParameters({
                        id: algorithmId,
                        parameters: algorithm.parameters,
                        defaultParameters: defaultParameters
                    }, paramsContainer);
                })
                .catch(error => {
                    console.error('Error fetching default parameters:', error);
                    // Use algorithm parameters with fallback defaults
                    populateTradingAlgorithmParameters({
                        id: algorithmId,
                        parameters: algorithm.parameters,
                        defaultParameters: getTradingDefaultParameters(algorithmId)
                    }, paramsContainer);
                });
        })
        .catch(error => {
            console.error('Error fetching algorithm details:', error);
            // Fallback to hardcoded defaults
            const defaultParams = getTradingDefaultParameters(algorithmId);
            
            populateTradingAlgorithmParameters({
                id: algorithmId,
                parameters: defaultParams,
                defaultParameters: defaultParams
            }, paramsContainer);
        });
}

/**
 * Populates algorithm parameters in the container for trading form
 * 
 * @param {Object} algorithm - The algorithm object
 * @param {HTMLElement} container - The container element
 */
function populateTradingAlgorithmParameters(algorithm, container) {
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
            label.setAttribute('for', `trading-param-${param}`);
            label.className = 'form-label';
            label.textContent = param;
            
            const input = document.createElement('input');
            input.type = 'text';
            input.className = 'form-control';
            input.id = `trading-param-${param}`;
            input.name = `trading-param-${param}`;
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
                } else if (param.toLowerCase().includes('position') && 
                          param.toLowerCase().includes('size')) {
                    defaultValue = 0.1;
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
                     param.toLowerCase().includes('fee') ||
                     param.toLowerCase().includes('tax') ||
                     param.toLowerCase().includes('percentage') ||
                     param.toLowerCase().includes('slippage')) {
                input.type = 'number';
                input.step = '0.01';
                
                // Use smaller steps for fee rates specifically
                if (param.toLowerCase() === 'feerate') {
                    input.step = '0.0001';
                }
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
 * Gets default parameters for a given algorithm ID for trading
 * 
 * @param {string} algorithmId - The algorithm ID
 * @returns {Object} Object with parameter defaults
 */
function getTradingDefaultParameters(algorithmId) {
    switch (algorithmId) {
        case 'simple-moving-average':
            return {
                shortPeriod: 5,
                longPeriod: 20,
                positionSize: 0.1,
                feeRate: 0.002,
                taxRate: 0.15
            };
        case 'arbitrage':
            return {
                minProfitPercentage: 1.5,
                maxTransactionFee: 0.5,
                positionSize: 0.1,
                feeRate: 0.002,
                taxRate: 0.15
            };
        case 'relative-strength-index':
            return {
                period: 14,
                overboughtThreshold: 70,
                oversoldThreshold: 30,
                positionSize: 0.1,
                feeRate: 0.002,
                taxRate: 0.15
            };
        case 'bollinger-bands':
            return {
                period: 20,
                deviationMultiple: 2.0,
                positionSize: 0.1,
                feeRate: 0.002,
                taxRate: 0.15
            };
        default:
            return {
                period: 10,
                threshold: 0.5,
                positionSize: 0.1,
                feeRate: 0.002,
                taxRate: 0.15
            };
    }
}

/**
 * Starts live trading with the specified parameters
 * 
 * @param {Event} event - The form submission event
 */
function startLiveTrading(event) {
    event.preventDefault();
    
    // Check if API credentials are configured
    const isCredentialsConfigured = confirm(
        'To proceed with live trading, you need to have API credentials configured for the selected exchange. ' +
        'Do you have valid API credentials configured?'
    );
    
    if (!isCredentialsConfigured) {
        alert('Please configure API credentials before starting live trading.');
        return;
    }
    
    // Get form values
    const algorithmId = document.getElementById('trading-algorithm').value;
    const exchange = document.getElementById('trading-exchange').value;
    const tradingPair = document.getElementById('trading-pair').value;
    
    // Collect algorithm parameters
    const algorithmParams = {};
    const paramInputs = document.querySelectorAll('#trading-algorithm-params-container input');
    
    paramInputs.forEach(input => {
        const paramName = input.id.replace('trading-param-', '');
        let paramValue = input.value;
        
        // Convert to number if possible
        if (!isNaN(paramValue)) {
            paramValue = parseFloat(paramValue);
        }
        
        algorithmParams[paramName] = paramValue;
    });
    
    // Create request body
    const requestBody = {
        command: 'start',
        algorithmId,
        exchange,
        tradingPair,
        parameters: algorithmParams
    };
    
    // Start live trading
    fetch('/api/trading/system-control', {
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
        alert(data.message || 'Live trading started successfully');
        updateLiveTradingStatus(true, algorithmId, exchange, tradingPair);
    })
    .catch(error => {
        console.error('Error starting live trading:', error);
        alert('Failed to start live trading. See console for details.');
    });
}

/**
 * Stops live trading
 */
function stopLiveTrading() {
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
        alert(data.message || 'Live trading stopped successfully');
        updateLiveTradingStatus(false);
    })
    .catch(error => {
        console.error('Error stopping live trading:', error);
        alert('Failed to stop live trading. See console for details.');
    });
}

/**
 * Updates the live trading status display
 * 
 * @param {boolean} isActive - Whether trading is active
 * @param {string} algorithmId - The algorithm ID
 * @param {string} exchange - The exchange name
 * @param {string} tradingPair - The trading pair
 */
function updateLiveTradingStatus(isActive, algorithmId = null, exchange = null, tradingPair = null) {
    const statusElement = document.getElementById('live-trading-status');
    const algorithmElement = document.getElementById('live-algorithm');
    const exchangeElement = document.getElementById('live-exchange');
    const pairElement = document.getElementById('live-pair');
    const runningSinceElement = document.getElementById('live-running-since');
    const stopButton = document.getElementById('stop-live-trading-btn');
    const startButton = document.getElementById('start-live-trading-btn');
    
    if (isActive) {
        statusElement.textContent = 'Active';
        statusElement.className = 'badge bg-success';
        
        fetch(`/api/algorithms/${algorithmId}`)
            .then(response => response.json())
            .then(algorithm => {
                algorithmElement.textContent = algorithm.name;
            })
            .catch(error => {
                algorithmElement.textContent = algorithmId;
                console.error('Error fetching algorithm details:', error);
            });
        
        exchangeElement.textContent = exchange;
        pairElement.textContent = tradingPair;
        runningSinceElement.textContent = new Date().toLocaleString();
        
        stopButton.disabled = false;
        startButton.disabled = true;
    } else {
        statusElement.textContent = 'Inactive';
        statusElement.className = 'badge bg-secondary';
        
        algorithmElement.textContent = 'None';
        exchangeElement.textContent = '-';
        pairElement.textContent = '-';
        runningSinceElement.textContent = '-';
        
        stopButton.disabled = true;
        startButton.disabled = false;
    }
}