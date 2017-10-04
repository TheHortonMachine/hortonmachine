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

import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW09_NetworBufferMergerHolesRemover.inInundationArea_DESCR;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW09_NetworBufferMergerHolesRemover.outInundationArea_DESCR;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.OmsLW09_NetworBufferMergerHolesRemover;

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

@Description(OmsLW09_NetworBufferMergerHolesRemover.DESCRIPTION)
@Author(name = OmsLW09_NetworBufferMergerHolesRemover.AUTHORS, contact = OmsLW09_NetworBufferMergerHolesRemover.CONTACTS)
@Keywords(OmsLW09_NetworBufferMergerHolesRemover.KEYWORDS)
@Label(OmsLW09_NetworBufferMergerHolesRemover.LABEL)
@Name("_" + OmsLW09_NetworBufferMergerHolesRemover.NAME)
@Status(OmsLW09_NetworBufferMergerHolesRemover.STATUS)
@License(OmsLW09_NetworBufferMergerHolesRemover.LICENSE)
public class LW09_NetworBufferMergerHolesRemover extends HMModel {

    @Description(inInundationArea_DESCR)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inInundationArea = null;

    @Description(outInundationArea_DESCR)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outInundationArea = null;

    @Execute
    public void process() throws Exception {
        OmsLW09_NetworBufferMergerHolesRemover bufferMergerHolesRemover = new OmsLW09_NetworBufferMergerHolesRemover();
        bufferMergerHolesRemover.inInundationArea = getVector(inInundationArea);
        bufferMergerHolesRemover.pm = pm;
        bufferMergerHolesRemover.process();
        dumpVector(bufferMergerHolesRemover.outInundationArea, outInundationArea);
    }

}
