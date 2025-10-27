package com.example.control.e2e.fixtures;

/**
 * Test user constants for E2E tests.
 * <p>
 * Provides centralized access to test user information including
 * usernames, passwords, roles, and team memberships.
 * </p>
 */
public class TestUsers {

    // Admin user
    public static final String ADMIN = "admin";
    public static final String ADMIN_PASSWORD = "admin123";
    public static final String ADMIN_ROLE = "SYS_ADMIN";
    public static final String ADMIN_TEAM = null; // Admin has no team

    // User1 - Team1 member, manager
    public static final String USER1 = "user1";
    public static final String USER1_PASSWORD = "user123";
    public static final String USER1_ROLE = "USER";
    public static final String USER1_TEAM = "team1";
    public static final String USER1_MANAGER_ID = null; // User1 is a manager

    // User2 - Team1 member, reports to user1
    public static final String USER2 = "user2";
    public static final String USER2_PASSWORD = "user123";
    public static final String USER2_ROLE = "USER";
    public static final String USER2_TEAM = "team1";
    public static final String USER2_MANAGER_ID = USER1; // Reports to user1

    // User3 - Team2 member, team lead
    public static final String USER3 = "user3";
    public static final String USER3_PASSWORD = "user123";
    public static final String USER3_ROLE = "USER";
    public static final String USER3_TEAM = "team2";
    public static final String USER3_MANAGER_ID = null; // User3 is a team lead

    // User4 - Team2 member, reports to user3
    public static final String USER4 = "user4";
    public static final String USER4_PASSWORD = "user123";
    public static final String USER4_ROLE = "USER";
    public static final String USER4_TEAM = "team2";
    public static final String USER4_MANAGER_ID = USER3; // Reports to user3

    // User5 - No team, can request ownership
    public static final String USER5 = "user5";
    public static final String USER5_PASSWORD = "user123";
    public static final String USER5_ROLE = "USER";
    public static final String USER5_TEAM = null; // No team membership
    public static final String USER5_MANAGER_ID = null; // No manager

    // Team constants
    public static final String TEAM1 = "team1";
    public static final String TEAM2 = "team2";

    // Role constants
    public static final String ROLE_SYS_ADMIN = "SYS_ADMIN";
    public static final String ROLE_USER = "USER";

    // Test data prefixes
    public static final String TEST_SERVICE_PREFIX = "e2e-test-service";
    public static final String TEST_INSTANCE_PREFIX = "e2e-test-instance";
    public static final String TEST_DRIFT_PREFIX = "e2e-test-drift";

    /**
     * Get all test usernames.
     *
     * @return array of test usernames
     */
    public static String[] getAllUsernames() {
        return new String[]{ADMIN, USER1, USER2, USER3, USER4, USER5};
    }

    /**
     * Get all team usernames (excluding admin).
     *
     * @return array of team usernames
     */
    public static String[] getTeamUsernames() {
        return new String[]{USER1, USER2, USER3, USER4, USER5};
    }

    /**
     * Get team1 usernames.
     *
     * @return array of team1 usernames
     */
    public static String[] getTeam1Usernames() {
        return new String[]{USER1, USER2}; // Restored: user2 is back in team1
    }

    /**
     * Get team2 usernames.
     *
     * @return array of team2 usernames
     */
    public static String[] getTeam2Usernames() {
        return new String[]{USER3, USER4}; // Restored: user4 is back in team2
    }

    /**
     * Get users without team membership.
     *
     * @return array of users without teams
     */
    public static String[] getUsersWithoutTeams() {
        return new String[]{ADMIN, USER5};
    }

    /**
     * Get users with managers.
     *
     * @return array of users with managers
     */
    public static String[] getUsersWithManagers() {
        return new String[]{USER2, USER4}; // Both have managers now
    }

    /**
     * Get users without managers (managers/leads).
     *
     * @return array of users without managers
     */
    public static String[] getUsersWithoutManagers() {
        return new String[]{ADMIN, USER1, USER3, USER5};
    }

    /**
     * Check if user belongs to team1.
     *
     * @param username the username
     * @return true if user belongs to team1
     */
    public static boolean isTeam1Member(String username) {
        return USER1.equals(username) || USER2.equals(username);
    }

    /**
     * Check if user belongs to team2.
     *
     * @param username the username
     * @return true if user belongs to team2
     */
    public static boolean isTeam2Member(String username) {
        return USER3.equals(username) || USER4.equals(username);
    }

    /**
     * Check if user is admin.
     *
     * @param username the username
     * @return true if user is admin
     */
    public static boolean isAdmin(String username) {
        return ADMIN.equals(username);
    }

    /**
     * Check if user has no team membership.
     *
     * @param username the username
     * @return true if user has no team
     */
    public static boolean hasNoTeam(String username) {
        return ADMIN.equals(username) || USER5.equals(username);
    }

    /**
     * Get team for user.
     *
     * @param username the username
     * @return team name or null if no team
     */
    public static String getTeamForUser(String username) {
        if (USER1.equals(username) || USER2.equals(username)) {
            return TEAM1;
        } else if (USER3.equals(username) || USER4.equals(username)) {
            return TEAM2;
        } else if (ADMIN.equals(username) || USER5.equals(username)) {
            return null;
        }
        return null;
    }

    /**
     * Get manager for user.
     *
     * @param username the username
     * @return manager username or null if no manager
     */
    public static String getManagerForUser(String username) {
        if (USER2.equals(username)) {
            return USER1; // user2's manager is user1
        } else if (USER4.equals(username)) {
            return USER3; // user4's manager is user3
        }
        return null;
    }
}
