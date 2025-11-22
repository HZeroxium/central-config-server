package com.example.control.infrastructure.adapter.external.keycloak.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO representing a Keycloak Client Secret response from Admin REST API.
 * <p>
 * Maps to Keycloak's client secret response structure.
 * Used for retrieving and rotating client secrets.
 * </p>
 *
 * @see <a href="https://www.keycloak.org/docs-api/latest/rest-api/index.html#_clientsecretrepresentation">Keycloak ClientSecretRepresentation</a>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeycloakClientSecretResponse {

    @JsonProperty("type")
    private String type;

    @JsonProperty("value")
    private String value;
}

