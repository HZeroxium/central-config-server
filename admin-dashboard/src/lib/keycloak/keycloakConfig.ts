import Keycloak from 'keycloak-js';

const keycloakConfig = {
  url: 'http://localhost:8080',
  realm: 'config-control',
  clientId: 'admin-dashboard',
};

export const keycloak = new Keycloak(keycloakConfig);

export default keycloakConfig;
