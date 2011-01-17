/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jgrasstools.gears.modules.v.rastercattofeatureattribute;

import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;
import static org.jgrasstools.gears.utils.geometry.GeometryUtilities.getGeometryType;

import java.awt.image.RenderedImage;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import oms3.annotations.Author;
import oms3.annotations.Label;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.features.FeatureExtender;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities.GEOMETRYTYPE;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

@Description("Module that extracts raster categories and adds them to a feature collection.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Raster, Vector")
@Status(Status.TESTED)
@Label(JGTConstants.VECTORPROCESSING)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
@SuppressWarnings("nls")
public class RasterCatToFeatureAttribute {

    @Description("The coverage on which to map the features.")
    @In
    public GridCoverage2D inCoverage;

    @Description("The feature collection to use for the geometric mapping.")
    @In
    public SimpleFeatureCollection inFC = null;

    @Description("The name for the new field to create.")
    @In
    public String fNew = "new";

    @Description("The position of the coordinate to take in the case of multi geometries.")
    @In
    public String pPos = MIDDLE;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The extended features.")
    @Out
    public SimpleFeatureCollection outGeodata = null;

    private static final String MIDDLE = "middle";
    private static final String START = "start";
    private static final String END = "end";

    private RandomIter inIter = null;

    private GridGeometry2D gridGeometry;

    @Execute
    public void process() throws Exception {
        if (inIter == null) {
            RenderedImage inputRI = inCoverage.getRenderedImage();
            inIter = RandomIterFactory.create(inputRI, null);

            // HashMap<String, Double> regionMap = getRegionParamsFromGridCoverage(inCoverage);
            // height = regionMap.get(ROWS).intValue();
            // width = regionMap.get(COLS).intValue();
            // xRes = regionMap.get(XRES);
            // yRes = regionMap.get(YRES);

            gridGeometry = inCoverage.getGridGeometry();
            // GridSampleDimension[] sampleDimensions = inCoverage.getSampleDimensions();
            // double[] noDataValues = sampleDimensions[0].getNoDataValues();
            // System.out.println(noDataValues);
        }

        SimpleFeatureType featureType = inFC.getSchema();

        FeatureExtender fExt = new FeatureExtender(featureType, new String[]{fNew},
                new Class< ? >[]{Double.class});

        Envelope2D inCoverageEnvelope = inCoverage.getEnvelope2D();
        outGeodata = FeatureCollections.newCollection();
        FeatureIterator<SimpleFeature> featureIterator = inFC.features();
        int all = inFC.size();
        int id = 0;
        pm.beginTask("Extracting raster information...", all);
        while( featureIterator.hasNext() ) {
            SimpleFeature feature = featureIterator.next();
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            double value = -1;
            Coordinate c;
            Coordinate[] coordinates = geometry.getCoordinates();
            if (getGeometryType(geometry) == GEOMETRYTYPE.POINT
                    || getGeometryType(geometry) == GEOMETRYTYPE.MULTIPOINT) {
                c = coordinates[0];
            } else if (getGeometryType(geometry) == GEOMETRYTYPE.LINE
                    || getGeometryType(geometry) == GEOMETRYTYPE.MULTILINE) {
                if (pPos.trim().equalsIgnoreCase(START)) {
                    c = coordinates[0];
                } else if (pPos.trim().equalsIgnoreCase(END)) {
                    c = coordinates[coordinates.length - 1];
                } else {// (pPos.trim().equalsIgnoreCase(MIDDLE)) {
                    c = coordinates[coordinates.length / 2];
                }
            } else if (getGeometryType(geometry) == GEOMETRYTYPE.POLYGON
                    || getGeometryType(geometry) == GEOMETRYTYPE.MULTIPOLYGON) {
                Point centroid = geometry.getCentroid();
                if (geometry.contains(centroid)) {
                    c = centroid.getCoordinate();
                } else {
                    c = coordinates[0];
                }
            } else {
                throw new ModelsIllegalargumentException("The Geometry type is not supported.",
                        this);
            }

            if (!inCoverageEnvelope.contains(c.x, c.y)) {
                continue;
            }

            GridCoordinates2D gridCoord = gridGeometry.worldToGrid(new DirectPosition2D(c.x, c.y));
            value = inIter.getSampleDouble(gridCoord.x, gridCoord.y, 0);

            // TODO make this better
            if (isNovalue(value) || value >= Float.MAX_VALUE || value <= -Float.MAX_VALUE) {
                value = -9999.0;
            }

            SimpleFeature extendedFeature = fExt.extendFeature(feature, new Object[]{value}, id++);

            outGeodata.add(extendedFeature);
            pm.worked(1);
        }
        inFC.close(featureIterator);
        pm.done();

    }
}
