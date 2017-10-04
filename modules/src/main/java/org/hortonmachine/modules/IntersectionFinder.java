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
package org.hortonmachine.modules;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSINTERSECTIONFINDER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSINTERSECTIONFINDER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSINTERSECTIONFINDER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSINTERSECTIONFINDER_IN_MAP_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSINTERSECTIONFINDER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSINTERSECTIONFINDER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSINTERSECTIONFINDER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSINTERSECTIONFINDER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSINTERSECTIONFINDER_OUT_LINES_MAP_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSINTERSECTIONFINDER_OUT_POINTS_MAP_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSINTERSECTIONFINDER_STATUS;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.v.intersections.OmsIntersectionFinder;

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

@Description(OMSINTERSECTIONFINDER_DESCRIPTION)
@Author(name = OMSINTERSECTIONFINDER_AUTHORNAMES, contact = OMSINTERSECTIONFINDER_AUTHORCONTACTS)
@Keywords(OMSINTERSECTIONFINDER_KEYWORDS)
@Label(OMSINTERSECTIONFINDER_LABEL)
@Name("_" + OMSINTERSECTIONFINDER_NAME)
@Status(OMSINTERSECTIONFINDER_STATUS)
@License(OMSINTERSECTIONFINDER_LICENSE)
public class IntersectionFinder extends HMModel {

    @Description(OMSINTERSECTIONFINDER_IN_MAP_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inMap = null;

    @Description(OMSINTERSECTIONFINDER_OUT_POINTS_MAP_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outPointsMap = null;

    @Description(OMSINTERSECTIONFINDER_OUT_LINES_MAP_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outLinesMap = null;

    @Execute
    public void process() throws Exception {
        OmsIntersectionFinder intersectionfinder = new OmsIntersectionFinder();
        intersectionfinder.inMap = getVector(inMap);
        intersectionfinder.pm = pm;
        intersectionfinder.doProcess = doProcess;
        intersectionfinder.doReset = doReset;
        intersectionfinder.process();
        dumpVector(intersectionfinder.outPointsMap, outPointsMap);
        dumpVector(intersectionfinder.outLinesMap, outLinesMap);
    }
}
