/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package eu.hydrologis.jgrass.jgrassgears.modules.r.coveragereprojector;

import javax.media.jai.Interpolation;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.hydrologis.jgrass.jgrassgears.libs.modules.HMModel;

@Description("Module for raster reprojection")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Crs, Reprojection, Raster")
@Status(Status.TESTED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class CoverageReprojector extends HMModel {

    @Description("The coverage that has to be reprojected.")
    @In
    public GridCoverage2D inMap;

    @Description("The code defining the target coordinate reference system, composed by authority and code number (ex. EPSG:4328).")
    @In
    public String pCode;

    @Description("The interpolation type to use: nearest neightbour (0), bilinear (1), bicubic (2)")
    @In
    public int pInterpolation = 0;

    @Description("The resulting map.")
    @Out
    public GridCoverage2D outMap = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outMap == null, doReset)) {
            return;
        }

        CoordinateReferenceSystem targetCrs = CRS.decode(pCode);

        Interpolation interpolationType = null;
        if (pInterpolation == 1) {
            interpolationType = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
        } else if (pInterpolation == 2) {
            interpolationType = Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
        } else if (pInterpolation == 3) {
            interpolationType = Interpolation.getInstance(Interpolation.INTERP_BICUBIC_2);
        }else{
            // default to nearest neighbour
            interpolationType = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
        }

        outMap = (GridCoverage2D) Operations.DEFAULT.resample(inMap, targetCrs, null,
                interpolationType);

    }

}


