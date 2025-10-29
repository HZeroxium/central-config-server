package com.example.control.api.dto.domain;

import com.example.control.api.dto.common.PageDtos;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTOs for ApprovalRequest API operations.
 * <p>
 * Provides request/response DTOs for managing approval requests with
 * proper validation and JSON serialization.
 * </p>
 */
@Schema(name = "ApprovalRequestDtos", description = "DTOs for ApprovalRequest API operations")
public class ApprovalRequestDtos {

    /**
     * Request DTO for creating a new approval request.
     */
    @Schema(name = "ApprovalRequestCreateRequest", description = "Request to create a new approval request")
    public record CreateRequest(
            @NotBlank(message = "Service ID is required")
            @Size(max = 100, message = "Service ID must not exceed 100 characters")
            @Schema(description = "Service ID to request ownership for", example = "payment-service", maxLength = 100)
            String serviceId,

            @NotBlank(message = "Target team ID is required")
            @Size(max = 100, message = "Target team ID must not exceed 100 characters")
            @Schema(description = "Team requesting ownership", example = "team_core", maxLength = 100)
            String targetTeamId,

            @Size(max = 500, message = "Note must not exceed 500 characters")
            @Schema(description = "Optional note explaining the request", example = "Our team will be responsible for maintaining this service", maxLength = 500)
            String note
    ) {
    }

    /**
     * Request DTO for submitting a decision on an approval request.
     */
    @Schema(name = "ApprovalRequestDecisionRequest", description = "Request to submit a decision on an approval request")
    public record DecisionRequest(
            @NotBlank(message = "Decision is required")
            @Size(max = 20, message = "Decision must not exceed 20 characters")
            @Schema(description = "Approval decision", example = "APPROVE", allowableValues = {"APPROVE", "REJECT"})
            String decision,

            @Size(max = 500, message = "Note must not exceed 500 characters")
            @Schema(description = "Optional note explaining the decision", example = "Approved based on team capacity and expertise", maxLength = 500)
            String note
    ) {
    }

    /**
     * Response DTO for approval request details.
     */
    @Schema(name = "ApprovalRequestResponse", description = "Approval request details response")
    public record Response(
            @JsonProperty("id")
            String id,

            @JsonProperty("requesterUserId")
            String requesterUserId,

            @JsonProperty("requestType")
            String requestType,

            @JsonProperty("target")
            ApprovalTarget target,

            @JsonProperty("required")
            List<ApprovalGate> required,

            @JsonProperty("status")
            String status,

            @JsonProperty("snapshot")
            RequesterSnapshot snapshot,

            @JsonProperty("counts")
            Map<String, Integer> counts,

            @JsonProperty("createdAt")
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
            Instant createdAt,

            @JsonProperty("updatedAt")
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
            Instant updatedAt,

            @JsonProperty("note")
            String note,

            @JsonProperty("cancelReason")
            String cancelReason
    ) {
    }

    /**
     * Query filter DTO for searching approval requests.
     */
    @Schema(name = "ApprovalRequestQueryFilter", description = "Query filter for searching approval requests")
    public record QueryFilter(
            @Size(max = 100, message = "Requester user ID must not exceed 100 characters")
            String requesterUserId,

            @Size(max = 20, message = "Status must not exceed 20 characters")
            String status,

            @Size(max = 50, message = "Request type must not exceed 50 characters")
            String requestType,

            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
            Instant fromDate,

            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
            Instant toDate
    ) {
    }

    /**
     * Approval target information.
     */
    @Schema(name = "ApprovalRequestApprovalTarget", description = "Approval target information")
    public record ApprovalTarget(
            @JsonProperty("serviceId")
            String serviceId,

            @JsonProperty("teamId")
            String teamId
    ) {
    }

    /**
     * Approval gate information.
     */
    @Schema(name = "ApprovalRequestApprovalGate", description = "Approval gate information")
    public record ApprovalGate(
            @JsonProperty("gate")
            String gate,

            @JsonProperty("minApprovals")
            Integer minApprovals
    ) {
    }

    /**
     * Requester snapshot information.
     */
    @Schema(name = "ApprovalRequestRequesterSnapshot", description = "Requester snapshot information")
    public record RequesterSnapshot(
            @JsonProperty("teamIds")
            List<String> teamIds,

            @JsonProperty("managerId")
            String managerId,

            @JsonProperty("roles")
            List<String> roles
    ) {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ApprovalRequestPageResponse", description = "Page response for approval requests")
    public static class ApprovalRequestPageResponse {
        @Schema(description = "List of approval requests in current page")
        private List<ApprovalRequestDtos.Response> items;

        @Schema(description = "Pagination metadata")
        private PageDtos.PageMetadata metadata;
    }
}