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
                        input.value = '0.001';  // smaller amount for live trading
                        input.step = '0.001';
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