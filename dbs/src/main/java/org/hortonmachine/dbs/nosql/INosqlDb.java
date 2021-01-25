package org.hortonmachine.dbs.nosql;

import java.util.List;

import org.hortonmachine.dbs.compat.ConnectionData;
import org.hortonmachine.dbs.compat.EDb;

public interface INosqlDb extends AutoCloseable {

    EDb getType();

    void setCredentials( String user, String password );

    ConnectionData getConnectionData();

    boolean open( String dbPath ) throws Exception;

    void close() throws Exception;

    String[] getDbInfo();

    String getDbEngineUrl();

    String getDbName();

    List<String> getDatabasesNames();

    List<String> getCollections( boolean doOrder ) throws Exception;

    boolean hasCollection( String name ) throws Exception;

    public INosqlCollection getCollection( String name );

    void createCollection( String newName );

    void drop();

}