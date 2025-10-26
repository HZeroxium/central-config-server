package com.example.control.seeding.config;

import net.datafaker.Faker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;
import java.util.Random;

/**
 * Configuration class for the data seeding system.
 * <p>
 * This configuration:
 * <ul>
 * <li>Enables {@link SeederConfigProperties} for type-safe configuration
 * access</li>
 * <li>Creates a {@link Faker} bean for generating realistic mock data</li>
 * <li>Scans the seeding package for component discovery</li>
 * <li>Only activates when {@code seeding.enabled=true}</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Usage:</strong>
 * </p>
 * 
 * <pre>
 * # Enable seeding in application.yml or via profile
 * seeding:
 *   enabled: true
 *   auto-run-on-startup: true
 * </pre>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "seeding", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(SeederConfigProperties.class)
@ComponentScan(basePackages = "com.example.control.seeding")
public class SeederConfig {

  /**
   * Creates a {@link Faker} bean for generating realistic mock data.
   * <p>
   * The Faker instance is configured with:
   * <ul>
   * <li>English locale for consistent data generation</li>
   * <li>Fixed random seed for reproducible results across runs</li>
   * </ul>
   * </p>
   *
   * <p>
   * <strong>Reproducibility:</strong> Using a fixed seed ensures that
   * the same mock data is generated every time, which is useful for:
   * <ul>
   * <li>Consistent test environments</li>
   * <li>Debugging data-related issues</li>
   * <li>Demo and development workflows</li>
   * </ul>
   * </p>
   *
   * @return configured Faker instance
   */
  @Bean
  public Faker faker() {
    // Use fixed seed for reproducible data generation
    Random random = new Random(42L);
    return new Faker(Locale.ENGLISH, random);
  }
}
