import React from "react";
import { ReactKeycloakProvider } from "@react-keycloak/web";
import { keycloak } from "./keycloakConfig";
import { CircularProgress, Box } from "@mui/material";

interface KeycloakProviderProps {
  children: React.ReactNode;
}

const LoadingComponent: React.FC = () => (
  <Box
    display="flex"
    justifyContent="center"
    alignItems="center"
    minHeight="100vh"
  >
    <CircularProgress />
  </Box>
);

const KeycloakProvider: React.FC<KeycloakProviderProps> = ({ children }) => {
  const initOptions = {
    onLoad: "login-required",
    // Avoid iframe polling and reduce double-init issues in dev
    checkLoginIframe: false,
    pkceMethod: "S256",
    enableLogging: true,
  };

  return (
    <ReactKeycloakProvider
      authClient={keycloak}
      initOptions={initOptions}
      LoadingComponent={<LoadingComponent />}
      onEvent={(event, error) => {
        console.log('Keycloak event:', event, error);
        if (event === 'onAuthError') {
          console.error('Keycloak auth error:', error);
        }
      }}
    >
      {children}
    </ReactKeycloakProvider>
  );
};

export default KeycloakProvider;
