// package com.example.control.api.controller;

// import com.example.control.application.service.KeycloakSyncService;
// import com.example.control.config.security.UserContext;
// import io.swagger.v3.oas.annotations.Operation;
// import io.swagger.v3.oas.annotations.security.SecurityRequirement;
// import io.swagger.v3.oas.annotations.tags.Tag;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.web.bind.annotation.*;

// /**
//  * REST controller for Keycloak synchronization.
//  * <p>
//  * Provides endpoints for manually triggering synchronization of user and team
//  * data from Keycloak. All endpoints require SYS_ADMIN role.
//  * </p>
//  */
// @Slf4j
// @RestController
// @RequestMapping("/api/admin/sync")
// @RequiredArgsConstructor
// @Tag(name = "Keycloak Sync", description = "Keycloak synchronization endpoints")
// @SecurityRequirement(name = "bearerAuth")
// public class KeycloakSyncController {

//     private final KeycloakSyncService keycloakSyncService;

//     /**
//      * Synchronize users from Keycloak.
//      *
//      * @param userContext current user context
//      * @return sync result
//      */
//     @PostMapping("/users")
//     @Operation(summary = "Sync users from Keycloak", description = "Trigger manual synchronization of users from Keycloak")
//     @PreAuthorize("hasRole('SYS_ADMIN')")
//     public ResponseEntity<KeycloakSyncService.SyncResult> syncUsers(UserContext userContext) {
//         log.info("Triggering user sync by user: {}", userContext.getUserId());

//         KeycloakSyncService.SyncResult result = keycloakSyncService.syncUsers();

//         log.info("User sync completed: {} synced, {} errors", result.synced(), result.errors());
//         return ResponseEntity.ok(result);
//     }

//     /**
//      * Synchronize teams from Keycloak.
//      *
//      * @param userContext current user context
//      * @return sync result
//      */
//     @PostMapping("/teams")
//     @Operation(summary = "Sync teams from Keycloak", description = "Trigger manual synchronization of teams from Keycloak")
//     @PreAuthorize("hasRole('SYS_ADMIN')")
//     public ResponseEntity<KeycloakSyncService.SyncResult> syncTeams(UserContext userContext) {
//         log.info("Triggering team sync by user: {}", userContext.getUserId());

//         KeycloakSyncService.SyncResult result = keycloakSyncService.syncTeams();

//         log.info("Team sync completed: {} synced, {} errors", result.synced(), result.errors());
//         return ResponseEntity.ok(result);
//     }

//     /**
//      * Synchronize both users and teams from Keycloak.
//      *
//      * @param userContext current user context
//      * @return combined sync result
//      */
//     @PostMapping("/all")
//     @Operation(summary = "Sync all from Keycloak", description = "Trigger full synchronization of users and teams from Keycloak")
//     @PreAuthorize("hasRole('SYS_ADMIN')")
//     public ResponseEntity<KeycloakSyncService.SyncResult> syncAll(UserContext userContext) {
//         log.info("Triggering full sync by user: {}", userContext.getUserId());

//         KeycloakSyncService.SyncResult result = keycloakSyncService.syncAll();

//         log.info("Full sync completed: {} synced, {} errors", result.synced(), result.errors());
//         return ResponseEntity.ok(result);
//     }
// }
