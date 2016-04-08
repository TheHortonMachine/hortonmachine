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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSCONTOUREXTRACTOR_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCONTOUREXTRACTOR_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCONTOUREXTRACTOR_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCONTOUREXTRACTOR_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCONTOUREXTRACTOR_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCONTOUREXTRACTOR_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCONTOUREXTRACTOR_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCONTOUREXTRACTOR_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCONTOUREXTRACTOR_IN_COVERAGE_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCONTOUREXTRACTOR_OUT_GEODATA_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCONTOUREXTRACTOR_P_INTERVAL_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCONTOUREXTRACTOR_P_MAX_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCONTOUREXTRACTOR_P_MIN_DESCRIPTION;
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
import org.jgrasstools.gears.modules.v.contoursextractor.OmsContourExtractor;

@Description(OMSCONTOUREXTRACTOR_DESCRIPTION)
@Author(name = OMSCONTOUREXTRACTOR_AUTHORNAMES, contact = OMSCONTOUREXTRACTOR_AUTHORCONTACTS)
@Keywords(OMSCONTOUREXTRACTOR_KEYWORDS)
@Label(OMSCONTOUREXTRACTOR_LABEL)
@Name("_" + OMSCONTOUREXTRACTOR_NAME)
@Status(OMSCONTOUREXTRACTOR_STATUS)
@License(OMSCONTOUREXTRACTOR_LICENSE)
public class ContourExtractor extends JGTModel {

    @Description(OMSCONTOUREXTRACTOR_IN_COVERAGE_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inCoverage;

    @Description(OMSCONTOUREXTRACTOR_P_MIN_DESCRIPTION)
    @In
    public Double pMin;

    @Description(OMSCONTOUREXTRACTOR_P_MAX_DESCRIPTION)
    @In
    public Double pMax;

    @Description(OMSCONTOUREXTRACTOR_P_INTERVAL_DESCRIPTION)
    @In
    public Double pInterval;

    @Description(OMSCONTOUREXTRACTOR_OUT_GEODATA_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outGeodata = null;

    @Execute
    public void process() throws Exception {
        OmsContourExtractor ex = new OmsContourExtractor();
        ex.pm = pm;
        ex.inCoverage = getRaster(inCoverage);
        ex.pMin = pMin;
        ex.pMax = pMax;
        ex.pInterval = pInterval;
        ex.process();
        dumpVector(ex.outGeodata, outGeodata);
    }
}
