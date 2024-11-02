package org.hortonmachine.gears.io.wcs;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.hortonmachine.gears.io.wcs.readers.CoverageReaderParameters;
import org.locationtech.jts.geom.Envelope;

public class TestIsric {

    /* working url
     * 
     * https://maps.isric.org/mapserv?map=/map/nitrogen.map
     *      &SERVICE=WCS
     *      &VERSION=2.0.1
     *      &REQUEST=GetCoverage
     *      &COVERAGEID=nitrogen_5-15cm_Q0.5
     *      &FORMAT=image/tiff
     *      &SUBSET=X(-1784000,-1140000)
     *      &SUBSET=Y(1356000,1863000)
     *      &SUBSETTINGCRS=http://www.opengis.net/def/crs/EPSG/0/152160
     */

    public static void main(String[] args) throws Exception {

        String outFolder = "C:\\Users\\hydrologis\\Desktop\\TMP\\";

        String SERVICE_URL = "https://maps.isric.org/mapserv?map=/map/nitrogen.map"; 
        String coverageId = "nitrogen_0-5cm_Q0.95";
        String name = "isric_";
        String version = null;
        int resol = 2000;

        Wcs wcs = new Wcs(SERVICE_URL, version);

        System.out.println("Version:" + wcs.version());
        System.out.println("Formats: " + wcs.formats().stream().collect(Collectors.joining(", ")));
        System.out.println("Srids: " + Arrays.toString(wcs.srids()));

        var ids = wcs.ids();
        if (ids != null){
            System.out.println("Available coverages:"); 
            for( String id : ids ) {                
                var summary = wcs.summary(id);
                System.out.println(id + ") Title: " + summary.getTitle()); 
            }
        }


        // get coverage
        if(coverageId != null) {
            // get coverage
            var parameters = wcs.getReaderParameters(coverageId);
            parameters.format("image/tiff");

            Envelope env = Wcs.evelope(12.263, -17.05, 16.763, -11.365);
            parameters.bbox(env, 4326);
            parameters.rowsCols(resol, resol);
            parameters.outputSrid(4326);
    
            File file = new File(outFolder, name + "_" + resol + ".tiff");
            var cUrl = wcs.dumpCoverage(file.getAbsolutePath(), parameters);
            
            System.out.println("Coverage url: " + cUrl);
        }

    }
}
