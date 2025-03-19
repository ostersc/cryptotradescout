/**
 * Main JavaScript file for handling the tab navigation and initialization
 */

document.addEventListener('DOMContentLoaded', function() {
    // Set up tab navigation
    const tabs = document.querySelectorAll('.nav-link');
    const sections = document.querySelectorAll('.content-section');
    
    tabs.forEach(tab => {
        tab.addEventListener('click', function(event) {
            event.preventDefault();
            
            // Remove active class from all tabs
            tabs.forEach(t => t.classList.remove('active'));
            
            // Add active class to clicked tab
            this.classList.add('active');
            
            // Hide all sections
            hideAllSections();
            
            // Show the section corresponding to the clicked tab
            const targetId = this.getAttribute('data-target');
            document.getElementById(targetId).classList.remove('d-none');
            
            // Initialize the content based on the selected tab
            initializeTabContent(targetId);
        });
    });
    
    // Hide all sections except the first one
    function hideAllSections() {
        sections.forEach(section => {
            section.classList.add('d-none');
        });
    }
    
    // Initialize content based on tab
    function initializeTabContent(targetId) {
        switch (targetId) {
            case 'dashboard-section':
                if (typeof initializeDashboard === 'function') {
                    initializeDashboard();
                }
                break;
            case 'algorithms-section':
                if (typeof loadAlgorithms === 'function') {
                    loadAlgorithms();
                }
                break;
            case 'backtest-section':
                if (typeof initializeBacktestForm === 'function') {
                    initializeBacktestForm();
                }
                break;
            case 'trading-section':
                if (typeof initializeTradingForm === 'function') {
                    initializeTradingForm();
                }
                break;
        }
    }
    
    // Click the first tab to initialize the application
    tabs[0].click();
});