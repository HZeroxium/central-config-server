package com.example.control.infrastructure.adapter.external.keycloak.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * DTO representing a Keycloak User Representation from Admin REST API.
 * <p>
 * Maps to Keycloak's UserRepresentation structure.
 * Only includes fields relevant for IAM user mapping.
 * </p>
 *
 * @see <a href="https://www.keycloak.org/docs-api/latest/rest-api/index.html#_userrepresentation">Keycloak UserRepresentation</a>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeycloakUserRepresentation {

    @JsonProperty("id")
    private String id;

    @JsonProperty("username")
    private String username;

    @JsonProperty("email")
    private String email;

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("enabled")
    private Boolean enabled;

    @JsonProperty("emailVerified")
    private Boolean emailVerified;

    @JsonProperty("attributes")
    private Map<String, List<String>> attributes;

    @JsonProperty("realmAccess")
    private RealmAccess realmAccess;

    @JsonProperty("groups")
    private List<String> groups;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RealmAccess {
        @JsonProperty("roles")
        private List<String> roles;
    }
}

