package org.hortonmachine.dbs.nosql;

import java.util.List;

public interface INosqlCollection {

    public List<INosqlDocument> find( String query, int limit );

    public INosqlDocument getFirst();

    public long getCount();

    public void drop();

    void insert( INosqlDocument document );

    void insert( String documentJson );

    void deleteByOid( String oid );

    void updateByOid( String oid, String documentJson );

}
