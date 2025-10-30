package com.example.control.api.controller.infra;

import com.example.control.domain.port.NotificationServicePort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  /**
   * DTO for test email request.
   */
  @Data
  @Schema(description = "Request to send a test email")
  public static class TestEmailRequest {
    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Recipient email address", example = "test@example.com", required = true)
    private String to;

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
  @PostMapping("/test-email")
  @PreAuthorize("hasRole('SYS_ADMIN')")
  @Operation(summary = "Send test email", description = "Sends a test email to verify email notification functionality. "
      +
      "Requires SYS_ADMIN role.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Email sent successfully", content = @Content(schema = @Schema(implementation = String.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = com.example.control.api.exception.ErrorResponse.class))),
      @ApiResponse(responseCode = "403", description = "Forbidden - SYS_ADMIN role required", content = @Content(schema = @Schema(implementation = com.example.control.api.exception.ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = com.example.control.api.exception.ErrorResponse.class)))
  })
  public ResponseEntity<String> sendTestEmail(@Valid @RequestBody TestEmailRequest request) {
    log.info("Sending test email to: {} with subject: {}", request.getTo(), request.getSubject());
    try {
      notificationServicePort.sendEmail(request.getTo(), request.getSubject(), request.getBody());
      return ResponseEntity.ok("Test email sent successfully to: " + request.getTo());
    } catch (NotificationServicePort.NotificationException e) {
      log.error("Failed to send test email", e);
      throw new RuntimeException("Failed to send test email: " + e.getMessage(), e);
    }
  }
}
