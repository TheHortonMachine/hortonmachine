package org.hortonmachine.dbs.compat;

import java.io.Serializable;

public class ConnectionData implements Serializable {
    private static final long serialVersionUID = 1L;

    public int dbType;
    public String connectionLabel;
    public String connectionUrl;
    public String user;
    public String password;
    
}
