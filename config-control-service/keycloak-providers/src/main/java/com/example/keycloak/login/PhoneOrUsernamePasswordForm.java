package com.example.keycloak.login;

import com.example.keycloak.util.PhoneNumberUtil;
import jakarta.ws.rs.core.MultivaluedMap;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Custom authenticator extending Keycloak's UsernamePasswordForm to allow
 * login via either username or phone number. If input looks like phone number,
 * this class resolves the actual username (via attribute lookup) then proceeds
 * with default username+password authentication logic.
 */
public class PhoneOrUsernamePasswordForm extends UsernamePasswordForm {
    private static final Logger log = LoggerFactory.getLogger(PhoneOrUsernamePasswordForm.class);
    // The user attribute key under which phone numbers are stored in Keycloak
    private static final String PHONE_ATTRIBUTE = "phone";

    /**
     * Override validateForm so we intercept the submitted username field.
     * If it is a phone number, we attempt to lookup user by phone attribute
     * and rewrite the username field accordingly.
     *
     * @param context  AuthenticationFlowContext
     * @param formData submitted form parameters
     * @return true if further validation (super) passes, false otherwise
     */
    @Override
    protected boolean validateForm(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        String rawInput = formData.getFirst("username");
        if (rawInput != null) {
            String trimmed = rawInput.trim();
            if (PhoneNumberUtil.isPhone(trimmed)) {
                String normalized = PhoneNumberUtil.normalize(trimmed);
                log.info("Detected phone login attempt: {} → normalized {}", trimmed, normalized);
                UserModel foundUser = findByPhoneVariants(context, normalized);
                if (foundUser != null) {
                    String actualUsername = foundUser.getUsername();
                    log.info("Resolved phone {} to username {}", normalized, actualUsername);
                    // replace username in formData so default logic uses actual username
                    formData.putSingle("username", actualUsername);
                    // optional: set the user to context to shortcut
                    context.setUser(foundUser);
                } else {
                    log.warn("No user found for phone number {}", normalized);
                }
            } else {
                log.debug("Input appears to be username: {}", trimmed);
            }
        }

        // Proceed with normal UsernamePasswordForm validation (password check etc.)
        return super.validateForm(context, formData);
    }

    /**
     * Attempt to find a user by phone attribute, checking multiple possible stored variants
     * (E.164 format, local “0…” format, “84…” format).
     *
     * @param ctx          authentication flow context
     * @param e164Variant  normalized phone number in E.164 format
     * @return UserModel if found, else null
     */
    private UserModel findByPhoneVariants(AuthenticationFlowContext ctx, String e164Variant) {
        RealmModel realm = ctx.getRealm();
        KeycloakSession session = ctx.getSession();

        // Derive possible formats stored in attribute
        String local0 = null;
        if (e164Variant.startsWith("+84")) {
            local0 = "0" + e164Variant.substring(3);
        }
        String noPlus = e164Variant.startsWith("+") ? e164Variant.substring(1) : null;

        Stream<String> variants = Stream.of(e164Variant, local0, noPlus)
                .filter(Objects::nonNull);

        for (String v : (Iterable<String>) variants::iterator) {
            Stream<UserModel> users = session.users().searchForUserByUserAttributeStream(realm, PHONE_ATTRIBUTE, v);
            UserModel user = users.findFirst().orElse(null);
            if (user != null) {
                return user;
            }
        }
        return null;
    }
}
