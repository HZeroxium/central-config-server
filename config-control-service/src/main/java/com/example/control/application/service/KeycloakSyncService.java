package com.example.control.application.service;

import com.example.control.domain.object.IamTeam;
import com.example.control.domain.object.IamUser;
import com.example.control.domain.id.IamTeamId;
import com.example.control.domain.id.IamUserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Service for synchronizing user and team data from Keycloak.
 * <p>
 * This service provides manual sync endpoints for administrators to
 * refresh the cached user and team projections from Keycloak.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakSyncService {

    private final IamUserService iamUserService;
    private final IamTeamService iamTeamService;
    private final RestTemplate restTemplate;

    @Value("${keycloak.admin.url:http://keycloak:8080}")
    private String keycloakUrl;

    @Value("${keycloak.admin.realm:config-control}")
    private String realm;

    @Value("${keycloak.admin.client-id:admin-cli}")
    private String clientId;

    @Value("${keycloak.admin.client-secret:}")
    private String clientSecret;

    /**
     * Synchronize all users from Keycloak.
     * <p>
     * Fetches all users from the Keycloak realm and updates the local cache.
     * This operation clears existing users and replaces them with fresh data.
     *
     * @return sync statistics
     */
    public SyncResult syncUsers() {
        log.info("Starting user sync from Keycloak");
        
        try {
            // Get admin token
            String adminToken = getAdminToken();
            
            // Fetch all users from Keycloak
            String usersUrl = String.format("%s/admin/realms/%s/users", keycloakUrl, realm);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map[]> response = restTemplate.exchange(
                usersUrl, HttpMethod.GET, entity, Map[].class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to fetch users from Keycloak: " + response.getStatusCode());
            }
            
            Map[] keycloakUsers = response.getBody();
            if (keycloakUsers == null) {
                log.warn("No users returned from Keycloak");
                return new SyncResult(0, 0, List.of("No users returned from Keycloak"));
            }
            
            // Clear existing users
            iamUserService.deleteAll();
            
            // Convert and save users
            int synced = 0;
            int errors = 0;
            List<String> errorMessages = new java.util.ArrayList<>();
            
            for (Map<String, Object> keycloakUser : keycloakUsers) {
                try {
                    IamUser user = convertKeycloakUser(keycloakUser);
                    iamUserService.save(user);
                    synced++;
                } catch (Exception e) {
                    errors++;
                    String errorMsg = String.format("Failed to sync user %s: %s", 
                        keycloakUser.get("username"), e.getMessage());
                    errorMessages.add(errorMsg);
                    log.error(errorMsg, e);
                }
            }
            
            log.info("User sync completed: {} synced, {} errors", synced, errors);
            return new SyncResult(synced, errors, errorMessages);
            
        } catch (Exception e) {
            log.error("User sync failed", e);
            return new SyncResult(0, 1, List.of("User sync failed: " + e.getMessage()));
        }
    }

    /**
     * Synchronize all teams from Keycloak.
     * <p>
     * Fetches all groups from the Keycloak realm and updates the local cache.
     * This operation clears existing teams and replaces them with fresh data.
     *
     * @return sync statistics
     */
    public SyncResult syncTeams() {
        log.info("Starting team sync from Keycloak");
        
        try {
            // Get admin token
            String adminToken = getAdminToken();
            
            // Fetch all groups from Keycloak
            String groupsUrl = String.format("%s/admin/realms/%s/groups", keycloakUrl, realm);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map[]> response = restTemplate.exchange(
                groupsUrl, HttpMethod.GET, entity, Map[].class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to fetch groups from Keycloak: " + response.getStatusCode());
            }
            
            Map[] keycloakGroups = response.getBody();
            if (keycloakGroups == null) {
                log.warn("No groups returned from Keycloak");
                return new SyncResult(0, 0, List.of("No groups returned from Keycloak"));
            }
            
            // Clear existing teams
            iamTeamService.deleteAll();
            
            // Convert and save teams
            int synced = 0;
            int errors = 0;
            List<String> errorMessages = new java.util.ArrayList<>();
            
            for (Map<String, Object> keycloakGroup : keycloakGroups) {
                try {
                    IamTeam team = convertKeycloakGroup(keycloakGroup);
                    iamTeamService.save(team);
                    synced++;
                } catch (Exception e) {
                    errors++;
                    String errorMsg = String.format("Failed to sync team %s: %s", 
                        keycloakGroup.get("name"), e.getMessage());
                    errorMessages.add(errorMsg);
                    log.error(errorMsg, e);
                }
            }
            
            log.info("Team sync completed: {} synced, {} errors", synced, errors);
            return new SyncResult(synced, errors, errorMessages);
            
        } catch (Exception e) {
            log.error("Team sync failed", e);
            return new SyncResult(0, 1, List.of("Team sync failed: " + e.getMessage()));
        }
    }

    /**
     * Synchronize both users and teams from Keycloak.
     *
     * @return combined sync statistics
     */
    public SyncResult syncAll() {
        log.info("Starting full sync from Keycloak");
        
        SyncResult userResult = syncUsers();
        SyncResult teamResult = syncTeams();
        
        return new SyncResult(
            userResult.synced() + teamResult.synced(),
            userResult.errors() + teamResult.errors(),
            List.of(
                String.format("Users: %d synced, %d errors", userResult.synced(), userResult.errors()),
                String.format("Teams: %d synced, %d errors", teamResult.synced(), teamResult.errors())
            )
        );
    }

    /**
     * Get admin access token from Keycloak.
     */
    private String getAdminToken() {
        String tokenUrl = String.format("%s/realms/master/protocol/openid-connect/token", keycloakUrl);
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/x-www-form-urlencoded");
        
        String body = String.format(
            "grant_type=client_credentials&client_id=%s&client_secret=%s",
            clientId, clientSecret);
        
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        
        ResponseEntity<Map> response = restTemplate.exchange(
            tokenUrl, HttpMethod.POST, entity, Map.class);
        
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to get admin token: " + response.getStatusCode());
        }
        
        Map<String, Object> tokenResponse = response.getBody();
        if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
            throw new RuntimeException("No access token in response");
        }
        
        return (String) tokenResponse.get("access_token");
    }

    /**
     * Convert Keycloak user to IamUser domain object.
     */
    private IamUser convertKeycloakUser(Map<String, Object> keycloakUser) {
        String userId = (String) keycloakUser.get("id");
        String username = (String) keycloakUser.get("username");
        String email = (String) keycloakUser.get("email");
        String firstName = (String) keycloakUser.get("firstName");
        String lastName = (String) keycloakUser.get("lastName");
        
        // Extract manager ID from attributes
        String managerId = null;
        Map<String, Object> attributes = (Map<String, Object>) keycloakUser.get("attributes");
        if (attributes != null && attributes.containsKey("manager_id")) {
            List<String> managerIds = (List<String>) attributes.get("manager_id");
            if (managerIds != null && !managerIds.isEmpty()) {
                managerId = managerIds.get(0);
            }
        }
        
        // Extract team IDs from groups (simplified - would need additional API call)
        List<String> teamIds = new java.util.ArrayList<>();
        
        // Extract roles from realm roles
        List<String> roles = new java.util.ArrayList<>();
        Map<String, Object> realmAccess = (Map<String, Object>) keycloakUser.get("realmAccess");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            roles = (List<String>) realmAccess.get("roles");
        }
        
        Instant now = Instant.now();
        
        return IamUser.builder()
            .userId(IamUserId.of(userId))
            .username(username)
            .email(email)
            .firstName(firstName)
            .lastName(lastName)
            .teamIds(teamIds)
            .managerId(managerId)
            .roles(roles)
            .createdAt(now)
            .updatedAt(now)
            .syncedAt(now)
            .build();
    }

    /**
     * Convert Keycloak group to IamTeam domain object.
     */
    private IamTeam convertKeycloakGroup(Map<String, Object> keycloakGroup) {
        String teamId = (String) keycloakGroup.get("name");
        String displayName = (String) keycloakGroup.get("name");
        
        // Extract members (simplified - would need additional API call)
        List<String> members = new java.util.ArrayList<>();
        
        Instant now = Instant.now();
        
        return IamTeam.builder()
            .teamId(IamTeamId.of(teamId))
            .displayName(displayName)
            .members(members)
            .createdAt(now)
            .updatedAt(now)
            .syncedAt(now)
            .build();
    }

    /**
     * Result of a sync operation.
     */
    public record SyncResult(
        int synced,
        int errors,
        List<String> messages
    ) {}
}
