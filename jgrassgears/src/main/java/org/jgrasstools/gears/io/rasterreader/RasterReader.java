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
package org.jgrasstools.gears.io.rasterreader;

import static org.jgrasstools.gears.libs.modules.JGTConstants.AIG;
import static org.jgrasstools.gears.libs.modules.JGTConstants.ESRIGRID;
import static org.jgrasstools.gears.libs.modules.JGTConstants.GEOTIF;
import static org.jgrasstools.gears.libs.modules.JGTConstants.GEOTIFF;
import static org.jgrasstools.gears.libs.modules.JGTConstants.GRASSRASTER;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
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
import org.geotools.coverageio.gdal.BaseGDALGridCoverage2DReader;
import org.geotools.coverageio.gdal.aig.AIGReader;
import org.geotools.factory.Hints;
import org.geotools.gce.arcgrid.ArcGridReader;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.grassraster.GrassCoverageReader;
import org.geotools.gce.grassraster.JGrassMapEnvironment;
import org.geotools.gce.grassraster.JGrassRegion;
import org.geotools.gce.grassraster.format.GrassCoverageFormat;
import org.geotools.gce.grassraster.format.GrassCoverageFormatFactory;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
@Description("Generic geotools coverage reader.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Coverage, Raster, Reading")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class RasterReader extends JGTModel {
    @Description("The coverage file path.")
    @In
    public String file = null;

    @Role(Role.PARAMETER)
    @Description("The file novalue.")
    @In
    public Double fileNovalue = null;

    @Role(Role.PARAMETER)
    @Description("The novalue wanted in the coverage.")
    @In
    public Double geodataNovalue = null;

    @Description("The optional requested boundary coordinates as array of [n, s, w, e].")
    @In
    public double[] pBounds = null;

    @Description("The optional requested resolution in x and y as [xres, yres].")
    @In
    public double[] pRes = null;

    @Description("The optional requested numer of rows and columns as [rows, cols].")
    @In
    public double[] pRowcol = null;

    @Role(Role.PARAMETER)
    @Description("The novalue wanted in the coverage.")
    @In
    public String pType = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The read output coverage map.")
    @Out
    public GridCoverage2D geodata = null;

    private GeneralParameterValue[] generalParameter = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(geodata == null, doReset)) {
            return;
        }
        if (pType == null) {
            // try to guess from the extension
            if (file.toLowerCase().endsWith(ESRIGRID)) {
                pType = ESRIGRID;
            } else if (file.toLowerCase().endsWith(AIG)) {
                pType = AIG;
            } else if (file.toLowerCase().endsWith(GEOTIFF) || file.toLowerCase().endsWith(GEOTIF)) {
                pType = GEOTIFF;
            } else if (isGrass(file)) {
                pType = GRASSRASTER;
            } else
                throw new ModelsIllegalargumentException(
                        "A coverage type to read has to be supplied.", this.getClass()
                                .getSimpleName());
        }

        if (pBounds != null) {
            if (pRes != null) {
                generalParameter = CoverageUtilities.createGridGeometryGeneralParameter(pRes[0],
                        pRes[1], pBounds[0], pBounds[1], pBounds[3], pBounds[2], null);
            } else if (pRowcol != null) {
                generalParameter = CoverageUtilities.createGridGeometryGeneralParameter(pRowcol[0],
                        pRowcol[1], pBounds[0], pBounds[1], pBounds[3], pBounds[2], null);
            } else {
                pm
                        .errorMessage("Reading the whole file. Bounds without resolution or row/cols suuplied.");
            }
        }

        File mapFile = new File(file);
        if (pType.equals(ESRIGRID)) {
            readArcGrid(mapFile);
        } else if (pType.equals(GEOTIFF)) {
            readGeotiff(mapFile);
        } else if (pType.equals(AIG) || pType.endsWith("w001001x.adf")) {
            readAig(mapFile);
        } else if (pType.equals(GRASSRASTER)) {
            readGrass(mapFile);
        } else {
            throw new ModelsIllegalargumentException("Data type not supported: " + pType, this
                    .getClass().getSimpleName());
        }

    }

    private boolean isGrass( String path ) {
        File file = new File(path);
        File cellFolderFile = file.getParentFile();
        File mapsetFile = cellFolderFile.getParentFile();
        File windFile = new File(mapsetFile, "WIND");

        return cellFolderFile.getName().toLowerCase().equals("cell") && windFile.exists();
    }

    private void readGrass( File mapFile ) throws Exception {
        JGrassMapEnvironment mapEnvironment = new JGrassMapEnvironment(new File(file));
        CoordinateReferenceSystem crs = mapEnvironment.getCoordinateReferenceSystem();
        JGrassRegion jGrassRegion = mapEnvironment.getActiveRegion();

        if (generalParameter == null) {
            generalParameter = CoverageUtilities.createGridGeometryGeneralParameter(jGrassRegion
                    .getCols(), jGrassRegion.getRows(), jGrassRegion.getWest(), jGrassRegion
                    .getEast(), jGrassRegion.getSouth(), jGrassRegion.getNorth(), crs);
        }

        GrassCoverageFormat format = new GrassCoverageFormatFactory().createFormat();
        GrassCoverageReader reader = format.getReader(mapEnvironment.getCELL());
        geodata = (GridCoverage2D) reader.read(generalParameter);
        checkNovalues();
    }

    private void readAig( File mapFile ) throws IllegalArgumentException, IOException {
        final ImageLayout l = new ImageLayout();
        l.setTileGridXOffset(0).setTileGridYOffset(0).setTileHeight(512).setTileWidth(512);

        Hints hints = new Hints();
        hints.add(new RenderingHints(JAI.KEY_IMAGE_LAYOUT, l));

        final URL url = mapFile.toURI().toURL();
        final Object source = url;
        final BaseGDALGridCoverage2DReader reader = new AIGReader(source, hints);
        GridCoverage2D coverage = (GridCoverage2D) reader.read(generalParameter);
        geodata = coverage.view(ViewType.GEOPHYSICS);
        checkNovalues();
    }

    private void readGeotiff( File mapFile ) throws IOException {
        GeoTiffReader geoTiffReader = new GeoTiffReader(mapFile);
        GridCoverage2D coverage = geoTiffReader.read(generalParameter);
        geodata = coverage.view(ViewType.GEOPHYSICS);
        checkNovalues();
    }

    private void readArcGrid( File mapFile ) throws IllegalArgumentException, IOException {
        ArcGridReader arcGridReader = new ArcGridReader(mapFile);
        GridCoverage2D coverage = arcGridReader.read(generalParameter);
        geodata = coverage.view(ViewType.GEOPHYSICS);
        checkNovalues();
    }

    private void checkNovalues() {
        // TODO make this nice, this can't be the way
        if (fileNovalue == null || geodataNovalue == null) {
            return;
        }
        if (isNovalue(fileNovalue) && isNovalue(geodataNovalue)) {
            return;
        }
        if (fileNovalue != geodataNovalue) {
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

    /**
     * Utility method to quickly read a grid in default mode.
     * 
     * @param path the path to the file.
     * @return the read coverage.
     * @throws Exception
     */
    public static GridCoverage2D readCoverage( String path ) throws Exception {
        RasterReader reader = new RasterReader();
        reader.file = path;
        reader.process();
        GridCoverage2D geodata = reader.geodata;
        return geodata;
    }

}
