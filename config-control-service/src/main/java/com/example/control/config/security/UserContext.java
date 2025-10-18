package com.example.control.config.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents the authenticated user context derived from a JWT or a set of Spring Security {@link GrantedAuthority} entries.
 *
 * <p><strong>Purpose</strong>:
 * This class centralizes user-identifying attributes (e.g., {@code userId}, {@code username}, {@code email})
 * and authorization artifacts (e.g., {@code roles}, {@code teamIds}) for application-layer checks.
 * It is intended to be used by service/controller layers to perform simple, readable authorization checks
 * such as {@link #hasRole(String)} or {@link #isMemberOfTeam(String)}.</p>
 *
 * <p><strong>Key mappings</strong> (typical with Keycloak + Spring Security Resource Server):
 * <ul>
 *   <li><b>User ID</b>: {@code sub} claim (JWT Subject)</li>
 *   <li><b>Username</b>: {@code preferred_username}</li>
 *   <li><b>Email</b>: {@code email}</li>
 *   <li><b>First/Last name</b>: {@code given_name}, {@code family_name}</li>
 *   <li><b>Teams</b>: {@code groups} claim; entries commonly look like {@code /teams/<teamSlug>}. This class strips the {@code /teams/} prefix.</li>
 *   <li><b>Roles</b>: realm roles from {@code realm_access.roles}</li>
 *   <li><b>Manager ID</b>: custom claim {@code manager_id} (if present)</li>
 * </ul>
 * </p>
 *
 * <p><strong>Usage example</strong>:
 * <pre>{@code
 * Authentication auth = SecurityContextHolder.getContext().getAuthentication();
 * if (auth instanceof JwtAuthenticationToken token) {
 *     Jwt jwt = token.getToken();
 *     UserContext ctx = UserContext.fromJwt(jwt);
 *     if (ctx.isSysAdmin() || ctx.isMemberOfTeam("alpha")) {
 *         // proceed with privileged operation
 *     }
 * }
 * }</pre>
 * </p>
 *
 * <p><strong>Thread-safety</strong>:
 * Instances are <em>mutable</em> due to Lombok {@code @Data} (setters). Do not share a mutable instance across threads
 * without external synchronization. Prefer constructing new instances per request via {@link #fromJwt(Jwt)} or
 * {@link #fromAuthorities(List)}.</p>
 *
 * @since 1.0
 * @see Jwt
 * @see GrantedAuthority
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {

    /**
     * Stable identifier for the user, typically sourced from the JWT {@code sub} (subject) claim.
     * <p>Example: {@code "e2b8f0c1-..."}.</p>
     */
    private String userId;

    /**
     * Human-readable login name, typically from {@code preferred_username}.
     * <p>Example: {@code "jane.doe"}.</p>
     */
    private String username;

    /**
     * Primary email address, typically from the {@code email} claim.
     */
    private String email;

    /**
     * Given name, typically from {@code given_name}.
     */
    private String firstName;

    /**
     * Family name, typically from {@code family_name}.
     */
    private String lastName;

    /**
     * Logical team identifiers derived from the {@code groups} claim.
     * <p>Implementation note: For group entries that start with {@code /teams/}, the {@code /teams/} prefix is removed.
     * If the {@code groups} claim is absent, this list is empty.</p>
     */
    private List<String> teamIds;

    /**
     * Realm-level role names derived from {@code realm_access.roles}.
     * <p>If {@code realm_access.roles} is absent, this list is empty.</p>
     */
    private List<String> roles;

    /**
     * Optional manager identifier taken from the custom claim {@code manager_id}, if present.
     */
    private String managerId;

    /**
     * Timestamp when the user was created.
     */
    private Instant createdAt;

    /**
     * Timestamp when the user last logged in.
     */
    private Instant lastLoginAt;

    /**
     * Timestamp when the user context was last updated.
     */
    private Instant updatedAt;

    /**
     * Builds a {@link UserContext} from a JWT by reading standard and custom claims.
     *
     * <p><strong>Claims read</strong>:
     * <ul>
     *     <li>{@code sub} → {@link #userId}</li>
     *     <li>{@code preferred_username} → {@link #username}</li>
     *     <li>{@code email} → {@link #email}</li>
     *     <li>{@code given_name} → {@link #firstName}</li>
     *     <li>{@code family_name} → {@link #lastName}</li>
     *     <li>{@code groups} (list) → {@link #teamIds} (with {@code /teams/} prefix stripped if present)</li>
     *     <li>{@code realm_access.roles} (list) → {@link #roles}</li>
     *     <li>{@code manager_id} (custom) → {@link #managerId}</li>
     * </ul>
     * Missing claims result in empty values (e.g., empty lists) rather than errors.</p>
     *
     * <p><strong>Null-safety</strong>:
     * {@code jwt} must be non-null. Behavior on a null input is undefined (may throw {@link NullPointerException}).</p>
     *
     * @param jwt the validated JWT representing the current principal; must not be {@code null}
     * @return an immutable-by-convention (but technically mutable) {@link UserContext} built from claims
     * @see Jwt#getSubject()
     * @see Jwt#getClaimAsString(String)
     * @see Jwt#getClaimAsStringList(String)
     * @see Jwt#getClaimAsMap(String)
     */
    public static UserContext fromJwt(Jwt jwt) {
        // Extract groups (teams) from groups claim
        List<String> groups = jwt.getClaimAsStringList("groups");
        List<String> teamIds = groups != null
            ? groups.stream()
                .map(group -> group.replaceFirst("^/teams/", "")) // Remove /teams/ prefix
                .collect(Collectors.toList())
            : List.of();

        // Extract roles from realm_access claim
        List<String> roles = List.of();
        if (jwt.hasClaim("realm_access")) {
            var realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                @SuppressWarnings("unchecked")
                List<String> realmRoles = (List<String>) realmAccess.get("roles");
                roles = realmRoles != null ? realmRoles : List.of();
            }
        }

        return UserContext.builder()
                .userId(jwt.getSubject())
                .username(jwt.getClaimAsString("preferred_username"))
                .email(jwt.getClaimAsString("email"))
                .firstName(jwt.getClaimAsString("given_name"))
                .lastName(jwt.getClaimAsString("family_name"))
                .teamIds(teamIds)
                .roles(roles)
                .managerId(jwt.getClaimAsString("manager_id"))
                .build();
    }

    /**
     * Builds a {@link UserContext} from a set of {@link GrantedAuthority} values,
     * following naming conventions commonly used in Spring Security adapters:
     *
     * <ul>
     *   <li>Authorities prefixed with {@code ROLE_} are mapped to {@link #roles} by stripping the prefix.</li>
     *   <li>Authorities prefixed with {@code GROUP_} are mapped to {@link #teamIds} by stripping the prefix.</li>
     * </ul>
     *
     * <p><strong>Examples</strong>:
     * <ul>
     *   <li>{@code ROLE_SYS_ADMIN} → role {@code SYS_ADMIN}</li>
     *   <li>{@code GROUP_alpha} → teamId {@code alpha}</li>
     * </ul>
     * </p>
     *
     * <p>Only roles and teamIds are populated; identity fields (e.g., {@code userId}, {@code username}) remain {@code null}.</p>
     *
     * @param authorities the granted authorities for the current principal; may be empty but not {@code null}
     * @return a {@link UserContext} containing derived {@link #roles} and {@link #teamIds}
     */
    public static UserContext fromAuthorities(List<GrantedAuthority> authorities) {
        List<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.substring(5)) // Remove "ROLE_" prefix
                .collect(Collectors.toList());

        List<String> teamIds = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("GROUP_"))
                .map(auth -> auth.substring(6)) // Remove "GROUP_" prefix
                .collect(Collectors.toList());

        return UserContext.builder()
                .roles(roles)
                .teamIds(teamIds)
                .build();
    }

    /**
     * Returns whether the user possesses the specified role.
     * <p>Comparison is case-sensitive and expects the bare role name (e.g., {@code "SYS_ADMIN"}), not the
     * authority form (e.g., {@code "ROLE_SYS_ADMIN"}).</p>
     *
     * @param role the role to test; must not be {@code null}
     * @return {@code true} if {@link #roles} contains {@code role}; {@code false} otherwise, including when {@link #roles} is {@code null}
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * Convenience method indicating whether the user is a system administrator.
     *
     * @return {@code true} if the user has role {@code SYS_ADMIN}; otherwise {@code false}
     */
    public boolean isSysAdmin() {
        return hasRole("SYS_ADMIN");
    }

    /**
     * Returns whether the user is a member of the given team.
     * <p>Comparison is exact and case-sensitive.</p>
     *
     * @param teamId application-level team identifier (e.g., {@code "alpha"}); must not be {@code null}
     * @return {@code true} if {@link #teamIds} contains {@code teamId}; {@code false} otherwise, including when {@link #teamIds} is {@code null}
     */
    public boolean isMemberOfTeam(String teamId) {
        return teamIds != null && teamIds.contains(teamId);
    }

    /**
     * Returns whether the user belongs to <em>any</em> of the specified teams.
     * <p>Both this context's {@link #teamIds} and the input list must be non-null to match.</p>
     *
     * @param teamIds a non-null list of candidate team identifiers
     * @return {@code true} if there is at least one common team ID; otherwise {@code false}
     */
    public boolean isMemberOfAnyTeam(List<String> teamIds) {
        if (this.teamIds == null || teamIds == null) {
            return false;
        }
        return this.teamIds.stream().anyMatch(teamIds::contains);
    }

    /**
     * Computes a human-friendly full name.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Both {@code firstName} and {@code lastName} present → {@code "firstName lastName"}</li>
     *   <li>Only {@code firstName} present → {@code "firstName"}</li>
     *   <li>Only {@code lastName} present → {@code "lastName"}</li>
     *   <li>Fallback → {@code username}</li>
     * </ol>
     * </p>
     *
     * @return the best-effort full name; may be {@code null} if all source fields are {@code null}
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        } else {
            return username;
        }
    }

    /**
     * Get the creation timestamp.
     * @return the creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Get the last login timestamp.
     * @return the last login timestamp
     */
    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    /**
     * Get the last update timestamp.
     * @return the last update timestamp
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
