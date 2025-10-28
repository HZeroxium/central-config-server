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
    onLoad: "check-sso",
    silentCheckSsoRedirectUri:
      window.location.origin + "/silent-check-sso.html",
    checkLoginIframe: false,
  };

  return (
    <ReactKeycloakProvider
      authClient={keycloak}
      initOptions={initOptions}
      LoadingComponent={<LoadingComponent />}
    >
      {children}
    </ReactKeycloakProvider>
  );
};

export default KeycloakProvider;
