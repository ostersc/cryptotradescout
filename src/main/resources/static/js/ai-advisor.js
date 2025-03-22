/**
 * AI Advisor JavaScript file for handling AI-powered trading strategy recommendations
 */

// Store recommended parameters for each algorithm to pass between tabs
let recommendedAlgorithmParameters = {};

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
        
        // Reset recommended parameters
        recommendedAlgorithmParameters = {};
        
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
                displayAIAnalysis(data);
            })
            .catch(error => {
                console.error('Error fetching AI recommendations:', error);
                loadingElement.classList.add('d-none');
                noResultsElement.classList.remove('d-none');
                
                // Show error message
                noResultsElement.innerHTML = `
                    <div class="alert alert-danger">
                        <strong>Error:</strong> Could not get AI recommendations. ${error.message}
                    </div>
                `;
            });
    } catch (error) {
        console.error('Error in getAIRecommendations:', error);
    }
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
            
            // Clear the recommended parameters storage
            recommendedAlgorithmParameters = {};
            
            // Sort suggestions by confidence score (descending)
            const sortedSuggestions = [...analysis.algorithmSuggestions].sort((a, b) => 
                b.confidenceScore - a.confidenceScore
            );
            
            // Store the recommended parameters for each algorithm
            sortedSuggestions.forEach(suggestion => {
                // Store the parameters for later use
                if (suggestion.algorithmId && suggestion.recommendedParameters) {
                    recommendedAlgorithmParameters[suggestion.algorithmId] = suggestion.recommendedParameters;
                    console.log(`Stored recommended parameters for ${suggestion.algorithmId}:`, suggestion.recommendedParameters);
                }
                
                // Create cards for each suggestion
                const algorithmCard = createAlgorithmSuggestionCard(suggestion);
                strategiesContainer.appendChild(algorithmCard);
            });
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
                        
                        paramInputs.forEach(input => {
                            const paramName = input.id.replace('trading-param-', '');
                            
                            // Check if we have a recommended value for this parameter
                            if (params.hasOwnProperty(paramName)) {
                                input.value = params[paramName];
                                console.log(`Set parameter ${paramName} to ${params[paramName]}`);
                            }
                        });
                        
                        // Show a confirmation alert
                        alert(`Parameters for ${algorithmId} have been set. You can now start live trading with these optimized parameters.`);
                    }, 1000); // Wait for the parameters to be loaded
                }
            }, 500);
        }
    } catch (error) {
        console.error('Error using recommended parameters:', error);
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
                        
                        paramInputs.forEach(input => {
                            const paramName = input.id.replace('param-', '');
                            
                            // Check if we have a recommended value for this parameter
                            if (params.hasOwnProperty(paramName)) {
                                input.value = params[paramName];
                                console.log(`Set backtest parameter ${paramName} to ${params[paramName]}`);
                            }
                        });
                        
                        // Set the default dates
                        if (typeof setDefaultDates === 'function') {
                            setDefaultDates();
                        }
                        
                        // Show a confirmation alert
                        alert(`Parameters for ${algorithmId} have been set. You can now run a backtest with these optimized parameters.`);
                    }, 1000); // Wait for the parameters to be loaded
                }
            }, 500);
        }
    } catch (error) {
        console.error('Error setting up backtest:', error);
    }
}