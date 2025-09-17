package com.example.user.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for WebConfig.
 * Tests Spring MVC configuration setup.
 */
@SpringJUnitConfig
@DisplayName("WebConfig Tests")
class WebConfigTest {

    @Test
    @DisplayName("Should create WebConfig bean successfully")
    void shouldCreateWebConfigBeanSuccessfully() {
        // When
        WebConfig webConfig = new WebConfig();

        // Then
        assertThat(webConfig).isNotNull();
        assertThat(webConfig).isInstanceOf(WebConfig.class);
    }

    @Test
    @DisplayName("Should implement WebMvcConfigurer interface")
    void shouldImplementWebMvcConfigurerInterface() {
        // Given
        WebConfig webConfig = new WebConfig();

        // When & Then
        assertThat(webConfig).isInstanceOf(org.springframework.web.servlet.config.annotation.WebMvcConfigurer.class);
    }

    @Test
    @DisplayName("Should be a configuration class")
    void shouldBeAConfigurationClass() {
        // Given
        WebConfig webConfig = new WebConfig();

        // When & Then
        assertThat(webConfig.getClass().isAnnotationPresent(org.springframework.context.annotation.Configuration.class))
                .isTrue();
    }
}
