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
package org.hortonmachine.hmachine.modules.basin.basinshape;

import static org.hortonmachine.gears.libs.modules.HMConstants.BASIN;
import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.hmachine.modules.basin.basinshape.OmsBasinShape.OMSBASINSHAPE_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.modules.basin.basinshape.OmsBasinShape.OMSBASINSHAPE_AUTHORNAMES;
import static org.hortonmachine.hmachine.modules.basin.basinshape.OmsBasinShape.OMSBASINSHAPE_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.basin.basinshape.OmsBasinShape.OMSBASINSHAPE_KEYWORDS;
import static org.hortonmachine.hmachine.modules.basin.basinshape.OmsBasinShape.OMSBASINSHAPE_LABEL;
import static org.hortonmachine.hmachine.modules.basin.basinshape.OmsBasinShape.OMSBASINSHAPE_LICENSE;
import static org.hortonmachine.hmachine.modules.basin.basinshape.OmsBasinShape.OMSBASINSHAPE_NAME;
import static org.hortonmachine.hmachine.modules.basin.basinshape.OmsBasinShape.OMSBASINSHAPE_STATUS;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.DirectPosition2D;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.ModelsSupporter;
import org.hortonmachine.gears.modules.v.vectorize.OmsVectorizer;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.hmachine.modules.network.networkattributes.NetworkChannel;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

@Description(OMSBASINSHAPE_DESCRIPTION)
@Author(name = OMSBASINSHAPE_AUTHORNAMES, contact = OMSBASINSHAPE_AUTHORCONTACTS)
@Keywords(OMSBASINSHAPE_KEYWORDS)
@Label(OMSBASINSHAPE_LABEL)
@Name("_" + OMSBASINSHAPE_NAME)
@Status(OMSBASINSHAPE_STATUS)
@License(OMSBASINSHAPE_LICENSE)
public class OmsBasinShape extends HMModel {

    @Description(OMSBASINSHAPE_inElev_DESCRIPTION)
    @In
    public GridCoverage2D inElev = null;

    @Description(OMSBASINSHAPE_inBasins_DESCRIPTION)
    @In
    public GridCoverage2D inBasins = null;

    @Description(OMSBASINSHAPE_outBasins_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outBasins = null;

    // VARS DOC START
    public static final String OMSBASINSHAPE_DESCRIPTION = "Creates a Feature collection of the subbasins created with the netnumbering module.";
    public static final String OMSBASINSHAPE_DOCUMENTATION = "OmsBasinShape.html";
    public static final String OMSBASINSHAPE_KEYWORDS = "Basin, Geomorphology";
    public static final String OMSBASINSHAPE_LABEL = BASIN;
    public static final String OMSBASINSHAPE_NAME = "basinshape";
    public static final int OMSBASINSHAPE_STATUS = 40;
    public static final String OMSBASINSHAPE_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSBASINSHAPE_AUTHORNAMES = "Erica Ghesla, Andrea Antonello";
    public static final String OMSBASINSHAPE_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSBASINSHAPE_inElev_DESCRIPTION = "The elevation map.";
    public static final String OMSBASINSHAPE_inBasins_DESCRIPTION = "The map of the numbered basins.";
    public static final String OMSBASINSHAPE_outBasins_DESCRIPTION = "The extracted basins vector map.";
    // VARS DOC END

    private int nCols;
    private int nRows;

    private RandomIter pitRandomIter;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outBasins == null, doReset)) {
            return;
        }
        checkNull(inBasins);

        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inBasins);
        nCols = regionMap.get(CoverageUtilities.COLS).intValue();
        nRows = regionMap.get(CoverageUtilities.ROWS).intValue();
        // double xRes = regionMap.get(CoverageUtilities.XRES);
        // double yRes = regionMap.get(CoverageUtilities.YRES);

        RenderedImage basinsRI = inBasins.getRenderedImage();
        RenderedImage pitRI = null;
        if (inElev != null)
            pitRI = inElev.getRenderedImage();

        int[] nstream = new int[1];
        // nstream[0] = 1508;
        WritableRaster basinsWR = CoverageUtilities.renderedImage2WritableRaster(basinsRI, true);
        RandomIter basinsRandomIter = RandomIterFactory.create(basinsWR, null);

        for( int j = 0; j < nRows; j++ ) {
            for( int i = 0; i < nCols; i++ ) {
                if (!isNovalue(basinsRandomIter.getSampleDouble(i, j, 0))
                        && basinsRandomIter.getSampleDouble(i, j, 0) > (double) nstream[0]) {
                    nstream[0] = (int) basinsRandomIter.getSampleDouble(i, j, 0);
                }

            }
        }

        WritableRaster subbasinsWR = CoverageUtilities.createWritableRaster(basinsRI.getWidth(), basinsRI.getHeight(), null,
                basinsRI.getSampleModel(), doubleNovalue);

        // create the feature type
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        // set the name
        b.setName("basinshape"); //$NON-NLS-1$
        // add a geometry property
        String defaultGeometryName = "the_geom";//$NON-NLS-1$
        b.setCRS(inBasins.getCoordinateReferenceSystem());
        b.add(defaultGeometryName, MultiPolygon.class);
        // add some properties
        b.add("area", Float.class); //$NON-NLS-1$
        b.add("perimeter", Float.class); //$NON-NLS-1$
        b.add(NetworkChannel.NETNUMNAME, Integer.class); // $NON-NLS-1$
        b.add("maxelev", Float.class); //$NON-NLS-1$
        b.add("minelev", Float.class); //$NON-NLS-1$
        b.add("avgelev", Float.class); //$NON-NLS-1$
        b.add(NetworkChannel.BARICENTERELEVNAME, Float.class); // $NON-NLS-1$

        // build the type
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

        outBasins = new DefaultFeatureCollection();

        // for each stream correct problems with basins and create geometries
        pm.beginTask("Extracting basins...", nstream[0]);
        for( int num = 1; num <= nstream[0]; num++ ) {
            Object[] values = new Object[8];

            int nordRow = -1;
            int southRow = 0;
            int eastCol = -1;
            int westCol = nCols;
            int numPixel = 0;

            double minZ = Double.MAX_VALUE;
            double maxZ = Double.MIN_VALUE;
            double averageZ = 0.0;
            if (pitRI != null)
                pitRandomIter = RandomIterFactory.create(pitRI, null);
            WritableRandomIter subbasinIter = RandomIterFactory.createWritable(subbasinsWR, null);
            for( int j = 0; j < nRows; j++ ) {
                for( int i = 0; i < nCols; i++ ) {
                    double basinId = basinsRandomIter.getSampleDouble(i, j, 0);
                    if (isNovalue(basinId)) {
                        continue;
                    }
                    int basinNum = (int) basinId;
                    if (basinNum == num) {
                        if (nordRow == -1) {
                            nordRow = i;
                        }
                        if (i > nordRow) {
                            southRow = i;
                        }
                        if (westCol > j) {
                            westCol = j;
                        }
                        if (eastCol < j) {
                            eastCol = j;
                        }
                        subbasinIter.setSample(i, j, 0, basinNum);
                        if (pitRI != null) {
                            double elevation = pitRandomIter.getSampleDouble(i, j, 0);
                            if (!isNovalue(elevation)) {
                                minZ = elevation < minZ ? elevation : minZ;
                                maxZ = elevation > maxZ ? elevation : maxZ;
                                averageZ = averageZ + elevation;
                            } else {
                                minZ = -1;
                                maxZ = -1;
                                averageZ = 0;
                            }
                        }
                        numPixel++;
                    }
                }
            }

            if (numPixel != 0) {
                // min, max and average
                values[3] = num;
                values[4] = maxZ;
                values[5] = minZ;
                values[6] = averageZ / numPixel;

                numPixel = 0;
                for( int i = nordRow; i < southRow + 1; i++ ) {
                    for( int j = westCol; j < eastCol + 1; j++ ) {
                        if (isNovalue(subbasinIter.getSampleDouble(i, j, 0))) {
                            for( int k = 1; k <= 8; k++ ) {
                                // index.setFlow(k);
                                int indexI = i + ModelsSupporter.DIR[k][1]; // index.getParameters()[
                                // 0];
                                int indexJ = j + ModelsSupporter.DIR[k][0]; // index.getParameters()[
                                // 1];
                                if (!isNovalue(subbasinIter.getSampleDouble(indexI, indexJ, 0))) {
                                    numPixel++;
                                }
                                k++;
                            }
                            if (numPixel == 4) {
                                subbasinIter.setSample(i, j, 0, num);
                            }
                        }
                        numPixel = 0;
                    }
                }

                // extract the feature polygon of that basin number
                OmsVectorizer vectorizer = new OmsVectorizer();
                try {
                    vectorizer.inRaster = inBasins;
                    vectorizer.pm = pm;
                    vectorizer.doReset = true;
                    vectorizer.pValue = (double) num;
                    vectorizer.process();
                } catch (Exception e) {
                    pm.errorMessage(e.getLocalizedMessage());
                    continue;
                }

                SimpleFeatureCollection outGeodata = vectorizer.outVector;
                FeatureIterator<SimpleFeature> outGeodataIterator = outGeodata.features();
                List<Polygon> polygons = new ArrayList<Polygon>();
                while( outGeodataIterator.hasNext() ) {
                    SimpleFeature feature = outGeodataIterator.next();
                    polygons.add((Polygon) feature.getDefaultGeometry());
                }
                outGeodataIterator.close();

                MultiPolygon geometry = GeometryUtilities.gf()
                        .createMultiPolygon((Polygon[]) polygons.toArray(new Polygon[polygons.size()]));
                values[0] = geometry;
                values[1] = geometry.getArea();
                values[2] = geometry.getLength();

                Point centroid = geometry.getCentroid();
                if (centroid == null || centroid.isEmpty()) {
                    pm.errorMessage("Unable to extract basin: " + num);
                    continue;
                }
                Coordinate centroidCoords = centroid.getCoordinate();

                GridGeometry2D gridGeometry = inBasins.getGridGeometry();
                GridCoordinates2D worldToGrid = gridGeometry
                        .worldToGrid(new DirectPosition2D(centroidCoords.x, centroidCoords.y));

                int[] rowColPoint = new int[]{worldToGrid.y, worldToGrid.x};
                double centroidElevation = -1;;
                if (pitRI != null) {
                    double elev = pitRandomIter.getSampleDouble(rowColPoint[1], rowColPoint[0], 0);
                    if (!isNovalue(elev)) {
                        centroidElevation = elev;
                    }
                }
                values[7] = centroidElevation;
                subbasinIter.done();
                subbasinsWR = CoverageUtilities.createWritableRaster(nCols, nRows, null, null, doubleNovalue);

                // add the values
                builder.addAll(values);
                // build the feature with provided ID
                SimpleFeature feature = builder.buildFeature(type.getTypeName() + "." + num);
                ((DefaultFeatureCollection) outBasins).add(feature);
            }
            pm.worked(1);
        }
        pm.done();

        basinsRandomIter.done();
        basinsWR = null;
    }

}
