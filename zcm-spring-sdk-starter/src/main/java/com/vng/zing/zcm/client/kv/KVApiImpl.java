package com.vng.zing.zcm.client.kv;

import com.vng.zing.zcm.config.SdkProperties;
import com.vng.zing.zcm.kv.KVStructuredFormat;
import com.vng.zing.zcm.kv.KVTokenProvider;
import com.vng.zing.zcm.kv.dto.*;
import com.vng.zing.zcm.kv.exceptions.KVAccessDeniedException;
import com.vng.zing.zcm.kv.exceptions.KVAuthenticationException;
import com.vng.zing.zcm.kv.exceptions.KVClientException;
import com.vng.zing.zcm.kv.exceptions.KVServerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of KVApi using RestClient to call config-control-service KV endpoints.
 */
@Slf4j
@RequiredArgsConstructor
public class KVApiImpl implements KVApi {

  private final RestClient restClient;
  private final KVTokenProvider tokenProvider;
  private final SdkProperties sdkProperties;

  @Override
  public String getString(String serviceId, String key) {
    try {
      URI uri = buildGetUri(serviceId, key, false);
      log.debug("Getting KV string for service: {}, key: {}, URI: {}", serviceId, key, uri);
      
      var requestBuilder = restClient.get()
          .uri(uri)
          .accept(MediaType.APPLICATION_JSON);
      addAuthHeaders(requestBuilder);
      
      try {
        KVResponseDtos.EntryResponse response = requestBuilder
            .retrieve()
            .onStatus(status -> status.value() == 404, (req, res) -> {
              log.debug("KV entry not found (404) for service: {}, key: {}", serviceId, key);
              // Return null by throwing a special exception that we'll catch
              throw new HttpClientErrorException(res.getStatusCode(), "KV entry not found");
            })
            .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
              handleClientError(res.getStatusCode(), req.getURI().toString());
            })
            .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
              throw new KVServerException("KV server error: " + res.getStatusCode());
            })
            .body(KVResponseDtos.EntryResponse.class);

        if (response == null) {
          return null;
        }

        KVEntry entry = toKVEntry(response);
        return entry.getValueAsString();
      } catch (HttpClientErrorException e) {
        if (e.getStatusCode().value() == 404) {
          log.debug("KV entry not found for service: {}, key: {}", serviceId, key);
          return null;
        }
        throw e;
      }
    } catch (KVAuthenticationException | KVAccessDeniedException | KVServerException e) {
      throw e;
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 404) {
        log.debug("KV entry not found for service: {}, key: {}", serviceId, key);
        return null;
      }
      throw new KVClientException("KV client error: " + e.getMessage(), e);
    } catch (HttpServerErrorException e) {
      log.error("KV server error for service: {}, key: {}", serviceId, key, e);
      throw new KVServerException("KV server error: " + e.getStatusCode(), e);
    } catch (Exception e) {
      log.error("Error getting KV entry for service: {}, key: {}", serviceId, key, e);
      throw new KVClientException("Failed to get KV entry: " + e.getMessage(), e);
    }
  }

  @Override
  public Integer getInteger(String serviceId, String key) {
    String value = getString(serviceId, key);
    if (value == null || value.isBlank()) {
      return null;
    }

    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      log.warn("Failed to parse integer value for service: {}, key: {}, value: {}", serviceId, key, value, e);
      return null;
    }
  }

  @Override
  public Long getLong(String serviceId, String key) {
    String value = getString(serviceId, key);
    if (value == null || value.isBlank()) {
      return null;
    }

    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException e) {
      log.warn("Failed to parse long value for service: {}, key: {}, value: {}", serviceId, key, value, e);
      return null;
    }
  }

  @Override
  public Boolean getBoolean(String serviceId, String key) {
    String value = getString(serviceId, key);
    if (value == null || value.isBlank()) {
      return null;
    }

    String trimmed = value.trim().toLowerCase();
    if ("true".equals(trimmed) || "1".equals(trimmed) || "yes".equals(trimmed)) {
      return true;
    } else if ("false".equals(trimmed) || "0".equals(trimmed) || "no".equals(trimmed)) {
      return false;
    } else {
      log.warn("Failed to parse boolean value for service: {}, key: {}, value: {}", serviceId, key, value);
      return null;
    }
  }

  @Override
  public Double getDouble(String serviceId, String key) {
    String value = getString(serviceId, key);
    if (value == null || value.isBlank()) {
      return null;
    }

    try {
      return Double.parseDouble(value.trim());
    } catch (NumberFormatException e) {
      log.warn("Failed to parse double value for service: {}, key: {}, value: {}", serviceId, key, value, e);
      return null;
    }
  }

  @Override
  public byte[] getBytes(String serviceId, String key) {
    try {
      URI uri = buildGetUri(serviceId, key, true);
      log.debug("Getting KV bytes for service: {}, key: {}, URI: {}", serviceId, key, uri);
      
      var requestBuilder = restClient.get()
          .uri(uri)
          .accept(MediaType.APPLICATION_OCTET_STREAM);
      addAuthHeaders(requestBuilder);
      
      try {
        byte[] response = requestBuilder
            .retrieve()
            .onStatus(status -> status.value() == 404, (req, res) -> {
              log.debug("KV entry not found (404) for service: {}, key: {}", serviceId, key);
              throw new HttpClientErrorException(res.getStatusCode(), "KV entry not found");
            })
            .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
              handleClientError(res.getStatusCode(), req.getURI().toString());
            })
            .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
              throw new KVServerException("KV server error: " + res.getStatusCode());
            })
            .body(byte[].class);

        return response;
      } catch (HttpClientErrorException e) {
        if (e.getStatusCode().value() == 404) {
          log.debug("KV entry not found for service: {}, key: {}", serviceId, key);
          return null;
        }
        throw e;
      }
    } catch (KVAuthenticationException | KVAccessDeniedException | KVServerException e) {
      throw e;
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 404) {
        log.debug("KV entry not found for service: {}, key: {}", serviceId, key);
        return null;
      }
      throw new KVClientException("KV client error: " + e.getMessage(), e);
    } catch (HttpServerErrorException e) {
      log.error("KV server error for service: {}, key: {}", serviceId, key, e);
      throw new KVServerException("KV server error: " + e.getStatusCode(), e);
    } catch (Exception e) {
      log.error("Error getting raw KV entry for service: {}, key: {}", serviceId, key, e);
      throw new KVClientException("Failed to get raw KV entry: " + e.getMessage(), e);
    }
  }

  @Override
  public List<String> getList(String serviceId, String key) {
    // First try to get as comma-separated string
    String value = getString(serviceId, key);
    if (value != null && !value.isBlank()) {
      try {
        // Parse comma-separated string with whitespace trimming
        List<String> elements = new ArrayList<>();
        String[] parts = value.split(",");
        for (String part : parts) {
          String trimmed = part.trim();
          if (!trimmed.isEmpty()) {
            elements.add(trimmed);
          }
        }
        return elements;
      } catch (Exception e) {
        log.warn("Failed to parse comma-separated list for service: {}, key: {}", serviceId, key, e);
      }
    }

    // If that fails, try as structured list
    try {
      List<Map<String, Object>> structuredList = getStructuredList(serviceId, key);
      if (!structuredList.isEmpty()) {
        // Convert structured list to list of strings by extracting values
        List<String> result = new ArrayList<>();
        for (Map<String, Object> item : structuredList) {
          // Try to convert item to string representation
          if (item.size() == 1 && item.values().iterator().next() instanceof String) {
            result.add((String) item.values().iterator().next());
          } else {
            // If item has multiple fields, convert to JSON-like string
            result.add(item.toString());
          }
        }
        return result;
      }
    } catch (Exception e) {
      log.debug("Not a structured list for service: {}, key: {}", serviceId, key);
    }

    return new ArrayList<>();
  }

  @Override
  public List<Map<String, Object>> getStructuredList(String serviceId, String prefix) {
    try {
      String url = buildStructuredUrl(serviceId, prefix, "list");
      var requestBuilder = restClient.get()
          .uri(url)
          .accept(MediaType.APPLICATION_JSON);
      addAuthHeaders(requestBuilder);
      try {
        KVResponseDtos.ListResponseV2 response = requestBuilder
            .retrieve()
            .onStatus(status -> status.value() == 404, (req, res) -> {
              log.debug("KV structured list not found (404) for service: {}, prefix: {}", serviceId, prefix);
              throw new HttpClientErrorException(res.getStatusCode(), "KV structured list not found");
            })
            .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> handleClientError(res.getStatusCode(), req.getURI().toString()))
            .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
              throw new KVServerException("KV server error: " + res.getStatusCode());
            })
            .body(KVResponseDtos.ListResponseV2.class);

        if (response == null || response.items() == null) {
          return new ArrayList<>();
        }

        // Extract items.data and return as List<Map<String, Object>>
        // Ignore manifest as per requirements
        List<Map<String, Object>> result = new ArrayList<>();
        for (KVResponseDtos.ListResponseV2.ListItem item : response.items()) {
          if (item.data() != null) {
            result.add(new LinkedHashMap<>(item.data()));
          } else {
            result.add(new LinkedHashMap<>());
          }
        }
        return result;
      } catch (HttpClientErrorException e) {
        if (e.getStatusCode().value() == 404) {
          log.debug("KV structured list not found for service: {}, prefix: {}", serviceId, prefix);
          return new ArrayList<>();
        }
        throw e;
      }
    } catch (KVAuthenticationException | KVAccessDeniedException | KVServerException e) {
      throw e;
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 404) {
        log.debug("KV structured list not found for service: {}, prefix: {}", serviceId, prefix);
        return new ArrayList<>();
      }
      throw new KVClientException("KV client error: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("Error getting structured list for service: {}, prefix: {}", serviceId, prefix, e);
      throw new KVClientException("Failed to get structured list: " + e.getMessage(), e);
    }
  }

  @Override
  public Map<String, Object> getMap(String serviceId, String prefix) {
    try {
      String url = buildListUrl(serviceId, prefix, false);
      var requestBuilder = restClient.get()
          .uri(url)
          .accept(MediaType.APPLICATION_JSON);
      addAuthHeaders(requestBuilder);
      try {
        KVResponseDtos.ListResponse response = requestBuilder
            .retrieve()
            .onStatus(status -> status.value() == 404, (req, res) -> {
              log.debug("KV entries not found (404) for service: {}, prefix: {}", serviceId, prefix);
              throw new HttpClientErrorException(res.getStatusCode(), "KV entries not found");
            })
            .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
              handleClientError(res.getStatusCode(), req.getURI().toString());
            })
            .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
              throw new KVServerException("KV server error: " + res.getStatusCode());
            })
            .body(KVResponseDtos.ListResponse.class);

        if (response == null || response.items() == null) {
          return new HashMap<>();
        }

        // Convert list of entries to flat map
        Map<String, Object> map = new LinkedHashMap<>();
        for (KVResponseDtos.EntryResponse entry : response.items()) {
          KVEntry kvEntry = toKVEntry(entry);
          String path = kvEntry.path();
          // Remove prefix from path if present
          String key = path;
          if (StringUtils.hasText(prefix) && path.startsWith(prefix)) {
            key = path.substring(prefix.length());
            // Remove leading slash if present
            if (key.startsWith("/")) {
              key = key.substring(1);
            }
          }
          map.put(key, kvEntry.getValueAsString());
        }

        return map;
      } catch (HttpClientErrorException e) {
        if (e.getStatusCode().value() == 404) {
          log.debug("KV entries not found for service: {}, prefix: {}", serviceId, prefix);
          return new HashMap<>();
        }
        throw e;
      }
    } catch (KVAuthenticationException | KVAccessDeniedException | KVServerException e) {
      throw e;
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 404) {
        log.debug("KV entries not found for service: {}, prefix: {}", serviceId, prefix);
        return new HashMap<>();
      }
      throw new KVClientException("KV client error: " + e.getMessage(), e);
    } catch (HttpServerErrorException e) {
      log.error("KV server error for service: {}, prefix: {}", serviceId, prefix, e);
      throw new KVServerException("KV server error: " + e.getStatusCode(), e);
    } catch (Exception e) {
      log.error("Error getting KV map for service: {}, prefix: {}", serviceId, prefix, e);
      throw new KVClientException("Failed to get KV map: " + e.getMessage(), e);
    }
  }

  @Override
  public List<String> listKeys(String serviceId, String prefix) {
    try {
      String url = buildListUrl(serviceId, prefix, true);
      var requestBuilder = restClient.get()
          .uri(url)
          .accept(MediaType.APPLICATION_JSON);
      addAuthHeaders(requestBuilder);
      try {
        KVResponseDtos.KeysResponse response = requestBuilder
            .retrieve()
            .onStatus(status -> status.value() == 404, (req, res) -> {
              log.debug("KV keys not found (404) for service: {}, prefix: {}", serviceId, prefix);
              throw new HttpClientErrorException(res.getStatusCode(), "KV keys not found");
            })
            .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
              handleClientError(res.getStatusCode(), req.getURI().toString());
            })
            .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
              throw new KVServerException("KV server error: " + res.getStatusCode());
            })
            .body(KVResponseDtos.KeysResponse.class);

        if (response == null || response.keys() == null) {
          return new ArrayList<>();
        }

        return new ArrayList<>(response.keys());
      } catch (HttpClientErrorException e) {
        if (e.getStatusCode().value() == 404) {
          log.debug("KV keys not found for service: {}, prefix: {}", serviceId, prefix);
          return new ArrayList<>();
        }
        throw e;
      }
    } catch (KVAuthenticationException | KVAccessDeniedException | KVServerException e) {
      throw e;
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 404) {
        log.debug("KV keys not found for service: {}, prefix: {}", serviceId, prefix);
        return new ArrayList<>();
      }
      throw new KVClientException("KV client error: " + e.getMessage(), e);
    } catch (HttpServerErrorException e) {
      log.error("KV server error for service: {}, prefix: {}", serviceId, prefix, e);
      throw new KVServerException("KV server error: " + e.getStatusCode(), e);
    } catch (Exception e) {
      log.error("Error listing KV keys for service: {}, prefix: {}", serviceId, prefix, e);
      throw new KVClientException("Failed to list KV keys: " + e.getMessage(), e);
    }
  }

  @Override
  public String view(String serviceId, String prefix, KVStructuredFormat format) {
    try {
      String baseUrl = sdkProperties.getControlUrl();
      if (!StringUtils.hasText(baseUrl)) {
        throw new IllegalStateException("Control URL not configured. Set zcm.sdk.control.url");
      }
      String normalized = normalizePrefix(prefix);
      
      UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
          .path("/api/application-services/{serviceId}/kv/view")
          .queryParam("format", format.name().toLowerCase());
      
      if (StringUtils.hasText(normalized)) {
        builder.queryParam("prefix", normalized);
      }
      
      String url = builder.buildAndExpand(serviceId)
          .encode(StandardCharsets.UTF_8)
          .toUriString();
      
      var requestBuilder = restClient.get()
          .uri(url)
          .accept(MediaType.ALL);
      addAuthHeaders(requestBuilder);
      try {
        String response = requestBuilder
            .retrieve()
            .onStatus(status -> status.value() == 404, (req, res) -> {
              log.debug("KV view not found (404) for service: {}, prefix: {}", serviceId, prefix);
              throw new HttpClientErrorException(res.getStatusCode(), "KV view not found");
            })
            .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> handleClientError(res.getStatusCode(), req.getURI().toString()))
            .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
              throw new KVServerException("KV server error: " + res.getStatusCode());
            })
            .body(String.class);
        return response;
      } catch (HttpClientErrorException e) {
        if (e.getStatusCode().value() == 404) {
          log.debug("KV view not found for service: {}, prefix: {}", serviceId, prefix);
          return null;
        }
        throw e;
      }
    } catch (KVAuthenticationException | KVAccessDeniedException | KVServerException e) {
      throw e;
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 404) {
        log.debug("KV view not found for service: {}, prefix: {}", serviceId, prefix);
        return null;
      }
      throw new KVClientException("KV client error: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("Error viewing KV prefix for service: {}, prefix: {}", serviceId, prefix, e);
      throw new KVClientException("Failed to view KV prefix: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean exists(String serviceId, String key) {
    try {
      // Try to get the entry - if it doesn't throw a 404, it exists
      URI uri = buildGetUri(serviceId, key, false);
      var requestBuilder = restClient.get()
          .uri(uri)
          .accept(MediaType.APPLICATION_JSON);
      addAuthHeaders(requestBuilder);
      requestBuilder
          .retrieve()
          .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
            // Don't throw for 404 - we'll catch it and return false
            if (res.getStatusCode().value() != 404) {
              handleClientError(res.getStatusCode(), req.getURI().toString());
            }
          })
          .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
            throw new KVServerException("KV server error: " + res.getStatusCode());
          })
          .body(KVResponseDtos.EntryResponse.class);

      // If we get here, the key exists
      return true;
    } catch (KVAuthenticationException | KVAccessDeniedException | KVServerException e) {
      throw e;
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 404) {
        return false;
      }
      // Other 4xx errors should have been handled by onStatus, but re-throw if not
      throw new KVClientException("KV client error: " + e.getMessage(), e);
    } catch (Exception e) {
      // Check if the underlying exception is a 404
      Throwable cause = e.getCause();
      if (cause instanceof HttpClientErrorException httpError) {
        if (httpError.getStatusCode().value() == 404) {
          return false;
        }
      }
      log.error("Error checking if KV entry exists for service: {}, key: {}", serviceId, key, e);
      throw new KVClientException("Failed to check if KV entry exists: " + e.getMessage(), e);
    }
  }

  /**
   * Adds authentication headers (API key or JWT) to a RestClient request builder.
   * <p>
   * API key takes precedence if configured and enabled. Otherwise, uses JWT token.
   * </p>
   *
   * @param requestBuilder the RestClient request builder
   * @return the request builder with authentication headers added
   */
  private <T extends RestClient.RequestHeadersSpec<?>> T addAuthHeaders(T requestBuilder) {
    // Prefer API key if configured and enabled
    if (sdkProperties != null 
        && sdkProperties.getApiKey() != null 
        && sdkProperties.getApiKey().isEnabled()
        && StringUtils.hasText(sdkProperties.getApiKey().getKey())) {
      requestBuilder.header("X-API-Key", sdkProperties.getApiKey().getKey());
      log.debug("Using API key for KV authentication");
    } else if (tokenProvider != null) {
      try {
        // Fall back to JWT token
        String token = tokenProvider.getAccessToken();
        requestBuilder.header("Authorization", "Bearer " + token);
        log.debug("Using JWT token for KV authentication (token length: {})", token != null ? token.length() : 0);
      } catch (Exception e) {
        log.error("Failed to get access token for KV authentication", e);
        throw new KVAuthenticationException("Failed to get access token: " + e.getMessage(), e);
      }
    } else {
      log.warn("No authentication method configured for KV API - request may fail");
    }
    return requestBuilder;
  }

  /**
   * Builds URL for GET operation.
   * <p>
   * Uses UriComponentsBuilder to properly encode the path, ensuring that
   * keys with special characters (like slashes, dots) are correctly handled.
   * <p>
   * The config-control-service uses pattern {@code /{*path}} which captures
   * all path segments after {@code /kv/} into a single path variable.
   * The key may contain slashes (e.g., "config/api.endpoint"), which need to be
   * properly encoded in the URL but will be decoded by Spring when matching the pattern.
   * </p>
   * <p>
   * Important: We use {@code buildAndExpand()} first to expand path variables,
   * then encode the entire URI. This ensures that slashes in the key are properly
   * encoded as %2F, which Spring will decode back to / when matching the pattern.
   * </p>
   */
  private URI buildGetUri(String serviceId, String key, boolean raw) {
    String baseUrl = sdkProperties.getControlUrl();
    if (!StringUtils.hasText(baseUrl)) {
      throw new IllegalStateException("Control URL not configured. Set zcm.sdk.control.url");
    }

    // Normalize key: remove leading slash if present
    String normalizedKey = key.startsWith("/") ? key.substring(1) : key;

    // Build URI: expand path variables first, then encode
    // This ensures that slashes in the key are properly encoded as %2F
    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
        .path("/api/application-services/{serviceId}/kv/{key}");

    if (raw) {
      builder.queryParam("raw", "true");
    }

    // Expand path variables first, then encode the entire URI
    URI uri = builder.buildAndExpand(serviceId, normalizedKey)
        .encode(StandardCharsets.UTF_8)
        .toUri();
    log.debug("Built KV GET URI: {} (from key: {})", uri, key);
    return uri;
  }

  /**
   * Builds URL for LIST operation.
   * <p>
   * Uses UriComponentsBuilder to properly encode query parameters.
   * </p>
   */
  private String buildListUrl(String serviceId, String prefix, boolean keysOnly) {
    String baseUrl = sdkProperties.getControlUrl();
    if (!StringUtils.hasText(baseUrl)) {
      throw new IllegalStateException("Control URL not configured. Set zcm.sdk.control.url");
    }

    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
        .path("/api/application-services/{serviceId}/kv")
        .queryParam("recurse", "true");

    if (StringUtils.hasText(prefix)) {
      builder.queryParam("prefix", prefix);
    }
    if (keysOnly) {
      builder.queryParam("keysOnly", "true");
    }

    return builder.buildAndExpand(serviceId)
        .encode(StandardCharsets.UTF_8)
        .toUriString();
  }

  /**
   * Converts EntryResponse DTO to KVEntry.
   */
  private KVEntry toKVEntry(KVResponseDtos.EntryResponse response) {
    return new KVEntry(
        response.path(),
        response.valueBase64(),
        response.modifyIndex(),
        response.createIndex(),
        response.flags()
    );
  }

  /**
   * Handles client errors (4xx) for KV operations.
   */
  private void handleClientError(HttpStatusCode status, String url) {
    int statusCode = status.value();
    if (statusCode == 401) {
      log.error("KV authentication failed: {}", url);
      throw new KVAuthenticationException("KV authentication failed (401)");
    } else if (statusCode == 403) {
      log.error("KV access denied: {}", url);
      throw new KVAccessDeniedException("KV access denied (403)");
    } else {
      log.error("KV client error ({}): {}", statusCode, url);
      throw new KVClientException("KV client error (" + statusCode + ")");
    }
  }
  
  /**
   * Builds URL for structured operations (list, view).
   * <p>
   * Uses UriComponentsBuilder to properly encode path and query parameters.
   * The prefix is passed as a query parameter, not a path variable.
   * </p>
   */
  private String buildStructuredUrl(String serviceId, String prefix, String suffix) {
    String baseUrl = sdkProperties.getControlUrl();
    if (!StringUtils.hasText(baseUrl)) {
      throw new IllegalStateException("Control URL not configured. Set zcm.sdk.control.url");
    }
    String normalized = normalizePrefix(prefix);
    
    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
        .path("/api/application-services/{serviceId}/kv/{suffix}");
    
    // Prefix is a query parameter, not a path variable
    if (StringUtils.hasText(normalized)) {
      builder.queryParam("prefix", normalized);
    }
    
    return builder.buildAndExpand(serviceId, suffix)
        .encode(StandardCharsets.UTF_8)
        .toUriString();
  }

  private String normalizePrefix(String prefix) {
    if (!StringUtils.hasText(prefix)) {
      return "";
    }
    return prefix.startsWith("/") ? prefix.substring(1) : prefix;
  }
}
