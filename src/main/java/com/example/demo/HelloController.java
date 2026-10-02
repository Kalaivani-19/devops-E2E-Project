package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/")
    public String hello() {
        return "Hello from DevOps Training Project!";
    }

    @GetMapping("/health")
    public String health() {
        return "UP";
    }
}
