package com.example.control.api.http.dto.kv;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Base64;
import java.util.List;

/**
 * DTOs for KV API operations.
 * <p>
 * Provides request/response DTOs for KV CRUD operations with proper validation
 * and encoding support (base64, utf8, raw).
 * </p>
 */
@Schema(name = "KVDtos", description = "DTOs for KV API operations")
public class KVDtos {

    /**
     * Request DTO for PUT operation.
     */
    @Schema(name = "KVPutRequest", description = "Request to create or update a KV entry")
    public record PutRequest(
            @NotNull(message = "Value is required")
            @Schema(description = "Value as string (will be encoded based on encoding field)", 
                    example = "postgres://user:pass@db/prod", required = true)
            String value,

            @Schema(description = "Value encoding: 'base64', 'utf8', or 'raw' (default: utf8)", 
                    example = "utf8", allowableValues = {"base64", "utf8", "raw"})
            String encoding,

            @Schema(description = "CAS (Compare-And-Set) modify index for conditional update", 
                    example = "12345")
            Long cas,

            @Min(value = 0, message = "Flags must be non-negative")
            @Schema(description = "Flags (arbitrary uint64 metadata)", example = "0")
            Long flags
    ) {
        /**
         * Convert value to bytes based on encoding.
         *
         * @return value as bytes
         */
        public byte[] valueBytes() {
            if (value == null) {
                return new byte[0];
            }

            String enc = encoding == null || encoding.isBlank() ? "utf8" : encoding.toLowerCase();
            return switch (enc) {
                case "base64" -> Base64.getDecoder().decode(value);
                case "raw" -> value.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
                default -> value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            };
        }
    }

    /**
     * Response DTO for KV entry (metadata format).
     */
    @Schema(name = "KVEntryResponse", description = "KV entry with metadata")
    public record EntryResponse(
            @JsonProperty("path")
            @Schema(description = "Relative path of the key", example = "config/db.url")
            String path,

            @JsonProperty("valueBase64")
            @Schema(description = "Value as base64-encoded string", example = "cG9zdGdyZXM6Ly8uLi4=")
            String valueBase64,

            @JsonProperty("modifyIndex")
            @Schema(description = "Modify index for CAS operations", example = "12345")
            Long modifyIndex,

            @JsonProperty("createIndex")
            @Schema(description = "Create index (when key was first created)", example = "100")
            Long createIndex,

            @JsonProperty("flags")
            @Schema(description = "Flags (arbitrary metadata)", example = "0")
            Long flags
    ) {
    }

    /**
     * Response DTO for list operation (entries).
     */
    @Schema(name = "KVListResponse", description = "List of KV entries")
    public record ListResponse(
            @JsonProperty("items")
            @Schema(description = "List of KV entries")
            List<EntryResponse> items
    ) {
    }

    /**
     * Response DTO for list operation (keys only).
     */
    @Schema(name = "KVKeysResponse", description = "List of KV keys")
    public record KeysResponse(
            @JsonProperty("keys")
            @Schema(description = "List of key paths (relative to service root)")
            List<String> keys
    ) {
    }

    /**
     * Response DTO for write operation result.
     */
    @Schema(name = "KVWriteResponse", description = "Result of PUT operation")
    public record WriteResponse(
            @JsonProperty("success")
            @Schema(description = "Whether the operation succeeded", example = "true")
            boolean success,

            @JsonProperty("modifyIndex")
            @Schema(description = "New modify index after the operation", example = "12346")
            Long modifyIndex
    ) {
    }

    /**
     * Response DTO for delete operation result.
     */
    @Schema(name = "KVDeleteResponse", description = "Result of DELETE operation")
    public record DeleteResponse(
            @JsonProperty("success")
            @Schema(description = "Whether the operation succeeded", example = "true")
            boolean success
    ) {
    }
}

