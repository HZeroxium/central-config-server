package com.example.control.domain.exception;

import com.example.control.api.http.exception.exceptions.ConfigControlException;

/**
 * Exception thrown when a config file is not found in GitHub repository.
 *
 * @author Config Control Team
 * @since 1.0.0
 */
public class ConfigFileNotFoundException extends ConfigControlException {

    public ConfigFileNotFoundException(String message) {
        super("CONFIG_FILE_NOT_FOUND", message);
    }

    public ConfigFileNotFoundException(String message, Throwable cause) {
        super("CONFIG_FILE_NOT_FOUND", message, cause);
    }

    public ConfigFileNotFoundException(String serviceId, String profile) {
        super("CONFIG_FILE_NOT_FOUND", 
            String.format("Config file not found for service %s with profile %s", serviceId, profile));
    }
}

