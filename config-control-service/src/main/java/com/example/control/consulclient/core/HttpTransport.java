package com.example.control.consulclient.core;

import com.example.control.consulclient.exception.ConsulException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP transport layer for Consul API calls using Spring RestClient.
 * Handles metadata extraction and error mapping.
 */
@Slf4j
@RequiredArgsConstructor
public class HttpTransport {
    
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ConsulConfig config;
    
    /**
     * Perform a GET request.
     * 
     * @param path the API path
     * @param queryOptions query options
     * @param responseType the response type
     * @return ConsulResponse with body and metadata
     */
    public <T> ConsulResponse<T> get(String path, QueryOptions queryOptions, Class<T> responseType) {
        return executeRequest(() -> {
            RestClient.RequestHeadersSpec<?> request = restClient.get()
                .uri(buildUri(path, queryOptions));
            
            addHeaders(request, queryOptions);
            
            return request.retrieve().toEntity(responseType);
        });
    }
    
    /**
     * Perform a GET request with generic type reference.
     * 
     * @param path the API path
     * @param queryOptions query options
     * @param typeRef the response type reference
     * @return ConsulResponse with body and metadata
     */
    public <T> ConsulResponse<T> get(String path, QueryOptions queryOptions, TypeReference<T> typeRef) {
        return executeRequest(() -> {
            RestClient.RequestHeadersSpec<?> request = restClient.get()
                .uri(buildUri(path, queryOptions));
            
            addHeaders(request, queryOptions);
            
            String jsonResponse = request.retrieve().body(String.class);
            T body = objectMapper.readValue(jsonResponse, typeRef);
            
            return new org.springframework.http.ResponseEntity<>(body, HttpStatus.OK);
        });
    }
    
    /**
     * Perform a PUT request.
     * 
     * @param path the API path
     * @param body the request body
     * @param writeOptions write options
     * @param responseType the response type
     * @return ConsulResponse with body and metadata
     */
    public <T> ConsulResponse<T> put(String path, Object body, WriteOptions writeOptions, Class<T> responseType) {
        return executeRequest(() -> {
            RestClient.RequestBodySpec request = restClient.put()
                .uri(buildUri(path, writeOptions))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
            
            addHeaders(request, writeOptions);
            
            return request.retrieve().toEntity(responseType);
        });
    }
    
    /**
     * Perform a POST request.
     * 
     * @param path the API path
     * @param body the request body
     * @param writeOptions write options
     * @param responseType the response type
     * @return ConsulResponse with body and metadata
     */
    public <T> ConsulResponse<T> post(String path, Object body, WriteOptions writeOptions, Class<T> responseType) {
        return executeRequest(() -> {
            RestClient.RequestBodySpec request = restClient.post()
                .uri(buildUri(path, writeOptions))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
            
            addHeaders(request, writeOptions);
            
            return request.retrieve().toEntity(responseType);
        });
    }
    
    /**
     * Perform a DELETE request.
     * 
     * @param path the API path
     * @param writeOptions write options
     * @param responseType the response type
     * @return ConsulResponse with body and metadata
     */
    public <T> ConsulResponse<T> delete(String path, WriteOptions writeOptions, Class<T> responseType) {
        return executeRequest(() -> {
            RestClient.RequestHeadersSpec<?> request = restClient.delete()
                .uri(buildUri(path, writeOptions));
            
            addHeaders(request, writeOptions);
            
            return request.retrieve().toEntity(responseType);
        });
    }
    
    /**
     * Execute a request and extract metadata from response.
     */
    private <T> ConsulResponse<T> executeRequest(RequestExecutor<T> executor) {
        try {
            org.springframework.http.ResponseEntity<T> response = executor.execute();
            
            Map<String, String> headers = new HashMap<>();
            response.getHeaders().forEach((name, values) -> {
                if (values != null && !values.isEmpty()) {
                    headers.put(name, values.get(0));
                }
            });
            
            return ConsulResponse.<T>builder()
                .body(response.getBody())
                .consulIndex(parseLongHeader(headers, "X-Consul-Index"))
                .knownLeader(parseBooleanHeader(headers, "X-Consul-KnownLeader"))
                .lastContact(parseDurationHeader(headers, "X-Consul-LastContact"))
                .headers(headers)
                .build();
                
        } catch (Exception e) {
            log.error("Request failed", e);
            throw new ConsulException("Request failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Build URI with query parameters.
     */
    private String buildUri(String path, QueryOptions options) {
        if (options == null) {
            return config.getConsulUrl() + path;
        }
        
        String queryString = options.buildQueryString();
        if (queryString.isEmpty()) {
            return config.getConsulUrl() + path;
        }
        
        return config.getConsulUrl() + path + "?" + queryString;
    }
    
    /**
     * Build URI with query parameters for write operations.
     */
    private String buildUri(String path, WriteOptions options) {
        if (options == null) {
            return config.getConsulUrl() + path;
        }
        
        String queryString = options.buildQueryString();
        if (queryString.isEmpty()) {
            return config.getConsulUrl() + path;
        }
        
        return config.getConsulUrl() + path + "?" + queryString;
    }
    
    /**
     * Add common headers to request.
     */
    private void addHeaders(RestClient.RequestHeadersSpec<?> request, QueryOptions options) {
        if (options != null && options.getToken() != null && !options.getToken().isEmpty()) {
            request.header("X-Consul-Token", options.getToken());
        } else if (config.getToken() != null && !config.getToken().isEmpty()) {
            request.header("X-Consul-Token", config.getToken());
        }
    }
    
    /**
     * Add common headers to request for write operations.
     */
    private void addHeaders(RestClient.RequestHeadersSpec<?> request, WriteOptions options) {
        if (options != null && options.getToken() != null && !options.getToken().isEmpty()) {
            request.header("X-Consul-Token", options.getToken());
        } else if (config.getToken() != null && !config.getToken().isEmpty()) {
            request.header("X-Consul-Token", config.getToken());
        }
    }
    
    /**
     * Parse long header value.
     */
    private Long parseLongHeader(Map<String, String> headers, String headerName) {
        String value = headers.get(headerName);
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse {} header: {}", headerName, value);
            return null;
        }
    }
    
    /**
     * Parse boolean header value.
     */
    private Boolean parseBooleanHeader(Map<String, String> headers, String headerName) {
        String value = headers.get(headerName);
        if (value == null || value.isEmpty()) {
            return null;
        }
        return Boolean.parseBoolean(value);
    }
    
    /**
     * Parse duration header value (in milliseconds).
     */
    private Duration parseDurationHeader(Map<String, String> headers, String headerName) {
        String value = headers.get(headerName);
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            long millis = Long.parseLong(value);
            return Duration.ofMillis(millis);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse {} header: {}", headerName, value);
            return null;
        }
    }
    
    /**
     * Functional interface for request execution.
     */
    @FunctionalInterface
    private interface RequestExecutor<T> {
        org.springframework.http.ResponseEntity<T> execute() throws Exception;
    }
}
