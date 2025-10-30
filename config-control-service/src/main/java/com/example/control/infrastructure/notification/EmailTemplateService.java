package com.example.control.infrastructure.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Service for rendering email templates using Thymeleaf.
 * <p>
 * Provides methods to render HTML email templates with variables.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTemplateService {

  @Qualifier("emailTemplateEngine")
  private final TemplateEngine templateEngine;

  /**
   * Renders an HTML email template with the given variables.
   *
   * @param templateName the template name (without extension, e.g.,
   *                     "approval-approved")
   * @param variables    the variables to pass to the template
   * @return rendered HTML string
   */
  public String renderTemplate(String templateName, Map<String, Object> variables) {
    try {
      log.debug("Rendering email template: {} with variables: {}", templateName, variables.keySet());
      Context context = new Context();
      variables.forEach(context::setVariable);

      String templatePath = "email/" + templateName;
      String html = templateEngine.process(templatePath, context);
      log.debug("Successfully rendered template: {}", templateName);
      return html;
    } catch (Exception e) {
      log.error("Failed to render email template: {}", templateName, e);
      throw new RuntimeException("Failed to render email template: " + templateName, e);
    }
  }
}
