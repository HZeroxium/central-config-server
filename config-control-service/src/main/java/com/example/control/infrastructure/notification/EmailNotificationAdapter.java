package com.example.control.infrastructure.notification;

import com.example.control.domain.port.NotificationServicePort;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Infrastructure adapter for email notification using JavaMailSender.
 * <p>
 * Implements NotificationServicePort to send emails via SMTP with full
 * resilience:
 * - Circuit Breaker: Opens on mail server failures
 * - Retry: Retries on transient failures with exponential backoff
 * - Bulkhead: Limits concurrent email sends to prevent resource exhaustion
 * </p>
 * <p>
 * Email notifications are best-effort - failures are logged but don't fail the
 * business operation.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationAdapter implements NotificationServicePort {

  private final JavaMailSender mailSender;

  @Override
  @CircuitBreaker(name = "email", fallbackMethod = "sendEmailFallback")
  @Retry(name = "email-send")
  @Bulkhead(name = "email")
  public void sendEmail(String to, String subject, String htmlBody) throws NotificationException {
    try {
      log.debug("Sending email to: {} with subject: {}", to, subject);

      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(htmlBody, true); // true = HTML content

      mailSender.send(message);
      log.info("Successfully sent email to: {} with subject: {}", to, subject);
    } catch (MessagingException e) {
      log.error("Failed to send email to: {} with subject: {}", to, subject, e);
      throw new NotificationException("Failed to send email", e);
    } catch (Exception e) {
      log.error("Unexpected error sending email to: {} with subject: {}", to, subject, e);
      throw new NotificationException("Unexpected error sending email", e);
    }
  }

  /**
   * Fallback method for email sending when circuit is open.
   * <p>
   * Logs the failure but doesn't throw exception - email notifications are
   * best-effort.
   * </p>
   */
  private void sendEmailFallback(String to, String subject, String htmlBody, Throwable t) {
    log.warn("Email circuit breaker OPEN or failed after retries. Email to {} with subject '{}' not sent. Error: {}",
        to, subject, t.getMessage());
    // Best-effort notification - don't throw exception, just log
    // The business operation should still succeed even if notification fails
  }
}
