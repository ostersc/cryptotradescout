/**
 * Algorithms JavaScript file for handling the algorithm listing and details
 */

/**
 * Loads the algorithms from the API and displays them
 */
function loadAlgorithms() {
    fetch('/api/algorithms')
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(algorithms => {
            displayAlgorithms(algorithms);
            populateAlgorithmDropdowns(algorithms);
        })
        .catch(error => {
            console.error('Error fetching algorithms:', error);
        });
}

/**
 * Displays the algorithms in the algorithms tab
 * 
 * @param {Array} algorithms - Array of algorithm objects
 */
function displayAlgorithms(algorithms) {
    const algorithmsList = document.getElementById('algorithms-list');
    algorithmsList.innerHTML = '';
    
    algorithms.forEach(algorithm => {
        const algorithmCard = createAlgorithmCard(algorithm);
        algorithmsList.appendChild(algorithmCard);
    });
}

/**
 * Creates an algorithm card element
 * 
 * @param {Object} algorithm - The algorithm object
 * @returns {HTMLElement} The algorithm card element
 */
function createAlgorithmCard(algorithm) {
    const col = document.createElement('div');
    col.className = 'col-md-6 col-lg-4 mb-4';
    
    col.innerHTML = `
        <div class="card algorithm-card">
            <div class="card-body">
                <h5 class="card-title">${algorithm.name}</h5>
                <p class="card-text">${algorithm.description}</p>
            </div>
            <div class="card-footer">
                <button class="btn btn-sm btn-primary view-algorithm-details" data-algorithm-id="${algorithm.id}">
                    View Details
                </button>
                <button class="btn btn-sm btn-success ms-2 backtest-algorithm" data-algorithm-id="${algorithm.id}">
                    Backtest
                </button>
            </div>
        </div>
    `;
    
    // Add event listeners to the buttons
    col.querySelector('.view-algorithm-details').addEventListener('click', () => {
        loadAlgorithmDetails(algorithm.id);
    });
    
    col.querySelector('.backtest-algorithm').addEventListener('click', () => {
        // Switch to backtest tab and select this algorithm
        document.getElementById('backtest-tab').click();
        document.getElementById('backtest-algorithm').value = algorithm.id;
        // Trigger the change event to load algorithm parameters
        const changeEvent = new Event('change');
        document.getElementById('backtest-algorithm').dispatchEvent(changeEvent);
    });
    
    return col;
}

/**
 * Loads the details of a specific algorithm
 * 
 * @param {string} algorithmId - The ID of the algorithm
 */
function loadAlgorithmDetails(algorithmId) {
    fetch(`/api/algorithms/${algorithmId}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(algorithm => {
            displayAlgorithmDetails(algorithm);
        })
        .catch(error => {
            console.error('Error fetching algorithm details:', error);
        });
}

/**
 * Displays the algorithm details in a modal
 * 
 * @param {Object} algorithm - The algorithm object
 */
function displayAlgorithmDetails(algorithm) {
    // Check if modal already exists, otherwise create it
    let modal = document.getElementById('algorithm-details-modal');
    
    if (!modal) {
        modal = document.createElement('div');
        modal.className = 'modal fade';
        modal.id = 'algorithm-details-modal';
        modal.setAttribute('tabindex', '-1');
        modal.setAttribute('aria-labelledby', 'algorithm-details-modal-label');
        modal.setAttribute('aria-hidden', 'true');
        
        modal.innerHTML = `
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="algorithm-details-modal-label">Algorithm Details</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body" id="algorithm-details-content">
                        <!-- Algorithm details will be inserted here -->
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                        <button type="button" class="btn btn-primary" id="backtest-selected-algorithm">Backtest</button>
                    </div>
                </div>
            </div>
        `;
        
        document.body.appendChild(modal);
        
        // Add event listener to the backtest button
        document.getElementById('backtest-selected-algorithm').addEventListener('click', () => {
            // Get the current algorithm ID
            const algorithmId = document.getElementById('algorithm-details-content').getAttribute('data-algorithm-id');
            // Close the modal
            const modalInstance = bootstrap.Modal.getInstance(modal);
            modalInstance.hide();
            // Switch to backtest tab and select this algorithm
            document.getElementById('backtest-tab').click();
            document.getElementById('backtest-algorithm').value = algorithmId;
            // Trigger the change event to load algorithm parameters
            const changeEvent = new Event('change');
            document.getElementById('backtest-algorithm').dispatchEvent(changeEvent);
        });
    }
    
    // Populate the modal content
    const modalContent = document.getElementById('algorithm-details-content');
    modalContent.setAttribute('data-algorithm-id', algorithm.id);
    
    let parametersHTML = '';
    if (algorithm.parameters) {
        parametersHTML = `
            <h6 class="mt-4">Parameters</h6>
            <table class="table table-sm">
                <thead>
                    <tr>
                        <th>Parameter</th>
                        <th>Description</th>
                    </tr>
                </thead>
                <tbody>
        `;
        
        for (const [param, description] of Object.entries(algorithm.parameters)) {
            parametersHTML += `
                <tr>
                    <td><code>${param}</code></td>
                    <td>${description}</td>
                </tr>
            `;
        }
        
        parametersHTML += `
                </tbody>
            </table>
        `;
    }
    
    modalContent.innerHTML = `
        <h4>${algorithm.name}</h4>
        <p>${algorithm.description}</p>
        <div class="alert alert-info">
            <strong>Algorithm ID:</strong> ${algorithm.id}
        </div>
        ${parametersHTML}
    `;
    
    // Initialize and show the modal
    const modalInstance = new bootstrap.Modal(modal);
    modalInstance.show();
}

/**
 * Populates the algorithm dropdown selectors
 * 
 * @param {Array} algorithms - Array of algorithm objects
 */
function populateAlgorithmDropdowns(algorithms) {
    const backtestAlgorithmSelect = document.getElementById('backtest-algorithm');
    const tradingAlgorithmSelect = document.getElementById('trading-algorithm');
    
    // Clear existing options
    backtestAlgorithmSelect.innerHTML = '';
    tradingAlgorithmSelect.innerHTML = '';
    
    // Add option for each algorithm
    algorithms.forEach(algorithm => {
        const backtestOption = document.createElement('option');
        backtestOption.value = algorithm.id;
        backtestOption.textContent = algorithm.name;
        backtestAlgorithmSelect.appendChild(backtestOption);
        
        const tradingOption = document.createElement('option');
        tradingOption.value = algorithm.id;
        tradingOption.textContent = algorithm.name;
        tradingAlgorithmSelect.appendChild(tradingOption);
    });
}