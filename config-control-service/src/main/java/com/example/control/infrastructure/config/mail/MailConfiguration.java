package com.example.control.infrastructure.config.mail;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Configuration for email and Thymeleaf template engine.
 * <p>
 * Configures Thymeleaf TemplateEngine for rendering HTML email templates.
 * JavaMailSender is auto-configured by Spring Boot based on
 * application.properties.
 * </p>
 */
@Configuration
public class MailConfiguration {

  /**
   * Configures Thymeleaf TemplateEngine for email templates.
   * <p>
   * Templates are located in classpath:/templates/email/
   * </p>
   *
   * @return configured TemplateEngine bean
   */
  @Bean("emailTemplateEngine")
  public TemplateEngine emailTemplateEngine() {
    SpringTemplateEngine templateEngine = new SpringTemplateEngine();
    templateEngine.setTemplateResolver(emailTemplateResolver());
    return templateEngine;
  }

  /**
   * Configures template resolver for email templates.
   *
   * @return configured SpringResourceTemplateResolver
   */
  private SpringResourceTemplateResolver emailTemplateResolver() {
    SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
    templateResolver.setPrefix("classpath:/templates/");
    templateResolver.setSuffix(".html");
    templateResolver.setTemplateMode(TemplateMode.HTML);
    templateResolver.setCharacterEncoding("UTF-8");
    templateResolver.setCacheable(false); // Disable cache for development
    return templateResolver;
  }
}
