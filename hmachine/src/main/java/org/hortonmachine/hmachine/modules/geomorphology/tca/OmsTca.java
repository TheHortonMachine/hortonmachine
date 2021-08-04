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
package org.hortonmachine.hmachine.modules.geomorphology.tca;

import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.hmachine.i18n.HortonMessages.*;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA_DOCUMENTATION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA_inFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA_doLoopCheck_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSTCA_outTca_DESCRIPTION;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.libs.modules.FlowNode;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.DirectPosition;

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

@Description(OMSTCA_DESCRIPTION)
@Documentation(OMSTCA_DOCUMENTATION)
@Author(name = OMSTCA_AUTHORNAMES, contact = OMSTCA_AUTHORCONTACTS)
@Keywords(OMSTCA_KEYWORDS)
@Label(OMSTCA_LABEL)
@Name(OMSTCA_NAME)
@Status(OMSTCA_STATUS)
@License(OMSTCA_LICENSE)
public class OmsTca extends HMModel {
    @Description(OMSTCA_inFlow_DESCRIPTION)
    @In
    public GridCoverage2D inFlow = null;

    @Description(OMSTCA_doLoopCheck_DESCRIPTION)
    @In
    public boolean doLoopCheck = false;

    @Description(OMSTCA_outTca_DESCRIPTION)
    @Out
    public GridCoverage2D outTca = null;

    @Description(OMSTCA_outLoop_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outLoop = null;

    private SimpleFeatureType loopFT;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outTca == null, doReset)) {
            return;
        }
        checkNull(inFlow);

        if (doLoopCheck) {
            // prepare the loop featurecollection
            SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            b.setName("loop");
            b.setCRS(inFlow.getCoordinateReferenceSystem());
            b.add("the_geom", LineString.class);
            loopFT = b.buildFeatureType();
            outLoop = new DefaultFeatureCollection();
        }

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        int novalue = HMConstants.getIntNovalue(inFlow);

        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster tcaWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, doubleNovalue);

        RandomIter flowIter = RandomIterFactory.create(flowRI, null);
        WritableRandomIter tcaIter = RandomIterFactory.createWritable(tcaWR, null);

        boolean loopError = false;

        TreeSet<CheckPoint> passedPoints = null;
        pm.beginTask("Calculating tca...", rows); //$NON-NLS-1$
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                FlowNode flowNode = new FlowNode(flowIter, cols, rows, c, r, novalue);
                if (flowNode.isSource()) {
                    double previousTcaValue = 0.0;

                    if (doLoopCheck)
                        passedPoints = new TreeSet<>();
                    int index = 0;
                    while( flowNode != null && flowNode.isValid() ) {
                        int col = flowNode.col;
                        int row = flowNode.row;

                        if (doLoopCheck && !passedPoints.add(new CheckPoint(col, row, index++))) {
                            // create a shapefile with the loop performed
                            GridGeometry2D gridGeometry = inFlow.getGridGeometry();
                            Iterator<CheckPoint> iterator = passedPoints.iterator();
                            GeometryFactory gf = GeometryUtilities.gf();
                            List<Coordinate> coordinates = new ArrayList<Coordinate>();
                            while( iterator.hasNext() ) {
                                CheckPoint checkPoint = (CheckPoint) iterator.next();
                                DirectPosition world = gridGeometry
                                        .gridToWorld(new GridCoordinates2D(checkPoint.col, checkPoint.row));
                                double[] coord = world.getCoordinate();
                                coordinates.add(new Coordinate(coord[0], coord[1]));
                            }
                            if (coordinates.size() == 1) {
                                Coordinate first = coordinates.get(0);
                                Coordinate dummy = new Coordinate(first.x + 0.000_000_1, first.y + 0.000_000_1);
                                coordinates.add(dummy);
                            }
                            LineString lineString = gf.createLineString(coordinates.toArray(new Coordinate[0]));
                            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(loopFT);
                            Object[] values = new Object[]{lineString};
                            builder.addAll(values);
                            SimpleFeature feature = builder.buildFeature(null);
                            ((DefaultFeatureCollection) outLoop).add(feature);

                            pm.errorMessage(MessageFormat.format(
                                    "The downstream sum passed twice through the same position, there might be an error in your flowdirections. col = {0} row = {1}",
                                    col, row));
                            loopError = true;
                            break;
                        }

                        double tmpTca = tcaIter.getSampleDouble(col, row, 0);
                        double newTcaValue;
                        /*
                         * cumulate only if first time passing, else just propagate
                         */
                        if (isNovalue(tmpTca)) {
                            tmpTca = 1.0;
                            newTcaValue = tmpTca + previousTcaValue;
                            previousTcaValue = newTcaValue;
                        } else {
                            newTcaValue = tmpTca + previousTcaValue;
                        }
                        tcaIter.setSample(col, row, 0, newTcaValue);
                        flowNode = flowNode.goDownstream();
                    }
                    if (loopError) {
                        break;
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
            outLoop = null;
            outTca = CoverageUtilities.buildCoverageWithNovalue("tca", tcaWR, regionMap, inFlow.getCoordinateReferenceSystem(),
                    doubleNovalue);
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
             * if row and col are equal, return 0, which will anyways trigger and exception
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

}