package com.example.control.domain.port;

/**
 * Port interface for sending email notifications.
 * <p>
 * This is a domain port that abstracts email sending functionality.
 * Implementations should be provided by infrastructure adapters.
 * </p>
 */
public interface NotificationServicePort {

  /**
   * Sends an HTML email.
   *
   * @param to       recipient email address
   * @param subject  email subject
   * @param htmlBody HTML email body content
   * @throws NotificationException if email sending fails
   */
  void sendEmail(String to, String subject, String htmlBody) throws NotificationException;

  /**
   * Exception thrown when email notification fails.
   */
  class NotificationException extends RuntimeException {
    public NotificationException(String message) {
      super(message);
    }

    public NotificationException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
