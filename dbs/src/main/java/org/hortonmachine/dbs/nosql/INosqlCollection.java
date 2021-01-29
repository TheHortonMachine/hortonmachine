package org.hortonmachine.dbs.nosql;

import java.util.HashMap;
import java.util.List;

import org.hortonmachine.dbs.compat.GeometryColumn;

public interface INosqlCollection {

    public List<INosqlDocument> find( String query, int limit );

    public INosqlDocument getFirst();

    public long getCount();

    public void drop();

    void insert( INosqlDocument document );

    void insert( String documentJson );

    void deleteByOid( String oid );

    void updateByOid( String oid, String documentJson );

    HashMap<String, GeometryColumn> getSpatialIndexes();

    String getName();

}
