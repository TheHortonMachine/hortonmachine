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
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * An ssh tunneling manager.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class SshTunnelHandler implements AutoCloseable {
    private Session tunnelingSession;

    private SshTunnelHandler( Session tunnelingSession ) {
        this.tunnelingSession = tunnelingSession;
    }

    /**
     * Open a tunnel to the remote host.
     * 
     * @param remoteHost the host to connect to (where ssh will login).
     * @param remoteSshUser the ssh user.
     * @param remoteSshPwd the ssh password.
     * @param localPort the local port to use for the port forwarding (usually the same as the remote).
     * @param remotePort the remote port to use.
     * @return the tunnel manager, used also to disconnect when necessary.
     * @throws JSchException
     */
    public static SshTunnelHandler openTunnel( String remoteHost, String remoteSshUser, String remoteSshPwd, int localPort,
            int remotePort ) throws JSchException {
        int port = 22;
        JSch jsch = new JSch();
        String sshKeyPath = SshUtilities.getPreference(SshUtilities.KEYPATH, "");
        if (sshKeyPath.trim().length() > 0) {
            jsch.addIdentity(sshKeyPath);
        }
        String sshKeyPassphrase = SshUtilities.getPreference(SshUtilities.KEYPASSPHRASE, null);

        Session tunnelingSession = jsch.getSession(remoteSshUser, remoteHost, port);
        tunnelingSession.setPassword(remoteSshPwd);
        HMUserInfo lui = new HMUserInfo("", sshKeyPassphrase);
        tunnelingSession.setUserInfo(lui);
        tunnelingSession.setConfig("StrictHostKeyChecking", "no");
        tunnelingSession.setPortForwardingL(localPort, "localhost", remotePort);
        tunnelingSession.connect();
        tunnelingSession.openChannel("direct-tcpip");
        return new SshTunnelHandler(tunnelingSession);
    }

    @Override
    public void close() throws Exception {
        if (tunnelingSession != null)
            tunnelingSession.disconnect();
    }

}
