/**
 * Main JavaScript file for handling the tab navigation and initialization
 */
document.addEventListener('DOMContentLoaded', function() {
    // Tab navigation
    const dashboardTab = document.getElementById('dashboard-tab');
    const algorithmsTab = document.getElementById('algorithms-tab');
    const backtestTab = document.getElementById('backtest-tab');
    const tradingTab = document.getElementById('trading-tab');

    const dashboardContent = document.getElementById('dashboard-content');
    const algorithmsContent = document.getElementById('algorithms-content');
    const backtestContent = document.getElementById('backtest-content');
    const tradingContent = document.getElementById('trading-content');

    // Function to hide all content sections
    function hideAllSections() {
        dashboardContent.classList.add('d-none');
        algorithmsContent.classList.add('d-none');
        backtestContent.classList.add('d-none');
        tradingContent.classList.add('d-none');

        // Remove active class from all tabs
        dashboardTab.classList.remove('active');
        algorithmsTab.classList.remove('active');
        backtestTab.classList.remove('active');
        tradingTab.classList.remove('active');
    }

    // Dashboard tab click
    dashboardTab.addEventListener('click', function(e) {
        e.preventDefault();
        hideAllSections();
        dashboardContent.classList.remove('d-none');
        dashboardTab.classList.add('active');
    });

    // Algorithms tab click
    algorithmsTab.addEventListener('click', function(e) {
        e.preventDefault();
        hideAllSections();
        algorithmsContent.classList.remove('d-none');
        algorithmsTab.classList.add('active');
    });

    // Backtest tab click
    backtestTab.addEventListener('click', function(e) {
        e.preventDefault();
        hideAllSections();
        backtestContent.classList.remove('d-none');
        backtestTab.classList.add('active');
    });

    // Trading tab click
    tradingTab.addEventListener('click', function(e) {
        e.preventDefault();
        hideAllSections();
        tradingContent.classList.remove('d-none');
        tradingTab.classList.add('active');
    });

    // Initialize the dashboard page (default active tab)
    initializeDashboard();
    loadAlgorithms();
    initializeBacktestForm();
    initializeTradingForm();
});