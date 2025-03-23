/**
 * AI Advisor JavaScript file for handling AI-powered trading strategy recommendations
 */

// Store recommended parameters for each algorithm to pass between tabs
let recommendedAlgorithmParameters = {};

// Initialize parameters from localStorage on page load
document.addEventListener('DOMContentLoaded', function() {
    try {
        const storedParams = localStorage.getItem('recommendedAlgorithmParameters');
        if (storedParams) {
            recommendedAlgorithmParameters = JSON.parse(storedParams);
            console.log('Loaded recommended parameters from localStorage:', recommendedAlgorithmParameters);
        }
    } catch (error) {
        console.error('Error loading parameters from localStorage:', error);
        recommendedAlgorithmParameters = {};
    }
});

/**
 * Initializes the AI advisor
 */
function initializeAIAdvisor() {
    try {
        // Set up event listeners
        const analysisForm = document.getElementById('ai-advisor-form');
        const getAnalysisBtn = document.getElementById('get-ai-analysis-btn');
        
        if (getAnalysisBtn) {
            getAnalysisBtn.addEventListener('click', getAIRecommendations);
        }
        
        // Log the available recommended parameters from localStorage
        if (Object.keys(recommendedAlgorithmParameters).length > 0) {
            console.log('AI Advisor initialized with existing parameters:', recommendedAlgorithmParameters);
        } else {
            console.log('AI Advisor initialized with no existing parameters');
        }
        
        console.log('AI Advisor initialized');
    } catch (error) {
        console.error('Error initializing AI Advisor:', error);
    }
}

/**
 * Gets AI recommendations for the selected market
 */
function getAIRecommendations() {
    try {
        // Get selected exchange and pair
        const exchange = document.getElementById('ai-advisor-exchange').value;
        const tradingPair = document.getElementById('ai-advisor-pair').value;
        
        // Show loading indicator
        const loadingElement = document.getElementById('ai-analysis-loading');
        const noResultsElement = document.getElementById('ai-analysis-no-results');
        const contentElement = document.getElementById('ai-analysis-content');
        const strategiesEmptyElement = document.getElementById('ai-strategies-empty');
        const strategiesContainer = document.getElementById('ai-strategies-container');
        
        loadingElement.classList.remove('d-none');
        noResultsElement.classList.add('d-none');
        contentElement.classList.add('d-none');
        strategiesEmptyElement.classList.add('d-none');
        strategiesContainer.classList.add('d-none');
        
        // Fetch AI recommendations from the backend
        fetch(`/api/v1/ai/recommendations?exchange=${encodeURIComponent(exchange)}&tradingPair=${encodeURIComponent(tradingPair)}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                console.log('AI analysis received:', data);
                
                // Store the recommended parameters for each algorithm in the global variable
                if (data.algorithmSuggestions && data.algorithmSuggestions.length > 0) {
                    data.algorithmSuggestions.forEach(suggestion => {
                        if (suggestion.algorithmId && suggestion.recommendedParameters) {
                            recommendedAlgorithmParameters[suggestion.algorithmId] = suggestion.recommendedParameters;
                        }
                    });
                }
                
                displayAIAnalysis(data);
            })
            .catch(error => {
                console.error('Error fetching AI recommendations:', error);
                loadingElement.classList.add('d-none');
                
                // For testing purposes, generate sample recommendations data with valid parameters
                if (confirm('Could not fetch AI recommendations from server. Would you like to use sample data for testing?')) {
                    const sampleData = generateSampleAIRecommendations(exchange, tradingPair);
                    console.log('Using sample AI recommendations for testing:', sampleData);
                    
                    // Store the sample recommended parameters for each algorithm
                    if (sampleData.algorithmSuggestions && sampleData.algorithmSuggestions.length > 0) {
                        sampleData.algorithmSuggestions.forEach(suggestion => {
                            if (suggestion.algorithmId && suggestion.recommendedParameters) {
                                recommendedAlgorithmParameters[suggestion.algorithmId] = suggestion.recommendedParameters;
                                console.log(`Stored sample parameters for ${suggestion.algorithmId}:`, suggestion.recommendedParameters);
                            }
                        });
                    }
                    
                    displayAIAnalysis(sampleData);
                } else {
                    noResultsElement.classList.remove('d-none');
                    
                    // Show error message
                    noResultsElement.innerHTML = `
                        <div class="alert alert-danger">
                            <strong>Error:</strong> Could not get AI recommendations. ${error.message}
                        </div>
                    `;
                }
            });
    } catch (error) {
        console.error('Error in getAIRecommendations:', error);
    }
}

/**
 * Generates sample AI recommendations for testing
 * 
 * @param {string} exchange - Exchange name
 * @param {string} tradingPair - Trading pair
 * @returns {Object} Sample AI recommendation data
 */
function generateSampleAIRecommendations(exchange, tradingPair) {
    const now = new Date();
    
    // Sample algorithm suggestions with valid parameters
    const algorithmSuggestions = [
        {
            algorithmId: 'simple-moving-average',
            algorithmName: 'Simple Moving Average Crossover',
            confidenceScore: 8.5,
            expectedReturnPercent: 4.2,
            reasoning: 'This trading pair is showing consistent trends with moderate volatility, making an SMA crossover strategy effective.',
            recommendedParameters: {
                shortPeriod: 5,
                longPeriod: 20,
                positionSize: 10,
                feeRate: 0.2,
                taxRate: 15
            }
        },
        {
            algorithmId: 'bollinger-bands',
            algorithmName: 'Bollinger Bands',
            confidenceScore: 7.8,
            expectedReturnPercent: 3.5,
            reasoning: 'The market is showing mean-reverting behavior with periodic overshoots, making Bollinger Bands a good strategy.',
            recommendedParameters: {
                period: 20,
                standardDeviation: 2.0,
                positionSize: 10,
                feeRate: 0.2,
                taxRate: 15
            }
        },
        {
            algorithmId: 'relative-strength-index',
            algorithmName: 'RSI (Relative Strength Index)',
            confidenceScore: 6.9,
            expectedReturnPercent: 2.8,
            reasoning: 'The market is showing periodic overbought and oversold conditions that can be captured by RSI.',
            recommendedParameters: {
                period: 14,
                overboughtThreshold: 70,
                oversoldThreshold: 30,
                positionSize: 10,
                feeRate: 0.2,
                taxRate: 15
            }
        }
    ];
    
    return {
        exchange: exchange,
        tradingPair: tradingPair,
        timestamp: now.toISOString(),
        marketTrend: 'Bullish',
        marketSentiment: 'Positive',
        volatilityScore: 6.5,
        analysisExplanation: 'The market is showing strong bullish momentum with moderate volatility. Technical indicators suggest a continuation of the uptrend in the short term.',
        algorithmSuggestions: algorithmSuggestions
    };
}

/**
 * Displays the AI analysis and recommendations
 * 
 * @param {Object} analysis - The analysis data from the AI
 */
function displayAIAnalysis(analysis) {
    try {
        // Hide loading and show content
        const loadingElement = document.getElementById('ai-analysis-loading');
        const contentElement = document.getElementById('ai-analysis-content');
        const strategiesEmptyElement = document.getElementById('ai-strategies-empty');
        const strategiesContainer = document.getElementById('ai-strategies-container');
        
        loadingElement.classList.add('d-none');
        contentElement.classList.remove('d-none');
        
        // Update the timestamp
        const timestampElement = document.getElementById('ai-analysis-timestamp');
        const formattedTimestamp = new Date(analysis.timestamp).toLocaleString();
        timestampElement.textContent = `Analysis as of ${formattedTimestamp}`;
        
        // Update market analysis
        document.getElementById('market-trend').textContent = analysis.marketTrend;
        document.getElementById('market-sentiment').textContent = analysis.marketSentiment;
        document.getElementById('volatility-score').textContent = analysis.volatilityScore.toFixed(1) + '/10';
        document.getElementById('analysis-explanation').textContent = analysis.analysisExplanation;
        
        // Update trend and sentiment styling
        const trendElement = document.getElementById('market-trend');
        if (analysis.marketTrend === 'Bullish') {
            trendElement.classList.add('text-success');
            trendElement.classList.remove('text-danger', 'text-warning');
        } else if (analysis.marketTrend === 'Bearish') {
            trendElement.classList.add('text-danger');
            trendElement.classList.remove('text-success', 'text-warning');
        } else {
            trendElement.classList.add('text-warning');
            trendElement.classList.remove('text-success', 'text-danger');
        }
        
        // Display algorithm suggestions
        if (analysis.algorithmSuggestions && analysis.algorithmSuggestions.length > 0) {
            strategiesEmptyElement.classList.add('d-none');
            strategiesContainer.classList.remove('d-none');
            
            // Clear previous suggestions
            strategiesContainer.innerHTML = '';
            
            // Clear the recommended parameters storage only for displayed algorithms
            // to avoid wiping out other algorithm data that might be useful
            const newRecommendedParams = { ...recommendedAlgorithmParameters };
            
            // Sort suggestions by confidence score (descending)
            const sortedSuggestions = [...analysis.algorithmSuggestions].sort((a, b) => 
                b.confidenceScore - a.confidenceScore
            );
            
            // Store the recommended parameters for each algorithm
            sortedSuggestions.forEach(suggestion => {
                // Store the parameters for later use
                if (suggestion.algorithmId && suggestion.recommendedParameters) {
                    newRecommendedParams[suggestion.algorithmId] = suggestion.recommendedParameters;
                    console.log(`Stored recommended parameters for ${suggestion.algorithmId}:`, suggestion.recommendedParameters);
                }
                
                // Create cards for each suggestion
                const algorithmCard = createAlgorithmSuggestionCard(suggestion);
                strategiesContainer.appendChild(algorithmCard);
            });
            
            // Update the recommended parameters
            recommendedAlgorithmParameters = newRecommendedParams;
            
            // Save to localStorage
            try {
                localStorage.setItem('recommendedAlgorithmParameters', JSON.stringify(recommendedAlgorithmParameters));
                console.log('Saved recommended parameters to localStorage');
            } catch (error) {
                console.error('Error saving to localStorage:', error);
            }
        } else {
            strategiesEmptyElement.classList.remove('d-none');
            strategiesContainer.classList.add('d-none');
        }
    } catch (error) {
        console.error('Error displaying AI analysis:', error);
    }
}

/**
 * Creates a card for an algorithm suggestion
 * 
 * @param {Object} suggestion - The algorithm suggestion data
 * @returns {HTMLElement} The algorithm suggestion card
 */
function createAlgorithmSuggestionCard(suggestion) {
    try {
        const colDiv = document.createElement('div');
        colDiv.className = 'col-md-6 mb-4';
        
        // Determine confidence class
        let confidenceClass = 'bg-warning';
        if (suggestion.confidenceScore >= 7) {
            confidenceClass = 'bg-success';
        } else if (suggestion.confidenceScore <= 4) {
            confidenceClass = 'bg-danger';
        }
        
        // Create HTML content
        colDiv.innerHTML = `
            <div class="card h-100">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <h5 class="mb-0">${suggestion.algorithmName}</h5>
                    <span class="badge ${confidenceClass} confidence-badge">${suggestion.confidenceScore.toFixed(1)}/10</span>
                </div>
                <div class="card-body">
                    <p>${suggestion.reasoning}</p>
                    <div class="mb-3">
                        <h6>Expected Return:</h6>
                        <h5 class="${suggestion.expectedReturnPercent >= 0 ? 'text-success' : 'text-danger'}">
                            ${suggestion.expectedReturnPercent >= 0 ? '+' : ''}${suggestion.expectedReturnPercent.toFixed(2)}%
                        </h5>
                    </div>
                    <div>
                        <h6>Recommended Parameters:</h6>
                        <div class="table-responsive">
                            <table class="table table-sm">
                                <tbody id="params-${suggestion.algorithmId}">
                                </tbody>
                            </table>
                        </div>
                    </div>
                    <div class="mt-3">
                        <a href="#" class="btn btn-sm btn-outline-primary use-params-link" data-algorithm-id="${suggestion.algorithmId}" 
                           onclick="useRecommendedParameters('${suggestion.algorithmId}'); return false;">
                            Use These Parameters
                        </a>
                        <a href="#" class="btn btn-sm btn-outline-success backtest-link" 
                           onclick="backtestWithParameters('${suggestion.algorithmId}'); return false;">
                            Backtest Strategy
                        </a>
                    </div>
                </div>
            </div>
        `;
        
        // Add parameters to the table
        const paramsTable = colDiv.querySelector(`#params-${suggestion.algorithmId}`);
        if (paramsTable && suggestion.recommendedParameters) {
            for (const [key, value] of Object.entries(suggestion.recommendedParameters)) {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td>${formatParameterName(key)}</td>
                    <td>${value}</td>
                `;
                paramsTable.appendChild(row);
            }
        }
        
        return colDiv;
    } catch (error) {
        console.error('Error creating algorithm suggestion card:', error);
        return document.createElement('div');
    }
}

/**
 * Formats a parameter name for display
 * 
 * @param {string} name - The parameter name in camelCase or snakeCase
 * @returns {string} The formatted parameter name
 */
function formatParameterName(name) {
    return name
        // Insert a space before all uppercase letters
        .replace(/([A-Z])/g, ' $1')
        // Replace underscores with spaces
        .replace(/_/g, ' ')
        // Capitalize the first letter
        .replace(/^./, str => str.toUpperCase())
        // Trim any extra spaces
        .trim();
}

/**
 * Uses the recommended parameters for the selected algorithm
 * 
 * @param {string} algorithmId - The ID of the algorithm
 */
function useRecommendedParameters(algorithmId) {
    try {
        // Make sure we have recommended parameters for this algorithm
        if (!recommendedAlgorithmParameters[algorithmId]) {
            console.error(`No recommended parameters found for algorithm ${algorithmId}`);
            return;
        }
        
        console.log(`Applying parameters for algorithm ${algorithmId}:`, recommendedAlgorithmParameters[algorithmId]);
        
        // Switch to the trading tab
        const tradingTab = document.getElementById('trading-tab');
        if (tradingTab) {
            tradingTab.click();
            
            // Set the algorithm in the trading form
            setTimeout(() => {
                const algorithmSelect = document.getElementById('trading-algorithm');
                if (algorithmSelect) {
                    console.log(`Setting trading algorithm selection to ${algorithmId}`);
                    // Set the algorithm selection
                    algorithmSelect.value = algorithmId;
                    
                    // Trigger the change event to load parameters
                    const event = new Event('change');
                    algorithmSelect.dispatchEvent(event);
                    
                    // Give some time for the parameters to be loaded
                    setTimeout(() => {
                        // Now set the recommended parameter values
                        const params = recommendedAlgorithmParameters[algorithmId];
                        
                        // Find all parameter input fields
                        const paramInputs = document.querySelectorAll('#trading-algorithm-params-container input');
                        
                        if (paramInputs.length === 0) {
                            console.error('No parameter input fields found in the trading form. Parameters may not have loaded yet.');
                            
                            // Try one more time with a longer delay
                            setTimeout(() => {
                                const retryParamInputs = document.querySelectorAll('#trading-algorithm-params-container input');
                                if (retryParamInputs.length === 0) {
                                    console.error('Still no parameter input fields found after retry.');
                                    alert('Could not set parameters. Please manually set your parameters.');
                                    return;
                                }
                                
                                applyParametersToInputs(retryParamInputs, params, algorithmId);
                            }, 2000);
                            return;
                        }
                        
                        applyParametersToInputs(paramInputs, params, algorithmId);
                    }, 1000); // Wait for the parameters to be loaded
                }
            }, 500);
        }
    } catch (error) {
        console.error('Error using recommended parameters:', error);
    }
}

/**
 * Applies algorithm parameters to input fields
 * 
 * @param {NodeList} inputElements - The input elements
 * @param {Object} params - The parameter values
 * @param {string} algorithmId - The algorithm ID
 */
function applyParametersToInputs(inputElements, params, algorithmId) {
    let parameterCount = 0;
    
    // Map algorithm parameter names to their equivalent trading form parameter names
    // This is needed because parameter names might vary slightly between components
    const parameterMap = {
        // Simple Moving Average
        'shortPeriod': 'shortPeriod',
        'longPeriod': 'longPeriod',
        // RSI
        'period': 'period',
        'overboughtThreshold': 'overboughtThreshold',
        'oversoldThreshold': 'oversoldThreshold',
        // Bollinger Bands
        'standardDeviation': 'standardDeviation',
        // Common parameters
        'positionSize': 'positionSize',
        'feeRate': 'feeRate',
        'taxRate': 'taxRate'
    };
    
    inputElements.forEach(input => {
        const rawParamName = input.id.replace('trading-param-', '');
        
        // Try both direct lookup and mapped lookup for the parameter
        if (params.hasOwnProperty(rawParamName)) {
            input.value = params[rawParamName];
            console.log(`Set parameter ${rawParamName} to ${params[rawParamName]}`);
            parameterCount++;
        } else if (parameterMap[rawParamName] && params.hasOwnProperty(parameterMap[rawParamName])) {
            // Try using the mapped parameter name
            const mappedName = parameterMap[rawParamName];
            input.value = params[mappedName];
            console.log(`Set mapped parameter ${rawParamName} to ${params[mappedName]}`);
            parameterCount++;
        }
    });
    
    // Save to localStorage again to ensure persistence
    try {
        localStorage.setItem('recommendedAlgorithmParameters', JSON.stringify(recommendedAlgorithmParameters));
        console.log('Saved parameters to localStorage after applying them');
    } catch (error) {
        console.error('Error saving to localStorage:', error);
    }
    
    if (parameterCount <= 0) {
        console.error('No parameters were set. Parameter names may not match.');
        alert('Could not set parameters. Please manually set your parameters.');
    }
}

/**
 * Opens the backtest tab with the recommended parameters
 * 
 * @param {string} algorithmId - The ID of the algorithm
 */
function backtestWithParameters(algorithmId) {
    try {
        // Make sure we have recommended parameters for this algorithm
        if (!recommendedAlgorithmParameters[algorithmId]) {
            console.error(`No recommended parameters found for algorithm ${algorithmId}`);
            return;
        }
        
        console.log(`Setting up backtest with parameters for algorithm ${algorithmId}:`, recommendedAlgorithmParameters[algorithmId]);
        
        // Switch to the backtest tab
        const backtestTab = document.getElementById('backtest-tab');
        if (backtestTab) {
            backtestTab.click();
            
            // Set the algorithm in the backtest form
            setTimeout(() => {
                const algorithmSelect = document.getElementById('backtest-algorithm');
                if (algorithmSelect) {
                    console.log(`Setting backtest algorithm selection to ${algorithmId}`);
                    // Set the algorithm selection
                    algorithmSelect.value = algorithmId;
                    
                    // Trigger the change event to load parameters
                    const event = new Event('change');
                    algorithmSelect.dispatchEvent(event);
                    
                    // Give some time for the parameters to be loaded
                    setTimeout(() => {
                        // Now set the recommended parameter values
                        const params = recommendedAlgorithmParameters[algorithmId];
                        
                        // Find all parameter input fields
                        const paramInputs = document.querySelectorAll('#algorithm-params-container input');
                        
                        if (paramInputs.length === 0) {
                            console.error('No parameter input fields found in the backtest form. Parameters may not have loaded yet.');
                            
                            // Try one more time with a longer delay
                            setTimeout(() => {
                                const retryParamInputs = document.querySelectorAll('#algorithm-params-container input');
                                if (retryParamInputs.length === 0) {
                                    console.error('Still no parameter input fields found after retry.');
                                    alert('Could not set backtest parameters. Please manually set your parameters.');
                                    return;
                                }
                                
                                // Apply parameters for backtest
                                applyBacktestParameters(retryParamInputs, params, algorithmId);
                            }, 2000);
                            return;
                        }
                        
                        // Apply parameters for backtest
                        applyBacktestParameters(paramInputs, params, algorithmId);
                    }, 1000); // Wait for the parameters to be loaded
                }
            }, 500);
        }
    } catch (error) {
        console.error('Error setting up backtest:', error);
    }
}

/**
 * Applies algorithm parameters to backtest input fields
 * 
 * @param {NodeList} inputElements - The input elements
 * @param {Object} params - The parameter values
 * @param {string} algorithmId - The algorithm ID
 */
function applyBacktestParameters(inputElements, params, algorithmId) {
    let parameterCount = 0;
    
    // Map algorithm parameter names to their equivalent backtest form parameter names
    const parameterMap = {
        // Simple Moving Average
        'shortPeriod': 'shortPeriod',
        'longPeriod': 'longPeriod',
        // RSI
        'period': 'period',
        'overboughtThreshold': 'overboughtThreshold',
        'oversoldThreshold': 'oversoldThreshold',
        // Bollinger Bands
        'standardDeviation': 'standardDeviation',
        // Common parameters
        'positionSize': 'positionSize',
        'feeRate': 'feeRate',
        'taxRate': 'taxRate'
    };
    
    inputElements.forEach(input => {
        const rawParamName = input.id.replace('param-', '');
        
        // Try both direct lookup and mapped lookup for the parameter
        if (params.hasOwnProperty(rawParamName)) {
            input.value = params[rawParamName];
            console.log(`Set backtest parameter ${rawParamName} to ${params[rawParamName]}`);
            parameterCount++;
        } else if (parameterMap[rawParamName] && params.hasOwnProperty(parameterMap[rawParamName])) {
            // Try using the mapped parameter name
            const mappedName = parameterMap[rawParamName];
            input.value = params[mappedName];
            console.log(`Set mapped backtest parameter ${rawParamName} to ${params[mappedName]}`);
            parameterCount++;
        }
    });
    
    // Save to localStorage again to ensure persistence
    try {
        localStorage.setItem('recommendedAlgorithmParameters', JSON.stringify(recommendedAlgorithmParameters));
        console.log('Saved parameters to localStorage after applying them to backtest');
    } catch (error) {
        console.error('Error saving to localStorage:', error);
    }
    
    // Set the default dates
    if (typeof setDefaultDates === 'function') {
        setDefaultDates();
    }
    
    if (parameterCount > 0) {
        // Show a confirmation alert
        alert(`Parameters for ${algorithmId} have been set. You can now run a backtest with these optimized parameters.`);
    } else {
        console.error('No backtest parameters were set. Parameter names may not match.');
        alert('Could not set all backtest parameters. Please check and manually adjust as needed.');
    }
}
