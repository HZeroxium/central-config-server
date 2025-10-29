package com.example.keycloak.ports;

import java.util.List;
import java.util.Optional;

/**
 * Port interface for enriching user profiles from external services.
 * This follows Hexagonal Architecture pattern - the domain defines the port,
 * infrastructure provides the implementation.
 */
public interface UserProfileEnricher {
    
    /**
     * Enrich user profile data based on username.
     * 
     * @param username the username to enrich
     * @return Optional containing enriched profile data, or empty if not found
     */
    Optional<EnrichedProfile> enrichProfile(String username);
    
    /**
     * Record representing enriched user profile data.
     * Maps to Keycloak user attributes and properties.
     */
    record EnrichedProfile(
        String email,
        String firstName,
        String lastName,
        String phone,
        String employeeId,
        String department,
        String jobTitle,
        String officeLocation,
        String hireDate,
        String managerUsername,
        List<String> realmRoles,
        List<String> groups
    ) {}
}
