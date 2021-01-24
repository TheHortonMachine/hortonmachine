package org.hortonmachine.dbs.nosql;

import java.util.List;

public interface INosqlDocument {

    public String toJson();

    public List<String[]> getFirstLevelKeysAndTypes();

}
