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
package org.jgrasstools.modules;

import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTER2XYZ_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTER2XYZ_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTER2XYZ_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTER2XYZ_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTER2XYZ_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTER2XYZ_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTER2XYZ_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTER2XYZ_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTER2XYZ_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTER2XYZ_doRemovenv_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTER2XYZ_inFile_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTER2XYZ_inRaster_DESCRIPTION;

import java.io.BufferedWriter;
import java.io.FileWriter;

import javax.media.jai.iterator.RandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.geometry.DirectPosition;

@Description(OMSRASTER2XYZ_DESCRIPTION)
@Documentation(OMSRASTER2XYZ_DOCUMENTATION)
@Author(name = OMSRASTER2XYZ_AUTHORNAMES, contact = OMSRASTER2XYZ_AUTHORCONTACTS)
@Keywords(OMSRASTER2XYZ_KEYWORDS)
@Label(OMSRASTER2XYZ_LABEL)
@Name(OMSRASTER2XYZ_NAME)
@Status(OMSRASTER2XYZ_STATUS)
@License(OMSRASTER2XYZ_LICENSE)
public class OmsRaster2Xyz extends JGTModel {

    @Description(OMSRASTER2XYZ_inRaster_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSRASTER2XYZ_inFile_DESCRIPTION)
    @In
    public String inFile;

    @Description(OMSRASTER2XYZ_doRemovenv_DESCRIPTION)
    @In
    public boolean doRemovenv = true;

    @SuppressWarnings("nls")
    @Execute
    public void process() throws Exception {
        checkNull(inRaster);

        RandomIter rasterIter = CoverageUtilities.getRandomIterator(inRaster);
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
        GridGeometry2D gridGeometry = inRaster.getGridGeometry();

        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(inFile));
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    double elevation = rasterIter.getSampleDouble(c, r, 0);
                    if (doRemovenv && JGTConstants.isNovalue(elevation)) {
                        continue;
                    }
                    DirectPosition position = gridGeometry.gridToWorld(new GridCoordinates2D(c, r));
                    double[] coordinate = position.getCoordinate();

                    StringBuilder sb = new StringBuilder();
                    sb.append(coordinate[0]);
                    sb.append("\t");
                    sb.append(coordinate[1]);
                    sb.append("\t");
                    sb.append(elevation);
                    sb.append("\n");
                    writer.write(sb.toString());
                }
            }
        } finally {
            if (writer != null)
                writer.close();
        }

    }

}
