package org.hortonmachine.dbs.nosql;

import java.util.List;

public interface INosqlDocument {

    public String toJson();

    public List<String[]> getFirstLevelKeysAndTypes();

    public <T> T adapt( Class<T> adaptee );

}
