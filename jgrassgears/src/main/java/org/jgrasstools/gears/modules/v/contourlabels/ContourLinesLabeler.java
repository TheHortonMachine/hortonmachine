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
package org.jgrasstools.gears.modules.v.contourlabels;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.data.FeatureSource;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IHMProgressMonitor;
import org.jgrasstools.gears.utils.features.FilterUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

@Description("Generates a layer of point features with a given label text and angle, following "
        + "reference lines intersecting them with a layer of countourlines.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Contourlines, Vector")
@Status(Status.TESTED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class ContourLinesLabeler extends JGTModel {

    @Description("The contour lines.")
    @In
    public FeatureCollection<SimpleFeatureType, SimpleFeature> inContour;

    @Description("Field name of the contour elevation")
    @In
    public String fElevation;

    @Description("The lines to intersect with the contours to generate label points.")
    @In
    public FeatureCollection<SimpleFeatureType, SimpleFeature> inLines;

    @Description("The buffer to consider for every line.")
    @In
    public double buffer;

    @Description("The progress monitor.")
    @In
    public IHMProgressMonitor pm = new DummyProgressMonitor();

    @Description("The labeled point layer.")
    @Out
    public FeatureCollection<SimpleFeatureType, SimpleFeature> outPoints = null;

    @SuppressWarnings("nls")
    @Execute
    public void process() throws Exception {
        if (!concatOr(outPoints == null, doReset)) {
            return;
        }
        SimpleFeatureType inSchema = inContour.getSchema();
        MemoryDataStore memDatastore = new MemoryDataStore(inContour);
        FeatureSource<SimpleFeatureType, SimpleFeature> contourSource = memDatastore
                .getFeatureSource(memDatastore.getTypeNames()[0]);

        CoordinateReferenceSystem crs = inSchema.getCoordinateReferenceSystem();

        outPoints = FeatureCollections.newCollection();
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("labelpoints");
        b.setCRS(crs);
        b.add("the_geom", Point.class);
        b.add("elevation", Double.class);
        b.add("azimuth", Double.class);
        b.add("minus90", Double.class);
        SimpleFeatureType outType = b.buildFeatureType();
        int count = 0;

        FeatureIterator<SimpleFeature> lineFeatureIterator = inLines.features();
        while( lineFeatureIterator.hasNext() ) {
            SimpleFeature line = lineFeatureIterator.next();
            Geometry lineGeom = (Geometry) line.getDefaultGeometry();
            Geometry lineBuffer = lineGeom.buffer(buffer);
            BoundingBox lineBounds = line.getBounds();

            Filter bboxFilter = FilterUtilities.getBboxFilter("the_geom", lineBounds);
            FeatureCollection<SimpleFeatureType, SimpleFeature> filteredContours = contourSource
                    .getFeatures(bboxFilter);

            FeatureIterator<SimpleFeature> contourIterator = filteredContours.features();
            while( contourIterator.hasNext() ) {
                SimpleFeature contour = contourIterator.next();
                Geometry contourGeom = (Geometry) contour.getDefaultGeometry();

                if (lineBuffer.intersects(contourGeom)) {
                    Geometry intersection = lineBuffer.intersection(contourGeom);

                    Coordinate[] coordinates = intersection.getCoordinates();
                    Coordinate first = coordinates[0];
                    Coordinate second = coordinates[1];

                    double azimuth = GeometryUtilities.azimuth(first, second);
                    double azimuthFrom90 = azimuth - 90.0;
                    if (azimuthFrom90 < 0) {
                        azimuthFrom90 = 360.0 + azimuthFrom90;
                    }
                    double elevation = ((Number) contour.getAttribute(fElevation)).doubleValue();
                    Point labelPoint = GeometryUtilities.gf().createPoint(first);

                    SimpleFeatureBuilder builder = new SimpleFeatureBuilder(outType);
                    Object[] values = new Object[]{labelPoint, elevation, azimuth, azimuthFrom90};
                    builder.addAll(values);
                    SimpleFeature pointFeature = builder.buildFeature(outType.getTypeName() + "."
                            + count++);
                    outPoints.add(pointFeature);
                }

            }

        }

    }

}
