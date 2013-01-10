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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSCARVER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCARVER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCARVER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCARVER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCARVER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCARVER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCARVER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCARVER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCARVER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCARVER_fDepthLines_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCARVER_fDepthPolygons_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCARVER_inCarverLines_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCARVER_inCarverPolygons_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCARVER_inRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCARVER_outRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCARVER_pDepthLines_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSCARVER_pDepthPolygons_DESCRIPTION;
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

import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.modules.r.carver.OmsCarver;

@Description(OMSCARVER_DESCRIPTION)
@Documentation(OMSCARVER_DOCUMENTATION)
@Author(name = OMSCARVER_AUTHORNAMES, contact = OMSCARVER_AUTHORCONTACTS)
@Keywords(OMSCARVER_KEYWORDS)
@Label(OMSCARVER_LABEL)
@Name("_" + OMSCARVER_NAME)
@Status(OMSCARVER_STATUS)
@License(OMSCARVER_LICENSE)
public class Carver extends JGTModel {

    @Description(OMSCARVER_inRaster_DESCRIPTION)
    @In
    public String inRaster = null;

    @Description(OMSCARVER_inCarverPolygons_DESCRIPTION)
    @In
    public String inCarverPolygons = null;

    @Description(OMSCARVER_inCarverLines_DESCRIPTION)
    @In
    public String inCarverLines = null;

    @Description(OMSCARVER_fDepthLines_DESCRIPTION)
    @In
    public String fDepthLines = null;

    @Description(OMSCARVER_pDepthLines_DESCRIPTION)
    @In
    public double pDepthLines = 6.0;

    @Description(OMSCARVER_fDepthPolygons_DESCRIPTION)
    @In
    public String fDepthPolygons = null;

    @Description(OMSCARVER_pDepthPolygons_DESCRIPTION)
    @In
    public double pDepthPolygons = 6.0;

    @Description(OMSCARVER_outRaster_DESCRIPTION)
    @Out
    public String outRaster = null;

    @Execute
    public void process() throws Exception {
        OmsCarver carver = new OmsCarver();
        carver.pm = pm;
        carver.inRaster = getRaster(inRaster);
        carver.inCarverPolygons = getVector(inCarverPolygons);
        carver.inCarverLines = getVector(inCarverLines);
        carver.fDepthLines = fDepthLines;
        carver.pDepthLines = pDepthLines;
        carver.fDepthPolygons = fDepthPolygons;
        carver.pDepthPolygons = pDepthPolygons;
        carver.process();
        dumpRaster(carver.outRaster, outRaster);
    }

}
