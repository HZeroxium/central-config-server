package com.example.control.api.dto.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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
public class ApprovalRequestDtos {

    /**
     * Request DTO for creating a new approval request.
     */
    public record CreateRequest(
            @NotBlank(message = "Service ID is required")
            @Size(max = 100, message = "Service ID must not exceed 100 characters")
            String serviceId,

            @NotBlank(message = "Target team ID is required")
            @Size(max = 100, message = "Target team ID must not exceed 100 characters")
            String targetTeamId,

            @Size(max = 500, message = "Note must not exceed 500 characters")
            String note
    ) {}

    /**
     * Request DTO for submitting a decision on an approval request.
     */
    public record DecisionRequest(
            @NotBlank(message = "Decision is required")
            @Size(max = 20, message = "Decision must not exceed 20 characters")
            String decision,

            @Size(max = 500, message = "Note must not exceed 500 characters")
            String note
    ) {}

    /**
     * Response DTO for approval request details.
     */
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
    ) {}

    /**
     * Query filter DTO for searching approval requests.
     */
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
    ) {}

    /**
     * Approval target information.
     */
    public record ApprovalTarget(
            @JsonProperty("serviceId")
            String serviceId,

            @JsonProperty("teamId")
            String teamId
    ) {}

    /**
     * Approval gate information.
     */
    public record ApprovalGate(
            @JsonProperty("gate")
            String gate,

            @JsonProperty("minApprovals")
            Integer minApprovals
    ) {}

    /**
     * Requester snapshot information.
     */
    public record RequesterSnapshot(
            @JsonProperty("teamIds")
            List<String> teamIds,

            @JsonProperty("managerId")
            String managerId,

            @JsonProperty("roles")
            List<String> roles
    ) {}
}