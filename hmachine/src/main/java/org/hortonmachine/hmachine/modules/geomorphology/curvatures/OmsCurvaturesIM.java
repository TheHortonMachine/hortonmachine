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
package org.hortonmachine.hmachine.modules.geomorphology.curvatures;

import static org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvatures.*;

import java.io.File;

import javax.media.jai.iterator.RandomIter;

import org.hortonmachine.gears.libs.modules.GridNode;
import org.hortonmachine.gears.libs.modules.HMModelIM;
import org.hortonmachine.gears.utils.colors.EColorTables;

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

@Description(OMSCURVATURES_DESCRIPTION)
@Documentation(OMSCURVATURES_DOCUMENTATION)
@Author(name = OMSCURVATURES_AUTHORNAMES, contact = OMSCURVATURES_AUTHORCONTACTS)
@Keywords(OMSCURVATURES_KEYWORDS)
@Label(OMSCURVATURES_LABEL)
@Name(OMSCURVATURES_NAME)
@Status(OMSCURVATURES_STATUS)
@License(OMSCURVATURES_LICENSE)
public class OmsCurvaturesIM extends HMModelIM {
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

        processByTileCells();

        makeMosaic();
        makeStyle(EColorTables.extrainbow, 0, 1);

        dispose();
    }

    @Override
    protected void processCell( int readCol, int readRow, int writeCol, int writeRow, int readCols, int readRows, int writeCols,
            int writeRows ) {

        RandomIter elevIter = inRasterIterators.get(0);
        Double novalue = inRasterNovalues.get(0);

        GridNode node = new GridNode(elevIter, readCols, readRows, xRes, yRes, readCol, readRow, novalue);
        OmsCurvatures.calculateCurvatures2(node, planTangProf);
        if (outPlan != null)
            outRasterIterators.get(0).setSample(writeCol, writeRow, 0, planTangProf[0]);
        if (outTang != null)
            outRasterIterators.get(1).setSample(writeCol, writeRow, 0, planTangProf[1]);
        if (outProf != null)
            outRasterIterators.get(2).setSample(writeCol, writeRow, 0, planTangProf[2]);

    }
}
