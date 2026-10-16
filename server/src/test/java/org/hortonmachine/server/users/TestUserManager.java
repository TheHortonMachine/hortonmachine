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

import java.io.File;
import java.nio.file.Files;

import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.server.ServerConfig;
import org.hortonmachine.server.db.ServerDb;

import junit.framework.TestCase;

/**
 * Tests of the users.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestUserManager extends TestCase {

    private File tmpFolder;
    private ServerDb db;
    private UserManager users;

    @Override
    protected void setUp() throws Exception {
        tmpFolder = Files.createTempDirectory("hm-users-test").toFile();
        ServerConfig config = new ServerConfig();
        config.workspace = tmpFolder;
        db = ServerDb.open(config);
        users = new UserManager(db);
    }

    @Override
    protected void tearDown() throws Exception {
        db.close();
        FileUtilities.deleteFileOrDir(tmpFolder);
    }

    private static char[] pwd( String password ) {
        return password.toCharArray();
    }

    public void testPasswordHasher() {
        String hash = PasswordHasher.hash(pwd("secret-password"));
        assertTrue(hash.startsWith("pbkdf2-sha512:" + PasswordHasher.ITERATIONS + ":"));
        assertTrue(PasswordHasher.verify(pwd("secret-password"), hash));
        assertFalse(PasswordHasher.verify(pwd("secret-passwore"), hash));
        // the salt makes the hashes differ
        assertFalse(hash.equals(PasswordHasher.hash(pwd("secret-password"))));
        assertFalse(PasswordHasher.verify(pwd("secret-password"), "plain"));
        assertFalse(PasswordHasher.verify(pwd("secret-password"), "pbkdf2-sha512:x:y:z"));
    }

    public void testCreateAndLogin() throws Exception {
        assertFalse(users.hasUsers());
        User admin = users.createUser(" Admin ", pwd("admin-password"), Role.ADMIN);
        assertEquals("admin", admin.username);
        assertTrue(admin.isAdmin());
        User user = users.createUser("jane.doe", pwd("jane-password"), Role.USER);
        assertTrue(users.hasUsers());
        assertEquals(2, users.listUsers().size());
        assertEquals("admin", users.listUsers().get(0).username);

        assertWrongUser("ab", "long-enough", "3 to 64");
        assertWrongUser("jane doe", "long-enough", "3 to 64");
        assertWrongUser("JANE.DOE", "long-enough", "already taken");
        assertWrongUser("john", "short", "at least 8");

        // the name is not case sensitive
        assertEquals(admin.id, users.login("ADMIN", pwd("admin-password")).id);
        assertNull(users.login("admin", pwd("wrong-password")));
        assertNull(users.login("nobody", pwd("admin-password")));

        users.changePassword(user.id, pwd("new-password"));
        assertNull(users.login("jane.doe", pwd("jane-password")));
        assertEquals(user.id, users.login("jane.doe", pwd("new-password")).id);

        // disabled users can't log in
        users.updateUser(user.id, Role.USER, true);
        assertNull(users.login("jane.doe", pwd("new-password")));
        assertTrue(users.getUser(user.id).disabled);
    }

    private void assertWrongUser( String username, String password, String error ) throws Exception {
        try {
            users.createUser(username, pwd(password), Role.USER);
            fail("Created: " + username);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(error));
        }
    }

    public void testLoginLock() throws Exception {
        users.createUser("jane", pwd("jane-password"), Role.USER);
        for( int i = 0; i < UserManager.MAX_FAILED_LOGINS; i++ ) {
            assertNull(users.login("jane", pwd("wrong-password")));
        }
        // now even the right password is refused for a while
        try {
            users.login("jane", pwd("jane-password"));
            fail();
        } catch (LoginLockedException e) {
            assertTrue(e.getMinutes() > 0 && e.getMinutes() <= UserManager.LOCK_MINUTES);
        }
        // other names are not locked
        users.createUser("john", pwd("john-password"), Role.USER);
        assertNotNull(users.login("john", pwd("john-password")));
    }

    public void testLastAdmin() throws Exception {
        User admin = users.createUser("admin", pwd("admin-password"), Role.ADMIN);
        User user = users.createUser("jane", pwd("jane-password"), Role.USER);

        assertLastAdmin(() -> users.updateUser(admin.id, Role.USER, false));
        assertLastAdmin(() -> users.updateUser(admin.id, Role.ADMIN, true));
        assertLastAdmin(() -> users.deleteUser(admin.id));

        // with a second administrator it can
        users.updateUser(user.id, Role.ADMIN, false);
        users.updateUser(admin.id, Role.USER, false);
        assertFalse(users.getUser(admin.id).isAdmin());
        users.deleteUser(admin.id);
        assertNull(users.getUser(admin.id));
        assertEquals(1, users.listUsers().size());
    }

    private interface Action {
        void run() throws Exception;
    }

    private static void assertLastAdmin( Action action ) throws Exception {
        try {
            action.run();
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("last administrator"));
        }
    }
}
