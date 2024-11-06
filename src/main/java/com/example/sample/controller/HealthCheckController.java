package com.example.sample.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/health-check")
public class HealthCheckController {

    @GetMapping
    public ResponseEntity<String> healthCheck() {
        Thread thread = Thread.currentThread();
        log.info("isVirtual :: {}, {}", thread.isVirtual(), thread);
        return ResponseEntity.ok("ok");
    }

}
