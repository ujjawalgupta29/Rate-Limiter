package com.example.rateLimiting.configurations;

import io.github.ujjawalgupta29.filters.RateLimitingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {
    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimiterFilter() {
        FilterRegistrationBean<RateLimitingFilter> filterBean = new FilterRegistrationBean<>();
        filterBean.setFilter(new RateLimitingFilter());
        filterBean.addUrlPatterns("/*");
        filterBean.setOrder(1);;
        return filterBean;
    }
}
