import Keycloak from "keycloak-js";

// Use environment variables with fallbacks
const keycloakUrl = import.meta.env.VITE_KEYCLOAK_URL || "http://keycloak:8080";
const keycloakRealm = import.meta.env.VITE_KEYCLOAK_REALM || "config-control";
const keycloakClientId =
  import.meta.env.VITE_KEYCLOAK_CLIENT_ID || "admin-dashboard";

console.log("Keycloak Configuration:", {
  url: keycloakUrl,
  realm: keycloakRealm,
  clientId: keycloakClientId,
});

const keycloakConfig = {
  url: keycloakUrl,
  realm: keycloakRealm,
  clientId: keycloakClientId,
  // Additional config for proper callback handling
  flow: "standard",
  responseMode: "fragment",
  scope: "openid profile email",
};

export const keycloak = new Keycloak(keycloakConfig);

export default keycloakConfig;
