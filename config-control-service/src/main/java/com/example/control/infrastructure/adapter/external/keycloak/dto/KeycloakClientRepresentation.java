package com.example.control.infrastructure.adapter.external.keycloak.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * DTO representing a Keycloak Client Representation from Admin REST API.
 * <p>
 * Maps to Keycloak's ClientRepresentation structure.
 * Used for creating and updating clients.
 * </p>
 *
 * @see <a href="https://www.keycloak.org/docs-api/latest/rest-api/index.html#_clientrepresentation">Keycloak ClientRepresentation</a>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeycloakClientRepresentation {

    @JsonProperty("id")
    private String id;

    @JsonProperty("clientId")
    private String clientId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("enabled")
    private Boolean enabled;

    @JsonProperty("clientAuthenticatorType")
    private String clientAuthenticatorType;

    @JsonProperty("secret")
    private String secret;

    @JsonProperty("standardFlowEnabled")
    private Boolean standardFlowEnabled;

    @JsonProperty("implicitFlowEnabled")
    private Boolean implicitFlowEnabled;

    @JsonProperty("directAccessGrantsEnabled")
    private Boolean directAccessGrantsEnabled;

    @JsonProperty("serviceAccountsEnabled")
    private Boolean serviceAccountsEnabled;

    @JsonProperty("publicClient")
    private Boolean publicClient;

    @JsonProperty("protocol")
    private String protocol;

    @JsonProperty("attributes")
    private Map<String, String> attributes;
}

