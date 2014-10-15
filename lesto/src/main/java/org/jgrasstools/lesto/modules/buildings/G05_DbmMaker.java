package org.jgrasstools.lesto.modules.buildings;

import java.io.File;
import java.io.FilenameFilter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.modules.r.scanline.OmsScanLineRasterizer;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

public class G05_DbmMaker extends JGTModel {

    public G05_DbmMaker() throws Exception {

        String pattern = "fino";
        String inBuildingsPath = "/media/BLACKSUN/geologico/" + pattern + "1400final/buildings_" + pattern
                + "1400_final_matched.shp";
        String ascFolderPath = "/media/BIGBOSS/geologico_2013/dati_originali/consegna_2011_05_27_" + pattern
                + "_1400m/Prodotti_quota_ortometrica/ASCII_GRID_1x1/DTM/";
        String outDBMFolderPath = "/media/BLACKSUN/geologico/dbm/" + pattern + "1400/";
        File outFolder = new File(outDBMFolderPath);

        File[] ascFiles = new File(ascFolderPath).listFiles(new FilenameFilter(){
            public boolean accept( File dir, String name ) {
                return name.endsWith("asc");
            }
        });

        SimpleFeatureCollection buildings = getVector(inBuildingsPath);

        for( File ascFile : ascFiles ) {
            pm.message("************************************");
            pm.message("PROCESSING: " + ascFile.getName());
            pm.message("************************************");
            File outFile = new File(outFolder, ascFile.getName());
            if (outFile.exists()) {
                continue;
            }

            GridCoverage2D dtm = getRaster(ascFile.getAbsolutePath());

            OmsScanLineRasterizer r = new OmsScanLineRasterizer();
            r.inRaster = dtm;
            r.inVector = buildings;
            r.fCat = "MAS_ELEV";
            r.process();
            GridCoverage2D buildingsGC = r.outRaster;

            // dumpRaster(buildingsGC, outFile.getAbsolutePath());
            GridCoverage2D mergedGC = CoverageUtilities.mergeCoverages(buildingsGC, dtm);
            dumpRaster(mergedGC, outFile.getAbsolutePath());
        }

    }
    public static void main( String[] args ) throws Exception {
        new G05_DbmMaker();

    }

}
