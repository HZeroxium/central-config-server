package com.example.control.infrastructure.adapter.external.keycloak;

import com.example.control.api.http.exception.exceptions.ExternalServiceException;
import com.example.control.domain.criteria.IamTeamCriteria;
import com.example.control.domain.criteria.IamUserCriteria;
import com.example.control.infrastructure.adapter.external.keycloak.dto.KeycloakGroupRepresentation;
import com.example.control.infrastructure.adapter.external.keycloak.dto.KeycloakTokenResponse;
import com.example.control.infrastructure.adapter.external.keycloak.dto.KeycloakUserRepresentation;
import com.example.control.infrastructure.config.keycloak.KeycloakAdminProperties;
import com.example.control.infrastructure.resilience.ResilienceDecoratorsFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * REST client for Keycloak Admin API.
 * <p>
 * Provides methods to query users and groups from Keycloak using OAuth2 client credentials flow.
 * All calls are protected with Circuit Breaker, Retry, Bulkhead patterns via Resilience4j.
 * </p>
 */
@Slf4j
@Service
public class KeycloakAdminRestService {

    private static final String SERVICE_NAME = "keycloak";
    private static final int TOKEN_REFRESH_BUFFER_SECONDS = 60; // Refresh token 60s before expiry

    private final KeycloakAdminProperties properties;
    private final ResilienceDecoratorsFactory resilienceFactory;
    private final RestClient restClient;

    // Token caching with expiration
    private volatile String cachedAccessToken;
    private volatile Instant tokenExpiresAt;
    private final ReentrantLock tokenLock = new ReentrantLock();

    public KeycloakAdminRestService(
            KeycloakAdminProperties properties,
            ResilienceDecoratorsFactory resilienceFactory,
            @Qualifier("keycloakRestClient") RestClient restClient) {
        this.properties = properties;
        this.resilienceFactory = resilienceFactory;
        this.restClient = restClient;
    }

    /**
     * Get admin access token using client credentials flow.
     * <p>
     * Caches token and refreshes automatically before expiration.
     * Thread-safe token retrieval.
     * </p>
     *
     * @return Access token
     */
    public String getAccessToken() {
        // Check if token is still valid
        if (cachedAccessToken != null && tokenExpiresAt != null
                && Instant.now().isBefore(tokenExpiresAt.minusSeconds(TOKEN_REFRESH_BUFFER_SECONDS))) {
            return cachedAccessToken;
        }

        // Acquire lock to prevent concurrent token requests
        tokenLock.lock();
        try {
            // Double-check after acquiring lock
            if (cachedAccessToken != null && tokenExpiresAt != null
                    && Instant.now().isBefore(tokenExpiresAt.minusSeconds(TOKEN_REFRESH_BUFFER_SECONDS))) {
                return cachedAccessToken;
            }

            // Fetch new token
            log.debug("Fetching new Keycloak admin token");
            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token",
                    properties.getUrl(), properties.getRealm());

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "client_credentials");
            formData.add("client_id", properties.getClientId());
            formData.add("client_secret", properties.getClientSecret());

            Supplier<String> tokenCall = () -> {
                try {
                    KeycloakTokenResponse response = restClient.post()
                            .uri(tokenUrl)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .body(formData)
                            .retrieve()
                            .body(KeycloakTokenResponse.class);

                    if (response == null || response.getAccessToken() == null) {
                        throw new ExternalServiceException("keycloak", "No access token in response");
                    }

                    // Cache token with expiration
                    cachedAccessToken = response.getAccessToken();
                    int expiresIn = response.getExpiresIn() != null ? response.getExpiresIn() : 300; // Default 5 min
                    tokenExpiresAt = Instant.now().plusSeconds(expiresIn);

                    log.debug("Keycloak admin token cached, expires at: {}", tokenExpiresAt);
                    return cachedAccessToken;
                } catch (Exception e) {
                    log.error("Failed to get Keycloak admin token", e);
                    throw new ExternalServiceException("keycloak", "Failed to get admin token: " + e.getMessage(), e);
                }
            };

            Function<Throwable, String> fallback = (Throwable t) -> {
                log.error("Keycloak token fetch failed, fallback returns null", t);
                throw new ExternalServiceException("keycloak", "Token fetch failed: " + t.getMessage(), t);
            };

            Supplier<String> decoratedCall = resilienceFactory.decorateSupplier(
                    SERVICE_NAME,
                    tokenCall,
                    fallback);

            return decoratedCall.get();
        } finally {
            tokenLock.unlock();
        }
    }

    /**
     * Get user by ID.
     *
     * @param userId Keycloak user ID
     * @return Optional containing user if found
     */
    public Optional<KeycloakUserRepresentation> getUser(String userId) {
        Supplier<Optional<KeycloakUserRepresentation>> apiCall = () -> {
            try {
                String url = String.format("%s/admin/realms/%s/users/%s",
                        properties.getUrl(), properties.getRealm(), userId);

                KeycloakUserRepresentation user = restClient.get()
                        .uri(url)
                        .headers(h -> h.setBearerAuth(getAccessToken()))
                        .retrieve()
                        .body(KeycloakUserRepresentation.class);

                return Optional.ofNullable(user);
            } catch (HttpClientErrorException.NotFound e) {
                log.debug("User not found: {}", userId);
                return Optional.empty();
            } catch (Exception e) {
                log.error("Failed to get user: {}", userId, e);
                throw new ExternalServiceException("keycloak", "Failed to get user: " + e.getMessage(), e);
            }
        };

        Function<Throwable, Optional<KeycloakUserRepresentation>> fallback = (Throwable t) -> {
            log.warn("Keycloak getUser fallback triggered for userId: {} due to: {}", userId, t.getMessage());
            return Optional.empty();
        };

        Supplier<Optional<KeycloakUserRepresentation>> decoratedCall = resilienceFactory.decorateSupplier(
                SERVICE_NAME,
                apiCall,
                fallback);

        return decoratedCall.get();
    }

    /**
     * Get users with filtering and pagination.
     *
     * @param criteria Filter criteria
     * @param pageable Pagination info
     * @return List of users
     */
    public List<KeycloakUserRepresentation> getUsers(IamUserCriteria criteria, Pageable pageable) {
        Supplier<List<KeycloakUserRepresentation>> apiCall = () -> {
            try {
                String url = String.format("%s/admin/realms/%s/users",
                        properties.getUrl(), properties.getRealm());

                // Build query parameters
                StringBuilder queryParams = new StringBuilder();
                List<String> params = new ArrayList<>();

                if (criteria != null) {
                    if (criteria.username() != null) {
                        params.add("username=" + criteria.username());
                    }
                    if (criteria.email() != null) {
                        params.add("email=" + criteria.email());
                    }
                    if (criteria.firstName() != null) {
                        params.add("firstName=" + criteria.firstName());
                    }
                    if (criteria.lastName() != null) {
                        params.add("lastName=" + criteria.lastName());
                    }
                }

                // Pagination
                if (pageable != null && !pageable.isUnpaged()) {
                    params.add("first=" + pageable.getOffset());
                    params.add("max=" + pageable.getPageSize());
                }

                if (!params.isEmpty()) {
                    queryParams.append("?").append(String.join("&", params));
                }

                String fullUrl = url + queryParams;

                List<KeycloakUserRepresentation> users = restClient.get()
                        .uri(fullUrl)
                        .headers(h -> h.setBearerAuth(getAccessToken()))
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<KeycloakUserRepresentation>>() {});

                return users != null ? users : Collections.emptyList();
            } catch (HttpClientErrorException.NotFound e) {
                log.debug("No users found with criteria: {}", criteria);
                return Collections.emptyList();
            } catch (Exception e) {
                log.error("Failed to get users with criteria: {}", criteria, e);
                throw new ExternalServiceException("keycloak", "Failed to get users: " + e.getMessage(), e);
            }
        };

        Function<Throwable, List<KeycloakUserRepresentation>> fallback = (Throwable t) -> {
            log.warn("Keycloak getUsers fallback triggered due to: {}", t.getMessage());
            return Collections.emptyList();
        };

        Supplier<List<KeycloakUserRepresentation>> decoratedCall = resilienceFactory.decorateSupplier(
                SERVICE_NAME,
                apiCall,
                fallback);

        return decoratedCall.get();
    }

    /**
     * Get groups that a user belongs to.
     *
     * @param userId Keycloak user ID
     * @return List of groups (paths)
     */
    public List<String> getUserGroups(String userId) {
        Supplier<List<String>> apiCall = () -> {
            try {
                String url = String.format("%s/admin/realms/%s/users/%s/groups",
                        properties.getUrl(), properties.getRealm(), userId);

                List<KeycloakGroupRepresentation> groups = restClient.get()
                        .uri(url)
                        .headers(h -> h.setBearerAuth(getAccessToken()))
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<KeycloakGroupRepresentation>>() {});

                if (groups == null) {
                    return Collections.emptyList();
                }

                return groups.stream()
                        .map(KeycloakGroupRepresentation::getPath)
                        .filter(path -> path != null)
                        .toList();
            } catch (HttpClientErrorException.NotFound e) {
                log.debug("User not found or has no groups: {}", userId);
                return Collections.emptyList();
            } catch (Exception e) {
                log.error("Failed to get user groups: {}", userId, e);
                throw new ExternalServiceException("keycloak", "Failed to get user groups: " + e.getMessage(), e);
            }
        };

        Function<Throwable, List<String>> fallback = (Throwable t) -> {
            log.warn("Keycloak getUserGroups fallback triggered for userId: {} due to: {}", userId, t.getMessage());
            return Collections.emptyList();
        };

        Supplier<List<String>> decoratedCall = resilienceFactory.decorateSupplier(
                SERVICE_NAME,
                apiCall,
                fallback);

        return decoratedCall.get();
    }

    /**
     * Get group by ID.
     *
     * @param groupId Keycloak group ID
     * @return Optional containing group if found
     */
    public Optional<KeycloakGroupRepresentation> getGroup(String groupId) {
        Supplier<Optional<KeycloakGroupRepresentation>> apiCall = () -> {
            try {
                String url = String.format("%s/admin/realms/%s/groups/%s",
                        properties.getUrl(), properties.getRealm(), groupId);

                KeycloakGroupRepresentation group = restClient.get()
                        .uri(url)
                        .headers(h -> h.setBearerAuth(getAccessToken()))
                        .retrieve()
                        .body(KeycloakGroupRepresentation.class);

                return Optional.ofNullable(group);
            } catch (HttpClientErrorException.NotFound e) {
                log.debug("Group not found: {}", groupId);
                return Optional.empty();
            } catch (Exception e) {
                log.error("Failed to get group: {}", groupId, e);
                throw new ExternalServiceException("keycloak", "Failed to get group: " + e.getMessage(), e);
            }
        };

        Function<Throwable, Optional<KeycloakGroupRepresentation>> fallback = (Throwable t) -> {
            log.warn("Keycloak getGroup fallback triggered for groupId: {} due to: {}", groupId, t.getMessage());
            return Optional.empty();
        };

        Supplier<Optional<KeycloakGroupRepresentation>> decoratedCall = resilienceFactory.decorateSupplier(
                SERVICE_NAME,
                apiCall,
                fallback);

        return decoratedCall.get();
    }

    /**
     * Get groups with filtering and pagination.
     * <p>
     * If parentGroupId is configured, fetches children groups from the parent group.
     * Otherwise, fetches root-level groups.
     * </p>
     *
     * @param criteria Filter criteria
     * @param pageable Pagination info
     * @return List of groups
     */
    public List<KeycloakGroupRepresentation> getGroups(IamTeamCriteria criteria, Pageable pageable) {
        Supplier<List<KeycloakGroupRepresentation>> apiCall = () -> {
            try {
                String url;
                
                // If parentGroupId is configured, fetch children groups from parent
                if (properties.getParentGroupId() != null && !properties.getParentGroupId().isBlank()) {
                    url = String.format("%s/admin/realms/%s/groups/%s/children",
                            properties.getUrl(), properties.getRealm(), properties.getParentGroupId());
                    log.debug("Fetching children groups from parent group: {}", properties.getParentGroupId());
                } else {
                    url = String.format("%s/admin/realms/%s/groups",
                            properties.getUrl(), properties.getRealm());
                    log.debug("Fetching root-level groups");
                }

                // Build query parameters
                List<String> params = new ArrayList<>();

                if (pageable != null && !pageable.isUnpaged()) {
                    params.add("first=" + pageable.getOffset());
                    params.add("max=" + pageable.getPageSize());
                }
                
                // Add briefRepresentation=true for better performance (as per user's curl example)
                params.add("briefRepresentation=true");

                String fullUrl = url;
                if (!params.isEmpty()) {
                    fullUrl += "?" + String.join("&", params);
                }

                List<KeycloakGroupRepresentation> groups = restClient.get()
                        .uri(fullUrl)
                        .headers(h -> h.setBearerAuth(getAccessToken()))
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<KeycloakGroupRepresentation>>() {});

                return groups != null ? groups : Collections.emptyList();
            } catch (HttpClientErrorException.NotFound e) {
                log.debug("No groups found with criteria: {}", criteria);
                return Collections.emptyList();
            } catch (Exception e) {
                log.error("Failed to get groups with criteria: {}", criteria, e);
                throw new ExternalServiceException("keycloak", "Failed to get groups: " + e.getMessage(), e);
            }
        };

        Function<Throwable, List<KeycloakGroupRepresentation>> fallback = (Throwable t) -> {
            log.warn("Keycloak getGroups fallback triggered due to: {}", t.getMessage());
            return Collections.emptyList();
        };

        Supplier<List<KeycloakGroupRepresentation>> decoratedCall = resilienceFactory.decorateSupplier(
                SERVICE_NAME,
                apiCall,
                fallback);

        return decoratedCall.get();
    }

    /**
     * Get group members with pagination.
     *
     * @param groupId  Keycloak group ID
     * @param pageable Pagination info
     * @return List of user IDs
     */
    public List<String> getGroupMembers(String groupId, Pageable pageable) {
        Supplier<List<String>> apiCall = () -> {
            try {
                String url = String.format("%s/admin/realms/%s/groups/%s/members",
                        properties.getUrl(), properties.getRealm(), groupId);

                // Build query parameters for pagination
                List<String> params = new ArrayList<>();
                if (pageable != null && !pageable.isUnpaged()) {
                    params.add("first=" + pageable.getOffset());
                    params.add("max=" + pageable.getPageSize());
                }

                String fullUrl = url;
                if (!params.isEmpty()) {
                    fullUrl += "?" + String.join("&", params);
                }

                List<KeycloakUserRepresentation> members = restClient.get()
                        .uri(fullUrl)
                        .headers(h -> h.setBearerAuth(getAccessToken()))
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<KeycloakUserRepresentation>>() {});

                if (members == null) {
                    return Collections.emptyList();
                }

                return members.stream()
                        .map(KeycloakUserRepresentation::getId)
                        .filter(id -> id != null)
                        .toList();
            } catch (HttpClientErrorException.NotFound e) {
                log.debug("Group not found or has no members: {}", groupId);
                return Collections.emptyList();
            } catch (Exception e) {
                log.error("Failed to get group members: {}", groupId, e);
                throw new ExternalServiceException("keycloak", "Failed to get group members: " + e.getMessage(), e);
            }
        };

        Function<Throwable, List<String>> fallback = (Throwable t) -> {
            log.warn("Keycloak getGroupMembers fallback triggered for groupId: {} due to: {}", groupId, t.getMessage());
            return Collections.emptyList();
        };

        Supplier<List<String>> decoratedCall = resilienceFactory.decorateSupplier(
                SERVICE_NAME,
                apiCall,
                fallback);

        return decoratedCall.get();
    }
}

