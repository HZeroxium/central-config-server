package com.example.keycloak.registration;

import com.example.keycloak.adapters.MockUserProfileEnricher;
import com.example.keycloak.ports.UserProfileEnricher;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.GroupModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Post-registration authenticator that enriches user attributes after user creation.
 * This authenticator runs after "Registration User Creation" step and enriches
 * the user with additional attributes from external profile service.
 */
public class PostRegistrationEnricherAuthenticator implements Authenticator {
    
    private static final Logger logger = LoggerFactory.getLogger(PostRegistrationEnricherAuthenticator.class);
    
    private final UserProfileEnricher profileEnricher;
    
    public PostRegistrationEnricherAuthenticator() {
        this.profileEnricher = new MockUserProfileEnricher();
    }
    
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        if (user == null) {
            logger.warn("No user found in authentication context");
            context.success();
            return;
        }
        
        String username = user.getUsername();
        logger.info("Enriching user profile for: {}", username);
        
        try {
            Optional<UserProfileEnricher.EnrichedProfile> profileOpt = profileEnricher.enrichProfile(username);
            
            if (profileOpt.isPresent()) {
                UserProfileEnricher.EnrichedProfile profile = profileOpt.get();
                enrichUserProfile(context, user, profile);
                logger.info("Successfully enriched profile for user: {}", username);
            } else {
                logger.warn("No profile data found for user: {} - setting profile_incomplete flag", username);
                user.setSingleAttribute("profile_incomplete", "true");
            }
            
        } catch (Exception e) {
            logger.error("Error enriching profile for user: {} - setting profile_incomplete flag", username, e);
            user.setSingleAttribute("profile_incomplete", "true");
        }
        
        // Always continue the flow - never block registration
        context.success();
    }
    
    private void enrichUserProfile(AuthenticationFlowContext context, UserModel user, UserProfileEnricher.EnrichedProfile profile) {
        RealmModel realm = context.getRealm();
        KeycloakSession session = context.getSession();
        
        // Set basic user properties
        if (profile.email() != null) {
            user.setEmail(profile.email());
        }
        if (profile.firstName() != null) {
            user.setFirstName(profile.firstName());
        }
        if (profile.lastName() != null) {
            user.setLastName(profile.lastName());
        }
        
        // Set custom attributes
        if (profile.phone() != null) {
            user.setSingleAttribute("phone", profile.phone());
        }
        if (profile.employeeId() != null) {
            user.setSingleAttribute("employee_id", profile.employeeId());
        }
        if (profile.department() != null) {
            user.setSingleAttribute("department", profile.department());
        }
        if (profile.jobTitle() != null) {
            user.setSingleAttribute("job_title", profile.jobTitle());
        }
        if (profile.officeLocation() != null) {
            user.setSingleAttribute("office_location", profile.officeLocation());
        }
        if (profile.hireDate() != null) {
            user.setSingleAttribute("hire_date", profile.hireDate());
        }
        
        // Resolve manager relationship
        if (profile.managerUsername() != null) {
            resolveManagerRelationship(session, realm, user, profile.managerUsername());
        }
        
        // Assign realm roles
        if (profile.realmRoles() != null && !profile.realmRoles().isEmpty()) {
            assignRealmRoles(session, realm, user, profile.realmRoles());
        }
        
        // Join groups
        if (profile.groups() != null && !profile.groups().isEmpty()) {
            joinGroups(session, realm, user, profile.groups());
        }
    }
    
    private void resolveManagerRelationship(KeycloakSession session, RealmModel realm, UserModel user, String managerUsername) {
        try {
            // Find manager user by username
            UserModel manager = session.users().getUserByUsername(realm, managerUsername);
            if (manager != null) {
                String managerId = manager.getId();
                user.setSingleAttribute("manager_id", managerId);
                logger.info("Set manager_id={} for user={} (manager={})", managerId, user.getUsername(), managerUsername);
            } else {
                logger.warn("Manager user not found: {} for user: {}", managerUsername, user.getUsername());
            }
        } catch (Exception e) {
            logger.error("Error resolving manager relationship for user: {} with manager: {}", 
                user.getUsername(), managerUsername, e);
        }
    }
    
    private void assignRealmRoles(KeycloakSession session, RealmModel realm, UserModel user, List<String> roleNames) {
        for (String roleName : roleNames) {
            try {
                RoleModel role = realm.getRole(roleName);
                if (role != null) {
                    user.grantRole(role);
                    logger.info("Assigned role {} to user {}", roleName, user.getUsername());
                } else {
                    logger.warn("Role not found: {} for user: {}", roleName, user.getUsername());
                }
            } catch (Exception e) {
                logger.error("Error assigning role {} to user: {}", roleName, user.getUsername(), e);
            }
        }
    }
    
    private void joinGroups(KeycloakSession session, RealmModel realm, UserModel user, List<String> groupNames) {
        for (String groupName : groupNames) {
            try {
                // Search for group by name
                GroupModel group = session.groups().getGroupsStream(realm)
                    .filter(g -> g.getName().equals(groupName))
                    .findFirst()
                    .orElse(null);
                
                if (group != null) {
                    user.joinGroup(group);
                    logger.info("Added user {} to group {}", user.getUsername(), groupName);
                } else {
                    logger.warn("Group not found: {} for user: {}", groupName, user.getUsername());
                }
            } catch (Exception e) {
                logger.error("Error adding user {} to group: {}", user.getUsername(), groupName, e);
            }
        }
    }
    
    @Override
    public void action(AuthenticationFlowContext context) {
        // No action needed - this is a one-time enrichment step
    }
    
    @Override
    public boolean requiresUser() {
        return true; // User must be created before enrichment
    }
    
    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true; // Always configured - no user-specific configuration needed
    }
    
    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // No required actions needed
    }
    
    @Override
    public void close() {
        // No resources to close
    }
}
