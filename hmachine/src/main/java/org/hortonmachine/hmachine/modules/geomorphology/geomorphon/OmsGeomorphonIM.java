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
package org.hortonmachine.hmachine.modules.geomorphology.geomorphon;
import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.lang.Math.sqrt;

import java.io.File;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.Unit;

import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModelIM;
import org.hortonmachine.gears.utils.colors.EColorTables;
import org.opengis.referencing.operation.TransformException;

@Description("The Geomorphon method for rasters - image mosaic version")
@Author(name = "Andrea Antonello, Silvia Franceschi", contact = "www.hydrologis.com")
@Keywords("raster, geomorphon")
@Label(HMConstants.RASTERPROCESSING)
@Name("geomorphonraster")
@Status(Status.EXPERIMENTAL)
@License(HMConstants.GPL3_LICENSE)
public class OmsGeomorphonIM extends HMModelIM {
    @Description("An elevation raster.")
    @In
    public String inElev;

    @Description("Maximum search radius")
    @Unit("m")
    @In
    public double pRadius;

    @Description("Vertical angle threshold.")
    @Unit("degree")
    @In
    public double pThreshold = 1;

    @Description("Output categories raster.")
    @Out
    public String outRaster;

    private double diagonalDelta;

    public void process() throws Exception {
        checkNull(inElev);

        if (pRadius <= 0) {
            throw new ModelsIllegalargumentException("The search radius has to be > 0.", this, pm);
        }
        diagonalDelta = pRadius / sqrt(2.0);

        addSource(new File(inElev));
        addDestination(new File(outRaster));

        // calculate cellbuffer through the search radius
        cellBuffer = (int) ceil(pRadius / max(xRes, yRes));
        pm.message("Using a cell buffer of: " + cellBuffer);

        processByTileCells();

        makeMosaic();
        makeStyle(EColorTables.geomorphon, 1000, 1008);
        
        dispose();
    }

    @Override
    protected void processCell( int readCol, int readRow, int writeCol, int writeRow, int readCols, int readRows, int writeCols,
            int writeRows ) {
        try {
            RandomIter elevIter = inRasterIterators.get(0);
            double classification = OmsGeomorphon.calculateGeomorphon(elevIter, readGridGeometry, pRadius, pThreshold,
                    diagonalDelta, readCol, readRow);
            WritableRandomIter outDataIter = outRasterIterators.get(0);
            outDataIter.setSample(writeCol, writeRow, 0, classification);
        } catch (TransformException e) {
            e.printStackTrace();
        }
    }

}
