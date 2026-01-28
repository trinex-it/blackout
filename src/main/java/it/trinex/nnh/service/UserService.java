package it.trinex.nnh.service;

import it.trinex.nnh.model.AuthAccount;
import it.trinex.nnh.model.AuthAccountType;

/**
 * Service for user management operations.
 *
 * <p>This service handles:</p>
 * <ul>
 *   <li>Creating new users</li>
 *   <li>Checking if users exist</li>
 *   <li>Managing user accounts</li>
 * </ul>
 */
public interface UserService {

    /**
     * Create a new user account.
     *
     * @param email the user's email
     * @param password the plain text password (will be encoded)
     * @param type the account type
     * @return the created auth account
     * @throws it.trinex.nnh.exception.UserAlreadyExistsException if user already exists
     */
    AuthAccount createUser(String email, String password, AuthAccountType type);

    /**
     * Create a new user account with default type (USER).
     *
     * @param email the user's email
     * @param password the plain text password (will be encoded)
     * @return the created auth account
     * @throws it.trinex.nnh.exception.UserAlreadyExistsException if user already exists
     */
    AuthAccount createUser(String email, String password);

    /**
     * Check if a user exists by email.
     *
     * @param email the email to check
     * @return true if user exists, false otherwise
     */
    boolean userExists(String email);

    /**
     * Find a user by email.
     *
     * @param email the email
     * @return the auth account, if found
     */
    java.util.Optional<AuthAccount> findByEmail(String email);
}
