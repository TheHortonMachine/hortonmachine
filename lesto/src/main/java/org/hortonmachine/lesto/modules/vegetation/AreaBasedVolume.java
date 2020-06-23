/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
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
package org.hortonmachine.lesto.modules.vegetation;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;
import static org.hortonmachine.lesto.modules.vegetation.OmsPointCloudMaximaFinder.outTops_DESCR;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.io.las.ALasDataManager;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.v.grids.OmsGridsGenerator;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.StringUtilities;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description("Area based approach to calculate the volumen from a point cloud.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("las, volume")
@Label(HMConstants.LESTO + "/vegetation")
@Name("AreaBasedVolume")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class AreaBasedVolume extends HMModel {

    @Description("Input las file.")
    @UI(HMConstants.FILEIN_UI_HINT_LAS)
    @In
    public String inLas = null;

    @Description("Input DTM for las normalization.")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inDtm;

    @Description("Tree height threshold.")
    @In
    public double pThreshold = 2.0;

    @Description("The classification codes for the vegetation.")
    @In
    public String pVegetationClass = "3, 4, 5";

    @Description("Calculation area size.")
    @In
    public double pAreaSize = 40.0;

    @Description(outTops_DESCR)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outAreas = null;

    @Execute
    public void process() throws Exception {
        checkNull(inLas, inDtm);

        GridCoverage2D inDtmGC = null;
        CoordinateReferenceSystem crs = null;

        inDtmGC = getRaster(inDtm);
        crs = inDtmGC.getCoordinateReferenceSystem();

        // create the base grid to work on
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inDtmGC);
        double newCols = Math.ceil(regionMap.getWidth() / pAreaSize);
        double newRows = Math.ceil(regionMap.getHeight() / pAreaSize);

        // create the support grid used to find the seeds
        OmsGridsGenerator gridGenerator = new OmsGridsGenerator();
        gridGenerator.inRaster = inDtmGC;
        gridGenerator.pCols = (int) newCols;
        gridGenerator.pRows = (int) newRows;
        gridGenerator.process();
        SimpleFeatureCollection outTilesFC = gridGenerator.outMap;
        List<Geometry> secGridGeoms = FeatureUtilities.featureCollectionToGeometriesList(outTilesFC, true, null);

        /// create the output featurecollection
        DefaultFeatureCollection outAreasFC = new DefaultFeatureCollection();
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("typename");
        b.setCRS(crs);
        b.add("the_geom", Polygon.class);
        b.add("volume", Double.class);
        b.add("H_mean1stR", Double.class);
        b.add("H_q502ndR", Double.class);
        b.add("count1st", Integer.class);
        b.add("count2nd", Integer.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

        // open the data source, supplying the dtm for normalization and the treshold for cleanup
        try (ALasDataManager lasData = ALasDataManager.getDataManager(new File(inLas), inDtmGC, pThreshold, crs)) {
            lasData.open();

            // only the vegetation class is kept
            if (pVegetationClass != null) {
                double[] classes = StringUtilities.stringToDoubleArray(pVegetationClass, ",");
                lasData.setClassesConstraint(classes);
            }

            // process the data, one grid cell at the time.
            pm.beginTask("Processing grid cells...", secGridGeoms.size());
            for( Geometry gridGeom : secGridGeoms ) {
                List<LasRecord> points = lasData.getPointsInGeometry(gridGeom, true);

                ConcurrentSkipListSet<Double> secondReturnsHeight = new ConcurrentSkipListSet<Double>();
                double sum = 0;
                int count1st = 0;
                int count2nd = 0;
                for( LasRecord dot : points ) {
                    if (dot.returnNumber == 1) {
                        sum += dot.groundElevation;
                        count1st++;
                    } else if (dot.returnNumber == 2) {
                        secondReturnsHeight.add(dot.groundElevation);
                        count2nd++;
                    }

                }

                double H_mean1stR = sum / count1st;
                double H_q502ndR = getMedianFromSet(secondReturnsHeight);
                double volume = -151.956 + 63.458 * H_mean1stR - 43.910 * H_q502ndR;
                if (count1st == 0 || count2nd == 0) {
                    H_mean1stR = 0;
                    H_q502ndR = 0;
                    volume = 0;
                }

                Object[] values = new Object[]{gridGeom, volume, H_mean1stR, H_q502ndR, count1st, count2nd};
                builder.addAll(values);
                SimpleFeature feature = builder.buildFeature(null);
                outAreasFC.add(feature);

                pm.worked(1);
            }
            pm.done();

            dumpVector(outAreasFC, outAreas);
        }
    }

    private double getMedianFromSet( final ConcurrentSkipListSet<Double> set ) {
        double threshold = 0;
        int halfNum = set.size() / 2;
        int count = 0;
        for( double value : set ) {
            if (count == halfNum) {
                threshold = value;
                break;
            }
            count++;
        }
        return threshold;
    }

    public static void main( String[] args ) throws Exception {
        String inLas = "/Users/hydrologis/TMP/VEGTEST/plot_77.las";
        String inDtm = "/Users/hydrologis/TMP/VEGTEST/plot_77_dtm.asc";
        String outShp = "/Users/hydrologis/TMP/VEGTEST/out.shp";
        double area = 5;

        AreaBasedVolume abv = new AreaBasedVolume();
        abv.inLas = inLas;
        abv.inDtm = inDtm;
        abv.pThreshold = 2;
        abv.pVegetationClass = "3,4,5";
        abv.pAreaSize = area;
        abv.outAreas = outShp;
        abv.process();
    }

}
