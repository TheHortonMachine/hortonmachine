/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
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
package org.hortonmachine.ssh;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

/**
 * An ssh session.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class HMSshSession implements AutoCloseable {

    private static JSch jsch = new JSch();
    private Session session;

    /**
     * Constructor without user and passowrd. 
     * 
     * <p>The pwd will be taken from the {@link SshUtilities#getPreference(String, String)} using the key {@link SshUtilities#PWD}. Same fo rthe user.
     */
    public HMSshSession() throws Exception {
        this(SshUtilities.getPreference(SshUtilities.HOST, ""), SshUtilities.getPreference(SshUtilities.USER, ""),
                SshUtilities.getPreference(SshUtilities.PWD, ""));
    }

    /**
     * Constructor.
     * 
     * @param host host for the session.
     * @param user the user.
     * @param pwd the password.
     * @throws Exception
     */
    public HMSshSession( String host, String user, String pwd ) throws Exception {
        session = jsch.getSession(user, host, 22);
        UserInfo ui = new HMUserInfo(pwd);
        session.setUserInfo(ui);
        session.setPassword(ui.getPassword().getBytes());
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect(3000);
    }

    /**
     * @return the jsch session.
     */
    public Session getSession() {
        return session;
    }

    @Override
    public void close() throws Exception {
        session.disconnect();
    }

}
