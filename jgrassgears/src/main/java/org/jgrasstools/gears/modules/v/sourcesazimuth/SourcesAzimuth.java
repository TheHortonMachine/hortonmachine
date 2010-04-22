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
package org.jgrasstools.gears.modules.v.sourcesazimuth;

import static java.lang.Math.sqrt;

import java.util.List;

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
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IHMProgressMonitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

@Description("Module to calculate the azimuth of a source point, following its steepest direction.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Azimuth, Vector")
@Status(Status.TESTED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class SourcesAzimuth extends JGTModel {

    @Description("The feature collection of points to analize.")
    @In
    public FeatureCollection<SimpleFeatureType, SimpleFeature> inPoints;

    @Description("The list of coverages that are used to get the elevation in a buffer of the points.")
    @In
    public List<GridCoverage2D> inCoverages = null;

    @Description("The number of cells to use around the point to calculate the slope.")
    @In
    public int pCells = 3;

    @Description("The progress monitor.")
    @In
    public IHMProgressMonitor pm = new DummyProgressMonitor();

    @Description("The original points collection with the additional attribute of the azimuth.")
    @Out
    public FeatureCollection<SimpleFeatureType, SimpleFeature> outPoints = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outPoints == null, doReset)) {
            return;
        }

        outPoints = FeatureCollections.newCollection();
        SimpleFeatureType featureType = inPoints.getSchema();

        FeatureIterator<SimpleFeature> inFeatureIterator = inPoints.features();
        int id = 0;
        while( inFeatureIterator.hasNext() ) {
            SimpleFeature feature = inFeatureIterator.next();
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            Point point = geometry.getCentroid();

            for( GridCoverage2D coverage : inCoverages ) {
                Envelope2D envelope2d = coverage.getEnvelope2D();
                if (envelope2d.contains(point.getX(), point.getY())) {
                    GridGeometry2D gridGeometry = coverage.getGridGeometry();
                    GridEnvelope2D gridRange = gridGeometry.getGridRange2D();
                    int height = gridRange.height;
                    int width = gridRange.width;

                    GridCoordinates2D onGrid = gridGeometry.worldToGrid(new DirectPosition2D(point
                            .getX(), point.getY()));
                    int col = onGrid.x;
                    int row = onGrid.y;

                    double[] value = new double[1];
                    coverage.evaluate(onGrid, value);
                    double center = value[0];

                    // v11 = col1, row1
                    double v11 = getValue(coverage, onGrid, -pCells, pCells);
                    double v12 = getValue(coverage, onGrid, -pCells, 0);
                    double v13 = getValue(coverage, onGrid, -pCells, -pCells);

                    double v21 = getValue(coverage, onGrid, 0, pCells);
                    double v22 = getValue(coverage, onGrid, 0, 0);
                    double v23 = getValue(coverage, onGrid, 0, -pCells);

                    double v31 = getValue(coverage, onGrid, pCells, pCells);
                    double v32 = getValue(coverage, onGrid, pCells, 0);
                    double v33 = getValue(coverage, onGrid, pCells, -pCells);

                    double d11 = (center - v11) / sqrt(2);
                    double d12 = center - v12;
                    double d13 = (center - v13) / sqrt(2);
                    double d21 = center - v21;
                    double d22 = center - v22;
                    double d23 = center - v23;
                    double d31 = (center - v31) / sqrt(2);
                    double d32 = center - v32;
                    double d33 = (center - v33) / sqrt(2);

                    if (d11 > d12 && d11 > d13 && d11 > d21 && d11 > d22 && d11 > d23 && d11 > d31
                            && d11 > d32 && d11 > d33) {

                    }

                }

            }

            List<Object> attributesList = feature.getAttributes();

            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
            builder.addAll(attributesList);
            SimpleFeature newFeature = builder.buildFeature(featureType.getTypeName() + "." + id++);

            // newFeature.setDefaultGeometry(reprojectedGeometry);
            outPoints.add(newFeature);
        }
        inPoints.close(inFeatureIterator);
    }

    private double getValue( GridCoverage2D coverage, GridCoordinates2D center, int dCol, int dRow ) {
        GridCoordinates2D g2D = new GridCoordinates2D(center.x + dCol, center.y + dRow);
        double[] value = new double[1];
        coverage.evaluate(g2D, value);
        return value[0];
    }
}
