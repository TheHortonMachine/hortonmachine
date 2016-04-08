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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSMAPCALC_OUT_RASTER_DESCRIPTION;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Name;
import oms3.annotations.UI;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.modules.r.mapcalc.OmsMapcalc;

@Name("mapcalc")
public class Mapcalc extends OmsMapcalc {

    @Description("Raster map to process.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inRaster1;

    @Description("Optional raster map to process.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inRaster2;

    @Description("Optional raster map to process.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inRaster3;

    @Description("Optional raster map to process.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inRaster4;

    @Description("Optional raster map to process.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inRaster5;

    @Description(OMSMAPCALC_OUT_RASTER_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster = null;

    @SuppressWarnings("nls")
    @Execute
    public void process() throws Exception {
        OmsMapcalc mapcalc = new OmsMapcalc();

        List<GridCoverage2D> coverages = new ArrayList<GridCoverage2D>();
        addRaster(coverages, inRaster1);
        addRaster(coverages, inRaster2);
        addRaster(coverages, inRaster3);
        addRaster(coverages, inRaster4);
        addRaster(coverages, inRaster5);

        mapcalc.inRasters = coverages;
        mapcalc.pFunction = pFunction;
        mapcalc.pm = pm;
        mapcalc.process();
        dumpRaster(mapcalc.outRaster, outRaster);
    }

    private void addRaster( List<GridCoverage2D> coverages, String raster ) throws Exception {
        if (raster != null && new File(raster).exists()) {
            GridCoverage2D coverage = getRaster(raster);
            coverages.add(coverage);
        }
    }

}
