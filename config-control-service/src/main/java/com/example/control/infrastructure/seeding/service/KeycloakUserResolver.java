package com.example.control.infrastructure.seeding.service;

import com.example.control.infrastructure.adapter.external.keycloak.KeycloakAdminRestService;
import com.example.control.infrastructure.adapter.external.keycloak.dto.KeycloakUserRepresentation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for resolving Keycloak user IDs during seeding operations.
 * <p>
 * Fetches all users from Keycloak at initialization and provides methods
 * to resolve user IDs by username and get random user IDs for assignment.
 * Fails fast with clear errors if Keycloak is unavailable or no users found.
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakUserResolver {

    private final KeycloakAdminRestService keycloakAdminRestService;

    /**
     * Cache of user ID to username mapping (all users, including admin).
     */
    private volatile Map<String, String> userIdToUsername = Collections.emptyMap();

    /**
     * Cache of username to user ID mapping (all users, including admin).
     */
    private volatile Map<String, String> usernameToUserId = Collections.emptyMap();

    /**
     * List of filtered user IDs (only users with username starting with "user", excluding admin).
     * Used for seeding operations.
     */
    private volatile List<String> filteredUserIds = Collections.emptyList();

    /**
     * Whether users have been loaded.
     */
    private volatile boolean usersLoaded = false;

    /**
     * Loads all users from Keycloak and caches them.
     * <p>
     * This method must be called before using other methods.
     * Caches ALL users (including admin) for admin resolution, but filters users
     * for seeding operations (only users with username starting with "user", excluding admin).
     * Fails with IllegalStateException if Keycloak is unavailable or no users found.
     * </p>
     *
     * @throws IllegalStateException if Keycloak is unavailable or no users found
     */
    public void loadUsers() {
        log.info("Loading users from Keycloak for seeding...");

        try {
            List<KeycloakUserRepresentation> users = keycloakAdminRestService.getUsers(
                    null, // No criteria - get all users
                    Pageable.unpaged()
            );

            if (users == null || users.isEmpty()) {
                throw new IllegalStateException(
                        "No users found in Keycloak. Seeding requires at least one user. " +
                                "Please ensure Keycloak is properly initialized with users.");
            }

            // Build caches for ALL users (including admin) - needed for admin resolution
            Map<String, String> userIdToUsernameMap = new HashMap<>();
            Map<String, String> usernameToUserIdMap = new HashMap<>();
            List<String> filteredUserIdsList = new ArrayList<>();

            for (KeycloakUserRepresentation user : users) {
                String userId = user.getId();
                String username = user.getUsername();

                if (userId != null && username != null) {
                    // Always add to full cache (for admin resolution)
                    userIdToUsernameMap.put(userId, username);
                    usernameToUserIdMap.put(username, userId);

                    // Filter: only include users with username starting with "user" and exclude admin
                    // This is for seeding operations (getAllUserIds())
                    if (username.startsWith("user") && !username.startsWith("admin")) {
                        filteredUserIdsList.add(userId);
                    }
                }
            }

            if (userIdToUsernameMap.isEmpty()) {
                throw new IllegalStateException(
                        "No valid users found in Keycloak (users without ID or username). " +
                                "Seeding requires at least one valid user.");
            }

            if (filteredUserIdsList.isEmpty()) {
                throw new IllegalStateException(
                        "No valid users found in Keycloak matching filter (username starts with 'user' and not 'admin'). " +
                                "Seeding requires at least one valid user. " +
                                "Please ensure Keycloak has users with username starting with 'user'.");
            }

            // Update caches atomically
            this.userIdToUsername = Collections.unmodifiableMap(userIdToUsernameMap);
            this.usernameToUserId = Collections.unmodifiableMap(usernameToUserIdMap);
            this.filteredUserIds = Collections.unmodifiableList(filteredUserIdsList);
            this.usersLoaded = true;

            log.info("Loaded {} total users from Keycloak ({} filtered for seeding: username starts with 'user', excluding 'admin')",
                    userIdToUsernameMap.size(), filteredUserIdsList.size());
        } catch (Exception e) {
            String errorMsg = String.format(
                    "Failed to load users from Keycloak. Seeding cannot proceed. " +
                            "Error: %s. Please ensure Keycloak is running and accessible.",
                    e.getMessage());
            log.error(errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
        }
    }

    /**
     * Resolves admin user ID from username.
     * <p>
     * Uses the username from configuration to find the corresponding Keycloak user ID.
     * </p>
     *
     * @param username admin username (e.g., "admin")
     * @return admin user ID
     * @throws IllegalStateException if users not loaded or username not found
     */
    public String resolveAdminUserId(String username) {
        if (!usersLoaded) {
            throw new IllegalStateException(
                    "Users not loaded. Call loadUsers() before resolving admin user ID.");
        }

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Admin username cannot be null or blank");
        }

        String userId = usernameToUserId.get(username);
        if (userId == null) {
            throw new IllegalStateException(
                    String.format("Admin user with username '%s' not found in Keycloak. " +
                                    "Available usernames: %s. " +
                                    "Please ensure the admin user exists in Keycloak.",
                            username, usernameToUserId.keySet()));
        }

        log.debug("Resolved admin user ID: {} -> {}", username, userId);
        return userId;
    }

    /**
     * Gets random user IDs for assignment.
     * <p>
     * Returns a list of randomly selected user IDs from filtered users (username starts with "user").
     * If count exceeds available users, returns all available user IDs (may contain duplicates if count > available).
     * </p>
     *
     * @param count number of user IDs to return
     * @return list of random user IDs (filtered, excluding admin)
     * @throws IllegalStateException if users not loaded
     */
    public List<String> getRandomUserIds(int count) {
        if (!usersLoaded) {
            throw new IllegalStateException(
                    "Users not loaded. Call loadUsers() before getting random user IDs.");
        }

        if (count <= 0) {
            return Collections.emptyList();
        }

        List<String> selected = new ArrayList<>();
        List<String> available = new ArrayList<>(filteredUserIds);

        if (available.isEmpty()) {
            return Collections.emptyList();
        }

        Random random = new Random();
        for (int i = 0; i < count; i++) {
            String userId = available.get(random.nextInt(available.size()));
            selected.add(userId);
        }

        log.debug("Selected {} random user IDs from {} available filtered users", count, available.size());
        return selected;
    }

    /**
     * Gets all available user IDs for seeding operations.
     * <p>
     * Returns only filtered user IDs (users with username starting with "user", excluding admin).
     * Admin users are excluded from this list as they should not be used as requesters in seeding.
     * </p>
     *
     * @return list of filtered user IDs (excluding admin)
     * @throws IllegalStateException if users not loaded
     */
    public List<String> getAllUserIds() {
        if (!usersLoaded) {
            throw new IllegalStateException(
                    "Users not loaded. Call loadUsers() before getting all user IDs.");
        }

        return new ArrayList<>(filteredUserIds);
    }

    /**
     * Gets username for a user ID.
     *
     * @param userId user ID
     * @return username or null if not found
     */
    public String getUsername(String userId) {
        if (!usersLoaded) {
            throw new IllegalStateException(
                    "Users not loaded. Call loadUsers() before getting username.");
        }

        return userIdToUsername.get(userId);
    }

    /**
     * Checks if users have been loaded.
     *
     * @return true if users are loaded
     */
    public boolean isUsersLoaded() {
        return usersLoaded;
    }
}


