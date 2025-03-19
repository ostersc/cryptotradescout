/**
 * Main JavaScript file for handling the tab navigation and initialization
 * with enhanced error handling and DOM safety
 */

document.addEventListener('DOMContentLoaded', function() {
    try {
        console.log('Initializing main application...');
        
        // Set up tab navigation with error checking
        const tabs = document.querySelectorAll('.nav-link');
        const sections = document.querySelectorAll('.content-section');
        
        if (!tabs || tabs.length === 0) {
            console.error('Navigation tabs not found in the document');
            return;
        }
        
        if (!sections || sections.length === 0) {
            console.error('Content sections not found in the document');
            return;
        }
        
        console.log(`Found ${tabs.length} tabs and ${sections.length} content sections`);
        
        tabs.forEach(tab => {
            if (!tab) return;
            
            tab.addEventListener('click', function(event) {
                try {
                    event.preventDefault();
                    
                    // Safety check on this element
                    if (!this) {
                        console.error('Tab element is null in click handler');
                        return;
                    }
                    
                    // Remove active class from all tabs with safety check
                    tabs.forEach(t => {
                        if (t && t.className) {
                            // Use direct style manipulation instead of classList
                            const currentClasses = t.className.split(' ').filter(c => c !== 'active');
                            t.className = currentClasses.join(' ');
                        }
                    });
                    
                    // Add active class to clicked tab with safety check
                    if (this.className) {
                        // Use direct string manipulation instead of classList
                        const currentClasses = this.className.split(' ');
                        if (!currentClasses.includes('active')) {
                            currentClasses.push('active');
                            this.className = currentClasses.join(' ');
                        }
                    }
                    
                    // Hide all sections
                    hideAllSections();
                    
                    // Show the section corresponding to the clicked tab
                    const targetId = this.getAttribute('data-target');
                    if (!targetId) {
                        console.error('Tab missing data-target attribute');
                        return;
                    }
                    
                    const targetSection = document.getElementById(targetId);
                    if (!targetSection) {
                        console.error(`Target section with ID ${targetId} not found`);
                        return;
                    }
                    
                    // Use direct style manipulation instead of classList
                    const sectionClasses = targetSection.className.split(' ').filter(c => c !== 'd-none');
                    targetSection.className = sectionClasses.join(' ');
                    
                    // Initialize the content based on the selected tab
                    initializeTabContent(targetId);
                    
                    console.log(`Tab changed to: ${targetId}`);
                } catch (error) {
                    console.error('Error in tab click handler:', error);
                }
            });
        });
        
        // Hide all sections except the first one
        function hideAllSections() {
            try {
                sections.forEach(section => {
                    if (!section) return;
                    
                    // Use direct style manipulation instead of classList
                    if (section.className) {
                        const currentClasses = section.className.split(' ');
                        if (!currentClasses.includes('d-none')) {
                            currentClasses.push('d-none');
                            section.className = currentClasses.join(' ');
                        }
                    }
                });
            } catch (error) {
                console.error('Error hiding sections:', error);
            }
        }
        
        // Initialize content based on tab
        function initializeTabContent(targetId) {
            try {
                if (!targetId) {
                    console.error('Cannot initialize tab content: Missing targetId');
                    return;
                }
                
                switch (targetId) {
                    case 'dashboard-content':
                        if (typeof initializeDashboard === 'function') {
                            initializeDashboard();
                        } else {
                            console.warn('Dashboard initialization function not found');
                        }
                        break;
                    case 'algorithms-content':
                        if (typeof loadAlgorithms === 'function') {
                            loadAlgorithms();
                        } else {
                            console.warn('Algorithms loading function not found');
                        }
                        break;
                    case 'backtest-content':
                        if (typeof initializeBacktestForm === 'function') {
                            initializeBacktestForm();
                        } else {
                            console.warn('Backtest form initialization function not found');
                        }
                        break;
                    case 'trading-content':
                        if (typeof initializeTradingForm === 'function') {
                            initializeTradingForm();
                        } else {
                            console.warn('Trading form initialization function not found');
                        }
                        break;
                    default:
                        console.warn(`Unknown tab target: ${targetId}`);
                }
            } catch (error) {
                console.error(`Error initializing content for tab ${targetId}:`, error);
            }
        }
        
        // Click the first tab to initialize the application
        if (tabs[0]) {
            console.log('Clicking first tab to initialize application');
            tabs[0].click();
        } else {
            console.error('No tabs available to initialize the application');
        }
        
        // Load required scripts
const scripts = [
    '/js/backtest.js'
];

scripts.forEach(src => {
    const script = document.createElement('script');
    script.src = src;
    document.head.appendChild(script);
});

console.log('Main application initialization complete');
    } catch (error) {
        console.error('Critical error in main application initialization:', error);
    }
});