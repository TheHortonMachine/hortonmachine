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
package org.jgrasstools.gears.modules.utils.coveragelist;

import static org.jgrasstools.gears.i18n.GearsMessages.OMSCOVERAGELISTER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCOVERAGELISTER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCOVERAGELISTER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCOVERAGELISTER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCOVERAGELISTER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCOVERAGELISTER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCOVERAGELISTER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCOVERAGELISTER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCOVERAGELISTER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCOVERAGELISTER_fileNovalue_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCOVERAGELISTER_geodataNovalue_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCOVERAGELISTER_inFiles_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCOVERAGELISTER_outGC_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCOVERAGELISTER_pCols_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCOVERAGELISTER_pEast_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCOVERAGELISTER_pNorth_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCOVERAGELISTER_pRows_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCOVERAGELISTER_pSouth_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCOVERAGELISTER_pWest_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCOVERAGELISTER_pXres_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCOVERAGELISTER_pYres_DESCRIPTION;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;

import java.util.ArrayList;
import java.util.List;

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
import oms3.annotations.UI;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.io.rasterreader.OmsRasterReader;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;

@Description(OMSCOVERAGELISTER_DESCRIPTION)
@Documentation(OMSCOVERAGELISTER_DOCUMENTATION)
@Author(name = OMSCOVERAGELISTER_AUTHORNAMES, contact = OMSCOVERAGELISTER_AUTHORCONTACTS)
@Keywords(OMSCOVERAGELISTER_KEYWORDS)
@Label(OMSCOVERAGELISTER_LABEL)
@Name(OMSCOVERAGELISTER_NAME)
@Status(OMSCOVERAGELISTER_STATUS)
@License(OMSCOVERAGELISTER_LICENSE)
public class OmsCoverageLister extends JGTModel {

    @Description(OMSCOVERAGELISTER_inFiles_DESCRIPTION)
    @UI(JGTConstants.FILESPATHLIST_UI_HINT)
    @In
    public List<String> inFiles;

    @Description(OMSCOVERAGELISTER_fileNovalue_DESCRIPTION)
    @In
    public Double fileNovalue = -9999.0;

    @Description(OMSCOVERAGELISTER_geodataNovalue_DESCRIPTION)
    @In
    public Double geodataNovalue = doubleNovalue;

    @Description(OMSCOVERAGELISTER_pNorth_DESCRIPTION)
    @UI(JGTConstants.PROCESS_NORTH_UI_HINT)
    @In
    public Double pNorth = null;

    @Description(OMSCOVERAGELISTER_pSouth_DESCRIPTION)
    @UI(JGTConstants.PROCESS_SOUTH_UI_HINT)
    @In
    public Double pSouth = null;

    @Description(OMSCOVERAGELISTER_pWest_DESCRIPTION)
    @UI(JGTConstants.PROCESS_WEST_UI_HINT)
    @In
    public Double pWest = null;

    @Description(OMSCOVERAGELISTER_pEast_DESCRIPTION)
    @UI(JGTConstants.PROCESS_EAST_UI_HINT)
    @In
    public Double pEast = null;

    @Description(OMSCOVERAGELISTER_pXres_DESCRIPTION)
    @UI(JGTConstants.PROCESS_XRES_UI_HINT)
    @In
    public Double pXres = null;

    @Description(OMSCOVERAGELISTER_pYres_DESCRIPTION)
    @UI(JGTConstants.PROCESS_YRES_UI_HINT)
    @In
    public Double pYres = null;

    @Description(OMSCOVERAGELISTER_pRows_DESCRIPTION)
    @UI(JGTConstants.PROCESS_ROWS_UI_HINT)
    @In
    public Integer pRows = null;

    @Description(OMSCOVERAGELISTER_pCols_DESCRIPTION)
    @UI(JGTConstants.PROCESS_COLS_UI_HINT)
    @In
    public Integer pCols = null;

    @Description(OMSCOVERAGELISTER_outGC_DESCRIPTION)
    @Out
    public List<GridCoverage2D> outGC = null;

    @Execute
    public void process() throws Exception {

        outGC = new ArrayList<GridCoverage2D>();

        for( String file : inFiles ) {
            OmsRasterReader reader = new OmsRasterReader();
            reader.file = file;
            reader.fileNovalue = fileNovalue;
            reader.geodataNovalue = geodataNovalue;
            reader.pNorth = pNorth;
            reader.pSouth = pSouth;
            reader.pWest = pWest;
            reader.pEast = pEast;
            reader.pXres = pXres;
            reader.pYres = pYres;
            reader.pRows = pRows;
            reader.pCols = pCols;
            reader.process();

            outGC.add(reader.outRaster);
        }

    }

}
