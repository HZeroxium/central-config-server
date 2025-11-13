package com.example.control.api.http.controller.kv;

import com.example.control.api.http.dto.kv.KVDtos;
import com.example.control.api.http.exception.ErrorResponse;
import com.example.control.api.http.mapper.kv.KVApiMapper;
import com.example.control.application.service.KVService;
import com.example.control.domain.model.kv.KVEntry;
import com.example.control.domain.model.kv.KVListStructure;
import com.example.control.domain.model.kv.KVPath;
import com.example.control.domain.model.kv.KVTransactionResponse;
import com.example.control.domain.port.KVStorePort;
import com.example.control.infrastructure.adapter.kv.PrefixPolicy;
import com.example.control.infrastructure.config.security.UserContext;
import com.example.control.infrastructure.config.security.UserContextExtractor;
import com.example.control.application.service.kv.KVTypeCodec;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for KV operations.
 * <p>
 * Provides CRUD endpoints for Key-Value operations organized per ApplicationService
 * with JWT authentication and team-based access control.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/application-services/{serviceId}/kv")
@RequiredArgsConstructor
@Tag(name = "Key-Value Store", description = "CRUD operations for Key-Value store per ApplicationService")
public class KVController {

    private final KVService kvService;
    private final PrefixPolicy prefixPolicy;

    /**
     * Get a single KV entry.
     *
     * @param serviceId the service ID
     * @param path      the key path (relative to service root)
     * @param raw       if true, return raw bytes; if false, return JSON metadata
     * @param consistent if true, use consistent read
     * @param stale     if true, use stale read
     * @param jwt       the JWT token
     * @return the KV entry or 404 if not found
     */
    @GetMapping(
        value = "/{*path}",
        produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE }
    )
    @Operation(
        summary = "Get a KV entry",
        description = """
            Retrieve a single KV entry by path.

            **Response Formats**
            - `?raw=true`: `application/octet-stream` (binary)
            - default: `application/json` (metadata + base64 value)
            """,
        security = {
            @SecurityRequirement(name = "oauth2_auth_code"),
            @SecurityRequirement(name = "oauth2_password")
        },
        operationId = "getKVEntry"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "KV entry found",
            headers = {
                @io.swagger.v3.oas.annotations.headers.Header(
                    name = "X-Consul-Index",
                    description = "Consul index for blocking queries",
                    schema = @Schema(type = "string")
                )
            },
            content = {
                // JSON metadata
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = KVDtos.EntryResponse.class),
                    examples = {
                        @ExampleObject(
                            name = "json-metadata",
                            value = """
                            {
                                "serviceId":"sample-service",
                                "key":"config/db.url",
                                "flags":0,
                                "modifyIndex":1234,
                                "valueBase64":"aHR0cDovL2RiLWxvY2Fs"
                            }
                            """
                        )
                    }
                ),
                // Raw bytes
                @Content(
                    mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                    schema = @Schema(type = "string", format = "binary")
                )
            }
        ),
        @ApiResponse(
            responseCode = "404",
            description = "KV entry not found or access denied",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<?> get(
            @Parameter(description = "Application service ID", example = "sample-service") @PathVariable String serviceId,
            @Parameter(description = "Key path (relative to service root)", example = "config/db.url") @PathVariable String path,
            @Parameter(description = "Return raw bytes instead of JSON") @RequestParam(defaultValue = "false") boolean raw,
            @Parameter(description = "Use consistent read") @RequestParam(defaultValue = "false") boolean consistent,
            @Parameter(description = "Use stale read") @RequestParam(defaultValue = "false") boolean stale,
            @AuthenticationPrincipal Jwt jwt) {
        // Normalize path: strip leading slash if present (/{*path} captures with leading slash)
        String normalizedPath = normalizePath(path);
        log.debug("Getting KV entry for service: {}, path: {} (normalized: {}), raw: {}", serviceId, path, normalizedPath, raw);

        // Extract UserContext from SecurityContext (handles both JWT and API key authentication)
        UserContext userContext = UserContextExtractor.extract();
        KVStorePort.KVReadOptions options = KVStorePort.KVReadOptions.builder()
                .raw(raw)
                .consistent(consistent)
                .stale(stale)
                .build();

        try {
            Optional<KVEntry> entryOpt = kvService.get(serviceId, normalizedPath, options, userContext);
            if (entryOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            KVEntry entry = entryOpt.get();

            if (raw) {
                // Return raw bytes
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header("X-Consul-Index", String.valueOf(entry.modifyIndex()))
                        .body(entry.value());
            } else {
                // Return JSON metadata
                KVDtos.EntryResponse response = KVApiMapper.toEntryResponse(entry, serviceId, prefixPolicy);
                return ResponseEntity.ok()
                        .header("X-Consul-Index", String.valueOf(entry.modifyIndex()))
                        .body(response);
            }
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for KV get: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * List KV entries or keys.
     *
     * @param serviceId  the service ID
     * @param prefix     the prefix to list (optional, defaults to root)
     * @param keysOnly   if true, return only keys; if false, return full entries
     * @param recurse    if true, recurse into subdirectories
     * @param separator  separator for directory-like listing (e.g., "/")
     * @param consistent if true, use consistent read
     * @param stale      if true, use stale read
     * @param jwt        the JWT token
     * @return list of entries or keys
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "List KV entries or keys",
        description = """
            List KV entries or keys under a prefix.

            **Response Forms**
            - `?keysOnly=true`: KeysResponse (array of key paths)
            - default: ListResponse (array of entries with metadata)
            """
        , security = {
            @SecurityRequirement(name = "oauth2_auth_code"),
            @SecurityRequirement(name = "oauth2_password")
        },
        operationId = "listKVEntries"
    )
    @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved list",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(oneOf = { KVDtos.ListResponse.class, KVDtos.KeysResponse.class }),
            examples = {
                @ExampleObject(
                    name = "keysOnly=true",
                    value = """
                    { "keys": ["config/db.url", "config/timeout", "secrets/enc.key"] }
                    """
                ),
                @ExampleObject(
                    name = "keysOnly=false (default)",
                    value = """
                    {
                        "entries": [
                            {
                                "serviceId": "sample-service",
                                "key": "config/db.url",
                                "flags": 0,
                                "modifyIndex": 1234,
                                "valueBase64": "aHR0cDovL2RiLWxvY2Fs"
                            }
                        ]
                    }
                    """
                )
            }
        )
    ),
    @ApiResponse(
        responseCode = "404",
        description = "Service not found or access denied",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))
    )
    })
    public ResponseEntity<?> list(
            @Parameter(description = "Application service ID", example = "sample-service") @PathVariable String serviceId,
            @Parameter(description = "Prefix to list (relative to service root)", example = "config/") @RequestParam(defaultValue = "") String prefix,
            @Parameter(description = "Return only keys, not full entries") @RequestParam(defaultValue = "false") boolean keysOnly,
            @Parameter(description = "Recurse into subdirectories") @RequestParam(defaultValue = "true") boolean recurse,
            @Parameter(description = "Separator for directory-like listing") @RequestParam(required = false) String separator,
            @Parameter(description = "Use consistent read") @RequestParam(defaultValue = "false") boolean consistent,
            @Parameter(description = "Use stale read") @RequestParam(defaultValue = "false") boolean stale,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            String normalizedPrefix = normalizePrefix(prefix);
            log.debug("Listing KV entries for service: {}, prefix: {} (normalized: {}), keysOnly: {}", serviceId, prefix, normalizedPrefix, keysOnly);

            // Extract UserContext from SecurityContext (handles both JWT and API key authentication)
        UserContext userContext = UserContextExtractor.extract();
            KVStorePort.KVListOptions options = KVStorePort.KVListOptions.builder()
                    .recurse(recurse)
                    .keysOnly(keysOnly)
                    .separator(separator)
                    .consistent(consistent)
                    .stale(stale)
                    .build();

            if (keysOnly) {
                List<String> keys = kvService.listKeys(serviceId, normalizedPrefix, options, userContext);
                return ResponseEntity.ok(new KVDtos.KeysResponse(keys));
            } else {
                List<KVEntry> entries = kvService.listEntries(serviceId, normalizedPrefix, options, userContext);
                KVDtos.ListResponse response = KVApiMapper.toListResponse(entries, serviceId, prefixPolicy);
                return ResponseEntity.ok(response);
            }
        } catch (IllegalArgumentException e) {
            // Check if this is a prefix validation error (from normalizePrefix) or service error
            // Prefix validation errors typically contain "Path" or "prefix" in the message
            if (e.getMessage() != null && (e.getMessage().contains("Path") || e.getMessage().contains("prefix"))) {
                log.warn("Invalid prefix format for KV list: {}", e.getMessage());
                return ResponseEntity.badRequest().build();
            }
            log.warn("Invalid request for KV list: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/list")
    @Operation(
            summary = "Get a structured list",
            description = """
                    Assemble a logical list stored under the given prefix.
                    
                    **Prefix Parameter:**
                    - Use `?prefix=...` to specify the prefix (relative to service root)
                    - Empty prefix `?prefix=` or omitted means root prefix
                    - Prefix is automatically normalized (trimmed, leading slashes removed)
                    """,
            security = {
                    @SecurityRequirement(name = "oauth2_auth_code"),
                    @SecurityRequirement(name = "oauth2_password")
            },
            operationId = "getKVList"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "List structure found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KVDtos.ListResponseV2.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "List not found or access denied",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<KVDtos.ListResponseV2> getList(
            @Parameter(description = "Application service ID", example = "sample-service") @PathVariable String serviceId,
            @Parameter(description = "Prefix to retrieve list from (relative to service root)", example = "config") @RequestParam(defaultValue = "") String prefix,
            @Parameter(description = "Use consistent read") @RequestParam(defaultValue = "false") boolean consistent,
            @Parameter(description = "Use stale read") @RequestParam(defaultValue = "false") boolean stale,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            String normalizedPrefix = normalizePrefix(prefix);
            log.debug("Getting KV list for service: {}, prefix: {} (normalized: {})", serviceId, prefix, normalizedPrefix);
            
            // Extract UserContext from SecurityContext (handles both JWT and API key authentication)
        UserContext userContext = UserContextExtractor.extract();
            KVStorePort.KVReadOptions options = KVStorePort.KVReadOptions.builder()
                    .consistent(consistent)
                    .stale(stale)
                    .build();
            return kvService.getList(serviceId, normalizedPrefix, options, userContext)
                    .map(KVApiMapper::toListResponse)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid prefix for KV list get: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping(value = "/list", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Create or update a structured list",
            description = """
                    Writes list items and manifest atomically using transactions.
                    
                    **Prefix Parameter:**
                    - Use `?prefix=...` to specify the prefix (relative to service root)
                    - Empty prefix `?prefix=` or omitted means root prefix
                    - Prefix is automatically normalized (trimmed, leading slashes removed)
                    """,
            security = {
                    @SecurityRequirement(name = "oauth2_auth_code"),
                    @SecurityRequirement(name = "oauth2_password")
            },
            operationId = "putKVList"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "List structure created/updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KVDtos.TransactionResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Service not found or access denied",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<KVDtos.TransactionResponse> putList(
            @Parameter(description = "Application service ID", example = "sample-service") @PathVariable String serviceId,
            @Parameter(description = "Prefix to store list under (relative to service root)", example = "config") @RequestParam(defaultValue = "") String prefix,
            @Parameter(description = "List structure to persist") @Valid @RequestBody KVDtos.ListWriteRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            String normalizedPrefix = normalizePrefix(prefix);
            log.info("Putting KV list for service: {}, prefix: {} (normalized: {})", serviceId, prefix, normalizedPrefix);
            
            // Extract UserContext from SecurityContext (handles both JWT and API key authentication)
        UserContext userContext = UserContextExtractor.extract();
            KVListStructure structure = KVApiMapper.toListStructure(request);
            KVTransactionResponse response = kvService.putList(serviceId, normalizedPrefix, structure, KVApiMapper.toDeleteIds(request), userContext);
            KVDtos.TransactionResponse dto = KVApiMapper.toTransactionResponse(serviceId, response, prefixPolicy);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid prefix for KV list put: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    /**
     * Create or update a KV entry.
     *
     * @param serviceId the service ID
     * @param path      the key path
     * @param request   the put request
     * @param ifMatch   If-Match header for CAS (optional)
     * @param jwt       the JWT token
     * @return write result
     */
    @Operation(summary = "Create or update a KV entry", description = """
            Create or update a KV entry. Supports CAS (Compare-And-Set) for conditional updates.
            
            **Access Control:**
            - User must have EDIT permission on the ApplicationService
            
            **CAS Support:**
            - Provide `cas` in request body, or use `If-Match` header
            - Returns 409 Conflict if CAS fails
            """, security = {
            @SecurityRequirement(name = "oauth2_auth_code"),
            @SecurityRequirement(name = "oauth2_password")
    }, operationId = "putKVEntry")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "KV entry created/updated successfully", content = @Content(schema = @Schema(implementation = KVDtos.WriteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Service not found or access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "CAS conflict", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{*path}")
    public ResponseEntity<KVDtos.WriteResponse> put(
            @Parameter(description = "Application service ID", example = "sample-service") @PathVariable String serviceId,
            @Parameter(description = "Key path (relative to service root)", example = "config/db.url") @PathVariable String path,
            @Parameter(description = "Put request with value and options") @Valid @RequestBody KVDtos.PutRequest request,
            @Parameter(description = "If-Match header for CAS (alternative to cas in body)") @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @AuthenticationPrincipal Jwt jwt) {
        // Normalize path: strip leading slash if present
        String normalizedPath = normalizePath(path);
        log.info("Putting KV entry for service: {}, path: {} (normalized: {})", serviceId, path, normalizedPath);

        // Extract UserContext from SecurityContext (handles both JWT and API key authentication)
        UserContext userContext = UserContextExtractor.extract();

        // Support CAS via If-Match header
        Long cas = request.cas();
        if (cas == null && ifMatch != null && !ifMatch.isBlank()) {
            try {
                // Remove quotes if present
                String casStr = ifMatch.replace("\"", "").trim();
                cas = Long.parseLong(casStr);
            } catch (NumberFormatException e) {
                log.warn("Invalid If-Match header value: {}", ifMatch);
                return ResponseEntity.badRequest().build();
            }
        }

        // Create write options with CAS
        KVStorePort.KVWriteOptions writeOptions = KVStorePort.KVWriteOptions.builder()
                .cas(cas)
                .flags(request.flags() != null ? request.flags() : 0L)
                .build();

        try {
            byte[] valueBytes = request.valueBytes();
            KVStorePort.KVWriteResult result = kvService.put(serviceId, normalizedPath, valueBytes, writeOptions, userContext);

            if (!result.success()) {
                // CAS conflict
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new KVDtos.WriteResponse(false, 0L));
            }

            KVDtos.WriteResponse response = KVApiMapper.toWriteResponse(result);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for KV put: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete a KV entry.
     *
     * @param serviceId the service ID
     * @param path      the key path
     * @param recurse   if true, delete all keys under prefix
     * @param cas       CAS modify index for conditional delete
     * @param jwt       the JWT token
     * @return delete result
     */
    @Operation(summary = "Delete a KV entry", description = """
            Delete a KV entry or prefix. Supports CAS for conditional delete.
            
            **Access Control:**
            - User must have EDIT permission on the ApplicationService
            
            **Recursive Delete:**
            - Use `?recurse=true` to delete all keys under a prefix
            - Use with caution as this operation is irreversible
            """, security = {
            @SecurityRequirement(name = "oauth2_auth_code"),
            @SecurityRequirement(name = "oauth2_password")
    }, operationId = "deleteKVEntry")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "KV entry deleted successfully", content = @Content(schema = @Schema(implementation = KVDtos.DeleteResponse.class))),
            @ApiResponse(responseCode = "404", description = "Service not found or access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "CAS conflict", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{*path}")
    public ResponseEntity<KVDtos.DeleteResponse> delete(
            @Parameter(description = "Application service ID", example = "sample-service") @PathVariable String serviceId,
            @Parameter(description = "Key path (relative to service root)", example = "config/db.url") @PathVariable String path,
            @Parameter(description = "Recurse delete (delete all keys under prefix)") @RequestParam(defaultValue = "false") boolean recurse,
            @Parameter(description = "CAS modify index for conditional delete") @RequestParam(required = false) Long cas,
            @AuthenticationPrincipal Jwt jwt) {
        // Normalize path: strip leading slash if present
        String normalizedPath = normalizePath(path);
        log.info("Deleting KV entry for service: {}, path: {} (normalized: {}), recurse: {}", serviceId, path, normalizedPath, recurse);

        // Extract UserContext from SecurityContext (handles both JWT and API key authentication)
        UserContext userContext = UserContextExtractor.extract();
        KVStorePort.KVDeleteOptions deleteOptions = KVStorePort.KVDeleteOptions.builder()
                .recurse(recurse)
                .cas(cas)
                .build();

        try {
            KVStorePort.KVDeleteResult result = kvService.delete(serviceId, normalizedPath, deleteOptions, userContext);

            if (!result.success()) {
                // CAS conflict or not found
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new KVDtos.DeleteResponse(false));
            }

            KVDtos.DeleteResponse response = KVApiMapper.toDeleteResponse(result);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for KV delete: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Normalize path by stripping leading slash if present.
     * <p>
     * Spring's `/{*path}` pattern captures remaining segments including the leading slash.
     * This method normalizes it to a relative path without leading slash.
     * </p>
     *
     * @param path the raw path from path variable
     * @return normalized path without leading slash, or empty string if path is null/empty
     */
    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        // Strip leading slash if present
        return path.startsWith("/") ? path.substring(1) : path;
    }

    /**
     * Normalize prefix using KVPath validation and normalization.
     * <p>
     * Automatically normalizes the prefix by:
     * - Trimming whitespace
     * - Removing leading slashes
     * - Collapsing multiple slashes
     * - Validating path format
     * </p>
     * <p>
     * Returns empty string for null/empty/blank prefixes (indicating root).
     * </p>
     *
     * @param prefix the raw prefix from query parameter
     * @return normalized prefix, or empty string if prefix is null/empty/blank
     * @throws IllegalArgumentException if prefix contains invalid characters or path traversal
     */
    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        try {
            KVPath path = KVPath.of(prefix);
            return path.isEmpty() ? "" : path.value();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid prefix format: {}, error: {}", prefix, e.getMessage());
            throw e;
        }
    }

    @GetMapping("/view")
    @Operation(
            summary = "View prefix as structured document",
            description = "Returns a read-only view of all keys under a prefix in JSON, YAML, or Properties format.",
            security = {
                    @SecurityRequirement(name = "oauth2_auth_code"),
                    @SecurityRequirement(name = "oauth2_password")
            },
            operationId = "viewKVPrefix"
    )
    public ResponseEntity<String> viewPrefix(
            @Parameter(description = "Application service ID", example = "sample-service") @PathVariable String serviceId,
            @Parameter(description = "Prefix to view (relative to service root)", example = "config/") @RequestParam(required = false, defaultValue = "") String prefix,
            @Parameter(description = "Output format: json, yaml, or properties", example = "json") @RequestParam(defaultValue = "json") String format,
            @Parameter(description = "Use consistent read") @RequestParam(defaultValue = "false") boolean consistent,
            @Parameter(description = "Use stale read") @RequestParam(defaultValue = "false") boolean stale,
            @AuthenticationPrincipal Jwt jwt) {

        String normalizedPrefix = normalizePrefix(prefix);
        // Extract UserContext from SecurityContext (handles both JWT and API key authentication)
        UserContext userContext = UserContextExtractor.extract();
        KVStorePort.KVReadOptions options = KVStorePort.KVReadOptions.builder()
                .consistent(consistent)
                .stale(stale)
                .build();

        KVTypeCodec.StructuredFormat structuredFormat = parseFormat(format);
        return kvService.view(serviceId, normalizedPrefix, structuredFormat, options, userContext)
                .map(bytes -> ResponseEntity.ok()
                        .contentType(mediaTypeFor(structuredFormat))
                        .body(new String(bytes, StandardCharsets.UTF_8)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private KVTypeCodec.StructuredFormat parseFormat(String format) {
        if (format == null) {
            return KVTypeCodec.StructuredFormat.JSON;
        }
        return switch (format.toLowerCase()) {
            case "yaml", "yml" -> KVTypeCodec.StructuredFormat.YAML;
            case "properties", "props", "prop" -> KVTypeCodec.StructuredFormat.PROPERTIES;
            default -> KVTypeCodec.StructuredFormat.JSON;
        };
    }

    private MediaType mediaTypeFor(KVTypeCodec.StructuredFormat format) {
        return switch (format) {
            case JSON -> MediaType.APPLICATION_JSON;
            case YAML -> MediaType.valueOf("application/x-yaml");
            case PROPERTIES -> MediaType.TEXT_PLAIN;
        };
    }
}

