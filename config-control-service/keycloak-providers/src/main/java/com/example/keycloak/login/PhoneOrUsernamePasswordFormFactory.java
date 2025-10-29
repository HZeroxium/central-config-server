package com.example.keycloak.login;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Collections;
import java.util.List;

/**
 * Factory for PhoneOrUsernamePasswordForm authenticator.
 * Needed by Keycloak SPI to instantiate the authenticator and expose metadata.
 */
public class PhoneOrUsernamePasswordFormFactory implements AuthenticatorFactory {
    public static final String PROVIDER_ID = "phone-or-username-password-form";

    @Override
    public Authenticator create(KeycloakSession session) {
        return new PhoneOrUsernamePasswordForm();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Phone or Username Form";
    }

    @Override
    public String getHelpText() {
        return "Allows login by phone number (normalized) or username; internally resolves phone→username.";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

    @Override
    public void init(org.keycloak.Config.Scope config) {
        // no initialization required
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // nothing here
    }

    @Override
    public void close() {
        // nothing to close
    }

    @Override
    public String getReferenceCategory() {
        return "login";
    }

    public String getDisplayTypeCategory() {
        return "Browser Forms";
    }

    @Override
    public int order() {
        return 10;
    }


    @Override
    public Requirement[] getRequirementChoices() {
        return new Requirement[]{
            Requirement.REQUIRED,
            Requirement.ALTERNATIVE,
            Requirement.DISABLED
        };
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }
}
