package com.example.control.api.http.controller.infra;

import com.example.control.api.http.exception.ErrorResponse;
import com.example.control.application.query.ApplicationServiceQueryService;
import com.example.control.application.query.IamUserQueryServiceV2;
import com.example.control.application.service.infra.EmailNotificationService;
import com.example.control.domain.event.ApprovalRequestApprovedEvent;
import com.example.control.domain.event.ApprovalRequestRejectedEvent;
import com.example.control.domain.event.DriftEventCreatedEvent;
import com.example.control.domain.model.ApplicationService;
import com.example.control.domain.model.DriftEvent;
import com.example.control.domain.port.NotificationServicePort;
import com.example.control.domain.valueobject.id.ApplicationServiceId;
import com.example.control.domain.valueobject.id.IamUserId;
import com.example.control.domain.model.IamUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for testing email notification functionality.
 * <p>
 * Provides endpoint to send test emails for development and debugging purposes.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Email notification testing endpoints")
public class NotificationController {

  private final NotificationServicePort notificationServicePort;
  private final IamUserQueryServiceV2 iamUserQueryService;
  private final EmailNotificationService emailNotificationService;
  private final ApplicationServiceQueryService applicationServiceQueryService;
  /**
   * DTO for test email request.
   */
  @Data
  @Schema(description = "Request to send a test email")
  public static class TestEmailRequest {
    // @NotBlank(message = "Recipient email is required")
    // @Email(message = "Invalid email format")
    // @Schema(description = "Recipient email address", example = "test@example.com", required = true)
    // private String to;

    @NotBlank(message = "Subject is required")
    @Schema(description = "Email subject", example = "Test Email", required = true)
    private String subject;

    @NotBlank(message = "Body is required")
    @Schema(description = "Email body (HTML)", example = "<h1>Test Email</h1><p>This is a test email.</p>", required = true)
    private String body;
  }

  /**
   * Send a test email.
   * <p>
   * This endpoint allows sending test emails for development and debugging.
   * Requires SYS_ADMIN role.
   * </p>
   *
   * @param request the test email request
   * @return success response
   */
  @PostMapping("/test-email/{userId}")
  @PreAuthorize("hasRole('SYS_ADMIN')")
  @Operation(summary = "Send test email", description = "Sends a test email to verify email notification functionality. "
      +
      "Requires SYS_ADMIN role.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Email sent successfully", content = @Content(schema = @Schema(implementation = String.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "403", description = "Forbidden - SYS_ADMIN role required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<String> sendTestEmail(@PathVariable String userId, @Valid @RequestBody TestEmailRequest request) {
    log.info("Sending test email to: {} with subject: {}", userId, request.getSubject());
    try {
      Optional<IamUser> userOpt = iamUserQueryService.findById(IamUserId.of(userId));
      if (userOpt.isEmpty()) {
        return ResponseEntity.badRequest().body("User not found");
      }
      IamUser user = userOpt.get();
      notificationServicePort.sendEmail(user.getEmail(), request.getSubject(), request.getBody());
      return ResponseEntity.ok("Test email sent successfully to: " + user.getEmail());
    } catch (NotificationServicePort.NotificationException e) {
      log.error("Failed to send test email", e);
      throw new RuntimeException("Failed to send test email: " + e.getMessage(), e);
    }
  }

  /**
   * Response DTO for test notification endpoints.
   */
  @Data
  @Schema(description = "Response from test notification endpoint")
  public static class TestNotificationResponse {
    @Schema(description = "Success message", example = "Test approval notification email sent successfully")
    private String message;

    @Schema(description = "Number of email recipients", example = "1")
    private Integer recipientsCount;
  }

  /**
   * Request DTO for testing approval approved notification.
   */
  @Data
  @Schema(description = "Request to test approval approved notification")
  public static class TestApprovalApprovedRequest {
    @NotBlank(message = "Requester user ID is required")
    @Schema(description = "User ID who requested approval", example = "user-123", required = true)
    private String requesterUserId;

    @NotBlank(message = "Service ID is required")
    @Schema(description = "Service ID that was requested", example = "service-456", required = true)
    private String serviceId;

    @Schema(description = "User ID who approved the request (default: SYSTEM)", example = "admin-789")
    private String approverUserId;

    @Schema(description = "Target team ID that will own the service (default: test-team-id)", example = "team-abc")
    private String targetTeamId;
  }

  /**
   * Request DTO for testing approval rejected notification.
   */
  @Data
  @Schema(description = "Request to test approval rejected notification")
  public static class TestApprovalRejectedRequest {
    @NotBlank(message = "Requester user ID is required")
    @Schema(description = "User ID who requested approval", example = "user-123", required = true)
    private String requesterUserId;

    @NotBlank(message = "Service ID is required")
    @Schema(description = "Service ID that was requested", example = "service-456", required = true)
    private String serviceId;

    @NotBlank(message = "Rejection reason is required")
    @Schema(description = "Reason for rejection", example = "Service already owned by another team", required = true)
    private String reason;

    @Schema(description = "User ID who rejected the request (default: SYSTEM)", example = "admin-789")
    private String rejectorUserId;

    @Schema(description = "Target team ID that was requested (default: test-team-id)", example = "team-abc")
    private String targetTeamId;
  }

  /**
   * Request DTO for testing drift event notification.
   */
  @Data
  @Schema(description = "Request to test drift event notification")
  public static class TestDriftEventRequest {
    @NotBlank(message = "Service ID is required")
    @Schema(description = "Service ID where drift occurred", example = "service-456", required = true)
    private String serviceId;

    @NotBlank(message = "Instance ID is required")
    @Schema(description = "Instance ID where drift occurred", example = "instance-789", required = true)
    private String instanceId;

    @Schema(description = "Team ID that owns the service", example = "team-abc")
    private String teamId;

    @Schema(description = "Environment where drift occurred (default: dev)", example = "dev")
    private String environment;

    @Schema(description = "Drift severity (default: MEDIUM)", example = "MEDIUM", allowableValues = {"LOW", "MEDIUM", "HIGH", "CRITICAL"})
    private String severity;
  }

  /**
   * Test endpoint for approval approved notification.
   * <p>
   * This endpoint allows testing approval approved email notifications by directly
   * triggering the email sending process.
   * Requires SYS_ADMIN role.
   * </p>
   *
   * @param request the test approval approved request
   * @return response with message and recipients count
   */
  @PostMapping("/test-approval-approved")
  @PreAuthorize("hasRole('SYS_ADMIN')")
  @Operation(
      summary = "Test approval approved notification",
      description = """
          Sends a test email notification for an approved approval request.
          
          **Use Case:**
          - Testing email templates and notification flow
          - Verifying email delivery configuration
          - Debugging notification issues
          
          **Behavior:**
          - Creates a mock ApprovalRequestApprovedEvent with provided data
          - Auto-generates requestId and approvedAt timestamp
          - Sends email to the requester user
          - Uses mock user data if requester not found in database
          
          **Requires SYS_ADMIN role.**
          """,
      tags = {"Notifications"}
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Test email sent successfully",
          content = @Content(schema = @Schema(implementation = TestNotificationResponse.class))
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Invalid request data",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "403",
          description = "Forbidden - SYS_ADMIN role required",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "500",
          description = "Internal server error",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  public ResponseEntity<TestNotificationResponse> testApprovalApproved(
      @Valid @RequestBody TestApprovalApprovedRequest request) {
    log.info("Testing approval approved notification for requester: {} service: {}",
        request.getRequesterUserId(), request.getServiceId());

    try {
      // Build ApprovalRequestApprovedEvent with auto-generated fields
      ApprovalRequestApprovedEvent event = ApprovalRequestApprovedEvent.builder()
          .requestId(UUID.randomUUID().toString())
          .requesterUserId(request.getRequesterUserId())
          .serviceId(request.getServiceId())
          .targetTeamId(request.getTargetTeamId() != null ? request.getTargetTeamId() : "test-team-id")
          .approverUserId(request.getApproverUserId() != null ? request.getApproverUserId() : "SYSTEM")
          .approvedAt(Instant.now())
          .build();

      // Send notification
      emailNotificationService.sendApprovalNotification(event);

      TestNotificationResponse response = new TestNotificationResponse();
      response.setMessage("Test approval approved notification email sent successfully");
      response.setRecipientsCount(1); // Single requester recipient

      log.info("Successfully sent test approval approved notification for requester: {}",
          request.getRequesterUserId());
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Failed to send test approval approved notification", e);
      throw new RuntimeException("Failed to send test approval approved notification: " + e.getMessage(), e);
    }
  }

  /**
   * Test endpoint for approval rejected notification.
   * <p>
   * This endpoint allows testing approval rejected email notifications by directly
   * triggering the email sending process.
   * Requires SYS_ADMIN role.
   * </p>
   *
   * @param request the test approval rejected request
   * @return response with message and recipients count
   */
  @PostMapping("/test-approval-rejected")
  @PreAuthorize("hasRole('SYS_ADMIN')")
  @Operation(
      summary = "Test approval rejected notification",
      description = """
          Sends a test email notification for a rejected approval request.
          
          **Use Case:**
          - Testing email templates and notification flow
          - Verifying email delivery configuration
          - Debugging notification issues
          
          **Behavior:**
          - Creates a mock ApprovalRequestRejectedEvent with provided data
          - Auto-generates requestId and rejectedAt timestamp
          - Sends email to the requester user
          - Uses mock user data if requester not found in database
          
          **Requires SYS_ADMIN role.**
          """,
      tags = {"Notifications"}
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Test email sent successfully",
          content = @Content(schema = @Schema(implementation = TestNotificationResponse.class))
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Invalid request data",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "403",
          description = "Forbidden - SYS_ADMIN role required",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "500",
          description = "Internal server error",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  public ResponseEntity<TestNotificationResponse> testApprovalRejected(
      @Valid @RequestBody TestApprovalRejectedRequest request) {
    log.info("Testing approval rejected notification for requester: {} service: {} reason: {}",
        request.getRequesterUserId(), request.getServiceId(), request.getReason());

    try {
      // Build ApprovalRequestRejectedEvent with auto-generated fields
      ApprovalRequestRejectedEvent event = ApprovalRequestRejectedEvent.builder()
          .requestId(UUID.randomUUID().toString())
          .requesterUserId(request.getRequesterUserId())
          .serviceId(request.getServiceId())
          .targetTeamId(request.getTargetTeamId() != null ? request.getTargetTeamId() : "test-team-id")
          .rejectorUserId(request.getRejectorUserId() != null ? request.getRejectorUserId() : "SYSTEM")
          .rejectedAt(Instant.now())
          .reason(request.getReason())
          .build();

      // Send notification
      emailNotificationService.sendRejectionNotification(event);

      TestNotificationResponse response = new TestNotificationResponse();
      response.setMessage("Test approval rejected notification email sent successfully");
      response.setRecipientsCount(1); // Single requester recipient

      log.info("Successfully sent test approval rejected notification for requester: {}",
          request.getRequesterUserId());
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Failed to send test approval rejected notification", e);
      throw new RuntimeException("Failed to send test approval rejected notification: " + e.getMessage(), e);
    }
  }

  /**
   * Test endpoint for drift event notification.
   * <p>
   * This endpoint allows testing drift event email notifications by directly
   * triggering the email sending process.
   * Requires SYS_ADMIN role.
   * </p>
   *
   * @param request the test drift event request
   * @return response with message and recipients count
   */
  @PostMapping("/test-drift-event")
  @PreAuthorize("hasRole('SYS_ADMIN')")
  @Operation(
      summary = "Test drift event notification",
      description = """
          Sends test email notifications for a configuration drift event.
          
          **Use Case:**
          - Testing email templates and notification flow
          - Verifying email delivery configuration
          - Debugging notification issues
          
          **Behavior:**
          - Creates a mock DriftEventCreatedEvent with provided data
          - Auto-generates driftEventId, serviceName, hashes, and detectedAt timestamp
          - Sends emails to all SYS_ADMIN users and service owner team members
          - Returns actual recipients count (SYS_ADMIN + team members)
          
          **Recipients:**
          - All users with SYS_ADMIN role
          - All users in the service owner team (if teamId provided)
          
          **Requires SYS_ADMIN role.**
          """,
      tags = {"Notifications"}
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Test emails sent successfully",
          content = @Content(schema = @Schema(implementation = TestNotificationResponse.class))
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Invalid request data",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "403",
          description = "Forbidden - SYS_ADMIN role required",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "500",
          description = "Internal server error",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  public ResponseEntity<TestNotificationResponse> testDriftEvent(
      @Valid @RequestBody TestDriftEventRequest request) {
    log.info("Testing drift event notification for service: {} instance: {}",
        request.getServiceId(), request.getInstanceId());

    try {
      // Lookup service name from ApplicationService if available
      String serviceName = request.getServiceId();
      Optional<ApplicationService> serviceOpt = applicationServiceQueryService.findById(
          ApplicationServiceId.of(request.getServiceId()));
      if (serviceOpt.isPresent()) {
        serviceName = serviceOpt.get().getDisplayName();
      }

      // Parse severity or default to MEDIUM
      DriftEvent.DriftSeverity severity = DriftEvent.DriftSeverity.MEDIUM;
      if (request.getSeverity() != null && !request.getSeverity().trim().isEmpty()) {
        try {
          severity = DriftEvent.DriftSeverity.valueOf(request.getSeverity().toUpperCase());
        } catch (IllegalArgumentException e) {
          log.warn("Invalid severity value: {}, using default MEDIUM", request.getSeverity());
        }
      }

      // Build DriftEventCreatedEvent with auto-generated fields
      DriftEventCreatedEvent event = DriftEventCreatedEvent.builder()
          .driftEventId(UUID.randomUUID().toString())
          .serviceName(serviceName)
          .instanceId(request.getInstanceId())
          .serviceId(request.getServiceId())
          .teamId(request.getTeamId())
          .environment(request.getEnvironment() != null ? request.getEnvironment() : "dev")
          .expectedHash("expected-hash-" + UUID.randomUUID())
          .appliedHash("applied-hash-" + UUID.randomUUID())
          .severity(severity)
          .detectedAt(Instant.now())
          .build();

      // Count recipients before sending (SYS_ADMIN + team members)
      int recipientsCount = 0;
      try {
        List<IamUser> sysAdmins = iamUserQueryService.findByRole("SYS_ADMIN");
        recipientsCount += sysAdmins.size();
      } catch (Exception e) {
        log.warn("Failed to count SYS_ADMIN users", e);
      }

      if (request.getTeamId() != null && !request.getTeamId().trim().isEmpty()) {
        try {
          List<IamUser> teamMembers = iamUserQueryService.findByTeam(request.getTeamId());
          recipientsCount += teamMembers.size();
        } catch (Exception e) {
          log.warn("Failed to count team members for teamId: {}", request.getTeamId(), e);
        }
      }

      // Send notification
      emailNotificationService.sendDriftEventNotification(event);

      TestNotificationResponse response = new TestNotificationResponse();
      response.setMessage("Test drift event notification emails sent successfully");
      response.setRecipientsCount(Math.max(recipientsCount, 0)); // Ensure non-negative

      log.info("Successfully sent test drift event notification for service: {} to {} recipients",
          request.getServiceId(), recipientsCount);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Failed to send test drift event notification", e);
      throw new RuntimeException("Failed to send test drift event notification: " + e.getMessage(), e);
    }
  }
}
