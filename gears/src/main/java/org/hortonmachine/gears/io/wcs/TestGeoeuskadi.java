package org.hortonmachine.gears.io.wcs;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Envelope;

public class TestGeoeuskadi {

    public static void main(String[] args) throws Exception {

        String outFolder = "C:\\Users\\hydrologis\\Desktop\\TMP\\";

        // REMOTE SENSING
        String url = "https://geo.hazi.eus/S2GEOEUSKADI_RGB/wcs";
        String coverageId = "S2GEOEUSKADI_RGB:S2A_20150818T110046_RGB";
        String name = "geo_hazi_";
        
        // LIDAR
//        String url = "https://www.geo.euskadi.eus/geoeuskadi/services/U11/WCS_KARTOGRAFIA/MapServer/WCSServer";
//        String coverageId = null;//"1";
//        String name = "geo_euskadi_";
        
        String version = "1.0.0";
        int sridInt = 25830;
        int resol = 2000;

        Wcs wcs = new Wcs(url, version);

        System.out.println(wcs.version());

        var ids = wcs.ids();
        if (ids != null){
            System.out.println("Available coverages:"); 
            for( String id : ids ) {                
                var summary = wcs.summary(id);
                System.out.println(id + ") Title: " + summary.getTitle()); 
                var describe = wcs.describe(id);
                System.out.println("\t" + "Formats: " + describe.getSupportedFormats().stream().collect(Collectors.joining(", ")));
                System.out.println("\t" + "Srids: " + Arrays.toString(describe.getSupportedSrids()));
            }
        }

        if(coverageId != null) {
            // get coverage
            var parameters = wcs.getReaderParameters(coverageId);
            Envelope env = Wcs.evelope(504787.0, 4734309.0, 536275.0, 4754145.0);
            parameters.bbox(env, sridInt);
            parameters.rowsCols(resol, resol);
            parameters.useExtendedAxisUrl(false);
            parameters.format("GeoTIFF");
    
            File file = new File(outFolder, name + "_" + resol + ".tiff");
            var cUrl = wcs.dumpCoverage(file.getAbsolutePath(), parameters);
            
            System.out.println("Coverage url: " + cUrl);
        }

    }
}
