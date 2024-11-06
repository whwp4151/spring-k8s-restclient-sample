package com.example.sample.controller;

import com.example.sample.config.SampleApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/order-service")
public class OrderServiceController {

    private final SampleApi sampleApi;

    @GetMapping
    public ResponseEntity<String> callOrderService() {
        String result = sampleApi.healthCheck();
        log.info("Response from order-service :: {}", result);
        return ResponseEntity.ok("Response from order-service :: " + result);
    }

}
