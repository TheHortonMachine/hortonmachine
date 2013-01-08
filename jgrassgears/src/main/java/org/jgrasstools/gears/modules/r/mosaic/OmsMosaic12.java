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
package org.jgrasstools.gears.modules.r.mosaic;

import static org.jgrasstools.gears.libs.modules.Variables.BICUBIC;
import static org.jgrasstools.gears.libs.modules.Variables.BILINEAR;
import static org.jgrasstools.gears.libs.modules.Variables.NEAREST_NEIGHTBOUR;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.io.rasterwriter.OmsRasterWriter;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;

@Description("Module for patching max 12 rasters.")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("OmsMosaic, Raster")
@Label(JGTConstants.RASTERPROCESSING)
@Name("mosaic12")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class OmsMosaic12 extends JGTModel {

    @Description("The Map N.1 to be patched")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inMap1;

    @Description("The Map N.2 to be patched")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inMap2;

    @Description("The optional Map N.3 to be patched")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inMap3;

    @Description("The optional Map N.4 to be patched")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inMap4;

    @Description("The optional Map N.5 to be patched")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inMap5;

    @Description("The optional Map N.6 to be patched")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inMap6;

    @Description("The optional Map N.7 to be patched")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inMap7;

    @Description("The optional Map N.8 to be patched")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inMap8;

    @Description("The optional Map N.9 to be patched")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inMap9;

    @Description("The optional Map N.10 to be patched")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inMap10;

    @Description("The optional Map N.11 to be patched")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inMap11;

    @Description("The optional Map N.12 to be patched")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inMap12;

    @Description("The interpolation type to use")
    @UI("combo:" + NEAREST_NEIGHTBOUR + "," + BILINEAR + "," + BICUBIC)
    @In
    public String pInterpolation = NEAREST_NEIGHTBOUR;

    @Description("The patched map.")
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outMap = null;

    public GridCoverage2D outRaster;

    public boolean testmode = false;

    @Execute
    public void process() throws Exception {
        if (!testmode)
            checkNull(outMap);

        List<File> filesList = new ArrayList<File>();
        checkMap(filesList, inMap1);
        checkMap(filesList, inMap2);
        checkMap(filesList, inMap3);
        checkMap(filesList, inMap4);
        checkMap(filesList, inMap5);
        checkMap(filesList, inMap6);
        checkMap(filesList, inMap7);
        checkMap(filesList, inMap8);
        checkMap(filesList, inMap9);
        checkMap(filesList, inMap10);
        checkMap(filesList, inMap11);
        checkMap(filesList, inMap12);

        if (filesList.size() < 2) {
            throw new ModelsIllegalargumentException("The patching module needs at least two maps to be patched.", this);
        }

        OmsMosaic mosaic = new OmsMosaic();
        mosaic.inFiles = filesList;
        mosaic.pm = pm;
        mosaic.process();

        outRaster = mosaic.outRaster;
        if (!testmode)
            OmsRasterWriter.writeRaster(outMap, outRaster);
    }

    private void checkMap( List<File> filesList, String inMap ) {
        if (inMap != null) {
            File tmp = new File(inMap);
            if (tmp.exists()) {
                filesList.add(tmp);
            }
        }
    }

}
