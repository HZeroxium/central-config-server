package com.example.control.api.controller;

import com.example.control.api.dto.domain.DriftEventDtos;
import com.example.control.api.mapper.domain.DriftEventApiMapper;
import com.example.control.application.service.DriftEventService;
import com.example.control.infrastructure.config.security.UserContext;
import com.example.control.domain.object.DriftEvent;
import com.example.control.domain.criteria.DriftEventCriteria;
import com.example.control.domain.id.DriftEventId;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.example.control.api.exception.ErrorResponse;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/drift-events")
@RequiredArgsConstructor
@Tag(name = "Drift Events", description = "CRUD and query for drift events")
public class DriftEventController {

    private final DriftEventService service;

    @PostMapping
    @Operation(
            summary = "Create drift event",
            description = """
                    Create a new drift event when configuration drift is detected.
                    
                    **Access Control:**
                    - Team members: Can create drift events for services owned by their team
                    - SYS_ADMIN: Can create drift events for any service
                    - System: Can create drift events automatically during heartbeat processing
                    """,
            security = {
                    @SecurityRequirement(name = "oauth2_auth_code"),
                    @SecurityRequirement(name = "oauth2_password")
            },
            operationId = "createDriftEvent"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Drift event created successfully",
                    content = @Content(schema = @Schema(implementation = DriftEventDtos.Response.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<DriftEventDtos.Response> create(
            @Parameter(description = "Drift event creation request",
                    schema = @Schema(implementation = DriftEventDtos.CreateRequest.class))
            @Valid @RequestBody DriftEventDtos.CreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UserContext userContext = UserContext.fromJwt(jwt);
        DriftEvent saved = service.save(DriftEventApiMapper.toDomain(request), userContext);
        return ResponseEntity.ok(DriftEventApiMapper.toResponse(saved));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get drift event by ID",
            description = """
                    Retrieve a specific drift event by its ID.
                    
                    **Access Control:**
                    - Team members: Can view drift events for services owned by their team
                    - SYS_ADMIN: Can view any drift event
                    - Shared access: Can view drift events for services shared with their team
                    """,
            security = {
                    @SecurityRequirement(name = "oauth2_auth_code"),
                    @SecurityRequirement(name = "oauth2_password")
            },
            operationId = "findDriftEventById"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Drift event found",
                    content = @Content(schema = @Schema(implementation = DriftEventDtos.Response.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Drift event not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<DriftEventDtos.Response> findById(
            @Parameter(description = "Drift event ID", example = "drift-12345")
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        UserContext userContext = UserContext.fromJwt(jwt);
        Optional<DriftEvent> opt = service.findById(DriftEventId.of(id), userContext);
        return opt.map(ev -> ResponseEntity.ok(DriftEventApiMapper.toResponse(ev)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Update drift event",
            description = """
                    Update an existing drift event (status and notes).
                    
                    **Access Control:**
                    - Team members: Can update drift events for services owned by their team
                    - SYS_ADMIN: Can update any drift event
                    - Updates include status changes (DETECTED, RESOLVED, IGNORED) and resolution notes
                    """,
            security = {
                    @SecurityRequirement(name = "oauth2_auth_code"),
                    @SecurityRequirement(name = "oauth2_password")
            },
            operationId = "updateDriftEvent"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Drift event updated successfully",
                    content = @Content(schema = @Schema(implementation = DriftEventDtos.Response.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Drift event not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<DriftEventDtos.Response> update(
            @Parameter(description = "Drift event ID", example = "drift-12345")
            @PathVariable String id,
            @Parameter(description = "Drift event update request",
                    schema = @Schema(implementation = DriftEventDtos.UpdateRequest.class))
            @Valid @RequestBody DriftEventDtos.UpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UserContext userContext = UserContext.fromJwt(jwt);
        DriftEvent ev = service.findById(DriftEventId.of(id), userContext).orElse(null);
        if (ev == null) return ResponseEntity.notFound().build();
        DriftEventApiMapper.apply(ev, request);
        DriftEvent saved = service.save(ev);
        return ResponseEntity.ok(DriftEventApiMapper.toResponse(saved));
    }

    @GetMapping
    @Operation(
            summary = "List drift events with filters and pagination",
            description = """
                    Retrieve a paginated list of drift events with optional filtering.
                    
                    **Access Control:**
                    - Team members: Can view drift events for services owned by their team
                    - SYS_ADMIN: Can view all drift events
                    - Shared access: Can view drift events for services shared with their team
                    - Results are automatically filtered based on user permissions
                    """,
            security = {
                    @SecurityRequirement(name = "oauth2_auth_code"),
                    @SecurityRequirement(name = "oauth2_password")
            },
            operationId = "findAllDriftEvents"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Drift events retrieved successfully",
                    content = @Content(schema = @Schema(implementation = DriftEventDtos.DriftEventPageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @Timed("api.drift-events.list")
    public ResponseEntity<DriftEventDtos.DriftEventPageResponse> findAll(
            @ParameterObject @Valid DriftEventDtos.QueryFilter filter,
            @ParameterObject @PageableDefault(size = 20, page = 0) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {

        UserContext userContext = UserContext.fromJwt(jwt);

        DriftEventCriteria criteria = DriftEventApiMapper.toCriteria(filter, userContext);
        Page<DriftEvent> page = service.findAll(criteria, pageable, userContext);
        DriftEventDtos.DriftEventPageResponse response = DriftEventApiMapper.toPageResponse(page);
        return ResponseEntity.ok(response);
    }
}


