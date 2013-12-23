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
package org.jgrasstools.hortonmachine.modules.geomorphology.gradient;

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_DOCUMENTATION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_doDegrees_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_outSlope_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_pMode_DESCRIPTION;

import java.io.File;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.WritableRandomIter;

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

import org.jgrasstools.gears.libs.modules.JGTModelIM;
import org.jgrasstools.gears.utils.colors.ColorTables;

@Description(OMSGRADIENT_DESCRIPTION)
@Documentation(OMSGRADIENT_DOCUMENTATION)
@Author(name = OMSGRADIENT_AUTHORNAMES, contact = OMSGRADIENT_AUTHORCONTACTS)
@Keywords(OMSGRADIENT_KEYWORDS)
@Label(OMSGRADIENT_LABEL)
@Name("gradient_im")
@Status(OMSGRADIENT_STATUS)
@License(OMSGRADIENT_LICENSE)
public class OmsGradientIM extends JGTModelIM {
    @Description("The imagemosaic map of the digital elevation model (DEM or pit).")
    @In
    public String inElev = null;

    @Description(OMSGRADIENT_pMode_DESCRIPTION)
    @In
    public int pMode = 0;

    @Description(OMSGRADIENT_doDegrees_DESCRIPTION)
    @In
    public boolean doDegrees = false;

    @Description(OMSGRADIENT_outSlope_DESCRIPTION)
    @Out
    public String outSlope = null;

    @Execute
    public void process() throws Exception {
        checkNull(inElev);

        cellBuffer = 1;
        if (pMode == 1) {
            pm.message("Using Horn formula");
        } else if (pMode == 2) {
            pm.message("Using Evans formula");
        } else {
            pm.message("Using finite differences");
        }

        addSource(new File(inElev));
        addDestination(new File(outSlope));

        processByTileCells();

        makeMosaic();
        makeStyle(ColorTables.extrainbow, 0, 1);
    }

    @Override
    protected void processCell( int readCol, int readRow, int writeCol, int writeRow, int readCols, int readRows, int writeCols,
            int writeRows ) {
        RandomIter elevIter = inRasterIterators.get(0);
        double gradient;
        switch( pMode ) {
        case 1:
            gradient = OmsGradient.doGradientHornOnCell(elevIter, readCol, readRow, xRes, yRes, doDegrees);
            break;
        case 2:
            gradient = OmsGradient.doGradientEvansOnCell(elevIter, readCol, readRow, xRes, yRes, doDegrees);
            break;
        default:
            gradient = OmsGradient.doGradientDiffOnCell(elevIter, readCol, readRow, xRes, yRes, doDegrees);
            break;
        }
        WritableRandomIter outDataIter = outRasters.get(0);
        outDataIter.setSample(writeCol, writeRow, 0, gradient);
    }

    public static void main( String[] args ) throws Exception {
        OmsGradientIM g = new OmsGradientIM();
        g.inElev = "/media/lacntfs/oceandtm/q1swb_2008_export_043_xyz2_1m/q1swb_2008_export_043_xyz2_1m.shp";
        g.outSlope = "/media/lacntfs/oceandtm/q1swb_2008_export_043_xyz2_1m_gradient//q1swb_2008_export_043_xyz2_1m_gradient.shp";
        g.process();

    }

}
