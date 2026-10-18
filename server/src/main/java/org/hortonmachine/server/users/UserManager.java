/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
 *
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.server.users;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.hortonmachine.server.db.ServerDb;

/**
 * The users of the server: creation, login and changes.
 *
 * <p>After {@value #MAX_FAILED_LOGINS} failed logins of a user name in {@value #LOCK_MINUTES} minutes,
 * its logins are refused until the oldest failure is older than that, to slow down the guessing of passwords.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class UserManager {
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_FAILED_LOGINS = 5;
    public static final int LOCK_MINUTES = 15;
    /** The start of the secrets of the tokens of the api, to recognize them. */
    public static final String TOKEN_PREFIX = "hm_";
    public static final int MAX_TOKEN_NAME_LENGTH = 100;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String USERNAME_PATTERN = "[a-z0-9._-]{3,64}";
    private static final int MAX_TRACKED_NAMES = 1000;

    /** Checked when the user doesn't exist, to take the same time as for a wrong password. */
    private static final String DUMMY_HASH = PasswordHasher.hash("dummy-password".toCharArray());

    private final ServerDb db;
    private final Map<String, Deque<Long>> failedLogins = new HashMap<>();
    private final Map<String, Long> sessionsValidFrom = new ConcurrentHashMap<>();

    public UserManager( ServerDb db ) {
        this.db = db;
    }

    /**
     * @return the name in lower case and trimmed, as stored.
     */
    public static String normalizeUsername( String username ) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Create a user.
     *
     * @param username the name, 3 to 64 letters, digits, dots, dashes or underscores; stored in lower case.
     * @param password the password, at least {@value #MIN_PASSWORD_LENGTH} characters.
     * @param role the role.
     * @return the user.
     * @throws IllegalArgumentException if the name or the password are not valid or the name is taken.
     */
    public synchronized User createUser( String username, char[] password, Role role ) throws Exception {
        String name = normalizeUsername(username);
        if (!name.matches(USERNAME_PATTERN)) {
            throw new IllegalArgumentException("The user name must have 3 to 64 letters, digits, dots, dashes or underscores.");
        }
        checkPassword(password);
        if (db.getUserByName(name) != null) {
            throw new IllegalArgumentException("The user name is already taken: " + name);
        }
        User user = new User(UUID.randomUUID().toString(), name, role, System.currentTimeMillis(), false);
        db.insertUser(user, PasswordHasher.hash(password));
        return user;
    }

    private static void checkPassword( char[] password ) {
        if (password == null || password.length < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("The password must have at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
    }

    /**
     * Check the credentials of a user logging in.
     *
     * @param username the name.
     * @param password the password.
     * @return the user, or <code>null</code> if the name or the password are wrong or the user is disabled.
     * @throws LoginLockedException if too many logins of the name failed recently.
     */
    public User login( String username, char[] password ) throws Exception {
        String name = normalizeUsername(username);
        long now = System.currentTimeMillis();
        synchronized (failedLogins) {
            Deque<Long> failures = recentFailures(name, now);
            if (failures.size() >= MAX_FAILED_LOGINS) {
                long unlock = failures.peekFirst() + TimeUnit.MINUTES.toMillis(LOCK_MINUTES);
                throw new LoginLockedException((unlock - now) / 60000 + 1);
            }
            if (failures.isEmpty()) {
                failedLogins.remove(name);
            }
        }

        User user = db.getUserByName(name);
        String hash = user != null ? db.getPasswordHash(user.id) : null;
        boolean valid = PasswordHasher.verify(password, hash != null ? hash : DUMMY_HASH) && hash != null;
        if (valid && !user.disabled) {
            synchronized (failedLogins) {
                failedLogins.remove(name);
            }
            return user;
        }
        synchronized (failedLogins) {
            recentFailures(name, now).addLast(now);
            if (failedLogins.size() > MAX_TRACKED_NAMES) {
                // forget the names without recent failures, not to grow with the names tried
                failedLogins.keySet().removeIf(n -> !n.equals(name) && recentFailures(n, now).isEmpty());
            }
        }
        return null;
    }

    private Deque<Long> recentFailures( String name, long now ) {
        Deque<Long> failures = failedLogins.computeIfAbsent(name, n -> new ArrayDeque<>());
        long limit = now - TimeUnit.MINUTES.toMillis(LOCK_MINUTES);
        while( !failures.isEmpty() && failures.peekFirst() < limit ) {
            failures.removeFirst();
        }
        return failures;
    }

    /**
     * @return the user, or <code>null</code> if it doesn't exist.
     */
    public User getUser( String userId ) throws Exception {
        return db.getUser(userId);
    }

    /**
     * @return the users, by name.
     */
    public List<User> listUsers() throws Exception {
        return db.listUsers();
    }

    /**
     * @return <code>true</code> if there is at least one user.
     */
    public boolean hasUsers() throws Exception {
        return !db.listUsers().isEmpty();
    }

    /**
     * Change the password of a user. The sessions of the user logged in before end.
     *
     * @return the time of the change: the sessions logged in before it are not valid anymore.
     * @throws IllegalArgumentException if the password is not valid.
     */
    public long changePassword( String userId, char[] password ) throws Exception {
        checkPassword(password);
        db.updatePasswordHash(userId, PasswordHasher.hash(password));
        long now = System.currentTimeMillis();
        sessionsValidFrom.put(userId, now);
        return now;
    }

    /**
     * @return the time from which the sessions of a user are valid, 0 for all. Kept in memory only,
     *         as the sessions.
     */
    public long getSessionsValidFrom( String userId ) {
        return sessionsValidFrom.getOrDefault(userId, 0L);
    }

    /**
     * Change the role of a user, or enable or disable it.
     *
     * @throws IllegalArgumentException if the user doesn't exist, or it is the last enabled administrator
     *         and would not be anymore.
     */
    public synchronized User updateUser( String userId, Role role, boolean disabled ) throws Exception {
        User user = getExistingUser(userId);
        if ((role != Role.ADMIN || disabled) && isLastAdmin(user)) {
            throw new IllegalArgumentException("The last administrator can't be changed to a user or disabled.");
        }
        User updated = new User(user.id, user.username, role, user.created, disabled);
        db.updateUser(updated);
        return updated;
    }

    /**
     * Delete a user. Its jobs stay, for the administrators.
     *
     * @throws IllegalArgumentException if the user doesn't exist or it is the last enabled administrator.
     */
    public synchronized void deleteUser( String userId ) throws Exception {
        User user = getExistingUser(userId);
        if (isLastAdmin(user)) {
            throw new IllegalArgumentException("The last administrator can't be deleted.");
        }
        db.deleteTokensOfUser(userId);
        db.deleteUser(userId);
    }

    /**
     * Create a token of the api for a user.
     *
     * @param userId the user.
     * @param name a name to recognize the token, as "qgis".
     * @return the secret of the token, to give to the user: it is not kept and can't be shown again.
     * @throws IllegalArgumentException if the name is empty or too long.
     */
    public String createToken( String userId, String name ) throws Exception {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty() || cleanName.length() > MAX_TOKEN_NAME_LENGTH) {
            throw new IllegalArgumentException("The name of the token must have 1 to " + MAX_TOKEN_NAME_LENGTH + " characters.");
        }
        getExistingUser(userId);
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String secret = TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        db.insertToken(new ApiToken(UUID.randomUUID().toString(), userId, cleanName, System.currentTimeMillis(), null),
                hashToken(secret));
        return secret;
    }

    /**
     * Find a token of the api by its secret.
     *
     * @param secret the secret of the token, as sent by the client.
     * @return the token, or <code>null</code> if it doesn't exist or its user is disabled.
     */
    public ApiToken authenticateToken( String secret ) throws Exception {
        if (secret == null || !secret.startsWith(TOKEN_PREFIX)) {
            return null;
        }
        return usableToken(db.getTokenByHash(hashToken(secret)));
    }

    /**
     * Find a token of the api by its id, as in a signed link.
     *
     * @param tokenId the id of the token.
     * @return the token, or <code>null</code> if it doesn't exist (anymore) or its user is disabled.
     */
    public ApiToken authenticateTokenId( String tokenId ) throws Exception {
        return tokenId != null ? usableToken(db.getToken(tokenId)) : null;
    }

    private ApiToken usableToken( ApiToken token ) throws Exception {
        if (token == null) {
            return null;
        }
        User user = db.getUser(token.userId);
        if (user == null || user.disabled) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (token.lastUsed == null || now - token.lastUsed > TimeUnit.MINUTES.toMillis(1)) {
            // not at every request
            db.updateTokenLastUsed(token.id, now);
        }
        return token;
    }

    /**
     * @return the tokens of a user, newest first.
     */
    public List<ApiToken> listTokens( String userId ) throws Exception {
        return db.listTokens(userId);
    }

    /**
     * Revoke a token of a user.
     *
     * @return <code>false</code> if the user has no such token.
     */
    public boolean revokeToken( String userId, String tokenId ) throws Exception {
        return db.deleteToken(userId, tokenId);
    }

    private static String hashToken( String secret ) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for( byte b : hash ) {
            hex.append(String.format(Locale.ROOT, "%02x", b));
        }
        return hex.toString();
    }

    private User getExistingUser( String userId ) throws Exception {
        User user = db.getUser(userId);
        if (user == null) {
            throw new IllegalArgumentException("The user doesn't exist: " + userId);
        }
        return user;
    }

    private boolean isLastAdmin( User user ) throws Exception {
        if (!user.isAdmin() || user.disabled) {
            return false;
        }
        for( User other : db.listUsers() ) {
            if (!other.id.equals(user.id) && other.isAdmin() && !other.disabled) {
                return false;
            }
        }
        return true;
    }
}
