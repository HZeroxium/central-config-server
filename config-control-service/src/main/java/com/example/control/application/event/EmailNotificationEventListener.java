package com.example.control.application.event;

import com.example.control.application.service.EmailNotificationService;
import com.example.control.domain.event.ApprovalRequestApprovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener for email notification events.
 * <p>
 * Listens for ApprovalRequestApprovedEvent and sends email notifications
 * asynchronously.
 * Uses @TransactionalEventListener with AFTER_COMMIT to ensure the main
 * transaction
 * completes before sending emails.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationEventListener {

  private final EmailNotificationService emailNotificationService;

  /**
   * Handles approval request approved event by sending email notification.
   * <p>
   * Executes asynchronously after transaction commit to avoid blocking the main
   * transaction.
   * </p>
   *
   * @param event the approval request approved event
   */
  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleApprovalRequestApproved(ApprovalRequestApprovedEvent event) {
    log.info("Received ApprovalRequestApprovedEvent for request: {}", event.getRequestId());
    try {
      emailNotificationService.sendApprovalNotification(event);
    } catch (Exception e) {
      log.error("Error processing approval notification event for request: {}",
          event.getRequestId(), e);
      // Don't rethrow - email failures should not affect the system
    }
  }
}
