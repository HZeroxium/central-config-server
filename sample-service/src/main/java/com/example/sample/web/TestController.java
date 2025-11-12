package com.example.sample.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/test/hello")
    public String hello() throws InterruptedException {
        // Simulate blocking I/O or slow processing
        Thread.sleep(200); 
        Thread current = Thread.currentThread();
        return String.format("Thread name: %s, isVirtual: %s", current, current.isVirtual());
    }
}
