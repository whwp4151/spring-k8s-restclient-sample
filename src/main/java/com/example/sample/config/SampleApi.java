package com.example.sample.config;

import org.springframework.web.service.annotation.GetExchange;

public interface SampleApi {

    @GetExchange("/api/v1/health-check")
    String healthCheck();

}
