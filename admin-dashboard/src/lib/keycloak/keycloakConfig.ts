import Keycloak from "keycloak-js";

// Use environment variables with runtime fallback
// For Docker: use window.location.origin to support both localhost and IP access
const getKeycloakUrl = (): string => {
  // Prefer build-time environment variable
  if (import.meta.env.VITE_KEYCLOAK_URL) {
    return import.meta.env.VITE_KEYCLOAK_URL;
  }
  
  // Fallback: use current origin (works for both localhost:3000 and IP:3000)
  // This ensures Keycloak URL matches the frontend URL
  if (typeof window !== "undefined") {
    return `${window.location.protocol}//${window.location.hostname}:8080`;
  }
  
  // Server-side fallback (shouldn't happen in browser)
  return "http://localhost:8080";
};

const keycloakUrl = getKeycloakUrl();
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
  scope: "openid profile email extended_profile",
  // Enable silent check sso for better session management
  enableLogging: true,
  // Add check login iframe URL if needed
  // checkLoginIframeUrl: `${keycloakUrl}/realms/${keycloakRealm}/protocol/openid-connect/login-status-iframe.html`,
};

export const keycloak = new Keycloak(keycloakConfig);

export default keycloakConfig;
