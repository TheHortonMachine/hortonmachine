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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESRASTERIZER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESRASTERIZER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESRASTERIZER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESRASTERIZER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESRASTERIZER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESRASTERIZER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESRASTERIZER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESRASTERIZER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESRASTERIZER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESRASTERIZER_fCat_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESRASTERIZER_inVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESRASTERIZER_outRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESRASTERIZER_pCat_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESRASTERIZER_pCols_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESRASTERIZER_pEast_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESRASTERIZER_pNorth_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESRASTERIZER_pRows_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESRASTERIZER_pSouth_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESRASTERIZER_pWest_DESCRIPTION;
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

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.modules.r.linesrasterizer.OmsLinesRasterizer;

@Description(OMSLINESRASTERIZER_DESCRIPTION)
@Documentation(OMSLINESRASTERIZER_DOCUMENTATION)
@Author(name = OMSLINESRASTERIZER_AUTHORNAMES, contact = OMSLINESRASTERIZER_AUTHORCONTACTS)
@Keywords(OMSLINESRASTERIZER_KEYWORDS)
@Label(OMSLINESRASTERIZER_LABEL)
@Name("_" + OMSLINESRASTERIZER_NAME)
@Status(OMSLINESRASTERIZER_STATUS)
@License(OMSLINESRASTERIZER_LICENSE)
public class LinesRasterizer extends JGTModel {

    @Description(OMSLINESRASTERIZER_inVector_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inVector = null;

    @Description(OMSLINESRASTERIZER_fCat_DESCRIPTION)
    @In
    public String fCat;

    @Description(OMSLINESRASTERIZER_pCat_DESCRIPTION)
    @In
    public double pCat = 1.0;

    @Description(OMSLINESRASTERIZER_pNorth_DESCRIPTION)
    @UI(JGTConstants.PROCESS_NORTH_UI_HINT)
    @In
    public Double pNorth = null;

    @Description(OMSLINESRASTERIZER_pSouth_DESCRIPTION)
    @UI(JGTConstants.PROCESS_SOUTH_UI_HINT)
    @In
    public Double pSouth = null;

    @Description(OMSLINESRASTERIZER_pWest_DESCRIPTION)
    @UI(JGTConstants.PROCESS_WEST_UI_HINT)
    @In
    public Double pWest = null;

    @Description(OMSLINESRASTERIZER_pEast_DESCRIPTION)
    @UI(JGTConstants.PROCESS_EAST_UI_HINT)
    @In
    public Double pEast = null;

    @Description(OMSLINESRASTERIZER_pRows_DESCRIPTION)
    @UI(JGTConstants.PROCESS_ROWS_UI_HINT)
    @In
    public Integer pRows = null;

    @Description(OMSLINESRASTERIZER_pCols_DESCRIPTION)
    @UI(JGTConstants.PROCESS_COLS_UI_HINT)
    @In
    public Integer pCols = null;

    @Description(OMSLINESRASTERIZER_outRaster_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster;

    @Execute
    public void process() throws Exception {
        OmsLinesRasterizer linesrasterizer = new OmsLinesRasterizer();
        linesrasterizer.inVector = getVector(inVector);
        linesrasterizer.fCat = fCat;
        linesrasterizer.pCat = pCat;
        linesrasterizer.pNorth = pNorth;
        linesrasterizer.pSouth = pSouth;
        linesrasterizer.pWest = pWest;
        linesrasterizer.pEast = pEast;
        linesrasterizer.pRows = pRows;
        linesrasterizer.pCols = pCols;
        linesrasterizer.pm = pm;
        linesrasterizer.doProcess = doProcess;
        linesrasterizer.doReset = doReset;
        linesrasterizer.process();
        dumpRaster(linesrasterizer.outRaster, outRaster);
    }
}
