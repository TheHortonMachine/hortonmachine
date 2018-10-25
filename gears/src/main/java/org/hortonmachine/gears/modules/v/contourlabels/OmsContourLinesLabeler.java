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
package org.hortonmachine.gears.modules.v.contourlabels;

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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSCONTOURLINESLABELER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCONTOURLINESLABELER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCONTOURLINESLABELER_BUFFER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCONTOURLINESLABELER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCONTOURLINESLABELER_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCONTOURLINESLABELER_F_ELEVATION_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCONTOURLINESLABELER_INLINES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCONTOURLINESLABELER_IN_CONTOUR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCONTOURLINESLABELER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCONTOURLINESLABELER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCONTOURLINESLABELER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCONTOURLINESLABELER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCONTOURLINESLABELER_OUTPOINTS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCONTOURLINESLABELER_STATUS;

import org.geotools.data.memory.MemoryDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FilterUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

@Description(OMSCONTOURLINESLABELER_DESCRIPTION)
@Documentation(OMSCONTOURLINESLABELER_DOCUMENTATION)
@Author(name = OMSCONTOURLINESLABELER_AUTHORNAMES, contact = OMSCONTOURLINESLABELER_AUTHORCONTACTS)
@Keywords(OMSCONTOURLINESLABELER_KEYWORDS)
@Label(OMSCONTOURLINESLABELER_LABEL)
@Name(OMSCONTOURLINESLABELER_NAME)
@Status(OMSCONTOURLINESLABELER_STATUS)
@License(OMSCONTOURLINESLABELER_LICENSE)
public class OmsContourLinesLabeler extends HMModel {

    @Description(OMSCONTOURLINESLABELER_IN_CONTOUR_DESCRIPTION)
    @In
    public SimpleFeatureCollection inContour;

    @Description(OMSCONTOURLINESLABELER_F_ELEVATION_DESCRIPTION)
    @In
    public String fElevation;

    @Description(OMSCONTOURLINESLABELER_INLINES_DESCRIPTION)
    @In
    public SimpleFeatureCollection inLines;

    @Description(OMSCONTOURLINESLABELER_BUFFER_DESCRIPTION)
    @In
    public double buffer;

    @Description(OMSCONTOURLINESLABELER_OUTPOINTS_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outPoints = null;

    @SuppressWarnings("nls")
    @Execute
    public void process() throws Exception {
        if (!concatOr(outPoints == null, doReset)) {
            return;
        }
        SimpleFeatureType inSchema = inContour.getSchema();
        MemoryDataStore memDatastore = new MemoryDataStore(inContour);
        SimpleFeatureSource contourSource = memDatastore.getFeatureSource(memDatastore.getTypeNames()[0]);

        CoordinateReferenceSystem crs = inSchema.getCoordinateReferenceSystem();

        outPoints = new DefaultFeatureCollection();
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
            SimpleFeatureCollection filteredContours = contourSource.getFeatures(bboxFilter);

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
                    SimpleFeature pointFeature = builder.buildFeature(outType.getTypeName() + "." + count++);
                    ((DefaultFeatureCollection) outPoints).add(pointFeature);
                }

            }

        }

    }

}
