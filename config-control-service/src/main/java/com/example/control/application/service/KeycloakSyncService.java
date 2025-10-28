// package com.example.control.application.service;

// import com.example.control.domain.id.IamTeamId;
// import com.example.control.domain.id.IamUserId;
// import com.example.control.domain.object.IamTeam;
// import com.example.control.domain.object.IamUser;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.core.ParameterizedTypeReference;
// import org.springframework.http.MediaType;
// import org.springframework.http.ResponseEntity;
// import org.springframework.stereotype.Service;
// import org.springframework.web.client.RestClient;

// import java.time.Instant;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.Map;

// @Slf4j
// @Service
// @RequiredArgsConstructor
// public class KeycloakSyncService {

// private final IamUserService iamUserService;
// private final IamTeamService iamTeamService;
// /** Replaced RestTemplate with RestClient */
// private final RestClient restClient;

// @Value("${keycloak.admin.url:http://keycloak:8080}")
// private String keycloakUrl;

// @Value("${keycloak.admin.realm:config-control}")
// private String realm;

// @Value("${keycloak.admin.client-id:admin-cli}")
// private String clientId;

// @Value("${keycloak.admin.client-secret:}")
// private String clientSecret;

// /**
// * Synchronize all users from Keycloak.
// */
// public SyncResult syncUsers() {
// log.info("Starting user sync from Keycloak");
// try {
// String adminToken = getAdminToken();

// String usersUrl = String.format("%s/admin/realms/%s/users", keycloakUrl,
// realm);

// ResponseEntity<List<Map<String, Object>>> response =
// restClient.get()
// .uri(usersUrl)
// .headers(h -> h.setBearerAuth(adminToken))
// .retrieve()
// .toEntity(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

// if (!response.getStatusCode().is2xxSuccessful()) {
// throw new RuntimeException("Failed to fetch users from Keycloak: " +
// response.getStatusCode());
// }

// List<Map<String, Object>> keycloakUsers = response.getBody();
// if (keycloakUsers == null || keycloakUsers.isEmpty()) {
// log.warn("No users returned from Keycloak");
// return new SyncResult(0, 0, List.of("No users returned from Keycloak"));
// }

// iamUserService.deleteAll();

// int synced = 0;
// int errors = 0;
// List<String> errorMessages = new ArrayList<>();

// for (Map<String, Object> keycloakUser : keycloakUsers) {
// try {
// IamUser user = convertKeycloakUser(keycloakUser);
// iamUserService.save(user);
// synced++;
// } catch (Exception e) {
// errors++;
// String username = getString(keycloakUser, "username");
// String errorMsg = String.format("Failed to sync user %s: %s", username,
// e.getMessage());
// errorMessages.add(errorMsg);
// log.error(errorMsg, e);
// }
// }

// log.info("User sync completed: {} synced, {} errors", synced, errors);
// return new SyncResult(synced, errors, errorMessages);

// } catch (Exception e) {
// log.error("User sync failed", e);
// return new SyncResult(0, 1, List.of("User sync failed: " + e.getMessage()));
// }
// }

// /**
// * Synchronize all teams from Keycloak.
// */
// public SyncResult syncTeams() {
// log.info("Starting team sync from Keycloak");
// try {
// String adminToken = getAdminToken();

// String groupsUrl = String.format("%s/admin/realms/%s/groups", keycloakUrl,
// realm);

// ResponseEntity<List<Map<String, Object>>> response =
// restClient.get()
// .uri(groupsUrl)
// .headers(h -> h.setBearerAuth(adminToken))
// .retrieve()
// .toEntity(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

// if (!response.getStatusCode().is2xxSuccessful()) {
// throw new RuntimeException("Failed to fetch groups from Keycloak: " +
// response.getStatusCode());
// }

// List<Map<String, Object>> keycloakGroups = response.getBody();
// if (keycloakGroups == null || keycloakGroups.isEmpty()) {
// log.warn("No groups returned from Keycloak");
// return new SyncResult(0, 0, List.of("No groups returned from Keycloak"));
// }

// iamTeamService.deleteAll();

// int synced = 0;
// int errors = 0;
// List<String> errorMessages = new ArrayList<>();

// for (Map<String, Object> keycloakGroup : keycloakGroups) {
// try {
// IamTeam team = convertKeycloakGroup(keycloakGroup);
// iamTeamService.save(team);
// synced++;
// } catch (Exception e) {
// errors++;
// String name = getString(keycloakGroup, "name");
// String errorMsg = String.format("Failed to sync team %s: %s", name,
// e.getMessage());
// errorMessages.add(errorMsg);
// log.error(errorMsg, e);
// }
// }

// log.info("Team sync completed: {} synced, {} errors", synced, errors);
// return new SyncResult(synced, errors, errorMessages);

// } catch (Exception e) {
// log.error("Team sync failed", e);
// return new SyncResult(0, 1, List.of("Team sync failed: " + e.getMessage()));
// }
// }

// /**
// * Synchronize both users and teams from Keycloak.
// */
// public SyncResult syncAll() {
// log.info("Starting full sync from Keycloak");
// SyncResult userResult = syncUsers();
// SyncResult teamResult = syncTeams();

// return new SyncResult(
// userResult.synced() + teamResult.synced(),
// userResult.errors() + teamResult.errors(),
// List.of(
// String.format("Users: %d synced, %d errors", userResult.synced(),
// userResult.errors()),
// String.format("Teams: %d synced, %d errors", teamResult.synced(),
// teamResult.errors())
// )
// );
// }

// /**
// * Get admin access token from Keycloak.
// */
// private String getAdminToken() {
// String tokenUrl =
// String.format("%s/realms/master/protocol/openid-connect/token", keycloakUrl);

// String formBody = String.format(
// "grant_type=client_credentials&client_id=%s&client_secret=%s",
// clientId, clientSecret
// );

// ResponseEntity<Map<String, Object>> response =
// restClient.post()
// .uri(tokenUrl)
// .contentType(MediaType.APPLICATION_FORM_URLENCODED)
// .body(formBody)
// .retrieve()
// .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

// if (!response.getStatusCode().is2xxSuccessful()) {
// throw new RuntimeException("Failed to get admin token: " +
// response.getStatusCode());
// }

// Map<String, Object> tokenResponse = response.getBody();
// String accessToken = tokenResponse != null ? getString(tokenResponse,
// "access_token") : null;
// if (accessToken == null || accessToken.isBlank()) {
// throw new RuntimeException("No access token in response");
// }
// return accessToken;
// }

// /**
// * Convert Keycloak user to IamUser domain object (type-safe).
// */
// private IamUser convertKeycloakUser(Map<String, Object> keycloakUser) {
// String userId = getString(keycloakUser, "id");
// String username = getString(keycloakUser, "username");
// String email = getString(keycloakUser, "email");
// String firstName = getString(keycloakUser, "firstName");
// String lastName = getString(keycloakUser, "lastName");

// // Attributes (safe access)
// Map<String, Object> attributes = getMap(keycloakUser, "attributes");
// List<String> managerIds = getStringList(attributes, "manager_id");
// String managerId = !managerIds.isEmpty() ? managerIds.getFirst() : null;

// // Team IDs - left empty (requires extra calls if needed)
// List<String> teamIds = List.of();

// // Realm roles (safe access)
// Map<String, Object> realmAccess = getMap(keycloakUser, "realmAccess");
// List<String> roles = getStringList(realmAccess, "roles");

// Instant now = Instant.now();

// return IamUser.builder()
// .userId(IamUserId.of(userId))
// .username(username)
// .email(email)
// .firstName(firstName)
// .lastName(lastName)
// .teamIds(teamIds)
// .managerId(managerId)
// .roles(roles)
// .createdAt(now)
// .updatedAt(now)
// .syncedAt(now)
// .build();
// }

// /**
// * Convert Keycloak group to IamTeam domain object (type-safe).
// */
// private IamTeam convertKeycloakGroup(Map<String, Object> keycloakGroup) {
// String teamId = getString(keycloakGroup, "name");
// String displayName = getString(keycloakGroup, "name");

// // Members - left empty (requires additional API calls)
// List<String> members = List.of();

// Instant now = Instant.now();

// return IamTeam.builder()
// .teamId(IamTeamId.of(teamId))
// .displayName(displayName)
// .members(members)
// .createdAt(now)
// .updatedAt(now)
// .syncedAt(now)
// .build();
// }

// /**
// * Type-safe helpers to avoid raw types and unchecked casts.
// */
// private static Map<String, Object> getMap(Map<String, Object> src, String
// key) {
// Object v = src.get(key);
// if (v instanceof Map<?, ?> m) {
// // Best-effort narrowing: keys that are not String are ignored
// return m.entrySet().stream()
// .filter(e -> e.getKey() instanceof String)
// .collect(java.util.stream.Collectors.toMap(
// e -> (String) e.getKey(),
// Map.Entry::getValue
// ));
// }
// return Map.of();
// }

// private static String getString(Map<String, Object> src, String key) {
// Object v = src.get(key);
// return (v instanceof String s) ? s : null;
// }

// private static List<String> getStringList(Map<String, Object> src, String
// key) {
// Object v = src.get(key);
// if (v instanceof List<?> list) {
// List<String> out = new ArrayList<>(list.size());
// for (Object o : list) {
// if (o instanceof String s) {
// out.add(s);
// }
// }
// return List.copyOf(out);
// }
// return List.of();
// }

// /**
// * Result of a sync operation.
// */
// public record SyncResult(
// int synced,
// int errors,
// List<String> messages
// ) {}
// }
