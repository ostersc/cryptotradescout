package com.crypto.trading.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web view controller for handling frontend routes
 */
@Controller
public class WebViewController {

    /**
     * Main dashboard page
     * 
     * @return the index view
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }
    
    /**
     * Backtest page
     * 
     * @return the backtest view
     */
    @GetMapping("/backtest")
    public String backtest() {
        return "index";
    }
    
    /**
     * Algorithms page
     * 
     * @return the algorithms view
     */
    @GetMapping("/algorithms")
    public String algorithms() {
        return "index";
    }
    
    /**
     * Trading page
     * 
     * @return the trading view
     */
    @GetMapping("/trading")
    public String trading() {
        return "index";
    }
}