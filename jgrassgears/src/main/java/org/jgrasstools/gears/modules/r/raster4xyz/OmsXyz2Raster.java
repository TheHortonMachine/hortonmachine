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
package org.jgrasstools.gears.modules.r.raster4xyz;

import static org.jgrasstools.gears.i18n.GearsMessages.OMSXYZ2RASTER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSXYZ2RASTER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSXYZ2RASTER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSXYZ2RASTER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSXYZ2RASTER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSXYZ2RASTER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSXYZ2RASTER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSXYZ2RASTER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSXYZ2RASTER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSXYZ2RASTER_inFile_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSXYZ2RASTER_outRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSXYZ2RASTER_pCode_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSXYZ2RASTER_pRes_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSXYZ2RASTER_pSeparator_DESCRIPTION;

import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

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
import oms3.annotations.UI;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.CRS;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

@Description(OMSXYZ2RASTER_DESCRIPTION)
@Documentation(OMSXYZ2RASTER_DOCUMENTATION)
@Author(name = OMSXYZ2RASTER_AUTHORNAMES, contact = OMSXYZ2RASTER_AUTHORCONTACTS)
@Keywords(OMSXYZ2RASTER_KEYWORDS)
@Label(OMSXYZ2RASTER_LABEL)
@Name(OMSXYZ2RASTER_NAME)
@Status(OMSXYZ2RASTER_STATUS)
@License(OMSXYZ2RASTER_LICENSE)
public class OmsXyz2Raster extends JGTModel {

    @Description(OMSXYZ2RASTER_inFile_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inFile;

    @Description(OMSXYZ2RASTER_pRes_DESCRIPTION)
    @In
    public Double pRes;

    @Description(OMSXYZ2RASTER_pCode_DESCRIPTION)
    @UI(JGTConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description(OMSXYZ2RASTER_pSeparator_DESCRIPTION)
    @In
    public String pSeparator;

    @Description(OMSXYZ2RASTER_outRaster_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster;

    private double res = 0.0;

    @SuppressWarnings("nls")
    @Execute
    public void process() throws Exception {
        checkNull(inFile, pRes, pCode);

        CoordinateReferenceSystem crs = CRS.decode(pCode);
        res = pRes;

        Envelope env = null;
        pm.beginTask("Reading triplets file...", IJGTProgressMonitor.UNKNOWN);
        List<String> linesList = FileUtilities.readFileToLinesList(inFile);
        pm.done();

        List<Coordinate> coordList = new ArrayList<Coordinate>();

        double n = Double.NEGATIVE_INFINITY;
        double s = Double.POSITIVE_INFINITY;
        double e = Double.NEGATIVE_INFINITY;
        double w = Double.POSITIVE_INFINITY;
        if (pSeparator == null) {
            pSeparator = "\\s+";
        }
        pm.beginTask("Extracting data...", linesList.size());
        for( String line : linesList ) {
            String[] split = line.trim().split(pSeparator);
            if (split.length != 3) {
                pm.worked(1);
                continue;
            }
            double x = Double.parseDouble(split[0].trim());
            if (x > e) {
                e = x;
            }
            if (x < w) {
                w = x;
            }
            double y = Double.parseDouble(split[1].trim());
            if (y > n) {
                n = y;
            }
            if (y < s) {
                s = y;
            }
            double z = Double.parseDouble(split[2].trim());
            Coordinate c = new Coordinate(x, y, z);
            coordList.add(c);

            if (env == null) {
                env = new Envelope(c);
            } else {
                env.expandToInclude(c);
            }
            pm.worked(1);
        }
        pm.done();

        int rows = (int) ((n - s) / res);
        int cols = (int) ((e - w) / res);
        GridGeometry2D gridGeometry = CoverageUtilities.gridGeometryFromRegionValues(n, s, e, w, rows, cols, crs);

        WritableRaster writableRaster = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null,
                JGTConstants.doubleNovalue);

        pm.beginTask("Create raster...", coordList.size());
        DirectPosition2D world = new DirectPosition2D();
        for( Coordinate coordinate : coordList ) {
            world.setLocation(coordinate.x, coordinate.y);
            GridCoordinates2D grid = gridGeometry.worldToGrid(world);
            writableRaster.setSample(grid.x, grid.y, 0, coordinate.z);
            pm.worked(1);
        }
        pm.done();

        RegionMap regionMap = CoverageUtilities.gridGeometry2RegionParamsMap(gridGeometry);
        outRaster = CoverageUtilities.buildCoverage("fromxyz", writableRaster, regionMap, crs);

    }

}
