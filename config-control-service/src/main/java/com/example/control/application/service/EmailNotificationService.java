package com.example.control.application.service;

import com.example.control.application.query.ApplicationServiceQueryService;
import com.example.control.application.query.IamUserQueryService;
import com.example.control.domain.event.ApprovalRequestApprovedEvent;
import com.example.control.domain.id.ApplicationServiceId;
import com.example.control.domain.id.IamUserId;
import com.example.control.domain.object.ApplicationService;
import com.example.control.domain.object.IamUser;
import com.example.control.domain.port.NotificationServicePort;
import com.example.control.infrastructure.notification.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Orchestrator service for sending email notifications.
 * <p>
 * Coordinates between domain ports and infrastructure adapters to send
 * email notifications when approval requests are approved.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

  private final NotificationServicePort notificationServicePort;
  private final EmailTemplateService emailTemplateService;
  private final IamUserQueryService iamUserQueryService;
  private final ApplicationServiceQueryService applicationServiceQueryService;

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm:ss");

  /**
   * Sends an email notification when an approval request is approved.
   *
   * @param event the approval request approved event
   */
  public void sendApprovalNotification(ApprovalRequestApprovedEvent event) {
    try {
      log.info("Processing approval notification for request: {} to requester: {}",
          event.getRequestId(), event.getRequesterUserId());

      // Fetch requester details
      Optional<IamUser> requesterOpt = iamUserQueryService.findById(IamUserId.of(event.getRequesterUserId()));
      if (requesterOpt.isEmpty()) {
        // Currently implement a mock requester user for the approval request 
        requesterOpt = Optional.of(IamUser.builder()
            .userId(IamUserId.of(event.getRequesterUserId()))
            .email("john.doe@company.com")
            .firstName("John")
            .lastName("Doe")
            .username("john.doe")
            .build());
        // log.warn("Requester user not found: {}, skipping email notification", event.getRequesterUserId());
        // return;
      }

      IamUser requester = requesterOpt.get();
      String recipientEmail = requester.getEmail();
      if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
        log.warn("Requester {} has no email address, skipping email notification", event.getRequesterUserId());
        return;
      }

      // Fetch service details
      Optional<ApplicationService> serviceOpt = applicationServiceQueryService.findById(
          ApplicationServiceId.of(event.getServiceId()));
      String serviceName = serviceOpt.map(ApplicationService::getDisplayName)
          .orElse(event.getServiceId());

      // Fetch approver details
      String approverName = "System";
      if (!"SYSTEM".equals(event.getApproverUserId())) {
        Optional<IamUser> approverOpt = iamUserQueryService.findById(
            IamUserId.of(event.getApproverUserId()));
        if (approverOpt.isPresent()) {
          IamUser approver = approverOpt.get();
          approverName = approver.getFirstName() + " " + approver.getLastName();
          if (approverName.trim().isEmpty()) {
            approverName = approver.getUsername();
          }
        }
      }

      // Prepare template variables
      Map<String, Object> templateVariables = new HashMap<>();
      templateVariables.put("requesterName", getRequesterDisplayName(requester));
      templateVariables.put("serviceName", serviceName);
      templateVariables.put("serviceId", event.getServiceId());
      templateVariables.put("requestId", event.getRequestId());
      templateVariables.put("targetTeamId", event.getTargetTeamId());
      templateVariables.put("approverName", approverName);
      templateVariables.put("approvedAt",
          event.getApprovedAt().atZone(java.time.ZoneId.systemDefault()).format(DATE_FORMATTER));

      // Render HTML template
      String htmlBody = emailTemplateService.renderTemplate("approval-approved", templateVariables);

      // Send email
      String subject = String.format("Approval Request Approved: %s", serviceName);
      notificationServicePort.sendEmail(recipientEmail, subject, htmlBody);

      log.info("Successfully sent approval notification email to: {}", recipientEmail);
    } catch (Exception e) {
      log.error("Failed to send approval notification email for request: {}", event.getRequestId(), e);
      // Don't throw exception - email failure should not fail the approval
      // transaction
    }
  }

  /**
   * Gets display name for requester.
   *
   * @param requester the requester user
   * @return display name (firstName + lastName or username)
   */
  private String getRequesterDisplayName(IamUser requester) {
    String firstName = requester.getFirstName();
    String lastName = requester.getLastName();
    if (firstName != null && lastName != null && !firstName.trim().isEmpty() && !lastName.trim().isEmpty()) {
      return firstName + " " + lastName;
    }
    return requester.getUsername();
  }
}
