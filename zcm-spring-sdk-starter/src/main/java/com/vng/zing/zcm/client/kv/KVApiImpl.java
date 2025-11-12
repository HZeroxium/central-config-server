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
import java.util.List;
import java.util.Optional;

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
  public Optional<KVEntry> get(String serviceId, String key) {
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

      return Optional.ofNullable(response).map(this::toKVEntry);
    } catch (KVAuthenticationException | KVAccessDeniedException | KVServerException e) {
      throw e;
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 404) {
        log.debug("KV entry not found for service: {}, key: {}", serviceId, key);
        return Optional.empty();
      }
      // Other 4xx errors should have been handled by onStatus, but catch here as fallback
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
  public Optional<byte[]> getRaw(String serviceId, String key) {
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

      return Optional.ofNullable(response);
    } catch (KVAuthenticationException | KVAccessDeniedException | KVServerException e) {
      throw e;
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 404) {
        log.debug("KV entry not found for service: {}, key: {}", serviceId, key);
        return Optional.empty();
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
  public Optional<String> getString(String serviceId, String key) {
    Optional<KVEntry> entry = get(serviceId, key);
    return entry.map(KVEntry::getValueAsString);
  }

  @Override
  public Optional<List<String>> getLeafList(String serviceId, String key) {
    Optional<String> valueOpt = getString(serviceId, key);
    if (valueOpt.isEmpty()) {
      return Optional.empty();
    }

    String value = valueOpt.get();
    if (value == null || value.isEmpty()) {
      return Optional.of(new ArrayList<>());
    }

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
      return Optional.of(elements);
    } catch (Exception e) {
      log.warn("Failed to parse LEAF_LIST value for service: {}, key: {}, returning empty list", serviceId, key, e);
      return Optional.of(new ArrayList<>());
    }
  }

  @Override
  public List<KVEntry> list(String serviceId, String prefix) {
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
        return new ArrayList<>();
      }

      return response.items().stream()
          .map(this::toKVEntry)
          .toList();
    } catch (KVAuthenticationException | KVAccessDeniedException | KVServerException e) {
      throw e;
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 404) {
        log.debug("KV entries not found for service: {}, prefix: {}", serviceId, prefix);
        return new ArrayList<>();
      }
      throw new KVClientException("KV client error: " + e.getMessage(), e);
    } catch (HttpServerErrorException e) {
      log.error("KV server error for service: {}, prefix: {}", serviceId, prefix, e);
      throw new KVServerException("KV server error: " + e.getStatusCode(), e);
    } catch (Exception e) {
      log.error("Error listing KV entries for service: {}, prefix: {}", serviceId, prefix, e);
      throw new KVClientException("Failed to list KV entries: " + e.getMessage(), e);
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

      return response.keys();
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
  public Optional<KVListResult> getList(String serviceId, String prefix) {
    try {
      String url = buildStructuredUrl(serviceId, prefix, "list");
      var requestBuilder = restClient.get()
          .uri(url)
          .accept(MediaType.APPLICATION_JSON);
      addAuthHeaders(requestBuilder);
      KVResponseDtos.ListResponseV2 response = requestBuilder
          .retrieve()
          .onStatus(status -> status.value() == 404, (req, res) -> {
          })
          .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> handleClientError(res.getStatusCode(), req.getURI().toString()))
          .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
            throw new KVServerException("KV server error: " + res.getStatusCode());
          })
          .body(KVResponseDtos.ListResponseV2.class);
      if (response == null) {
        return Optional.empty();
      }
      KVListManifest manifest = response.manifest() == null
          ? KVListManifest.empty()
          : new KVListManifest(response.manifest().order(), response.manifest().version(), response.manifest().etag(), response.manifest().metadata());
      List<KVListResult.KVListItem> items = response.items() == null ? List.of()
          : response.items().stream()
              .map(item -> new KVListResult.KVListItem(item.id(), item.data()))
              .toList();
      return Optional.of(new KVListResult(items, manifest, response.type()));
    } catch (KVAuthenticationException | KVAccessDeniedException | KVServerException e) {
      throw e;
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 404) {
        log.debug("KV list not found for service: {}, prefix: {}", serviceId, prefix);
        return Optional.empty();
      }
      throw new KVClientException("KV client error: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("Error getting KV list for service: {}, prefix: {}", serviceId, prefix, e);
      throw new KVClientException("Failed to get KV list: " + e.getMessage(), e);
    }
  }

  @Override
  public KVTransactionResult putList(String serviceId, String prefix, KVListWriteRequest request) {
    try {
      String url = buildStructuredUrl(serviceId, prefix, "list");
      KVResponseDtos.ListManifest manifestDto = new KVResponseDtos.ListManifest(
          request.manifest().order(),
          request.manifest().version(),
          request.manifest().etag(),
          request.manifest().metadata()
      );
      List<KVResponseDtos.ListResponseV2.ListItem> dtoItems = request.items().stream()
          .map(item -> new KVResponseDtos.ListResponseV2.ListItem(item.id(), item.data()))
          .toList();
      KVRequestDtos.ListWriteRequest payload = new KVRequestDtos.ListWriteRequest(dtoItems, manifestDto, request.deletes());

      var requestBuilder = restClient.put()
          .uri(url)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON);
      addAuthHeaders(requestBuilder);
      KVResponseDtos.TransactionResponse response = requestBuilder
          .body(payload)
          .retrieve()
          .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> handleClientError(res.getStatusCode(), req.getURI().toString()))
          .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
            throw new KVServerException("KV server error: " + res.getStatusCode());
          })
          .body(KVResponseDtos.TransactionResponse.class);
      return toTransactionResult(response);
    } catch (Exception e) {
      throw translateException(e, "put KV list", serviceId, prefix);
    }
  }

  @Override
  public KVTransactionResult executeTransaction(String serviceId, List<KVTransactionOperationRequest> operations) {
    try {
      String url = sdkProperties.getControlUrl()
          + "/api/application-services/" + serviceId + "/kv/txn";
      List<KVRequestDtos.TransactionRequest.TransactionOperation> ops = operations.stream()
          .map(op -> new KVRequestDtos.TransactionRequest.TransactionOperation(
              op.op(), op.path(), op.value(), op.encoding(), op.flags(), op.cas()
          )).toList();
      KVRequestDtos.TransactionRequest payload = new KVRequestDtos.TransactionRequest(ops);

      var requestBuilder = restClient.post()
          .uri(url)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON);
      addAuthHeaders(requestBuilder);
      KVResponseDtos.TransactionResponse response = requestBuilder
          .body(payload)
          .retrieve()
          .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> handleClientError(res.getStatusCode(), req.getURI().toString()))
          .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
            throw new KVServerException("KV server error: " + res.getStatusCode());
          })
          .body(KVResponseDtos.TransactionResponse.class);
      return toTransactionResult(response);
    } catch (Exception e) {
      throw translateException(e, "execute KV transaction", serviceId, null);
    }
  }

  @Override
  public Optional<String> view(String serviceId, String prefix, KVStructuredFormat format) {
    try {
      String url = buildStructuredUrl(serviceId, prefix, "view") + "?format=" + format.name().toLowerCase();
      var requestBuilder = restClient.get()
          .uri(url)
          .accept(MediaType.ALL);
      addAuthHeaders(requestBuilder);
      String response = requestBuilder
          .retrieve()
          .onStatus(status -> status.value() == 404, (req, res) -> {
          })
          .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> handleClientError(res.getStatusCode(), req.getURI().toString()))
          .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
            throw new KVServerException("KV server error: " + res.getStatusCode());
          })
          .body(String.class);
      return Optional.ofNullable(response);
    } catch (KVAuthenticationException | KVAccessDeniedException | KVServerException e) {
      throw e;
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 404) {
        log.debug("KV view not found for service: {}, prefix: {}", serviceId, prefix);
        return Optional.empty();
      }
      throw new KVClientException("KV client error: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("Error viewing KV prefix for service: {}, prefix: {}", serviceId, prefix, e);
      throw new KVClientException("Failed to view KV prefix: " + e.getMessage(), e);
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

  private KVTransactionResult toTransactionResult(KVResponseDtos.TransactionResponse response) {
    if (response == null) {
      return new KVTransactionResult(
          false,
          List.of(),
          "Transaction response was empty"
      );
    }
    List<KVTransactionResult.OperationResult> ops = response.results() == null ? List.of()
        : response.results().stream()
            .map(result -> new KVTransactionResult.OperationResult(
                result.path(),
                result.success(),
                result.modifyIndex(),
                result.message()))
            .toList();
    return new KVTransactionResult(response.success(), ops, response.error());
  }

  private RuntimeException translateException(Exception e, String action, String serviceId, String prefix) {
    if (e instanceof KVClientException
        || e instanceof KVServerException
        || e instanceof KVAuthenticationException
        || e instanceof KVAccessDeniedException) {
      return (RuntimeException) e;
    }
    String msg = String.format("Failed to %s for service %s prefix %s: %s",
        action, serviceId, prefix, e.getMessage());
    log.error(msg, e);
    return new KVClientException(msg, e);
  }
}

