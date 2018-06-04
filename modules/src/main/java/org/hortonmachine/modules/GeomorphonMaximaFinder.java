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
import static org.hortonmachine.lesto.modules.vegetation.OmsGeomorphonMaximaFinder.inDSM_DESC;
import static org.hortonmachine.lesto.modules.vegetation.OmsGeomorphonMaximaFinder.inDTM_DESC;
import static org.hortonmachine.lesto.modules.vegetation.OmsGeomorphonMaximaFinder.outMaxima_DESC;
import static org.hortonmachine.lesto.modules.vegetation.OmsGeomorphonMaximaFinder.pElevDiffThres_UNIT;
import static org.hortonmachine.lesto.modules.vegetation.OmsGeomorphonMaximaFinder.pElevDiff_DESC;
import static org.hortonmachine.lesto.modules.vegetation.OmsGeomorphonMaximaFinder.pRadius_DESC;
import static org.hortonmachine.lesto.modules.vegetation.OmsGeomorphonMaximaFinder.pRadius_UNIT;
import static org.hortonmachine.lesto.modules.vegetation.OmsGeomorphonMaximaFinder.pThreshold_DESC;
import static org.hortonmachine.lesto.modules.vegetation.OmsGeomorphonMaximaFinder.pThreshold_UNIT;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.lesto.modules.vegetation.OmsGeomorphonMaximaFinder;

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
import oms3.annotations.Unit;

@Description(OmsGeomorphonMaximaFinder.DESCRIPTION)
@Author(name = OmsGeomorphonMaximaFinder.AUTHORS, contact = OmsGeomorphonMaximaFinder.CONTACTS)
@Keywords(OmsGeomorphonMaximaFinder.KEYWORDS)
@Label(OmsGeomorphonMaximaFinder.LABEL)
@Name(OmsGeomorphonMaximaFinder.NAME)
@Status(OmsGeomorphonMaximaFinder.STATUS)
@License(OmsGeomorphonMaximaFinder.LICENSE)
public class GeomorphonMaximaFinder extends HMModel {
    @Description(inDTM_DESC)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inDTM;

    @Description(inDSM_DESC)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inDSM;

    @Description(pRadius_DESC)
    @Unit(pRadius_UNIT)
    @In
    public double pRadius;

    @Description(pThreshold_DESC)
    @Unit(pThreshold_UNIT)
    @In
    public double pThreshold = 1;

    @Description(pElevDiff_DESC)
    @Unit(pElevDiffThres_UNIT)
    @In
    public double pElevDiffThres = 1;

    @Description(outMaxima_DESC)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outMaxima;

    @Execute
    public void process() throws Exception {
        OmsGeomorphonMaximaFinder gmf = new OmsGeomorphonMaximaFinder();
        gmf.inDSM = getRaster(inDSM);
        gmf.inDTM = getRaster(inDTM);
        gmf.pRadius = pRadius;
        gmf.pThreshold = pThreshold;
        gmf.pElevDiffThres = pElevDiffThres;
        gmf.pm = pm;
        gmf.process();
        dumpVector(gmf.outMaxima, outMaxima);
    }

}
