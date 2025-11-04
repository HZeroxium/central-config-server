package com.example.control.infrastructure.adapter.external.keycloak.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * DTO representing a Keycloak Group Representation from Admin REST API.
 * <p>
 * Maps to Keycloak's GroupRepresentation structure.
 * Only includes fields relevant for IAM team mapping.
 * </p>
 *
 * @see <a href="https://www.keycloak.org/docs-api/latest/rest-api/index.html#_grouprepresentation">Keycloak GroupRepresentation</a>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeycloakGroupRepresentation {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("path")
    private String path;

    @JsonProperty("attributes")
    private Map<String, List<String>> attributes;
}

