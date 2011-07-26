/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.hortonmachine.modules.geomorphology.tca;

import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
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
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.ModelsEngine;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.DirectPosition;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

@Description("Calculates the contributing areas that represent the areas (in number of pixels) afferent to each point.")
@Documentation("Tca.html")
@Author(name = "Antonello Andrea, Rigon Riccardo", contact = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon")
@Keywords("Geomorphology, DrainDir, Tca3D, Ab, Multitca")
@Label(JGTConstants.GEOMORPHOLOGY)
@Name("tca")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class Tca extends JGTModel {
    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The map of total contributing areas.")
    @Out
    public GridCoverage2D outTca = null;

    @Description("The vector containing loops, if there are any.")
    @Out
    public SimpleFeatureCollection outLoop = null;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    private int cols;
    private int rows;

    private SimpleFeatureType loopFT;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outTca == null, doReset)) {
            return;
        }

        // prepare the loop featurecollection
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("loop");
        b.setCRS(inFlow.getCoordinateReferenceSystem());
        b.add("the_geom", LineString.class);
        loopFT = b.buildFeatureType();
        outLoop = FeatureCollections.newCollection();

        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        cols = regionMap.get(CoverageUtilities.COLS).intValue();
        rows = regionMap.get(CoverageUtilities.ROWS).intValue();

        RenderedImage flowRI = inFlow.getRenderedImage();

        pm.message(msg.message("tca.initializematrix"));

        // Initialize new RasterData and set value
        WritableRaster tcaWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, 1.0);

        RandomIter flowIter = RandomIterFactory.create(flowRI, null);
        WritableRandomIter tcaIter = RandomIterFactory.createWritable(tcaWR, null);

        pm.beginTask(msg.message("tca.workingon"), cols);

        boolean loopError = false;

        final int[] point = new int[2];
        for( int col = 0; col < cols; col++ ) {
            for( int row = 0; row < rows; row++ ) {
                // get the directions of the current pixel.
                double flowValue = flowIter.getSampleDouble(col, row, 0);
                if (isNovalue(flowValue)) {
                    tcaIter.setSample(col, row, 0, doubleNovalue);
                } else {
                    boolean isSource = ModelsEngine.isSourcePixel(flowIter, col, row);
                    if (!isSource) {
                        continue;
                    }
                    double tcaValue = tcaIter.getSampleDouble(col, row, 0);
                    double previousTcaValue = tcaValue;
                    point[0] = col;
                    point[1] = row;
                    // leave the current and go one down
                    if (!ModelsEngine.go_downstream(point, flowValue)) {
                        throw new RuntimeException();
                    }
                    flowValue = flowIter.getSampleDouble(point[0], point[1], 0);
                    tcaValue = tcaIter.getSampleDouble(point[0], point[1], 0);

                    TreeSet<CheckPoint> passedPoints = new TreeSet<CheckPoint>();
                    int index = 0;
                    while( flowValue < 9 && !isNovalue(flowValue) && flowValue != 0 ) {
                        if (!passedPoints.add(new CheckPoint(point[0], point[1], index++))) {
                            // create a shapefile with the loop performed
                            GridGeometry2D gridGeometry = inFlow.getGridGeometry();
                            Iterator<CheckPoint> iterator = passedPoints.iterator();
                            GeometryFactory gf = GeometryUtilities.gf();
                            List<Coordinate> coordinates = new ArrayList<Coordinate>();
                            while( iterator.hasNext() ) {
                                Tca.CheckPoint checkPoint = (Tca.CheckPoint) iterator.next();
                                DirectPosition world = gridGeometry.gridToWorld(new GridCoordinates2D(checkPoint.col,
                                        checkPoint.row));
                                double[] coord = world.getCoordinate();
                                coordinates.add(new Coordinate(coord[0], coord[1]));
                            }
                            LineString lineString = gf.createLineString(coordinates.toArray(new Coordinate[0]));
                            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(loopFT);
                            Object[] values = new Object[]{lineString};
                            builder.addAll(values);
                            SimpleFeature feature = builder.buildFeature(null);
                            outLoop.add(feature);

                            pm.errorMessage(MessageFormat
                                    .format("The downstream sum passed twice through the same position, there might be an error in your flowdirections. col = {0} row = {1}",
                                            col, row));
                            loopError = true;
                            break;
                        }

                        double newTcaValue = tcaValue + previousTcaValue;
                        tcaIter.setSample(point[0], point[1], 0, newTcaValue);
                        if (tcaValue == 1) {
                            /*
                             * if the tcavalue was one, then it is the first time
                             * we are passing over it, so it is ok to set the previous 
                             * tca value to cumulate values.
                             * 
                             * Instead if the pixel was already touched, then we do 
                             * not cumulate, we just sum the tca that pas previous to 
                             * the join point.
                             */
                            previousTcaValue = newTcaValue;
                        }

                        if (!ModelsEngine.go_downstream(point, flowValue)) {
                            throw new RuntimeException();
                        }

                        // update the tca and flow values to those of the new position
                        // downstream
                        tcaValue = tcaIter.getSampleDouble(point[0], point[1], 0);
                        flowValue = flowIter.getSampleDouble(point[0], point[1], 0);
                    }
                    if (loopError) {
                        break;
                    }

                    if (flowValue == 10) {
                        tcaIter.setSample(point[0], point[1], 0, tcaValue + 1);
                    }
                }
            }
            if (loopError) {
                break;
            }
            pm.worked(1);
        }
        pm.done();

        flowIter.done();
        tcaIter.done();

        if (loopError) {
            outTca = CoverageUtilities.buildDummyCoverage();
        } else {
            outTca = CoverageUtilities.buildCoverage("tca", tcaWR, regionMap, inFlow.getCoordinateReferenceSystem());
        }
    }

    /**
     * Class to check if the downstream trip gets into a loop.
     * 
     * @author Andrea Antonello (www.hydrologis.com)
     */
    public class CheckPoint implements Comparable<CheckPoint> {
        public int col;
        public int row;
        public int index;

        public CheckPoint( int col, int row, int index ) {
            this.col = col;
            this.row = row;
            this.index = index;
        }

        public int compareTo( CheckPoint o ) {
            /*
             * if row and col are equal, return 0, which will 
             * anyways trigger and exception
             */
            if (col == o.col && row == o.row) {
                return 0;
            }

            /*
             * in the case of non equal row/col, we need to make the normal sort
             */
            if (index < o.index) {
                return -1;
            } else if (index > o.index) {
                return 1;
            } else {
                return 0;
            }

        }

    }

    public static void main( String[] args ) {
        TreeSet<CheckPoint> p = new TreeSet<CheckPoint>();
        System.out.println(p.add(new Tca().new CheckPoint(1, 3, 0)));
        System.out.println(p.add(new Tca().new CheckPoint(1, 3, 1)));
    }
}