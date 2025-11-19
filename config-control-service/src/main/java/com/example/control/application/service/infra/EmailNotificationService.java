package com.example.control.application.service.infra;

import com.example.control.application.query.ApplicationServiceQueryService;
import com.example.control.application.query.IamUserQueryServiceV2;
import com.example.control.domain.event.ApprovalRequestApprovedEvent;
import com.example.control.domain.event.ApprovalRequestRejectedEvent;
import com.example.control.domain.event.DriftEventCreatedEvent;
import com.example.control.domain.valueobject.id.ApplicationServiceId;
import com.example.control.domain.valueobject.id.IamUserId;
import com.example.control.domain.model.ApplicationService;
import com.example.control.domain.model.IamUser;
import com.example.control.domain.port.NotificationServicePort;
import com.example.control.domain.port.repository.IamUserRepositoryPort;
import com.example.control.infrastructure.notification.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
  private final IamUserQueryServiceV2 iamUserQueryService;
  // private final IamUserRepositoryPort iamUserRepositoryPort;
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

      // If requester not found, try to find by username (for testing purposes)
      if (requesterOpt.isEmpty()) {
        requesterOpt = iamUserQueryService.findByUsername(event.getRequesterUserId());
      }

      // If still not found, try to create a mock requester user (for testing purposes)
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
   * Sends an email notification when an approval request is rejected.
   *
   * @param event the approval request rejected event
   */
  public void sendRejectionNotification(ApprovalRequestRejectedEvent event) {
    try {
      log.info("Processing rejection notification for request: {} to requester: {}",
          event.getRequestId(), event.getRequesterUserId());

      // Fetch requester details
      Optional<IamUser> requesterOpt = iamUserQueryService.findById(IamUserId.of(event.getRequesterUserId()));

      // If requester not found, try to find by username (for testing purposes)
      if (requesterOpt.isEmpty()) {
        requesterOpt = iamUserQueryService.findByUsername(event.getRequesterUserId());
      }

      // If still not found, try to create a mock requester user (for testing purposes)
      if (requesterOpt.isEmpty()) {
        requesterOpt = Optional.of(IamUser.builder()
            .userId(IamUserId.of(event.getRequesterUserId()))
            .email("john.doe@company.com")
            .firstName("John")
            .lastName("Doe")
            .username("john.doe")
            .build());
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

      // Fetch rejector details
      String rejectorName = "System";
      if (!"SYSTEM".equals(event.getRejectorUserId())) {
        Optional<IamUser> rejectorOpt = iamUserQueryService.findById(
            IamUserId.of(event.getRejectorUserId()));
        if (rejectorOpt.isPresent()) {
          IamUser rejector = rejectorOpt.get();
          rejectorName = rejector.getFirstName() + " " + rejector.getLastName();
          if (rejectorName.trim().isEmpty()) {
            rejectorName = rejector.getUsername();
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
      templateVariables.put("rejectorName", rejectorName);
      templateVariables.put("reason", event.getReason());
      templateVariables.put("rejectedAt",
          event.getRejectedAt().atZone(java.time.ZoneId.systemDefault()).format(DATE_FORMATTER));

      // Render HTML template
      String htmlBody = emailTemplateService.renderTemplate("approval-rejected", templateVariables);

      // Send email
      String subject = String.format("Approval Request Rejected: %s", serviceName);
      notificationServicePort.sendEmail(recipientEmail, subject, htmlBody);

      log.info("Successfully sent rejection notification email to: {}", recipientEmail);
    } catch (Exception e) {
      log.error("Failed to send rejection notification email for request: {}", event.getRequestId(), e);
      // Don't throw exception - email failure should not fail the rejection transaction
    }
  }

  /**
   * Sends email notifications when a drift event is created.
   * <p>
   * Sends emails to:
   * - All users with SYS_ADMIN role
   * - All users in the service owner team (if teamId is not null)
   * </p>
   *
   * @param event the drift event created event
   */
  public void sendDriftEventNotification(DriftEventCreatedEvent event) {
    try {
      log.info("Processing drift event notification for event: {} service: {} instance: {}",
          event.getDriftEventId(), event.getServiceName(), event.getInstanceId());

      // Collect recipients: SYS_ADMIN users + service owner team members
      Set<IamUser> recipients = new HashSet<>();

      // 1. Fetch all SYS_ADMIN users
      try {
        List<IamUser> sysAdmins = iamUserQueryService.findByRole("SYS_ADMIN");
        recipients.addAll(sysAdmins);
        log.debug("Found {} SYS_ADMIN users for drift event notification", sysAdmins.size());
      } catch (Exception e) {
        log.warn("Failed to fetch SYS_ADMIN users for drift event notification", e);
      }

      // 2. Fetch all users in service owner team (if teamId is not null)
      if (event.getTeamId() != null && !event.getTeamId().trim().isEmpty()) {
        try {
          List<IamUser> teamMembers = iamUserQueryService.findByTeam(event.getTeamId());
          recipients.addAll(teamMembers);
          log.debug("Found {} team members for drift event notification (teamId: {})",
              teamMembers.size(), event.getTeamId());
        } catch (Exception e) {
          log.warn("Failed to fetch team members for drift event notification (teamId: {})",
              event.getTeamId(), e);
        }
      }

      if (recipients.isEmpty()) {
        log.warn("No recipients found for drift event notification: {}", event.getDriftEventId());
        return;
      }

      // Deduplicate by email address
      Map<String, IamUser> recipientsByEmail = recipients.stream()
          .filter(user -> user.getEmail() != null && !user.getEmail().trim().isEmpty())
          .collect(Collectors.toMap(
              IamUser::getEmail,
              user -> user,
              (existing, replacement) -> existing // Keep first occurrence if duplicate
          ));

      log.info("Sending drift event notification to {} recipients (deduplicated from {} total)",
          recipientsByEmail.size(), recipients.size());

      // Fetch service details
      Optional<ApplicationService> serviceOpt = applicationServiceQueryService.findById(
          ApplicationServiceId.of(event.getServiceId()));
      String serviceName = serviceOpt.map(ApplicationService::getDisplayName)
          .orElse(event.getServiceName());

      // Prepare base template variables (same for all recipients)
      Map<String, Object> baseTemplateVariables = new HashMap<>();
      baseTemplateVariables.put("serviceName", serviceName);
      baseTemplateVariables.put("instanceId", event.getInstanceId());
      baseTemplateVariables.put("environment", event.getEnvironment() != null ? event.getEnvironment() : "N/A");
      baseTemplateVariables.put("severity", event.getSeverity() != null ? event.getSeverity().name() : "MEDIUM");
      baseTemplateVariables.put("expectedHash", event.getExpectedHash());
      baseTemplateVariables.put("appliedHash", event.getAppliedHash());
      baseTemplateVariables.put("detectedAt",
          event.getDetectedAt().atZone(java.time.ZoneId.systemDefault()).format(DATE_FORMATTER));
      baseTemplateVariables.put("driftEventId", event.getDriftEventId());

      // Send email to each recipient individually with personalized greeting
      int successCount = 0;
      int failureCount = 0;
      for (Map.Entry<String, IamUser> entry : recipientsByEmail.entrySet()) {
        String recipientEmail = entry.getKey();
        IamUser recipient = entry.getValue();

        try {
          // Create personalized template variables for this recipient
          Map<String, Object> templateVariables = new HashMap<>(baseTemplateVariables);
          templateVariables.put("recipientName", getUserDisplayName(recipient));

          // Render HTML template with personalized greeting
          String htmlBody = emailTemplateService.renderTemplate("drift-event-detected", templateVariables);

          String subject = String.format("Configuration Drift Detected: %s (%s)", serviceName, event.getInstanceId());
          notificationServicePort.sendEmail(recipientEmail, subject, htmlBody);
          successCount++;
        } catch (Exception e) {
          log.error("Failed to send drift event notification email to: {}", recipientEmail, e);
          failureCount++;
        }
      }

      log.info("Drift event notification completed: {} sent, {} failed (event: {})",
          successCount, failureCount, event.getDriftEventId());
    } catch (Exception e) {
      log.error("Failed to process drift event notification for event: {}", event.getDriftEventId(), e);
      // Don't throw exception - email failure should not affect drift event creation
    }
  }

  /**
   * Gets display name for a user.
   *
   * @param user the user
   * @return display name (firstName + lastName or username)
   */
  private String getUserDisplayName(IamUser user) {
    String firstName = user.getFirstName();
    String lastName = user.getLastName();
    if (firstName != null && lastName != null && !firstName.trim().isEmpty() && !lastName.trim().isEmpty()) {
      return firstName + " " + lastName;
    }
    return user.getUsername();
  }

  /**
   * Gets display name for requester.
   *
   * @param requester the requester user
   * @return display name (firstName + lastName or username)
   */
  private String getRequesterDisplayName(IamUser requester) {
    return getUserDisplayName(requester);
  }
}
