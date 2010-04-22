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
package org.jgrasstools.gears.io.arcgrid;

import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.HashMap;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Role;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.ViewType;
import org.geotools.gce.arcgrid.ArcGridReader;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

@Description("Utility class for reading arcgrids to geotools coverages.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Arcgrid, Coverage, Raster, Reading")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class ArcgridCoverageReader extends JGTModel {
    @Description("The arcgrid file path.")
    @In
    public String file = null;

    @Role(Role.PARAMETER)
    @Description("The file novalue.")
    @In
    public double fileNovalue = -9999.0;

    @Role(Role.PARAMETER)
    @Description("The novalue wanted in the coverage.")
    @In
    public double geodataNovalue = doubleNovalue;

    @Description("The read output coverage map.")
    @Out
    public GridCoverage2D geodata = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(geodata == null, doReset)) {
            return;
        }

        ArcGridReader arcGridReader = new ArcGridReader(new File(file));
        geodata = arcGridReader.read(null);
        geodata = geodata.view(ViewType.GEOPHYSICS);

        if (isNovalue(fileNovalue) && isNovalue(geodataNovalue)) {
            return;
        }
        if (fileNovalue != geodataNovalue) {
            // need to adapt it, for now do it dirty
            HashMap<String, Double> params = CoverageUtilities
                    .getRegionParamsFromGridCoverage(geodata);
            int height = params.get(CoverageUtilities.ROWS).intValue();
            int width = params.get(CoverageUtilities.COLS).intValue();
            WritableRaster tmpWR = CoverageUtilities.createDoubleWritableRaster(width, height,
                    null, null, null);
            WritableRandomIter tmpIter = RandomIterFactory.createWritable(tmpWR, null);
            RenderedImage readRI = geodata.getRenderedImage();
            RandomIter readIter = RandomIterFactory.create(readRI, null);
            for( int r = 0; r < height; r++ ) {
                for( int c = 0; c < width; c++ ) {
                    double value = readIter.getSampleDouble(c, r, 0);

                    if (isNovalue(value) || value == fileNovalue) {
                        tmpIter.setSample(c, r, 0, geodataNovalue);
                    } else {
                        tmpIter.setSample(c, r, 0, value);
                    }
                }
            }
            geodata = CoverageUtilities.buildCoverage("newcoverage", tmpWR, params, geodata
                    .getCoordinateReferenceSystem());
        }

    }

}
