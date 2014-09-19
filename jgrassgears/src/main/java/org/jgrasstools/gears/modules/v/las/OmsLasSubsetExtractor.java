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
package org.jgrasstools.gears.modules.v.las;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.io.las.core.ALasReader;
import org.jgrasstools.gears.io.las.core.ALasWriter;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.io.las.core.v_1_0.LasWriter;
import org.jgrasstools.gears.io.rasterreader.OmsRasterReader;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;

@Description("Module that can create subsets of a las file.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("las, lidar, extract")
@Label(JGTConstants.LAS)
@Name("lassubsetextractor")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
@SuppressWarnings("nls")
public class OmsLasSubsetExtractor extends JGTModel {

    @Description("The las file.")
    @In
    public String inFile = null;

    @Description("An optional raster map to use for extraction.")
    @In
    public GridCoverage2D inRaster;

    @Description("An optional vector map to use for extraction.")
    @In
    public SimpleFeatureCollection inVector;

    @Description("If true and the input raster is supplied normalization is also applied.")
    @In
    public boolean doDifference = true;

    @Description("The output las file.")
    @In
    public String outLasFile = null;

    @Execute
    public void process() throws Exception {
        checkNull(inFile);

        if (inRaster == null && inVector == null) {
            throw new ModelsIllegalargumentException("One of raster or vector need to be supplied.", this);
        }

        Polygon polygon;
        CoordinateReferenceSystem crs;
        if (inRaster != null) {
            polygon = CoverageUtilities.getRegionPolygon(inRaster);
            crs = inRaster.getCoordinateReferenceSystem();
        } else {
            List<Geometry> geoms = FeatureUtilities.featureCollectionToGeometriesList(inVector, true, null);
            polygon = (Polygon) geoms.get(0);
            crs = inVector.getSchema().getCoordinateReferenceSystem();
        }
        PreparedGeometry preparedBounds = PreparedGeometryFactory.prepare(polygon);

        double xMin = Double.POSITIVE_INFINITY;
        double yMin = Double.POSITIVE_INFINITY;
        double zMin = Double.POSITIVE_INFINITY;
        double xMax = Double.NEGATIVE_INFINITY;
        double yMax = Double.NEGATIVE_INFINITY;
        double zMax = Double.NEGATIVE_INFINITY;
        List<LasRecord> points = new ArrayList<>();
        try (ALasReader lasReader = ALasReader.getReader(new File(inFile), crs)) {
            lasReader.open();
            long recordsCount = lasReader.getHeader().getRecordsCount();
            pm.beginTask("Reading and filtering data...", (int) recordsCount);
            while( lasReader.hasNextPoint() ) {
                LasRecord dot = lasReader.getNextPoint();

                Coordinate coordinate = new Coordinate(dot.x, dot.y);
                Point point = gf.createPoint(coordinate);
                if (preparedBounds.intersects(point)) {
                    if (doDifference && inRaster != null) {
                        double value = CoverageUtilities.getValue(inRaster, coordinate);
                        if (!JGTConstants.isNovalue(value)) {
                            dot.z = dot.z - value;
                        }else{
                            dot.z = 0.0;
                        }
                    }
                    xMin = min(xMin, dot.x);
                    yMin = min(yMin, dot.y);
                    zMin = min(zMin, dot.z);
                    xMax = max(xMax, dot.x);
                    yMax = max(yMax, dot.y);
                    zMax = max(zMax, dot.z);
                    points.add(dot);
                }
                pm.worked(1);
            }
            pm.done();
        }

        pm.beginTask("Writing las file...", points.size());
        File outFile = new File(outLasFile);
        try (ALasWriter w = new LasWriter(outFile, crs)) {
            w.setBounds(xMin, xMax, yMin, yMax, zMin, zMax);
            w.open();
            for( LasRecord lasRecord : points ) {
                w.addPoint(lasRecord);
                pm.worked(1);
            }
        }
        pm.done();
    }



}
