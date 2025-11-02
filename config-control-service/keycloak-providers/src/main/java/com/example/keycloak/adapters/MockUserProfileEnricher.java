package com.example.keycloak.adapters;

import com.example.keycloak.ports.UserProfileEnricher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Mock implementation of UserProfileEnricher for testing and development.
 * Uses static data matching the users.json configuration.
 */
public class MockUserProfileEnricher implements UserProfileEnricher {

    private static final Logger logger = LoggerFactory.getLogger(MockUserProfileEnricher.class);

    // Static mock data based on users.json
    private static final Map<String, EnrichedProfile> MOCK_PROFILES = Map.of(
            "admin", new EnrichedProfile(
                    "admin@example.com",
                    "System",
                    "Administrator",
                    "+84912345000",
                    "IT",
                    null, // No manager
                    List.of("SYS_ADMIN"),
                    List.of()),
            "admin2", new EnrichedProfile(
                    "admin2@example.com",
                    "System",
                    "Administrator 2",
                    "+84912345001",
                    "IT",
                    null, // No manager
                    List.of("SYS_ADMIN"),
                    List.of()),
            "user1", new EnrichedProfile(
                    "user1@example.com",
                    "User",
                    "One",
                    "+84912345001",
                    "Engineering",
                    null, // No manager
                    List.of("USER"),
                    List.of("team1")),
            "user2", new EnrichedProfile(
                    "user2@example.com",
                    "User",
                    "Two",
                    "+84912345002",
                    "Engineering",
                    "user1", // Manager username
                    List.of("USER"),
                    List.of("team1")),
            "user3", new EnrichedProfile(
                    "user3@example.com",
                    "User",
                    "Three",
                    "+84912345003",
                    "Engineering",
                    null, // No manager
                    List.of("USER"),
                    List.of("team2")),
            "user4", new EnrichedProfile(
                    "user4@example.com",
                    "User",
                    "Four",
                    "+84912345004",
                    "Engineering",
                    "user3", // Manager username
                    List.of("USER"),
                    List.of("team2")),
            "user5", new EnrichedProfile(
                    "user5@example.com",
                    "User",
                    "Five",
                    "+84912345005",
                    "Engineering",
                    null, // No manager
                    List.of("USER"),
                    List.of()),
            // Additional test users for registration testing
            "user6", new EnrichedProfile(
                    "user6@example.com",
                    "User",
                    "Six",
                    "+84912345006",
                    "Engineering",
                    "user1", // Manager username
                    List.of("USER"),
                    List.of("team1")),
            "user7", new EnrichedProfile(
                    "user7@example.com",
                    "User",
                    "Seven",
                    "+84912345007",
                    "Engineering",
                    "user3", // Manager username
                    List.of("USER"),
                    List.of("team2")));

    @Override
    public Optional<EnrichedProfile> enrichProfile(String username) {
        logger.info("Enriching profile for username: {}", username);

        EnrichedProfile profile = MOCK_PROFILES.get(username);
        if (profile != null) {
            logger.info("Found profile for username: {} - email: {}, phone: {}",
                    username, profile.email(), profile.phone());
            return Optional.of(profile);
        } else {
            logger.warn("No profile found for username: {} - will proceed with minimal registration", username);
            return Optional.empty();
        }
    }
}
