package com.example.sample.web;

import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * Custom refresh controller to test refresh functionality
 * This is a workaround for the missing refresh endpoint issue
 */
@RestController
@RequestMapping("/api")
public class RefreshController {

    private final ContextRefresher contextRefresher;

    public RefreshController(ContextRefresher contextRefresher) {
        this.contextRefresher = contextRefresher;
    }

    @PostMapping("/refresh")
    public Set<String> refresh() {
        return contextRefresher.refresh();
    }
}
