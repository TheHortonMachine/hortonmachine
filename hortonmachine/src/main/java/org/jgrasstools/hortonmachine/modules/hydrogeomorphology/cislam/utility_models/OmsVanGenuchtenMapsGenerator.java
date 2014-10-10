/*
 * This file is part of the "CI-slam module": an addition to JGrassTools
 * It has been entirely contributed by Marco Foi (www.mcfoi.it)
 * 
 * "CI-slam module" is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.utility_models;

import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_SUBMODULES_LABEL;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_LICENSE;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_OMSVANGENUCHMAPGEN_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_OMSVANGENUCHMAPGEN_KEYWORDS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_OMSVANGENUCHMAPGEN_NAME;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_OMSVANGENUCHMAPGEN_STATUS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_inGeo_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_outAlfaVanGen_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_outCohesion_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_outGamma_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_outKsat_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_outNVanGen_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_outPhi_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_outTheta_r_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_outTheta_s_DESCRIPTION;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Hashtable;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Out;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.GridNode;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Description(OMSCISLAM_OMSVANGENUCHMAPGEN_DESCRIPTION)
@Author(name = OMSCISLAM_AUTHORNAMES, contact = OMSCISLAM_AUTHORCONTACTS)
@Keywords(OMSCISLAM_OMSVANGENUCHMAPGEN_KEYWORDS)
@Label(OMSCISLAM_SUBMODULES_LABEL)
@Name(OMSCISLAM_OMSVANGENUCHMAPGEN_NAME)
@Status(OMSCISLAM_OMSVANGENUCHMAPGEN_STATUS)
@License(OMSCISLAM_LICENSE)
public class OmsVanGenuchtenMapsGenerator extends JGTModel {

    @Description(OMSCISLAM_inGeo_DESCRIPTION)
    @In
    public GridCoverage2D inGeo = null;

    @Description(OMSCISLAM_outCohesion_DESCRIPTION)
    @Out
    public GridCoverage2D outCohesion = null;

    @Description(OMSCISLAM_outPhi_DESCRIPTION)
    @Out
    public GridCoverage2D outPhi = null;

    @Description(OMSCISLAM_outGamma_DESCRIPTION)
    @Out
    public GridCoverage2D outGamma = null;

    @Description(OMSCISLAM_outKsat_DESCRIPTION)
    @Out
    public GridCoverage2D outKsat = null;

    @Description(OMSCISLAM_outTheta_s_DESCRIPTION)
    @Out
    public GridCoverage2D outTheta_s = null;

    @Description(OMSCISLAM_outTheta_r_DESCRIPTION)
    @Out
    public GridCoverage2D outTheta_r = null;

    @Description(OMSCISLAM_outAlfaVanGen_DESCRIPTION)
    @Out
    public GridCoverage2D outAlfaVanGen = null;

    @Description(OMSCISLAM_outNVanGen_DESCRIPTION)
    @Out
    public GridCoverage2D outNVanGen = null;

    // TODO Replace custom DEVELOPMENT class with default framework one before pulling into main JGrassTools repository
    // private HortonMessageHandler msg = HortonMessageHandler.getInstance();
    private HortonMessageHandler msg = HortonMessageHandler.getInstance();
    
    @Execute
    public void process() {
        if (!concatOr(outCohesion == null, outPhi == null, outGamma == null, outKsat == null, outTheta_s == null,
                outTheta_r == null, outAlfaVanGen == null, outNVanGen == null)) {
            return;
        }
        checkNull(inGeo);

        Hashtable<Integer, Double[]> vanGenchtenTable = new Hashtable<Integer, Double[]>(11, 1);
        /*
         * This table was derived from the R script written y Cristiano Lanni to develop and test the model on the Pizzano basin
         * so it is just here for development purposes as its values are applicable just on that basin as well as its geological classification codes
        ##==## |     |0        |1         |2      |3       |4      |5       |6   |7        |
        ##==## | geo | tetha r |alfaVanGen|   n   |cohesion|  phi  |  gamma |Ksat| theta s |
        ##==## | 1   | 0.0233  |  6.93    | 1.3165|  1.0   | 38.0  | 18.0   |1e-3| 0.2     |
        ##==## | 2   | 0.0227  |  8.18    | 1.3035|  1.0   | 38.0  | 19.0   |1e-3| 0.2     |
        ##==## | 3   | 0.0434  |  2.39    | 1.2361|  2.0   | 34.0  | 18.5   |1e-6| 0.26    |
        ##==## | 4   | 0.0463  |  1.27    | 1.3562|  5.0   | 18.0  | 18.0   |1e-6| 0.26    |
        ##==## | 5   | 0.0456  |  2.07    | 1.2676|  1.0   | 34.5  | 18.0   |1e-6| 0.23    |
        ##==## | 6   | 0.0369  |  3.29    | 1.2486|  1.0   | 30.0  | 18.0   |1e-7| 0.29    |
        ##==## | 7   | 0.0252  |  7.08    | 1.3849|  1.0   | 34.5  | 18.0   |1e-5| 0.28    |
        ##==## | 8   | 0.0363  |  4.46    | 1.2160|  0.5   | 39.0  | 18.5   |1e-5| 0.28    |
        ##==## | 9   | 0.0363  |  4.46    | 1.2160|  1.0   | 33.0  | 18.5   |1e-5| 0.23    |
        ##==## | 10  | 0.0583  |  1.52    | 1.3250| 20.0   | 28.0  | 17.0   |1e-8| 0.50    |
        ##==## | 11  | 0.0355  |  2.98    | 1.2616|  2.0   | 33.0  | 18.0   |1e-5| 0.23    |
        To skip the step on line 40 of R script (replacing all 0 available in geo map with code11)
        we are adding one more raw to the HashTable to associate key 0 with same values as key 11
        ##==## | 0   | 0.0355  |  2.98    | 1.2616|  2.0   | 33.0  | 18.0   |1e-5| 0.23    |
        */
        vanGenchtenTable.put(1, new Double[]{0.0233, 6.93, 1.3165, 1.0, 38.0, 18.0, 1e-3, 0.2});
        vanGenchtenTable.put(2, new Double[]{0.0227, 8.18, 1.3035, 1.0, 38.0, 19.0, 1e-3, 0.2});
        vanGenchtenTable.put(3, new Double[]{0.0434, 2.39, 1.2361, 2.0, 34.0, 18.5, 1e-6, 0.26});
        vanGenchtenTable.put(4, new Double[]{0.0463, 1.27, 1.3562, 5.0, 18.0, 18.0, 1e-6, 0.26});
        vanGenchtenTable.put(5, new Double[]{0.0456, 2.07, 1.2676, 1.0, 34.5, 18.0, 1e-6, 0.23});
        vanGenchtenTable.put(6, new Double[]{0.0369, 3.29, 1.2486, 1.0, 30.0, 18.0, 1e-7, 0.29});
        vanGenchtenTable.put(7, new Double[]{0.0252, 7.08, 1.3849, 1.0, 34.5, 18.0, 1e-5, 0.28});
        vanGenchtenTable.put(8, new Double[]{0.0363, 4.46, 1.2160, 0.5, 39.0, 18.5, 1e-5, 0.28});
        vanGenchtenTable.put(9, new Double[]{0.0363, 4.46, 1.2160, 1.0, 33.0, 18.5, 1e-5, 0.23});
        vanGenchtenTable.put(10, new Double[]{0.0583, 1.52, 1.3250, 20.0, 28.0, 17.0, 1e-8, 0.50});
        vanGenchtenTable.put(11, new Double[]{0.0355, 2.98, 1.2616, 2.0, 33.0, 18.0, 1e-5, 0.23});
        vanGenchtenTable.put(0, new Double[]{0.0355, 2.98, 1.2616, 2.0, 33.0, 18.0, 1e-5, 0.23});

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inGeo);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        double xRes = regionMap.getXres();
        double yRes = regionMap.getYres();

        RenderedImage geoImage = inGeo.getRenderedImage();
        RandomIter geoIter = RandomIterFactory.create(geoImage, null);

        WritableRaster outCohesionWR = CoverageUtilities.renderedImage2WritableRaster(geoImage, false);
        WritableRaster outPhiWR = CoverageUtilities.renderedImage2WritableRaster(geoImage, false);
        WritableRaster outGammaWR = CoverageUtilities.renderedImage2WritableRaster(geoImage, false);
        WritableRaster outKsatWR = CoverageUtilities.renderedImage2WritableRaster(geoImage, false);
        WritableRaster outTheta_sWR = CoverageUtilities.renderedImage2WritableRaster(geoImage, false);
        WritableRaster outTheta_rWR = CoverageUtilities.renderedImage2WritableRaster(geoImage, false);
        WritableRaster outAlfaVanGenWR = CoverageUtilities.renderedImage2WritableRaster(geoImage, false);
        WritableRaster outNVanGenWR = CoverageUtilities.renderedImage2WritableRaster(geoImage, false);

        WritableRandomIter outCohesionIter = RandomIterFactory.createWritable(outCohesionWR, null);
        WritableRandomIter outPhiIter = RandomIterFactory.createWritable(outPhiWR, null);
        WritableRandomIter outGammaIter = RandomIterFactory.createWritable(outGammaWR, null);
        WritableRandomIter outKsatIter = RandomIterFactory.createWritable(outKsatWR, null);
        WritableRandomIter outTheta_sIter = RandomIterFactory.createWritable(outTheta_sWR, null);
        WritableRandomIter outTheta_rIter = RandomIterFactory.createWritable(outTheta_rWR, null);
        WritableRandomIter outAlfaVanGenIter = RandomIterFactory.createWritable(outAlfaVanGenWR, null);
        WritableRandomIter outNVanGenIter = RandomIterFactory.createWritable(outNVanGenWR, null);

        //String message = msg.message("cislam.dev.computing.maps");
        String message = null;
        /*
        try {
            message = msg.message("cislam.dev.computing.maps"); 
        } catch (Exception e) {
            e.printStackTrace();
            message = "Computing maps..";
        }
        */
        message = "Computing maps..";  //$NON-NLS-1$
        pm.beginTask(message, rows);

        // Cycling into the valid region.
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                GridNode gridNode = new GridNode(geoIter, cols, rows, xRes, yRes, c, r);
                if (gridNode.isValid()) {
                    int geoClass = (int) gridNode.elevation;
                    if (geoClass >= 0) {
                        Double[] parameters = vanGenchtenTable.get(geoClass);
                        /*
                         * Parameter indexes in the 'parameters[]' array
                        ##==## |     |0        |1         |2      |3       |4      |5       |6   |7        |
                        ##==## | geo | tetha r |alfaVanGen|   n   |cohesion|  phi  |  gamma |Ksat| theta s |
                        */
                        double choesion = parameters[3];
                        gridNode.setValueInMap(outCohesionIter, choesion);
                        gridNode.setValueInMap(outPhiIter, parameters[4]);
                        gridNode.setValueInMap(outGammaIter, parameters[5]);
                        gridNode.setValueInMap(outKsatIter, parameters[6]);
                        gridNode.setValueInMap(outTheta_sIter, parameters[7]);
                        gridNode.setValueInMap(outTheta_rIter, parameters[0]);
                        gridNode.setValueInMap(outAlfaVanGenIter, parameters[1]);
                        gridNode.setValueInMap(outNVanGenIter, parameters[2]);
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();

        CoordinateReferenceSystem crs = inGeo.getCoordinateReferenceSystem();
        
        pm.beginTask("Saving files..." /*msg.message("cislam.dev.saving.files")*/, 8); //$NON-NLS-1$
        outCohesion = CoverageUtilities.buildCoverage("cohesion", outCohesionWR, regionMap, crs);
        pm.worked(1);
        outPhi = CoverageUtilities.buildCoverage("phi", outPhiWR, regionMap, crs);
        pm.worked(1);
        outGamma = CoverageUtilities.buildCoverage("gamma", outGammaWR, regionMap, crs);
        pm.worked(1);
        outKsat = CoverageUtilities.buildCoverage("ksat", outKsatWR, regionMap, crs);
        pm.worked(1);
        outTheta_s = CoverageUtilities.buildCoverage("thetas", outTheta_sWR, regionMap, crs);
        pm.worked(1);
        outTheta_r = CoverageUtilities.buildCoverage("thetar", outTheta_rWR, regionMap, crs);
        pm.worked(1);
        outAlfaVanGen = CoverageUtilities.buildCoverage("alfa", outAlfaVanGenWR, regionMap, crs);
        pm.worked(1);
        outNVanGen = CoverageUtilities.buildCoverage("n", outNVanGenWR, regionMap, crs);
        pm.worked(1);
        pm.done();
    }
}
