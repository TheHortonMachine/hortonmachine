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

import org.hortonmachine.dbs.log.Logger;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.ProxyHTTP;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

/**
 * An ssh session.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class HMSshSession implements AutoCloseable {
    // THESE NEED TO BE KEPT IN LINE WITH THOSE IN
    // org.hortonmachine.gears.utils.PreferencesHandler.*
    public static final String HM_PREF_PROXYPWD = "hm_pref_proxypwd";
    public static final String HM_PREF_PROXYUSER = "hm_pref_proxyuser";
    public static final String HM_PREF_PROXYPORT = "hm_pref_proxyport";
    public static final String HM_PREF_PROXYHOST = "hm_pref_proxyhost";
    public static final String HM_PREF_PROXYCHECK = "hm_pref_proxycheck";

    private static JSch jsch = new JSch();
    private Session session;

    /**
     * Constructor without user and passowrd. 
     * 
     * <p>The pwd will be taken from the {@link SshUtilities#getPreference(String, String)} using the key {@link SshUtilities#PWD}. Same for the user.
     */
    public HMSshSession() throws Exception {
        this(SshUtilities.getPreference(SshUtilities.HOST, ""),
                Integer.parseInt(SshUtilities.getPreference(SshUtilities.PORT, "22")),
                SshUtilities.getPreference(SshUtilities.USER, ""), SshUtilities.getPreference(SshUtilities.PWD, ""));
    }

    /**
     * Constructor.
     * 
     * @param host host for the session.
     * @param user the user.
     * @param pwd the password.
     * @throws Exception
     */
    public HMSshSession( String host, int port, String user, String pwd ) throws Exception {

        String sshKeyPath = SshUtilities.getPreference(SshUtilities.KEYPATH, "");
        if (sshKeyPath.trim().length() > 0) {
            jsch.addIdentity(sshKeyPath);
        }
        String sshKeyPassphrase = SshUtilities.getPreference(SshUtilities.KEYPASSPHRASE, null);

        session = jsch.getSession(user, host, port);
        UserInfo ui = new HMUserInfo(pwd, sshKeyPassphrase);
        session.setUserInfo(ui);
        if (pwd != null && pwd.length() > 0)
            session.setPassword(ui.getPassword().getBytes());
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);

        try {
            String doProxy = SshUtilities.getPreference(HM_PREF_PROXYCHECK, "false");
            if (Boolean.parseBoolean(doProxy)) {
                String proxyHost = SshUtilities.getPreference(HM_PREF_PROXYHOST, "");
                String proxyPort = SshUtilities.getPreference(HM_PREF_PROXYPORT, "");
                String proxyUser = SshUtilities.getPreference(HM_PREF_PROXYUSER, "");
                String proxyPwd = SshUtilities.getPreference(HM_PREF_PROXYPWD, "");

                int proxPort = Integer.parseInt(proxyPort);
                ProxyHTTP proxyHTTP = new ProxyHTTP(proxyHost, proxPort);
                if (proxyUser.length() > 0 && proxyPwd.length() > 0) {
                    proxyHTTP.setUserPasswd(proxyUser, proxyPwd);
                }
                session.setProxy(proxyHTTP);
            }
        } catch (Exception e) {
            Logger.INSTANCE.insertError("HMSshSession", "Error setting proxy", e);
        }

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
