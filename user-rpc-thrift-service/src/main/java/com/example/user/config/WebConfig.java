package com.example.user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration hook. Extend if you need CORS, formatters, etc.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {}


