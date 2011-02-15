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
package org.jgrasstools.gears.modules.r.transformer;

import javax.media.jai.Interpolation;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.JGTProcessingRegion;
import org.opengis.coverage.processing.Operation;
import org.opengis.parameter.ParameterValueGroup;

@Description("A simple middleman module  to do coverage conversion.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Coverage, Raster, Convert")
@Label(JGTConstants.RASTERPROCESSING)
@Status(Status.CERTIFIED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class CoverageResolutionResampler extends JGTModel {
    @Description("The input coverage.")
    @In
    public GridCoverage2D inGeodata;

    @Description("The interpolation type to use: nearest neightbour (0), bilinear (1), bicubic (2)")
    @In
    public int pInterpolation = 0;

    @Description("The new resolution in X")
    @In
    public Double pXres;

    @Description("The new resolution in Y (if null taken same as pXres)")
    @In
    public Double pYres;

    @Description("The output coverage.")
    @Out
    public GridCoverage2D outGeodata;

    @SuppressWarnings("nls")
    @Execute
    public void process() throws Exception {
        checkNull(inGeodata, pXres);

        if (pYres == null) {
            pYres = pXres;
        }
        JGTProcessingRegion region = new JGTProcessingRegion(inGeodata);
        region.setWEResolution(pXres);
        region.setNSResolution(pYres);
        GridGeometry2D newGridGeometry = region.getGridGeometry(inGeodata.getCoordinateReferenceSystem());

        CoverageProcessor processor = CoverageProcessor.getInstance();
        Operation resampleOp = processor.getOperation("Resample"); //$NON-NLS-1$

        ParameterValueGroup param = resampleOp.getParameters();
        param.parameter("Source").setValue(inGeodata);
        param.parameter("GridGeometry").setValue(newGridGeometry);
        String interpolationType = "NearestNeighbor";
        switch( pInterpolation ) {
        case Interpolation.INTERP_BILINEAR:
            interpolationType = "bilinear";
            break;
        case Interpolation.INTERP_BICUBIC:
            interpolationType = "bicubic";
            break;
        default:
            break;
        }
        param.parameter("InterpolationType").setValue(interpolationType);

        outGeodata = (GridCoverage2D) processor.doOperation(param);
    }

}
