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
package org.jgrasstools.modules;

import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_doBbox_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_doHeader_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_doInfo_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_inFile_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_inPolygons_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_outFile_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_pClasses_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_pEast_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_pImpulses_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_pIntensityrange_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_pNorth_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_pSouth_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_pWest_DESCRIPTION;
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

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.modules.v.vectorconverter.OmsLasConverter;

@Description(OMSLASCONVERTER_DESCRIPTION)
@Author(name = OMSLASCONVERTER_AUTHORNAMES, contact = OMSLASCONVERTER_AUTHORCONTACTS)
@Keywords(OMSLASCONVERTER_KEYWORDS)
@Label(OMSLASCONVERTER_LABEL)
@Name(OMSLASCONVERTER_NAME)
@Status(OMSLASCONVERTER_STATUS)
@License(OMSLASCONVERTER_LICENSE)
public class LasConverter extends JGTModel {

    @Description(OMSLASCONVERTER_inFile_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inFile;

    @Description(OMSLASCONVERTER_inPolygons_DESCRIPTION)
    @In
    public String inPolygons;

    @Description(OMSLASCONVERTER_pIntensityrange_DESCRIPTION)
    @In
    public String pIntensityrange;

    @Description(OMSLASCONVERTER_pImpulses_DESCRIPTION)
    @In
    public String pImpulses;

    @Description(OMSLASCONVERTER_pClasses_DESCRIPTION)
    @In
    public String pClasses;

    // @Description(OMSLASCONVERTER_pIndexrange_DESCRIPTION)
    // @In
    // public String pIndexrange;

    @Description(OMSLASCONVERTER_pNorth_DESCRIPTION)
    @UI(JGTConstants.PROCESS_NORTH_UI_HINT)
    @In
    public Double pNorth = null;

    @Description(OMSLASCONVERTER_pSouth_DESCRIPTION)
    @UI(JGTConstants.PROCESS_SOUTH_UI_HINT)
    @In
    public Double pSouth = null;

    @Description(OMSLASCONVERTER_pWest_DESCRIPTION)
    @UI(JGTConstants.PROCESS_WEST_UI_HINT)
    @In
    public Double pWest = null;

    @Description(OMSLASCONVERTER_pEast_DESCRIPTION)
    @UI(JGTConstants.PROCESS_EAST_UI_HINT)
    @In
    public Double pEast = null;

    @Description(OMSLASCONVERTER_doHeader_DESCRIPTION)
    @In
    public boolean doHeader = false;

    @Description(OMSLASCONVERTER_doInfo_DESCRIPTION)
    @In
    public boolean doInfo = false;

    @Description(OMSLASCONVERTER_doBbox_DESCRIPTION)
    @In
    public boolean doBbox = false;

    @Description(OMSLASCONVERTER_outFile_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outFile;

    private OmsLasConverter lasconverter;

    @Execute
    public void process() throws Exception {
        lasconverter = new OmsLasConverter();
        lasconverter.inFile = inFile;
        lasconverter.inPolygons = getVector(inPolygons);
        lasconverter.pIntensityrange = pIntensityrange;
        lasconverter.pImpulses = pImpulses;
        lasconverter.pClasses = pClasses;
        // lasconverter.pIndexrange = pIndexrange;
        lasconverter.pNorth = pNorth;
        lasconverter.pSouth = pSouth;
        lasconverter.pWest = pWest;
        lasconverter.pEast = pEast;
        lasconverter.doHeader = doHeader;
        lasconverter.doInfo = doInfo;
        lasconverter.doBbox = doBbox;
        lasconverter.outFile = outFile;
        lasconverter.pm = pm;
        lasconverter.process();
    }

}
