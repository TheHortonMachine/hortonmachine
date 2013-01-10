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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERCONVERTER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERCONVERTER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERCONVERTER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERCONVERTER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERCONVERTER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERCONVERTER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERCONVERTER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERCONVERTER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERCONVERTER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERCONVERTER_inRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERCONVERTER_outRaster_DESCRIPTION;
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

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.JGTModel;

@Description(OMSRASTERCONVERTER_DESCRIPTION)
@Documentation(OMSRASTERCONVERTER_DOCUMENTATION)
@Author(name = OMSRASTERCONVERTER_AUTHORNAMES, contact = OMSRASTERCONVERTER_AUTHORCONTACTS)
@Keywords(OMSRASTERCONVERTER_KEYWORDS)
@Label(OMSRASTERCONVERTER_LABEL)
@Name("_" + OMSRASTERCONVERTER_NAME)
@Status(OMSRASTERCONVERTER_STATUS)
@License(OMSRASTERCONVERTER_LICENSE)
public class OmsRasterConverter extends JGTModel {

    @Description(OMSRASTERCONVERTER_inRaster_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSRASTERCONVERTER_outRaster_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster;

    @Execute
    public void process() throws Exception {
        checkNull(inRaster);
        outRaster = inRaster;
    }

}
