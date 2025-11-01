package com.example.control.domain.valueobject.id;

import java.io.Serializable;
import java.util.Objects;

/**
 * Value object representing an identifier for IamTeam.
 * <p>
 * Wraps a single String ID for type safety and consistency with other ID types.
 * </p>
 *
 * @param teamId the IAM team identifier
 */
public record IamTeamId(String teamId) implements Serializable {

    /**
     * Compact constructor with validation.
     *
     * @param teamId the IAM team identifier
     * @throws IllegalArgumentException if teamId is null or blank
     */
    public IamTeamId {
        Objects.requireNonNull(teamId, "IAM team ID cannot be null");

        if (teamId.isBlank()) {
            throw new IllegalArgumentException("IAM team ID cannot be blank");
        }
    }

    /**
     * Factory method for creating IamTeamId.
     *
     * @param teamId the IAM team identifier
     * @return a new IamTeamId
     */
    public static IamTeamId of(String teamId) {
        return new IamTeamId(teamId);
    }
}
