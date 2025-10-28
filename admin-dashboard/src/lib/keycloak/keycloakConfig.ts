import Keycloak from 'keycloak-js';

// Use environment variable or fallback to localhost
const keycloakUrl = import.meta.env.VITE_KEYCLOAK_URL || 'http://keycloak:8080';

console.log('Keycloak URL:', keycloakUrl);

const keycloakConfig = {
  url: keycloakUrl,
  realm: 'config-control',
  clientId: 'admin-dashboard',
  // Additional config for proper callback handling
  flow: 'standard',
  responseMode: 'fragment',
  scope: 'openid profile email',
};

export const keycloak = new Keycloak(keycloakConfig);

export default keycloakConfig;
