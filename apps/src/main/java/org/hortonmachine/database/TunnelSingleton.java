package org.hortonmachine.database;

import org.hortonmachine.ssh.SshTunnelHandler;

public enum TunnelSingleton {
    INSTANCE;

    private SshTunnelHandler tunnelHandler;

    public void setTunnelObject( SshTunnelHandler tunnelHandler ) {
        if (tunnelHandler != null) {
            disconnectTunnel();
        }
        this.tunnelHandler = tunnelHandler;
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
