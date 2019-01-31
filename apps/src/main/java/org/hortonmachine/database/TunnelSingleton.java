package org.hortonmachine.database;

import org.hortonmachine.ssh.SshTunnelHandler;

import com.jcraft.jsch.JSchException;

public enum TunnelSingleton {
    INSTANCE;

    private SshTunnelHandler tunnelHandler;

    public void setTunnelObject( String remoteHost, String remoteSshUser, String remoteSshPwd, int localPort, int remotePort )
            throws JSchException {
        if (this.tunnelHandler != null) {
            disconnectTunnel();
        }
        this.tunnelHandler = SshTunnelHandler.openTunnel(remoteHost, remoteSshUser, remoteSshPwd, localPort, remotePort);
    }

    public void disconnectTunnel() {
        if (tunnelHandler != null)
            try {
                tunnelHandler.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
    }
}
