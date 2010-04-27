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

import static org.jgrasstools.gears.libs.modules.JGTConstants.AIG;
import static org.jgrasstools.gears.libs.modules.JGTConstants.ESRIGRID;
import static org.jgrasstools.gears.libs.modules.JGTConstants.GEOTIFF;

import java.awt.RenderingHints;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.ViewType;
import org.geotools.coverageio.gdal.BaseGDALGridCoverage2DReader;
import org.geotools.coverageio.gdal.aig.AIGReader;
import org.geotools.factory.Hints;
import org.geotools.gce.arcgrid.ArcGridReader;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.jgrasstools.gears.io.arcgrid.ArcgridCoverageWriter;
import org.jgrasstools.gears.io.tiff.GeoTiffCoverageWriter;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTModel;

@SuppressWarnings("nls")
@Description("Coverage converter.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Coverage, Raster, Convert")
@Status(Status.DRAFT)
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

        File mapFile = new File(inputFile);
        GridCoverage2D coverage = null;

        // read
        if (inputFile.toLowerCase().endsWith(ESRIGRID)) {
            coverage = readArcGrid(mapFile, false);
        } else if (inputFile.toLowerCase().endsWith(GEOTIFF)) {
            coverage = readGeotiff(mapFile, false);
        } else if (inputFile.toLowerCase().endsWith(AIG)) {
            coverage = readAig(mapFile, false);
        } else {
            throw new ModelsIllegalargumentException("Data type not supported.", this.getClass()
                    .getSimpleName());
        }

        // write
        if (pType.equals(ESRIGRID)) {
            ArcgridCoverageWriter.writeArcgrid(outputFile, coverage);
        } else if (pType.equals(GEOTIFF)) {
            GeoTiffCoverageWriter.writeGeotiff(outputFile, coverage);
        } else {
            throw new ModelsIllegalargumentException("Output data type not supported: " + pType,
                    this.getClass().getSimpleName());
        }

    }

    private GridCoverage2D readAig( File mapFile, boolean addToList )
            throws IllegalArgumentException, IOException {
        final ImageLayout l = new ImageLayout();
        l.setTileGridXOffset(0).setTileGridYOffset(0).setTileHeight(512).setTileWidth(512);

        Hints hints = new Hints();
        hints.add(new RenderingHints(JAI.KEY_IMAGE_LAYOUT, l));

        final URL url = mapFile.toURI().toURL();
        final Object source = url;
        final BaseGDALGridCoverage2DReader reader = new AIGReader(source, hints);
        return (GridCoverage2D) reader.read(null);
    }

    private GridCoverage2D readGeotiff( File mapFile, boolean addToList ) throws IOException {
        GeoTiffReader geoTiffReader = new GeoTiffReader(mapFile);
        GridCoverage2D coverage = geoTiffReader.read(null);
        return coverage.view(ViewType.GEOPHYSICS);
    }

    private GridCoverage2D readArcGrid( File mapFile, boolean addToList )
            throws IllegalArgumentException, IOException {
        ArcGridReader arcGridReader = new ArcGridReader(mapFile);
        GridCoverage2D coverage = arcGridReader.read(null);
        return coverage.view(ViewType.GEOPHYSICS);
    }

}
