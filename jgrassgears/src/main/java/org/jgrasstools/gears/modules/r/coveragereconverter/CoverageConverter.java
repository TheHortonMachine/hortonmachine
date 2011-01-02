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
package org.jgrasstools.gears.modules.r.coveragereconverter;

import static org.jgrasstools.gears.libs.modules.JGTConstants.ESRIGRID;
import static org.jgrasstools.gears.libs.modules.JGTConstants.*;
import oms3.annotations.Author;
import oms3.annotations.Category;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.io.arcgrid.ArcgridCoverageWriter;
import org.jgrasstools.gears.io.grass.JGrassCoverageWriter;
import org.jgrasstools.gears.io.rasterreader.RasterReader;
import org.jgrasstools.gears.io.tiff.GeoTiffCoverageWriter;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;

@SuppressWarnings("nls")
@Description("Coverage converter.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Coverage, Raster, Convert")
@Category(JGTConstants.RASTERPROCESSING)
@Status(Status.CERTIFIED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class CoverageConverter extends JGTModel {
    @Description("The coverage file path.")
    @In
    public String inputFile = null;

    @Description("The output type (ex. asc, tiff, adf).")
    @In
    public String pType = "asc";

    @Description("The output coverage file path.")
    @Out
    public String outputFile = null;

    @Execute
    public void process() throws Exception {

        // read
        RasterReader reader = new RasterReader();
        reader.file = inputFile;
        reader.process();
        GridCoverage2D coverage = reader.geodata;

        // write
        if (pType.equals(ESRIGRID)) {
            ArcgridCoverageWriter.writeArcgrid(outputFile, coverage);
        } else if (pType.equals(GEOTIFF)) {
            GeoTiffCoverageWriter.writeGeotiff(outputFile, coverage);
        } else if (pType.equals(GRASSRASTER)) {
            JGrassCoverageWriter.writeGrassRaster(outputFile, coverage);
        } else {
            throw new ModelsIllegalargumentException("Output data type not supported: " + pType,
                    this);
        }

    }

}
