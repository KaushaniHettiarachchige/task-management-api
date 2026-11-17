
package com.example.taskmanagement.config;

import com.example.taskmanagement.logging.RequestLoggingFilter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class RequestLoggingConfig {

    @Bean
    public FilterRegistrationBean<RequestLoggingFilter>
            requestLoggingFilterRegistration() {

        FilterRegistrationBean<RequestLoggingFilter> registration =
                new FilterRegistrationBean<>();

        registration.setFilter(new RequestLoggingFilter());

        registration.addUrlPatterns("/*");

        registration.setName("requestLoggingFilter");

        // Run early so the request ID is available
        // to Spring Security and controller error handling.
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);

        return registration;
    }
}
