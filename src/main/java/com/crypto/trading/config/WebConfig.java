package com.crypto.trading.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for the application.
 * 
 * This configuration handles static resources and view controllers.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configure view controllers for the application.
     * This ensures that page refreshes and direct URL access work correctly with
     * our single-page application architecture.
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.addViewController("/backtest").setViewName("forward:/index.html");
        registry.addViewController("/algorithms").setViewName("forward:/index.html");
        registry.addViewController("/trading").setViewName("forward:/index.html");
    }

    /**
     * Configure resource handlers for serving static resources.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}