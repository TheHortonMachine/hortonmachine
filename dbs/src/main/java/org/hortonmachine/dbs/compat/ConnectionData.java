package org.hortonmachine.dbs.compat;

import java.io.Serializable;

public class ConnectionData implements Serializable {
    private static final long serialVersionUID = 1L;

    public int dbType;
    public String connectionLabel;
    public String connectionUrl;
    public String user;
    public String password;
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((connectionLabel == null) ? 0 : connectionLabel.hashCode());
        result = prime * result + dbType;
        return result;
    }
    @Override
    public boolean equals( Object obj ) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConnectionData other = (ConnectionData) obj;
        if (connectionLabel == null) {
            if (other.connectionLabel != null)
                return false;
        } else if (!connectionLabel.equals(other.connectionLabel))
            return false;
        if (dbType != other.dbType)
            return false;
        return true;
    }

}
