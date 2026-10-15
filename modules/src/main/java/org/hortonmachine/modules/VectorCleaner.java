/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.modules;

import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OMSVECTORCLEANER_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OMSVECTORCLEANER_AUTHORNAMES;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OMSVECTORCLEANER_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OMSVECTORCLEANER_DOCUMENTATION;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OMSVECTORCLEANER_IN_MAP_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OMSVECTORCLEANER_KEYWORDS;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OMSVECTORCLEANER_LABEL;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OMSVECTORCLEANER_LICENSE;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OMSVECTORCLEANER_NAME;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OMSVECTORCLEANER_OUT_CHANGES_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OMSVECTORCLEANER_OUT_MAP_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OMSVECTORCLEANER_P_MAX_HOLE_AREA_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OMSVECTORCLEANER_P_MIN_SLIVER_LENGTH_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OMSVECTORCLEANER_P_MIN_WIDTH_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OMSVECTORCLEANER_P_OVERLAPS_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OMSVECTORCLEANER_P_PRECISION_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OMSVECTORCLEANER_STATUS;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OVERLAPS_FIRST;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OVERLAPS_LARGEST;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OVERLAPS_MARK;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.OVERLAPS_NONE;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.PRECISION_CM;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.PRECISION_DM;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.PRECISION_M;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.PRECISION_MM;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner;

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
import oms3.annotations.UI;

@Description(OMSVECTORCLEANER_DESCRIPTION)
@Documentation(OMSVECTORCLEANER_DOCUMENTATION)
@Author(name = OMSVECTORCLEANER_AUTHORNAMES, contact = OMSVECTORCLEANER_AUTHORCONTACTS)
@Keywords(OMSVECTORCLEANER_KEYWORDS)
@Label(OMSVECTORCLEANER_LABEL)
@Name("_" + OMSVECTORCLEANER_NAME)
@Status(OMSVECTORCLEANER_STATUS)
@License(OMSVECTORCLEANER_LICENSE)
public class VectorCleaner extends HMModel {

    @Description(OMSVECTORCLEANER_IN_MAP_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inMap = null;

    @Description(OMSVECTORCLEANER_P_MAX_HOLE_AREA_DESCRIPTION)
    @In
    public double pMaxHoleArea = 300.0;

    @Description(OMSVECTORCLEANER_P_MIN_WIDTH_DESCRIPTION)
    @In
    public double pMinWidth = 10.0;

    @Description(OMSVECTORCLEANER_P_MIN_SLIVER_LENGTH_DESCRIPTION)
    @In
    public double pMinSliverLength = 30.0;

    @Description(OMSVECTORCLEANER_P_PRECISION_DESCRIPTION)
    @UI("combo:" + PRECISION_MM + "," + PRECISION_CM + "," + PRECISION_DM + "," + PRECISION_M)
    @In
    public String pPrecision = PRECISION_MM;

    @Description(OMSVECTORCLEANER_P_OVERLAPS_DESCRIPTION)
    @UI("combo:" + OVERLAPS_NONE + "," + OVERLAPS_MARK + "," + OVERLAPS_LARGEST + "," + OVERLAPS_FIRST)
    @In
    public String pOverlaps = OVERLAPS_NONE;

    @Description(OMSVECTORCLEANER_OUT_MAP_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outMap = null;

    @Description(OMSVECTORCLEANER_OUT_CHANGES_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outChanges = null;

    @Execute
    public void process() throws Exception {
        OmsVectorCleaner vectorcleaner = new OmsVectorCleaner();
        vectorcleaner.inMap = getVector(inMap);
        vectorcleaner.pMaxHoleArea = pMaxHoleArea;
        vectorcleaner.pMinWidth = pMinWidth;
        vectorcleaner.pMinSliverLength = pMinSliverLength;
        vectorcleaner.pPrecision = pPrecision;
        vectorcleaner.pOverlaps = pOverlaps;
        vectorcleaner.pm = pm;
        vectorcleaner.doProcess = doProcess;
        vectorcleaner.doReset = doReset;
        vectorcleaner.process();
        dumpVector(vectorcleaner.outMap, outMap);
        if (outChanges != null) {
            dumpVector(vectorcleaner.outChanges, outChanges);
        }
    }
}
