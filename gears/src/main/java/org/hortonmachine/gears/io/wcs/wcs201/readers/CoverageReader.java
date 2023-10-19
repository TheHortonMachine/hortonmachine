package org.hortonmachine.gears.io.wcs.wcs201.readers;

public class CoverageReader {
    



    public Object readCoverage(String version){
        if (version.equals("1.0.0")) {
            // TODO
        } else if (version.equals("1.1.0")) {
            return read110();
        } else if (version.equals("1.1.1")) {
            // TODO
        } else if (version.equals("2.0.0")) {
            // TODO
        }
        return null;
    }

    private Object read110() {


        return null;
    }
}
