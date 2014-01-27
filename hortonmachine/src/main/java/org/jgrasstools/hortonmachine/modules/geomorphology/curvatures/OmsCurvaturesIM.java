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
package org.jgrasstools.hortonmachine.modules.geomorphology.curvatures;

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_DOCUMENTATION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_inElev_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_outPlan_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_outProf_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCURVATURES_outTang_DESCRIPTION;

import java.io.File;

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
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.jgrasstools.gears.libs.modules.JGTModelIM;
import org.jgrasstools.gears.utils.colors.ColorTables;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;
import org.jgrasstools.hortonmachine.modules.geomorphology.aspect.OmsAspectIM;

@Description(OMSCURVATURES_DESCRIPTION)
@Documentation(OMSCURVATURES_DOCUMENTATION)
@Author(name = OMSCURVATURES_AUTHORNAMES, contact = OMSCURVATURES_AUTHORCONTACTS)
@Keywords(OMSCURVATURES_KEYWORDS)
@Label(OMSCURVATURES_LABEL)
@Name(OMSCURVATURES_NAME)
@Status(OMSCURVATURES_STATUS)
@License(OMSCURVATURES_LICENSE)
public class OmsCurvaturesIM extends JGTModelIM {
    @Description(OMSCURVATURES_inElev_DESCRIPTION)
    @In
    public String inElev = null;

    // output
    @Description(OMSCURVATURES_outPlan_DESCRIPTION)
    @Out
    public String outPlan = null;

    @Description(OMSCURVATURES_outTang_DESCRIPTION)
    @Out
    public String outTang = null;

    @Description(OMSCURVATURES_outProf_DESCRIPTION)
    @Out
    public String outProf = null;

    private double[] planTangProf = new double[3];
    private double disXX;
    private double disYY;

    @Execute
    public void process() throws Exception {
        addSource(new File(inElev));
        if (outPlan != null)
            addDestination(new File(outPlan), 0);
        if (outTang != null)
            addDestination(new File(outTang), 1);
        if (outProf != null)
            addDestination(new File(outProf), 2);

        cellBuffer = 1;
        disXX = Math.pow(xRes, 2.0);
        disYY = Math.pow(yRes, 2.0);

        processByTileCells();

        makeMosaic();
        makeStyle(ColorTables.extrainbow, 0, 1);
    }

    @Override
    protected void processCell( int readCol, int readRow, int writeCol, int writeRow, int readCols, int readRows, int writeCols,
            int writeRows ) {

        RandomIter elevIter = inRasterIterators.get(0);
        OmsCurvatures.calculateCurvatures(elevIter, planTangProf, readCol, readRow, xRes, yRes, disXX, disYY);
        if (outPlan != null)
            outRasters.get(0).setSample(writeCol, writeRow, 0, planTangProf[0]);
        if (outTang != null)
            outRasters.get(1).setSample(writeCol, writeRow, 0, planTangProf[1]);
        if (outProf != null)
            outRasters.get(2).setSample(writeCol, writeRow, 0, planTangProf[2]);

    }
}
