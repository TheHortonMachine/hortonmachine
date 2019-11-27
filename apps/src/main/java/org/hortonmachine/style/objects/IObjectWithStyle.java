package org.hortonmachine.style.objects;

import java.io.File;

public interface IObjectWithStyle {

    public void setDataFile( File dataFile, String name ) throws Exception;

    File getDataFile();

    String getName();

    String getNormalizedPath();

    boolean isVector();

    boolean isRaster();

    String getSldString() throws Exception;

    void saveSld( String xml ) throws Exception;
}
