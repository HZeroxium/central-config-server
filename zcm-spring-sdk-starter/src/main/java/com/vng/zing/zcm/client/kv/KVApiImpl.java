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
      String url = buildGetUrl(serviceId, key, false);
      var requestBuilder = restClient.get()
          .uri(url)
          .accept(MediaType.APPLICATION_JSON);
      addAuthHeaders(requestBuilder);
      KVResponseDtos.EntryResponse response = requestBuilder
          .retrieve()
          .onStatus(status -> status.value() == 404, (req, res) -> {
            // Handle 404 gracefully - don't throw
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
      String url = buildGetUrl(serviceId, key, true);
      var requestBuilder = restClient.get()
          .uri(url)
          .accept(MediaType.APPLICATION_OCTET_STREAM);
      addAuthHeaders(requestBuilder);
      byte[] response = requestBuilder
          .retrieve()
          .onStatus(status -> status.value() == 404, (req, res) -> {
            // Handle 404 gracefully - don't throw
          })
          .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
            handleClientError(res.getStatusCode(), req.getURI().toString());
          })
          .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
            throw new KVServerException("KV server error: " + res.getStatusCode());
          })
          .body(byte[].class);

      return response;
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
      KVResponseDtos.ListResponseV2 response = requestBuilder
          .retrieve()
          .onStatus(status -> status.value() == 404, (req, res) -> {
            // Handle 404 gracefully - don't throw
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
      KVResponseDtos.ListResponse response = requestBuilder
          .retrieve()
          .onStatus(status -> status.value() == 404, (req, res) -> {
            // Handle 404 gracefully - don't throw
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
      KVResponseDtos.KeysResponse response = requestBuilder
          .retrieve()
          .onStatus(status -> status.value() == 404, (req, res) -> {
            // Handle 404 gracefully - don't throw
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
      String url = buildStructuredUrl(serviceId, prefix, "view") + "?format=" + format.name().toLowerCase();
      var requestBuilder = restClient.get()
          .uri(url)
          .accept(MediaType.ALL);
      addAuthHeaders(requestBuilder);
      String response = requestBuilder
          .retrieve()
          .onStatus(status -> status.value() == 404, (req, res) -> {
            // Handle 404 gracefully - don't throw
          })
          .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> handleClientError(res.getStatusCode(), req.getURI().toString()))
          .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
            throw new KVServerException("KV server error: " + res.getStatusCode());
          })
          .body(String.class);
      return response;
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
      String url = buildGetUrl(serviceId, key, false);
      var requestBuilder = restClient.get()
          .uri(url)
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
      // Fall back to JWT token
      requestBuilder.header("Authorization", "Bearer " + tokenProvider.getAccessToken());
      log.debug("Using JWT token for KV authentication");
    }
    return requestBuilder;
  }

  /**
   * Builds URL for GET operation.
   */
  private String buildGetUrl(String serviceId, String key, boolean raw) {
    String baseUrl = sdkProperties.getControlUrl();
    if (!StringUtils.hasText(baseUrl)) {
      throw new IllegalStateException("Control URL not configured. Set zcm.sdk.control.url");
    }

    // Normalize key: remove leading slash if present
    String normalizedKey = key.startsWith("/") ? key.substring(1) : key;

    String url = baseUrl + "/api/application-services/" + serviceId + "/kv/" + normalizedKey;
    if (raw) {
      url += "?raw=true";
    }
    return url;
  }

  /**
   * Builds URL for LIST operation.
   */
  private String buildListUrl(String serviceId, String prefix, boolean keysOnly) {
    String baseUrl = sdkProperties.getControlUrl();
    if (!StringUtils.hasText(baseUrl)) {
      throw new IllegalStateException("Control URL not configured. Set zcm.sdk.control.url");
    }

    StringBuilder url = new StringBuilder(baseUrl)
        .append("/api/application-services/")
        .append(serviceId)
        .append("/kv");

    List<String> params = new ArrayList<>();
    if (StringUtils.hasText(prefix)) {
      params.add("prefix=" + prefix);
    }
    if (keysOnly) {
      params.add("keysOnly=true");
    }
    params.add("recurse=true");

    if (!params.isEmpty()) {
      url.append("?").append(String.join("&", params));
    }

    return url.toString();
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
  
  private String buildStructuredUrl(String serviceId, String prefix, String suffix) {
    String baseUrl = sdkProperties.getControlUrl();
    if (!StringUtils.hasText(baseUrl)) {
      throw new IllegalStateException("Control URL not configured. Set zcm.sdk.control.url");
    }
    String normalized = normalizePrefix(prefix);
    StringBuilder sb = new StringBuilder(baseUrl)
        .append("/api/application-services/")
        .append(serviceId)
        .append("/kv");
    if (StringUtils.hasText(normalized)) {
      sb.append("/").append(normalized);
    }
    sb.append("/").append(suffix);
    return sb.toString();
  }

  private String normalizePrefix(String prefix) {
    if (!StringUtils.hasText(prefix)) {
      return "";
    }
    return prefix.startsWith("/") ? prefix.substring(1) : prefix;
  }
}
