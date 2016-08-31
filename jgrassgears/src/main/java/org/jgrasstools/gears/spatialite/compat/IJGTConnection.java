package org.jgrasstools.gears.spatialite.compat;

public interface IJGTConnection extends AutoCloseable {
    public IJGTStatement createStatement() throws Exception;

    public boolean getAutoCommit() throws Exception;

    public void setAutoCommit( boolean b ) throws Exception;

    public void commit() throws Exception;

    public IJGTPreparedStatement prepareStatement( String sql ) throws Exception;

    public IJGTPreparedStatement prepareStatement( String sql, int returnGeneratedKeys ) throws Exception;
}
