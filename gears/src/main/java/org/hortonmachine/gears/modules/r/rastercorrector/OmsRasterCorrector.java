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
package org.hortonmachine.gears.modules.r.rastercorrector;

import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;
import static org.hortonmachine.gears.modules.r.rastercorrector.OmsRasterCorrector.OMSRASTERCORRECTOR_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.rastercorrector.OmsRasterCorrector.OMSRASTERCORRECTOR_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.rastercorrector.OmsRasterCorrector.OMSRASTERCORRECTOR_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.rastercorrector.OmsRasterCorrector.OMSRASTERCORRECTOR_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.rastercorrector.OmsRasterCorrector.OMSRASTERCORRECTOR_KEYWORDS;
import static org.hortonmachine.gears.modules.r.rastercorrector.OmsRasterCorrector.OMSRASTERCORRECTOR_LABEL;
import static org.hortonmachine.gears.modules.r.rastercorrector.OmsRasterCorrector.OMSRASTERCORRECTOR_LICENSE;
import static org.hortonmachine.gears.modules.r.rastercorrector.OmsRasterCorrector.OMSRASTERCORRECTOR_NAME;
import static org.hortonmachine.gears.modules.r.rastercorrector.OmsRasterCorrector.OMSRASTERCORRECTOR_STATUS;

import java.awt.Point;
import java.awt.image.DataBuffer;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.HMRaster.HMRasterWritableBuilder;
import org.locationtech.jts.geom.Coordinate;

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

@Description(OMSRASTERCORRECTOR_DESCRIPTION)
@Documentation(OMSRASTERCORRECTOR_DOCUMENTATION)
@Author(name = OMSRASTERCORRECTOR_AUTHORNAMES, contact = OMSRASTERCORRECTOR_AUTHORCONTACTS)
@Keywords(OMSRASTERCORRECTOR_KEYWORDS)
@Label(OMSRASTERCORRECTOR_LABEL)
@Name(OMSRASTERCORRECTOR_NAME)
@Status(OMSRASTERCORRECTOR_STATUS)
@License(OMSRASTERCORRECTOR_LICENSE)
public class OmsRasterCorrector extends HMModel {

    @Description(OMSRASTERCORRECTOR_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSRASTERCORRECTOR_P_CORRECTIONS_DESCRIPTION)
    @In
    public String pCorrections;

    @Description(OMSRASTERCORRECTOR_DO_COORDINATES_DESCRIPTION)
    @In
    public boolean doCoordinates = false;

    @Description(OMSRASTERCORRECTOR_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster;

    public static final String OMSRASTERCORRECTOR_DESCRIPTION = "Sets single cells of a raster to new values, given by their column and row or by their coordinates.";
    public static final String OMSRASTERCORRECTOR_DOCUMENTATION = """
            The corrections are a comma separated list of triplets, as \
            col1,row1,value1,col2,row2,value2: the column and row start from 0 at the north-west \
            corner, the row 0 being the northern one. With the coordinates switch the triplets \
            are east,north,value instead, in the reference system of the raster, as read off a \
            map: each one corrects the cell that contains the point. The cells outside of the \
            raster are left out with a warning. A value equal to the novalue of the raster \
            removes the cell. The byte, short and integer rasters keep their type, the others \
            become double.

            It is meant for a few manual fixes, as a spike in an elevation model or the outlet \
            cell that a network extraction needs to be a channel: to change many cells, Mapcalc \
            or RasterNull are the tools.
            """;
    public static final String OMSRASTERCORRECTOR_KEYWORDS = "Raster, Correct, Edit, Cells";
    public static final String OMSRASTERCORRECTOR_LABEL = RASTERPROCESSING;
    public static final String OMSRASTERCORRECTOR_NAME = "rcorrect";
    public static final int OMSRASTERCORRECTOR_STATUS = 40;
    public static final String OMSRASTERCORRECTOR_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSRASTERCORRECTOR_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSRASTERCORRECTOR_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSRASTERCORRECTOR_IN_RASTER_DESCRIPTION = "The raster to correct.";
    public static final String OMSRASTERCORRECTOR_P_CORRECTIONS_DESCRIPTION = "The corrections, as a comma separated list of triplets: col1,row1,value1,col2,row2,value2... or, with the coordinates, east1,north1,value1...";
    public static final String OMSRASTERCORRECTOR_DO_COORDINATES_DESCRIPTION = "If true, the triplets have the coordinates of the cells instead of their column and row.";
    public static final String OMSRASTERCORRECTOR_OUT_RASTER_DESCRIPTION = "The corrected raster.";

    @Execute
    public void process() throws Exception {
        checkNull(inRaster, pCorrections);

        String[] correctionSplit = pCorrections.split(","); //$NON-NLS-1$
        if (correctionSplit.length % 3 != 0) {
            throw new ModelsIllegalargumentException(
                    "The format of the corrections is: col1,row1,value1,col2,row2,value2... or east1,north1,value1...", this,
                    pm);
        }

        try (HMRaster inData = HMRaster.fromGridCoverage(inRaster); HMRaster outData = buildCopy(inData)) {
            for( int i = 0; i < correctionSplit.length; i = i + 3 ) {
                String first = correctionSplit[i].trim();
                String second = correctionSplit[i + 1].trim();
                double value = Double.parseDouble(correctionSplit[i + 2].trim());

                Point cell;
                if (doCoordinates) {
                    cell = outData.getCell(new Coordinate(Double.parseDouble(first), Double.parseDouble(second)));
                } else {
                    cell = new Point(Integer.parseInt(first), Integer.parseInt(second));
                }
                if (!outData.isContained(cell.x, cell.y)) {
                    pm.errorMessage("The correction at " + first + "," + second + " is outside of the raster, it is left out.");
                    continue;
                }
                outData.setValue(cell.x, cell.y, value);
            }

            outRaster = outData.buildCoverage();
        }
    }

    /**
     * A writable copy of the raster, that keeps its data type if it is an integer one.
     */
    private HMRaster buildCopy( HMRaster inData ) {
        HMRasterWritableBuilder builder = new HMRasterWritableBuilder().setName("corrected").setTemplate(inData)
                .setCopyValues(true);
        switch( inData.getDataType() ) {
        case DataBuffer.TYPE_BYTE:
            builder.setDoByte(true);
            break;
        case DataBuffer.TYPE_SHORT:
            builder.setDoShort(true);
            break;
        case DataBuffer.TYPE_INT:
            builder.setDoInteger(true);
            break;
        default:
            break;
        }
        return builder.build();
    }

}
