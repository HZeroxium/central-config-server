package com.example.control.api.http.controller.domain;

import com.example.control.api.http.dto.common.PageDtos;
import com.example.control.api.http.dto.domain.ApprovalDecisionDtos;
import com.example.control.api.http.mapper.domain.ApprovalDecisionApiMapper;
import com.example.control.application.service.ApprovalDecisionService;
import com.example.control.infrastructure.config.security.UserContext;
import com.example.control.domain.valueobject.id.ApprovalDecisionId;
import com.example.control.domain.model.ApprovalDecision;
import com.example.control.api.http.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for ApprovalDecision operations.
 * <p>
 * Provides endpoints for querying approval decisions with
 * JWT authentication and team-based access control.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/approval-decisions")
@RequiredArgsConstructor
@Tag(name = "Approval Decisions", description = "Query approval decisions for audit and tracking")
public class ApprovalDecisionController {

  private final ApprovalDecisionService approvalDecisionService;
  private final ApprovalDecisionApiMapper mapper;

  /**
   * Get approval decision by ID.
   *
   * @param id  the decision ID
   * @param jwt the JWT token
   * @return the decision details
   */
  @GetMapping("/{id}")
  @Operation(summary = "Get approval decision by ID", description = """
      Retrieve a specific approval decision by its ID.

      **Access Control:**
      - Users can view decisions for requests they have access to
      - SYS_ADMIN can view any decision
      """, security = {
      @SecurityRequirement(name = "oauth2_auth_code"),
      @SecurityRequirement(name = "oauth2_password")
  }, operationId = "findApprovalDecisionById")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Approval decision found", content = @Content(schema = @Schema(implementation = ApprovalDecisionDtos.Response.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Approval decision not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<ApprovalDecisionDtos.Response> findById(
      @Parameter(description = "Approval decision ID", example = "decision-12345") @PathVariable String id,
      @AuthenticationPrincipal Jwt jwt) {
    log.debug("Getting approval decision by ID: {}", id);

    UserContext userContext = UserContext.fromJwt(jwt);
    Optional<ApprovalDecision> decision = approvalDecisionService.findById(
        ApprovalDecisionId.of(id), userContext);
    if (decision.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    ApprovalDecisionDtos.Response response = mapper.toResponse(decision.get());
    return ResponseEntity.ok(response);
  }

  /**
   * List approval decisions for a specific request.
   *
   * @param requestId the request ID
   * @param pageable  pagination information
   * @param jwt       the JWT token
   * @return page of approval decisions
   */
  @GetMapping("/request/{requestId}")
  @Operation(summary = "List decisions for a request", description = """
      Retrieve all approval decisions for a specific approval request.

      **Access Control:**
      - Users can view decisions for requests they have access to
      - SYS_ADMIN can view decisions for any request
      """, security = {
      @SecurityRequirement(name = "oauth2_auth_code"),
      @SecurityRequirement(name = "oauth2_password")
  }, operationId = "findApprovalDecisionsByRequestId")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Approval decisions retrieved successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<ApprovalDecisionDtos.PageResponse> findByRequestId(
      @Parameter(description = "Approval request ID", example = "request-12345") @PathVariable String requestId,
      @ParameterObject @PageableDefault(size = 20, page = 0) Pageable pageable,
      @AuthenticationPrincipal Jwt jwt) {
    log.debug("Listing approval decisions for request: {}", requestId);

    UserContext userContext = UserContext.fromJwt(jwt);
    Page<ApprovalDecision> decisions = approvalDecisionService.findByRequestId(requestId, pageable, userContext);

    List<ApprovalDecisionDtos.Response> items = decisions.getContent().stream()
        .map(mapper::toResponse)
        .collect(Collectors.toList());

    ApprovalDecisionDtos.PageResponse response = new ApprovalDecisionDtos.PageResponse(
        items,
        PageDtos.PageMetadata.from(decisions));

    return ResponseEntity.ok(response);
  }

  /**
   * List approval decisions with filtering and pagination.
   *
   * @param filter   optional query filter
   * @param pageable pagination information
   * @param jwt      the JWT token
   * @return page of approval decisions
   */
  @GetMapping
  @Operation(summary = "List approval decisions", description = """
      Retrieve a paginated list of approval decisions with optional filtering.

      **Access Control:**
      - Users can view decisions for requests they have access to
      - SYS_ADMIN can view all decisions
      - Results are automatically filtered based on user permissions
      """, security = {
      @SecurityRequirement(name = "oauth2_auth_code"),
      @SecurityRequirement(name = "oauth2_password")
  }, operationId = "findAllApprovalDecisions")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Approval decisions retrieved successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<ApprovalDecisionDtos.PageResponse> findAll(
      @ParameterObject @Valid ApprovalDecisionDtos.QueryFilter filter,
      @ParameterObject @PageableDefault(size = 20, page = 0) Pageable pageable,
      @AuthenticationPrincipal Jwt jwt) {
    log.debug("Listing approval decisions with filter: {}", filter);

    UserContext userContext = UserContext.fromJwt(jwt);
    Page<ApprovalDecision> decisions = approvalDecisionService.findAll(
        mapper.toCriteria(filter), pageable, userContext);

    List<ApprovalDecisionDtos.Response> items = decisions.getContent().stream()
        .map(mapper::toResponse)
        .collect(Collectors.toList());

    ApprovalDecisionDtos.PageResponse response = new ApprovalDecisionDtos.PageResponse(
        items,
        PageDtos.PageMetadata.from(decisions));

    return ResponseEntity.ok(response);
  }
}
