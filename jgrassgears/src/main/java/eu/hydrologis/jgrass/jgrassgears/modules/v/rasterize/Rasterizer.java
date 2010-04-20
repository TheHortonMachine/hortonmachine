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
package eu.hydrologis.jgrass.jgrassgears.modules.v.rasterize;

import static eu.hydrologis.jgrass.jgrassgears.utils.coverage.CoverageUtilities.COLS;
import static eu.hydrologis.jgrass.jgrassgears.utils.coverage.CoverageUtilities.ROWS;
import static eu.hydrologis.jgrass.jgrassgears.utils.coverage.CoverageUtilities.gridGeometry2RegionParamsMap;

import java.awt.image.WritableRaster;
import java.util.HashMap;

import oms3.annotations.Author;
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
import org.geotools.coverage.grid.InvalidGridGeometryException;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.DirectPosition2D;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

import eu.hydrologis.jgrass.jgrassgears.libs.exceptions.ModelsIllegalargumentException;
import eu.hydrologis.jgrass.jgrassgears.libs.monitor.DummyProgressMonitor;
import eu.hydrologis.jgrass.jgrassgears.libs.monitor.IHMProgressMonitor;
import eu.hydrologis.jgrass.jgrassgears.utils.coverage.CoverageUtilities;
import eu.hydrologis.jgrass.jgrassgears.utils.geometry.GeometryUtilities;

@Description("Module for vector to raster conversion")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Raster, Vector")
@Status(Status.TESTED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
@SuppressWarnings("nls")
public class Rasterizer {

    @Description("The features to rasterize.")
    @In
    public FeatureCollection<SimpleFeatureType, SimpleFeature> inGeodata = null;

    @Description("The value to use as raster value if no field is given.")
    @In
    public double pValue = 0.0;

    @Description("The field to use to retrieve the category value for the raster.")
    @In
    public String fCat = null;

    @Description("The gridgeometry on which to perform rasterization.")
    @In
    public GridGeometry2D pGrid = null;

    @Description("The progress monitor.")
    @In
    public IHMProgressMonitor pm = new DummyProgressMonitor();

    @Description("The coverage that has to be converted.")
    @Out
    public GridCoverage2D outGeodata;

    private WritableRaster outWR;

    private int height;

    private int width;

    private GeometryFactory gf = GeometryUtilities.gf();

    @Execute
    public void process() throws Exception {
        if (outWR == null) {
            if (pGrid != null) {
                HashMap<String, Double> paramsMap = gridGeometry2RegionParamsMap(pGrid);
                height = paramsMap.get(ROWS).intValue();
                width = paramsMap.get(COLS).intValue();
            }

            outWR = CoverageUtilities.createDoubleWritableRaster(width, height, null, null, null);
        }

        String geometryType = inGeodata.getSchema().getGeometryDescriptor().getType().toString();
        if (geometryType.matches(".*[Pp][Oo][Ii][Nn][Tt].*")) {
        } else if (geometryType.matches(".*[Ll][Ii][Nn][Ee].*")) {
            // rasterizeLine(newCollection, outIter);
        } else if (geometryType.matches(".*[Pp][Oo][Ll][Yy][Gg][Oo][Nn].*")) {
            rasterizepolygon(inGeodata, outWR, pGrid, fCat, pValue);
        } else {
            throw new ModelsIllegalargumentException(
                    "Couldn't recognize the geometry type of the file.", this.getClass()
                            .getSimpleName());
        }

    }

    private void rasterizepolygon( FeatureCollection<SimpleFeatureType, SimpleFeature> polygonFC,
            WritableRaster rasterized, GridGeometry2D gridGeometry, String field, double cat )
            throws InvalidGridGeometryException, TransformException {

        int w = rasterized.getWidth();
        int h = rasterized.getHeight();

        int size = inGeodata.size();
        pm.beginTask("Rasterizing features...", size);
        FeatureIterator<SimpleFeature> featureIterator = inGeodata.features();
        while( featureIterator.hasNext() ) {
            SimpleFeature feature = featureIterator.next();
            Polygon polygon = (Polygon) feature.getDefaultGeometry();

            double delta = 0;// active.getWEResolution() / 4.0;

            for( int r = 0; r < h; r++ ) {
                // do scan line to fill the polygon
                double[] westPos = gridGeometry.gridToWorld(new GridCoordinates2D(0, r))
                        .getCoordinate();
                double[] eastPos = gridGeometry.gridToWorld(new GridCoordinates2D(w - 1, r))
                        .getCoordinate();
                Coordinate west = new Coordinate(westPos[0], westPos[1]);
                Coordinate east = new Coordinate(eastPos[0], eastPos[1]);
                LineString line = gf.createLineString(new Coordinate[]{west, east});
                if (polygon.intersects(line)) {
                    Geometry internalLines = polygon.intersection(line);
                    Coordinate[] coords = internalLines.getCoordinates();
                    for( int j = 0; j < coords.length; j = j + 2 ) {
                        Coordinate startC = new Coordinate(coords[j].x + delta, coords[j].y);
                        Coordinate endC = new Coordinate(coords[j + 1].x - delta, coords[j + 1].y);

                        GridCoordinates2D startGridCoord = gridGeometry
                                .worldToGrid(new DirectPosition2D(startC.x, startC.x));
                        GridCoordinates2D endGridCoord = gridGeometry
                                .worldToGrid(new DirectPosition2D(endC.x, endC.x));

                        /*
                         * the part in between has to be filled
                         */
                        for( int k = startGridCoord.x; k <= endGridCoord.x; k++ ) {
                            double value = pValue;
                            if (fCat != null) {
                                value = ((Number) feature.getAttribute(fCat)).doubleValue();
                            }
                            rasterized.setSample(k, r, 0, value);
                        }
                    }

                }
            }

            pm.worked(1);
        }
        pm.done();
        inGeodata.close(featureIterator);
    }
}
