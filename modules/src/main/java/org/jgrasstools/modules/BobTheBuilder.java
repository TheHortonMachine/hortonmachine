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

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.modules.r.bobthebuilder.OmsBobTheBuilder;

@Description("Builds rasterized artifacts on a raster.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Build, Raster")
@Name("_bobbuilder")
@Label(JGTConstants.RASTERPROCESSING)
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class BobTheBuilder extends JGTModel {

    @Description("The input raster.")
    @In
    public String inRaster = null;

    @Description("The vector map containing the polygonal area to modify.")
    @In
    public String inArea = null;

    @Description("The vector map containing the points that provide the new elevations.")
    @In
    public String inElevations = null;

    @Description("The maximum radius to use for interpolation.")
    @In
    public double pMaxbuffer = -1;

    @Description("The field of the elevations map that contain the elevation of the point.")
    @In
    public String fElevation = null;

    @Description("Switch that defines if the module should erode in places the actual raster is higher (default is false).")
    @In
    public boolean doErode = false;

    @Description("Switch that defines if the module should use only points contained in the polygon for the interpolation (default is false. i.e. use all).")
    @In
    public boolean doUseOnlyInternal = false;

    @Description("Switch that defines if the module should add the border of the polygon as elevation point to aid connection between new and old (default is false).")
    @In
    public boolean doPolygonborder = false;

    @Description("The modified raster map.")
    @In
    public String outRaster = null;

    @Execute
    public void process() throws Exception {
        checkNull(inRaster, inArea, inElevations, fElevation);

        OmsBobTheBuilder bob = new OmsBobTheBuilder();
        bob.pm = pm;
        bob.inRaster = getRaster(inRaster);
        bob.inArea = getVector(inArea);
        bob.inElevations = getVector(inElevations);
        bob.pMaxbuffer = pMaxbuffer;
        bob.fElevation = fElevation;
        bob.doErode = doErode;
        bob.doUseOnlyInternal = doUseOnlyInternal;
        bob.doPolygonborder = doPolygonborder;
        bob.process();

        dumpRaster(bob.outRaster, outRaster);
    }
}
