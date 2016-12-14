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
package org.jgrasstools.lesto.modules.vector;

import static java.lang.Math.round;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.awt.Point;
import java.awt.image.WritableRaster;
import java.io.File;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.io.las.core.ALasReader;
import org.jgrasstools.gears.io.las.core.ILasHeader;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.modules.v.vectorize.OmsVectorizer;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;

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

@Description("Module that creates a vector polygon shape from the las file.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("vector, lidar")
@Label(JGTConstants.LESTO + "/vector")
@Name("lasshapevectorizer")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
@SuppressWarnings("nls")
public class LasShapeVectorizer extends JGTModel {

    @Description("Las file path.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inLas = null;

    @Description("The x resolution to use when rasterizing pre vectorizing.")
    @In
    public double pXres = 0.5;

    @Description("The y resolution to use when rasterizing pre vectorizing.")
    @In
    public double pYres = 0.5;

    @Description("The output shapefile.")
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outShp = null;

    @Execute
    public void process() throws Exception {
        checkNull(inLas);

        CoordinateReferenceSystem crs = null;
        double west = Double.POSITIVE_INFINITY;
        double south = Double.POSITIVE_INFINITY;
        double east = Double.NEGATIVE_INFINITY;
        double north = Double.NEGATIVE_INFINITY;
        // check the real minimum bounds
        pm.beginTask("Reading real bounds...", IJGTProgressMonitor.UNKNOWN);
        long recordsCount = 0;
        try (ALasReader reader = ALasReader.getReader(new File(inLas), null)) {
            reader.open();
            ILasHeader header = reader.getHeader();
            crs = header.getCrs();
            recordsCount = header.getRecordsCount();
            while( reader.hasNextPoint() ) {
                LasRecord dot = reader.getNextPoint();
                west = Math.min(west, dot.x);
                south = Math.min(south, dot.y);
                east = Math.max(east, dot.x);
                north = Math.max(north, dot.y);
            }
        }
        pm.done();
        // buffer by one resolution
        north = north + pYres;
        south = south - pYres;
        west = west - pXres;
        east = east + pXres;
        int rows = (int) round((north - south) / pYres);
        int cols = (int) round((east - west) / pXres);
        // bounds snapped on grid
        east = west + cols * pXres;
        north = south + rows * pYres;

        final GridGeometry2D gridGeometry2D = CoverageUtilities.gridGeometryFromRegionValues(north, south, east, west, cols, rows,
                crs);
        RegionMap regionMap = CoverageUtilities.gridGeometry2RegionParamsMap(gridGeometry2D);
        final WritableRaster wr = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null,
                JGTConstants.doubleNovalue);
        // now map the points on the raster
        pm.beginTask("Mapping points on raster...", (int) recordsCount);
        try (ALasReader reader = ALasReader.getReader(new File(inLas), null)) {
            reader.open();
            final Point gridPoint = new Point();
            while( reader.hasNextPoint() ) {
                LasRecord dot = reader.getNextPoint();
                double dotZ = dot.z;
                Coordinate coordinate = new Coordinate(dot.x, dot.y, dotZ);
                CoverageUtilities.colRowFromCoordinate(coordinate, gridGeometry2D, gridPoint);

                wr.setSample(gridPoint.x, gridPoint.y, 0, 1);
                pm.worked(1);
            }
        }
        pm.done();

        GridCoverage2D gridCoverage2D = CoverageUtilities.buildCoverage("mapped", wr, regionMap, crs);
        OmsVectorizer vectorizer = new OmsVectorizer();
        vectorizer.inRaster = gridCoverage2D;
        vectorizer.pm = pm;
        vectorizer.process();
        SimpleFeatureCollection outVector = vectorizer.outVector;
        dumpVector(outVector, outShp);
    }

}
