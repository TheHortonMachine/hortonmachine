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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_F_ID_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_F_NEW_ID_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_IN_MAP_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_IN_POINTS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_OUT_MAP_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_STATUS;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.v.polygonize.OmsLinesPolygonizer;

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

@Description(OMSLINESPOLYGONIZER_DESCRIPTION)
@Author(name = OMSLINESPOLYGONIZER_AUTHORNAMES, contact = OMSLINESPOLYGONIZER_AUTHORCONTACTS)
@Keywords(OMSLINESPOLYGONIZER_KEYWORDS)
@Label(OMSLINESPOLYGONIZER_LABEL)
@Name("_" + OMSLINESPOLYGONIZER_NAME)
@Status(OMSLINESPOLYGONIZER_STATUS)
@License(OMSLINESPOLYGONIZER_LICENSE)
public class LinesPolygonizer extends HMModel {

    @Description(OMSLINESPOLYGONIZER_IN_MAP_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inMap = null;

    @Description(OMSLINESPOLYGONIZER_IN_POINTS_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inPoints = null;

    @Description(OMSLINESPOLYGONIZER_F_ID_DESCRIPTION)
    @In
    public String fId = null;

    @Description(OMSLINESPOLYGONIZER_F_NEW_ID_DESCRIPTION)
    @In
    public String fNewId = "id";

    @Description(OMSLINESPOLYGONIZER_OUT_MAP_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outMap = null;

    @Execute
    public void process() throws Exception {
        OmsLinesPolygonizer omslinespolygonizer = new OmsLinesPolygonizer();
        omslinespolygonizer.inMap = getVector(inMap);
        omslinespolygonizer.inPoints = getVector(inPoints);
        omslinespolygonizer.fId = fId;
        omslinespolygonizer.fNewId = fNewId;
        omslinespolygonizer.pm = pm;
        omslinespolygonizer.doProcess = doProcess;
        omslinespolygonizer.doReset = doReset;
        omslinespolygonizer.process();
        dumpVector(omslinespolygonizer.outMap, outMap);
    }
}
